package com.causal.inventory.model;

import java.io.Serializable;
import java.util.Objects;

public class ProcessedEventId implements Serializable {

    private String eventId;
    private String consumerId;

    public ProcessedEventId() {}

    public ProcessedEventId(String eventId, String consumerId) {
        this.eventId = eventId;
        this.consumerId = consumerId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProcessedEventId that)) return false;
        return Objects.equals(eventId, that.eventId) && Objects.equals(consumerId, that.consumerId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventId, consumerId);
    }
}
