package com.queueless.queue;

import com.queueless.audit.AuditService;
import com.queueless.common.exception.AppException;
import com.queueless.doctor.DoctorRepository;
import com.queueless.hospital.Department;
import com.queueless.hospital.DepartmentRepository;
import com.queueless.notification.NotificationService;
import com.queueless.user.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QueueServiceAccessTest {
    @Mock private QueueRepository queueRepository;
    @Mock private QueueTokenRepository tokenRepository;
    @Mock private com.queueless.appointment.AppointmentRepository appointmentRepository;
    @Mock private QueueEventRepository eventRepository;
    @Mock private DoctorRepository doctorRepository;
    @Mock private PatientProfileRepository profileRepository;
    @Mock private UserRepository userRepository;
    @Mock private DepartmentRepository departmentRepository;
    @Mock private com.queueless.hospital.HospitalBranchRepository branchRepository;
    @Mock private StaffAssignmentRepository assignmentRepository;
    @Mock private NotificationService notificationService;
    @Mock private AuditService auditService;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private User staff;
    private QueueService service;

    @BeforeEach
    void setUp() {
        service = new QueueService(queueRepository, tokenRepository, appointmentRepository,
                eventRepository, doctorRepository, profileRepository, userRepository,
                departmentRepository, branchRepository, assignmentRepository, notificationService, auditService, eventPublisher);
        when(staff.getRole()).thenReturn(UserRole.STAFF);
        when(staff.getId()).thenReturn(15L);
    }

    @Test
    void deniesStaffWithoutDepartmentAssignment() {
        when(departmentRepository.findById(4L)).thenReturn(Optional.of(department(2L)));
        when(assignmentRepository.findByUserId(15L)).thenReturn(Optional.empty());

        assertThrows(AppException.class, () -> service.assertStaffCanAccessDepartment(staff, 4L));
    }

    @Test
    void deniesStaffAssignedToDifferentBranchOrDepartment() {
        when(departmentRepository.findById(4L)).thenReturn(Optional.of(department(2L)));
        StaffAssignment assignment = new StaffAssignment();
        assignment.setBranchId(2L);
        assignment.setDepartmentId(8L);
        when(assignmentRepository.findByUserId(15L)).thenReturn(Optional.of(assignment));

        assertThrows(AppException.class, () -> service.assertStaffCanAccessDepartment(staff, 4L));
    }

    private Department department(Long branchId) {
        Department department = new Department();
        department.setBranchId(branchId);
        return department;
    }
}
