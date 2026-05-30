package com.causal.inventory.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "processed_events")
@IdClass(ProcessedEventId.class)
@Getter
@Setter
public class ProcessedEvent {

    @Id
    private String eventId;

    @Id
    private String consumerId;

    @Column(nullable = false, updatable = false)
    private Instant processedAt = Instant.now();

    public ProcessedEvent() {}

    public ProcessedEvent(String eventId, String consumerId) {
        this.eventId = eventId;
        this.consumerId = consumerId;
    }
}
