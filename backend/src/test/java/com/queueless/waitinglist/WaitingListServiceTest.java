package com.queueless.waitinglist;

import com.queueless.audit.AuditService;
import com.queueless.common.exception.AppException;
import com.queueless.doctor.DoctorRepository;
import com.queueless.hospital.Department;
import com.queueless.hospital.DepartmentRepository;
import com.queueless.user.PatientProfile;
import com.queueless.user.PatientProfileRepository;
import com.queueless.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WaitingListServiceTest {
    @Mock private WaitingListEntryRepository entryRepository;
    @Mock private PatientProfileRepository profileRepository;
    @Mock private DepartmentRepository departmentRepository;
    @Mock private DoctorRepository doctorRepository;
    @Mock private AuditService auditService;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private User patient;
    @Mock private User staff;
    @Mock private PatientProfile profile;
    private WaitingListService service;

    @BeforeEach
    void setUp() {
        service = new WaitingListService(entryRepository, profileRepository, departmentRepository,
                doctorRepository, auditService, eventPublisher);
    }

    @Test
    void joinsDepartmentWaitingListForAuthenticatedPatient() {
        when(patient.getId()).thenReturn(10L);
        when(profile.getId()).thenReturn(30L);
        when(profileRepository.findByUserId(10L)).thenReturn(Optional.of(profile));
        when(departmentRepository.findById(4L)).thenReturn(Optional.of(new Department()));
        when(entryRepository.existsByPatientIdAndDepartmentIdAndDoctorIdAndStatus(
                30L, 4L, null, WaitingListStatus.PENDING)).thenReturn(false);
        when(entryRepository.saveAndFlush(any(WaitingListEntry.class))).thenAnswer(call -> call.getArgument(0));

        WaitingListEntry entry = service.join(patient, 4L, null);

        assertEquals(30L, entry.getPatientId());
        assertEquals(4L, entry.getDepartmentId());
        assertEquals(WaitingListStatus.PENDING, entry.getStatus());
    }

    @Test
    void refusesDuplicatePendingRequest() {
        when(patient.getId()).thenReturn(10L);
        when(profile.getId()).thenReturn(30L);
        when(profileRepository.findByUserId(10L)).thenReturn(Optional.of(profile));
        when(departmentRepository.findById(4L)).thenReturn(Optional.of(new Department()));
        when(entryRepository.existsByPatientIdAndDepartmentIdAndDoctorIdAndStatus(
                30L, 4L, null, WaitingListStatus.PENDING)).thenReturn(true);

        assertThrows(AppException.class, () -> service.join(patient, 4L, null));
        verify(entryRepository, never()).save(any());
    }

    @Test
    void offeringOpeningNotifiesPatientAfterCommitAndAuditsStaffAction() {
        when(staff.getId()).thenReturn(20L);
        when(profile.getUserId()).thenReturn(10L);
        WaitingListEntry entry = new WaitingListEntry();
        entry.setPatientId(30L);
        entry.setDepartmentId(4L);
        when(entryRepository.findByIdAndDepartmentId(50L, 4L)).thenReturn(Optional.of(entry));
        when(profileRepository.findById(30L)).thenReturn(Optional.of(profile));
        when(entryRepository.save(entry)).thenReturn(entry);

        WaitingListEntry updated = service.offerOpening(staff, 4L, 50L);

        assertEquals(WaitingListStatus.FULFILLED, updated.getStatus());
        assertNotNull(updated.getFulfilledAt());
        verify(eventPublisher).publishEvent(any(WaitingListOfferEvent.class));
        verify(auditService).log(20L, "WAITING_LIST_OPENING_OFFERED",
                "waiting_list_entry", 50L, "Opening notification sent for department 4");
    }
}
