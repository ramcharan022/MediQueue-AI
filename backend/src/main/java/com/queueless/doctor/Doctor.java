package com.queueless.doctor;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "doctor")
public class Doctor {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "branch_id", nullable = false) private Long branchId;
    @Column(name = "department_id", nullable = false) private Long departmentId;
    @Column(name = "user_id") private Long userId;
    @Column(name = "full_name", nullable = false, length = 120) private String fullName;
    @Column(nullable = false, length = 120) private String specialization;
    @Column(length = 255) private String qualification;
    @Column(name = "consultation_minutes", nullable = false) private int consultationMinutes = 15;
    @Column(name = "is_active", nullable = false) private boolean active = true;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt = Instant.now();
    @Column(name = "updated_at", nullable = false) private Instant updatedAt = Instant.now();
    @PreUpdate void onUpdate() { updatedAt = Instant.now(); }

    public Doctor() {}
    public Long getId() { return id; }
    public Long getBranchId() { return branchId; }
    public void setBranchId(Long branchId) { this.branchId = branchId; }
    public Long getDepartmentId() { return departmentId; }
    public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getSpecialization() { return specialization; }
    public void setSpecialization(String specialization) { this.specialization = specialization; }
    public String getQualification() { return qualification; }
    public void setQualification(String qualification) { this.qualification = qualification; }
    public int getConsultationMinutes() { return consultationMinutes; }
    public void setConsultationMinutes(int consultationMinutes) { this.consultationMinutes = consultationMinutes; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
