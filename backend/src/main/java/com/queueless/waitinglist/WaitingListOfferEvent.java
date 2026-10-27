package com.queueless.waitinglist;

public record WaitingListOfferEvent(Long patientUserId, Long entryId, Long departmentId) {}
