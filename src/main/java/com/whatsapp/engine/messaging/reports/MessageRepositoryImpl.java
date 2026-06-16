package com.whatsapp.engine.messaging.reports;

import com.whatsapp.engine.messaging.Message;
import com.whatsapp.engine.messaging.MessageDirection;
import com.whatsapp.engine.messaging.reports.dto.MessageReportFilter;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class MessageRepositoryImpl implements MessageRepositoryCustom {

    private final EntityManager entityManager;

    @Override
    public SummaryMetrics getDashboardSummary(
            UUID organizationId,
            MessageReportFilter filter
    ) {

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Object[]> cq = cb.createQuery(Object[].class);

        Root<Message> root = cq.from(Message.class);

        List<Predicate> predicates = new ArrayList<>();

        predicates.add(
                cb.equal(
                        root.get("organization").get("id"),
                        organizationId
                )
        );

        predicates.add(
                cb.equal(
                        root.get("direction"),
                        MessageDirection.OUTBOUND
                )
        );

        if (filter.dateFrom() != null) {
            predicates.add(
                    cb.greaterThanOrEqualTo(
                            root.get("createdAt"),
                            filter.dateFrom()
                    )
            );
        }

        if (filter.dateTo() != null) {
            predicates.add(
                    cb.lessThanOrEqualTo(
                            root.get("createdAt"),
                            filter.dateTo()
                    )
            );
        }

        if (filter.templateName() != null) {
            predicates.add(
                    cb.equal(
                            root.get("templateName"),
                            filter.templateName()
                    )
            );
        }

        if (filter.campaignId() != null) {
            predicates.add(
                    cb.equal(
                            root.get("campaign").get("id"),
                            filter.campaignId()
                    )
            );
        }

        if (filter.contactId() != null) {
            predicates.add(
                    cb.equal(
                            root.get("contact").get("id"),
                            filter.contactId()
                    )
            );
        }

        if (filter.status() != null) {
            predicates.add(
                    cb.equal(
                            root.get("status"),
                            filter.status()
                    )
            );
        }

        Instant todayStart = LocalDate.now(ZoneOffset.UTC)
                .atStartOfDay()
                .toInstant(ZoneOffset.UTC);

        cq.multiselect(
                cb.count(root),

                cb.sum(
                        cb.<Long>selectCase()
                                .when(cb.isNotNull(root.get("sentAt")), 1L)
                                .otherwise(0L)
                ),

                cb.sum(
                        cb.<Long>selectCase()
                                .when(cb.isNotNull(root.get("deliveredAt")), 1L)
                                .otherwise(0L)
                ),

                cb.sum(
                        cb.<Long>selectCase()
                                .when(cb.isNotNull(root.get("readAt")), 1L)
                                .otherwise(0L)
                ),

                cb.sum(
                        cb.<Long>selectCase()
                                .when(cb.isNotNull(root.get("failedAt")), 1L)
                                .otherwise(0L)
                ),

                cb.sum(
                        cb.<Long>selectCase()
                                .when(
                                        cb.greaterThanOrEqualTo(
                                                root.get("sentAt"),
                                                todayStart
                                        ),
                                        1L
                                )
                                .otherwise(0L)
                ),

                cb.sum(
                        cb.<Long>selectCase()
                                .when(
                                        cb.greaterThanOrEqualTo(
                                                root.get("deliveredAt"),
                                                todayStart
                                        ),
                                        1L
                                )
                                .otherwise(0L)
                ),

                cb.sum(
                        cb.<Long>selectCase()
                                .when(
                                        cb.greaterThanOrEqualTo(
                                                root.get("readAt"),
                                                todayStart
                                        ),
                                        1L
                                )
                                .otherwise(0L)
                )
        );

        cq.where(predicates.toArray(new Predicate[0]));

        Object[] row = entityManager
                .createQuery(cq)
                .getSingleResult();

        return new SummaryMetrics(
                getLong(row[0]),
                getLong(row[1]),
                getLong(row[2]),
                getLong(row[3]),
                getLong(row[4]),
                getLong(row[5]),
                getLong(row[6]),
                getLong(row[7])
        );
    }

    private long getLong(Object value) {
        return value == null ? 0 : ((Number) value).longValue();
    }
}