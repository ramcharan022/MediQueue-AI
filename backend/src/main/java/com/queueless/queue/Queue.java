package com.queueless.queue;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "queue",
       uniqueConstraints = @UniqueConstraint(name = "uk_queue_scope",
               columnNames = {"branch_id", "department_id", "service_date"}))
public class Queue {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "branch_id", nullable = false) private Long branchId;
    @Column(name = "department_id", nullable = false) private Long departmentId;
    @Column(name = "service_date", nullable = false) private LocalDate serviceDate;
    @Column(name = "last_token_sequence", nullable = false) private int lastTokenSequence;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt = Instant.now();

    public Queue() {}
    public Queue(Long branchId, Long departmentId, LocalDate serviceDate) {
        this.branchId = branchId;
        this.departmentId = departmentId;
        this.serviceDate = serviceDate;
    }

    public Long getId() { return id; }
    public Long getBranchId() { return branchId; }
    public Long getDepartmentId() { return departmentId; }
    public LocalDate getServiceDate() { return serviceDate; }
    public int getLastTokenSequence() { return lastTokenSequence; }
    public void setLastTokenSequence(int lastTokenSequence) { this.lastTokenSequence = lastTokenSequence; }
    public Instant getCreatedAt() { return createdAt; }
}
