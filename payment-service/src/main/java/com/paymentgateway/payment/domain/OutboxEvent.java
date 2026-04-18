package com.paymentgateway.payment.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Table;
import io.r2dbc.postgresql.codec.Json;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("outbox_events")
public class OutboxEvent implements Persistable<UUID> {

    @Id
    private UUID id;

    private String aggregateType;
    private UUID aggregateId;
    private String eventType;
    private Json payload;
    private boolean processed;
    private Instant createdAt;
    private Instant processedAt;

    /**
     * Always true — OutboxEvents are only ever inserted, never updated
     * by the application. Debezium marks them processed externally.
     */
    @Override
    @Transient
    public boolean isNew() {
        return createdAt == null;
    }
}