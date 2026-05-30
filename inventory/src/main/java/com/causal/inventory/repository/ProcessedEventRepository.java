package com.causal.inventory.repository;

import com.causal.inventory.model.ProcessedEvent;
import com.causal.inventory.model.ProcessedEventId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, ProcessedEventId> {

    boolean existsByEventIdAndConsumerId(String eventId, String consumerId);
}
