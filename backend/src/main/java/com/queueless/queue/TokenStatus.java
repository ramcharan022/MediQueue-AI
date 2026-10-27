package com.queueless.queue;

/**
 * Valid forward transitions:
 * WAITING → CALLED → IN_SERVICE → COMPLETED
 * WAITING or CALLED → SKIPPED | NO_SHOW | CANCELLED
 * SKIPPED → CALLED (recall by staff)
 */
public enum TokenStatus {
    WAITING, CALLED, IN_SERVICE, COMPLETED, SKIPPED, NO_SHOW, CANCELLED;

    public boolean canTransitionTo(TokenStatus next) {
        return switch (this) {
            case WAITING   -> next == CALLED || next == SKIPPED || next == NO_SHOW || next == CANCELLED;
            case CALLED    -> next == IN_SERVICE || next == SKIPPED || next == NO_SHOW || next == CANCELLED;
            case IN_SERVICE -> next == COMPLETED || next == NO_SHOW;
            case SKIPPED   -> next == CALLED; // recall
            default        -> false; // COMPLETED, NO_SHOW, CANCELLED are terminal
        };
    }
}
