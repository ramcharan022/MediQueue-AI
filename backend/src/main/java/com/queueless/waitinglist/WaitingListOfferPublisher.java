package com.queueless.waitinglist;

import com.queueless.notification.NotificationService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class WaitingListOfferPublisher {
    private final NotificationService notificationService;

    public WaitingListOfferPublisher(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(WaitingListOfferEvent event) {
        notificationService.createNotification(event.patientUserId(), "WAITING_LIST_OPENING",
                "An appointment opening may be available",
                "A place may now be available for your waiting-list request. Sign in to check appointment availability; this does not reserve a slot.",
                "waiting-list-" + event.entryId());
    }
}
