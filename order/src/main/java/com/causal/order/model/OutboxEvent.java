package com.causal.order.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "outbox")
@Getter
@Setter
public class OutboxEvent {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String aggregatetype;

    @Column(nullable = false)
    private String aggregateid;

    @Column(nullable = false)
    private String type;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> payload;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public OutboxEvent() {
        this.id = UUID.randomUUID();
    }

    public OutboxEvent(String aggregateType, String aggregateId, String type, Map<String, Object> payload) {
        this();
        this.aggregatetype = aggregateType;
        this.aggregateid = aggregateId;
        this.type = type;
        this.payload = payload;
    }
}
