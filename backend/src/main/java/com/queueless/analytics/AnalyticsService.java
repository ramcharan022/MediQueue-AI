package com.queueless.analytics;

import com.queueless.appointment.AppointmentRepository;
import com.queueless.appointment.AppointmentStatus;
import com.queueless.doctor.DoctorRepository;
import com.queueless.queue.QueueTokenRepository;
import com.queueless.queue.TokenStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

@Service
public class AnalyticsService {

    private final AppointmentRepository appointmentRepo;
    private final QueueTokenRepository tokenRepo;
    private final DoctorRepository doctorRepo;

    public AnalyticsService(AppointmentRepository appointmentRepo,
                             QueueTokenRepository tokenRepo,
                             DoctorRepository doctorRepo) {
        this.appointmentRepo = appointmentRepo;
        this.tokenRepo = tokenRepo;
        this.doctorRepo = doctorRepo;
    }

    public Map<String, Object> getSummaryStats() {
        Instant now = Instant.now();
        Instant today = now.truncatedTo(ChronoUnit.DAYS);
        Instant weekAgo = now.minus(7, ChronoUnit.DAYS);

        long totalAppointments = appointmentRepo.count();
        long completedAppointments = appointmentRepo.countByStatus(AppointmentStatus.COMPLETED);
        long cancelledAppointments = appointmentRepo.countByStatus(AppointmentStatus.CANCELLED);
        long confirmedAppointments = appointmentRepo.countByStatus(AppointmentStatus.CONFIRMED);

        long totalTokens = tokenRepo.count();
        long waitingTokens = tokenRepo.countByStatus(TokenStatus.WAITING);
        long completedTokens = tokenRepo.countByStatus(TokenStatus.COMPLETED);
        long noShowTokens = tokenRepo.countByStatus(TokenStatus.NO_SHOW);
        long totalDoctors = doctorRepo.countByActiveTrue();

        double completionRate = totalAppointments > 0
                ? (completedAppointments * 100.0 / totalAppointments) : 0;
        double noShowRate = totalTokens > 0
                ? (noShowTokens * 100.0 / totalTokens) : 0;

        return Map.ofEntries(
                Map.entry("totalAppointments", totalAppointments),
                Map.entry("completedAppointments", completedAppointments),
                Map.entry("cancelledAppointments", cancelledAppointments),
                Map.entry("confirmedAppointments", confirmedAppointments),
                Map.entry("totalQueueTokens", totalTokens),
                Map.entry("waitingPatients", waitingTokens),
                Map.entry("completedConsultations", completedTokens),
                Map.entry("noShows", noShowTokens),
                Map.entry("activeDoctors", totalDoctors),
                Map.entry("appointmentCompletionRate", Math.round(completionRate * 10.0) / 10.0),
                Map.entry("noShowRate", Math.round(noShowRate * 10.0) / 10.0)
        );
    }
}
