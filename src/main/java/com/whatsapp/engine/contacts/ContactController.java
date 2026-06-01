package com.whatsapp.engine.contacts;

import com.whatsapp.engine.auth.User;
import com.whatsapp.engine.common.api.ApiResponse;
import com.whatsapp.engine.contacts.dto.ContactRequest;
import com.whatsapp.engine.contacts.dto.ContactResponse;
import com.whatsapp.engine.contacts.dto.ContactUploadResponse;
import com.whatsapp.engine.contacts.service.ContactService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/contacts")
@PreAuthorize("hasAnyRole('ORG_ADMIN', 'AGENT')")
public class ContactController {

    private final ContactService contactService;

    public ContactController(ContactService contactService) {
        this.contactService = contactService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ContactResponse>> createContact(
            @Valid @RequestBody ContactRequest request,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Contact created", contactService.createContact(request, user)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ContactResponse>>> getContacts(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(ApiResponse.success("Contacts fetched", contactService.getContacts(search, pageable, user)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ContactResponse>> getContact(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(ApiResponse.success("Contact fetched", contactService.getContact(id, user)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ContactResponse>> updateContact(
            @PathVariable UUID id,
            @Valid @RequestBody ContactRequest request,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(ApiResponse.success("Contact updated", contactService.updateContact(id, request, user)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteContact(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user
    ) {
        contactService.deleteContact(id, user);
        return ResponseEntity.ok(ApiResponse.success("Contact deleted", null));
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ContactUploadResponse>> uploadContacts(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(ApiResponse.success("Contacts uploaded", contactService.uploadContacts(file, user)));
    }
}
