package com.queueless.waitinglist;

import com.queueless.common.exception.AppException;
import com.queueless.doctor.Doctor;
import com.queueless.doctor.DoctorRepository;
import com.queueless.hospital.DepartmentRepository;
import com.queueless.user.PatientProfile;
import com.queueless.user.PatientProfileRepository;
import com.queueless.user.User;
import com.queueless.audit.AuditService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
import java.util.List;

@Service
public class WaitingListService {
    private final WaitingListEntryRepository entryRepository;
    private final PatientProfileRepository profileRepository;
    private final DepartmentRepository departmentRepository;
    private final DoctorRepository doctorRepository;
    private final AuditService auditService;
    private final ApplicationEventPublisher eventPublisher;

    public WaitingListService(WaitingListEntryRepository entryRepository,
                              PatientProfileRepository profileRepository,
                              DepartmentRepository departmentRepository,
                              DoctorRepository doctorRepository,
                              AuditService auditService,
                              ApplicationEventPublisher eventPublisher) {
        this.entryRepository = entryRepository;
        this.profileRepository = profileRepository;
        this.departmentRepository = departmentRepository;
        this.doctorRepository = doctorRepository;
        this.auditService = auditService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    public List<WaitingListEntry> list(User user) {
        PatientProfile profile = profile(user);
        return entryRepository.findByPatientIdOrderByRequestedAtDesc(profile.getId());
    }

    @Transactional
    public WaitingListEntry join(User user, Long departmentId, Long doctorId) {
        PatientProfile profile = profile(user);
        departmentRepository.findById(departmentId)
                .orElseThrow(() -> AppException.notFound("Department not found."));
        if (doctorId != null) {
            Doctor doctor = doctorRepository.findById(doctorId)
                    .filter(Doctor::isActive)
                    .orElseThrow(() -> AppException.notFound("Doctor not found or unavailable."));
            if (!departmentId.equals(doctor.getDepartmentId())) {
                throw AppException.badRequest("The selected doctor does not belong to this department.");
            }
        }
        if (entryRepository.existsByPatientIdAndDepartmentIdAndDoctorIdAndStatus(
                profile.getId(), departmentId, doctorId, WaitingListStatus.PENDING)) {
            throw AppException.conflict("You are already on this waiting list.");
        }
        WaitingListEntry entry = new WaitingListEntry();
        entry.setPatientId(profile.getId());
        entry.setDepartmentId(departmentId);
        entry.setDoctorId(doctorId);
        try {
            return entryRepository.saveAndFlush(entry);
        } catch (DataIntegrityViolationException duplicate) {
            throw AppException.conflict("You are already on this waiting list.");
        }
    }

    @Transactional
    public WaitingListEntry cancel(User user, Long entryId) {
        PatientProfile profile = profile(user);
        WaitingListEntry entry = entryRepository.findByIdAndPatientId(entryId, profile.getId())
                .orElseThrow(() -> AppException.notFound("Waiting-list entry not found."));
        if (entry.getStatus() != WaitingListStatus.PENDING) {
            throw AppException.badRequest("Only pending waiting-list entries can be cancelled.");
        }
        entry.setStatus(WaitingListStatus.CANCELLED);
        return entry;
    }

    @Transactional(readOnly = true)
    public List<WaitingListEntry> listPendingForStaff(Long departmentId) {
        departmentRepository.findById(departmentId)
                .orElseThrow(() -> AppException.notFound("Department not found."));
        return entryRepository.findByDepartmentIdAndStatusOrderByRequestedAtAsc(
                departmentId, WaitingListStatus.PENDING);
    }

    @Transactional
    public WaitingListEntry offerOpening(User staff, Long departmentId, Long entryId) {
        WaitingListEntry entry = entryRepository.findByIdAndDepartmentId(entryId, departmentId)
                .orElseThrow(() -> AppException.notFound("Waiting-list entry not found."));
        if (entry.getStatus() != WaitingListStatus.PENDING) {
            throw AppException.conflict("This waiting-list request is no longer pending.");
        }
        PatientProfile patient = profileRepository.findById(entry.getPatientId())
                .orElseThrow(() -> AppException.notFound("Patient profile not found."));
        entry.setStatus(WaitingListStatus.FULFILLED);
        entry.setFulfilledAt(java.time.Instant.now());
        WaitingListEntry updated = entryRepository.save(entry);
        eventPublisher.publishEvent(new WaitingListOfferEvent(
                patient.getUserId(), entry.getId(), departmentId));
        auditService.log(staff.getId(), "WAITING_LIST_OPENING_OFFERED", "waiting_list_entry",
                entryId, "Opening notification sent for department " + departmentId);
        return updated;
    }

    private PatientProfile profile(User user) {
        return profileRepository.findByUserId(user.getId())
                .orElseThrow(() -> AppException.badRequest("Patient profile not found."));
    }
}
