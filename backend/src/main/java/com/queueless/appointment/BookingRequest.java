package com.queueless.appointment;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record BookingRequest(
        @NotNull @Positive Long doctorId,
        @NotNull LocalDateTime startsAt,
        @Size(max = 1000) String notes
) {}
