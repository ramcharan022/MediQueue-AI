package com.queueless.queue;

import java.time.Instant;

public record QueueUpdateEvent(Long queueId, Long tokenId, Long patientUserId,
                               String tokenNumber, String status,
                               Integer estimatedWaitMinutes, Instant updatedAt) {
    public static QueueUpdateEvent from(QueueToken token) {
        return new QueueUpdateEvent(token.getQueueId(), token.getId(), token.getPatientUserId(),
                token.getTokenNumber(), token.getStatus().name(),
                token.getEstimatedWaitMinutes(), token.getUpdatedAt());
    }
}
