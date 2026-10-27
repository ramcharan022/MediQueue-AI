package com.queueless.queue;

import com.queueless.user.UserRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class QueueUpdatePublisher {
    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;
    private final QueueTokenRepository tokenRepository;

    public QueueUpdatePublisher(SimpMessagingTemplate messagingTemplate, UserRepository userRepository,
                                QueueTokenRepository tokenRepository) {
        this.messagingTemplate = messagingTemplate;
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(QueueUpdateEvent update) {
        QueueUpdateSnapshot snapshot = new QueueUpdateSnapshot(update.queueId(), update.tokenId(),
                update.tokenNumber(), update.status(), update.estimatedWaitMinutes(), update.updatedAt());
        messagingTemplate.convertAndSend("/topic/queues/" + update.queueId() + "/updates", snapshot);

        Set<Long> patientIds = new HashSet<>();
        if (update.patientUserId() != null) patientIds.add(update.patientUserId());
        tokenRepository.findByQueueIdAndPatientUserIdIsNotNullAndStatusIn(update.queueId(),
                List.of(TokenStatus.WAITING, TokenStatus.CALLED, TokenStatus.IN_SERVICE))
                .stream().map(QueueToken::getPatientUserId).forEach(patientIds::add);
        patientIds.forEach(patientId -> userRepository.findById(patientId).ifPresent(user ->
                messagingTemplate.convertAndSendToUser(user.getEmail(), "/queue/updates", snapshot)));
    }

    record QueueUpdateSnapshot(Long queueId, Long tokenId, String tokenNumber, String status,
                               Integer estimatedWaitMinutes, java.time.Instant updatedAt) {}
}
