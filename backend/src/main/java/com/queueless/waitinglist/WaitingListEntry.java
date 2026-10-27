package com.queueless.waitinglist;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "waiting_list_entry")
public class WaitingListEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "department_id", nullable = false)
    private Long departmentId;

    @Column(name = "doctor_id")
    private Long doctorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WaitingListStatus status = WaitingListStatus.PENDING;

    @Column(name = "requested_at", nullable = false, updatable = false)
    private Instant requestedAt = Instant.now();

    @Column(name = "fulfilled_at")
    private Instant fulfilledAt;

    public Long getId() { return id; }
    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }
    public Long getDepartmentId() { return departmentId; }
    public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }
    public Long getDoctorId() { return doctorId; }
    public void setDoctorId(Long doctorId) { this.doctorId = doctorId; }
    public WaitingListStatus getStatus() { return status; }
    public void setStatus(WaitingListStatus status) { this.status = status; }
    public Instant getRequestedAt() { return requestedAt; }
    public Instant getFulfilledAt() { return fulfilledAt; }
    public void setFulfilledAt(Instant fulfilledAt) { this.fulfilledAt = fulfilledAt; }
}
