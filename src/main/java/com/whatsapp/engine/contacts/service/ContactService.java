package com.whatsapp.engine.contacts.service;

import com.whatsapp.engine.auth.User;
import com.whatsapp.engine.common.exception.ApplicationException;
import com.whatsapp.engine.contacts.Contact;
import com.whatsapp.engine.contacts.dto.ContactRequest;
import com.whatsapp.engine.contacts.dto.ContactResponse;
import com.whatsapp.engine.contacts.dto.ContactUploadResponse;
import com.whatsapp.engine.contacts.repository.ContactRepository;
import com.whatsapp.engine.organization.Organization;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
public class ContactService {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^91[6-9]\\d{9}$");

    private static final String COUNTRY_CODE = "91";

    private final ContactRepository contactRepository;

    public ContactService(ContactRepository contactRepository) {
        this.contactRepository = contactRepository;
    }

    @Transactional
    public ContactResponse createContact(ContactRequest request, User user) {
        UUID organizationId = user.getOrganization().getId();
        String phoneNumber = normalizePhoneNumber(request.phoneNumber());

        if (contactRepository.existsByOrganizationIdAndPhoneNumber(organizationId, phoneNumber)) {
            throw new ApplicationException(HttpStatus.CONFLICT, "CONTACT_EXISTS", "Contact phone number already exists");
        }

        Contact contact = new Contact();
        contact.setOrganization(user.getOrganization());
        applyRequest(contact, request, phoneNumber);
        contact.setActive(true);

        return toResponse(contactRepository.save(contact));
    }

    @Transactional(readOnly = true)
    public Page<ContactResponse> getContacts(String search, Pageable pageable, User user) {
        UUID organizationId = user.getOrganization().getId();
        Page<Contact> contacts = StringUtils.hasText(search)
                ? contactRepository.searchByNameOrPhone(organizationId, search.trim(), pageable)
                : contactRepository.findByOrganizationId(organizationId, pageable);

        return contacts.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ContactResponse getContact(UUID id, User user) {
        return toResponse(findTenantContact(id, user.getOrganization().getId()));
    }

    @Transactional
    public ContactResponse updateContact(UUID id, ContactRequest request, User user) {
        UUID organizationId = user.getOrganization().getId();
        Contact contact = findTenantContact(id, organizationId);
        String phoneNumber = normalizePhoneNumber(request.phoneNumber());

        contactRepository.findByOrganizationIdAndPhoneNumber(organizationId, phoneNumber)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new ApplicationException(HttpStatus.CONFLICT, "CONTACT_EXISTS", "Contact phone number already exists");
                });

        applyRequest(contact, request, phoneNumber);
        return toResponse(contactRepository.save(contact));
    }

    @Transactional
    public void deleteContact(UUID id, User user) {
        Contact contact = findTenantContact(id, user.getOrganization().getId());
        contactRepository.delete(contact);
    }

    @Transactional
    public ContactUploadResponse uploadContacts(MultipartFile file, User user) {
        if (file == null || file.isEmpty()) {
            throw new ApplicationException(HttpStatus.BAD_REQUEST, "CSV_FILE_REQUIRED", "CSV file is required");
        }

        List<ContactUploadResponse.RowError> errors = new ArrayList<>();
        int totalRows = 0;
        int created = 0;
        int updated = 0;
        Set<String> seenPhoneNumbers = new HashSet<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (!StringUtils.hasText(headerLine)) {
                throw new ApplicationException(HttpStatus.BAD_REQUEST, "CSV_EMPTY", "CSV file is empty");
            }

            Map<String, Integer> headers = parseHeaders(headerLine);
            validateRequiredHeaders(headers);

            String line;
            int rowNumber = 1;
            while ((line = reader.readLine()) != null) {
                rowNumber++;
                if (!StringUtils.hasText(line)) {
                    continue;
                }

                totalRows++;
                try {
                    boolean createdRow = upsertContact(parseCsvLine(line), headers, user.getOrganization(), seenPhoneNumbers);
                    if (createdRow) {
                        created++;
                    } else {
                        updated++;
                    }
                } catch (RuntimeException exception) {
                    errors.add(new ContactUploadResponse.RowError(rowNumber, exception.getMessage()));
                }
            }
        } catch (IOException exception) {
            throw new ApplicationException(HttpStatus.BAD_REQUEST, "CSV_READ_FAILED", "Unable to read CSV file");
        }

        log.info(
                "Contacts CSV processed. organizationId={}, totalRows={}, created={}, updated={}, failed={}",
                user.getOrganization().getId(),
                totalRows,
                created,
                updated,
                errors.size()
        );
        return new ContactUploadResponse(totalRows, created, updated, errors.size(), errors);
    }

    private boolean upsertContact(
            List<String> row,
            Map<String, Integer> headers,
            Organization organization,
            Set<String> seenPhoneNumbers
    ) {
        String name = getValue(row, headers, "name");
        String phoneNumber = normalizePhoneNumber(getValue(row, headers, "phoneNumber"));
        String email = getOptionalValue(row, headers, "email");
        String notes = getOptionalValue(row, headers, "notes");
        log.info("Upserting contact for phone {}", phoneNumber);

        if (!StringUtils.hasText(name)) {
            throw new IllegalArgumentException("Name is required");
        }

        // Convert 10-digit Indian numbers to E.164 format without +
        if (phoneNumber.length() == 10) {
            phoneNumber = COUNTRY_CODE + phoneNumber; // COUNTRY_CODE = "91"
        }

        // Validate after normalization
        if (!PHONE_PATTERN.matcher(phoneNumber).matches()) {
            throw new IllegalArgumentException(
                    "Phone number must be in E.164 format without + (e.g. 919876543210)");
        }

        // Check duplicates within the uploaded CSV
        if (!seenPhoneNumbers.add(phoneNumber)) {
            throw new IllegalArgumentException("Duplicate phone number in CSV file");
        }

        Contact contact = contactRepository
                .findByOrganizationIdAndPhoneNumber(
                        organization.getId(),
                        phoneNumber)
                .orElseGet(Contact::new);

        boolean isNew = contact.getId() == null;

        contact.setOrganization(organization);
        contact.setName(name.trim());
        contact.setPhoneNumber(phoneNumber);
        contact.setEmail(
                StringUtils.hasText(email)
                        ? email.trim().toLowerCase(Locale.ROOT)
                        : null);
        contact.setNotes(
                StringUtils.hasText(notes)
                        ? notes.trim()
                        : null);
        contact.setActive(true);

        contactRepository.save(contact);

        return isNew;
    }


    private Contact findTenantContact(UUID id, UUID organizationId) {
        return contactRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ApplicationException(HttpStatus.NOT_FOUND, "CONTACT_NOT_FOUND", "Contact not found"));
    }

    private void applyRequest(Contact contact, ContactRequest request, String phoneNumber) {
        contact.setName(request.name().trim());
        contact.setPhoneNumber(phoneNumber);
        contact.setEmail(StringUtils.hasText(request.email()) ? request.email().trim().toLowerCase(Locale.ROOT) : null);
        contact.setNotes(StringUtils.hasText(request.notes()) ? request.notes().trim() : null);
    }

    private ContactResponse toResponse(Contact contact) {
        return new ContactResponse(
                contact.getId(),
                contact.getOrganization().getId(),
                contact.getName(),
                contact.getPhoneNumber(),
                contact.getEmail(),
                contact.getNotes(),
                contact.isActive(),
                contact.getCreatedAt(),
                contact.getUpdatedAt()
        );
    }

    private String normalizePhoneNumber(String phoneNumber) {
        return phoneNumber == null ? "" : phoneNumber.trim().replace("+", "");
    }

    private Map<String, Integer> parseHeaders(String headerLine) {
        List<String> headers = parseCsvLine(headerLine);
        Map<String, Integer> index = new HashMap<>();
        for (int i = 0; i < headers.size(); i++) {
            index.put(normalizeHeader(headers.get(i)), i);
        }
        return index;
    }

    private void validateRequiredHeaders(Map<String, Integer> headers) {
        if (!headers.containsKey("name") || !headers.containsKey("phoneNumber")) {
            throw new ApplicationException(HttpStatus.BAD_REQUEST, "CSV_HEADERS_INVALID", "CSV must include name and phoneNumber columns");
        }
    }

    private String getValue(List<String> row, Map<String, Integer> headers, String key) {
        Integer index = headers.get(key);
        if (index == null || index >= row.size()) {
            return "";
        }
        return row.get(index);
    }

    private String getOptionalValue(List<String> row, Map<String, Integer> headers, String key) {
        return headers.containsKey(key) ? getValue(row, headers, key) : null;
    }

    private String normalizeHeader(String header) {
        String normalized = header.trim().replace("_", "").replace("-", "").toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "phonenumber", "phone", "mobile", "mobilenumber" -> "phoneNumber";
            default -> normalized;
        };
    }

    private List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;

        for (int i = 0; i < line.length(); i++) {
            char character = line.charAt(i);
            if (character == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    quoted = !quoted;
                }
            } else if (character == ',' && !quoted) {
                values.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(character);
            }
        }

        values.add(current.toString().trim());
        return values;
    }
}
