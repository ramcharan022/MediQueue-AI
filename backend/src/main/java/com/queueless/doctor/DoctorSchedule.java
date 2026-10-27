package com.queueless.doctor;

import jakarta.persistence.*;
import java.time.LocalTime;

@Entity
@Table(name = "doctor_schedule")
public class DoctorSchedule {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "doctor_id", nullable = false) private Long doctorId;
    @Column(name = "day_of_week", nullable = false, columnDefinition = "TINYINT") private int dayOfWeek;
    @Column(name = "starts_at", nullable = false) private LocalTime startsAt;
    @Column(name = "ends_at", nullable = false) private LocalTime endsAt;

    public DoctorSchedule() {}
    public Long getId() { return id; }
    public Long getDoctorId() { return doctorId; }
    public void setDoctorId(Long doctorId) { this.doctorId = doctorId; }
    public int getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(int dayOfWeek) { this.dayOfWeek = dayOfWeek; }
    public LocalTime getStartsAt() { return startsAt; }
    public void setStartsAt(LocalTime startsAt) { this.startsAt = startsAt; }
    public LocalTime getEndsAt() { return endsAt; }
    public void setEndsAt(LocalTime endsAt) { this.endsAt = endsAt; }
}
