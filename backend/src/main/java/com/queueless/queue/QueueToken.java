package com.queueless.queue;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "queue_token",
       uniqueConstraints = @UniqueConstraint(name = "uk_queue_token_number",
               columnNames = {"queue_id", "token_number"}))
public class QueueToken {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "queue_id", nullable = false) private Long queueId;
    @Column(name = "patient_user_id") private Long patientUserId;
    @Column(name = "appointment_id") private Long appointmentId;
    @Column(name = "doctor_id") private Long doctorId;
    @Column(name = "token_number", nullable = false, length = 10) private String tokenNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20) private TokenStatus status = TokenStatus.WAITING;

    @Column(name = "allocation_reason", length = 500) private String allocationReason;
    @Column(name = "estimated_wait_minutes") private Integer estimatedWaitMinutes;
    @Column(name = "started_at") private Instant startedAt;
    @Column(name = "completed_at") private Instant completedAt;

    @Version @Column(nullable = false) private int version;

    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt = Instant.now();
    @Column(name = "updated_at", nullable = false) private Instant updatedAt = Instant.now();
    @PreUpdate void onUpdate() { updatedAt = Instant.now(); }

    public QueueToken() {}

    // Getters & setters
    public Long getId() { return id; }
    public Long getQueueId() { return queueId; }
    public void setQueueId(Long queueId) { this.queueId = queueId; }
    public Long getPatientUserId() { return patientUserId; }
    public void setPatientUserId(Long patientUserId) { this.patientUserId = patientUserId; }
    public Long getAppointmentId() { return appointmentId; }
    public void setAppointmentId(Long appointmentId) { this.appointmentId = appointmentId; }
    public Long getDoctorId() { return doctorId; }
    public void setDoctorId(Long doctorId) { this.doctorId = doctorId; }
    public String getTokenNumber() { return tokenNumber; }
    public void setTokenNumber(String tokenNumber) { this.tokenNumber = tokenNumber; }
    public TokenStatus getStatus() { return status; }
    public void setStatus(TokenStatus status) { this.status = status; }
    public String getAllocationReason() { return allocationReason; }
    public void setAllocationReason(String allocationReason) { this.allocationReason = allocationReason; }
    public Integer getEstimatedWaitMinutes() { return estimatedWaitMinutes; }
    public void setEstimatedWaitMinutes(Integer estimatedWaitMinutes) { this.estimatedWaitMinutes = estimatedWaitMinutes; }
    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
    public int getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
