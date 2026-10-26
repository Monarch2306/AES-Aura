package com.aes.exams;

import com.aes.auth.UserPrincipal;
import com.aes.audit.AuditService;
import com.aes.common.ResourceAuthorizationService;
import com.aes.common.ResourceNotFoundException;
import com.aes.exams.dto.ExamCreateRequest;
import com.aes.users.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class ExamServiceTest {
    @Mock ExamRepository exams;
    @Mock ResourceAuthorizationService authorization;
    @Mock AuditService auditService;
    @InjectMocks ExamService service;

    @Test
    void createCopiesRequestAndTeacher() {
        User teacher = user(7L, "Teacher", com.aes.common.Role.TEACHER);
        UserPrincipal principal = new UserPrincipal(teacher);
        when(exams.save(any(Exam.class))).thenAnswer(invocation -> {
            Exam exam = invocation.getArgument(0);
            exam.setId(12L);
            return exam;
        });

        var response = service.create(new ExamCreateRequest("Midterm", "AI", LocalDate.of(2026, 10, 1)), principal);

        assertEquals(12L, response.id());
        assertEquals("Midterm", response.title());
        assertEquals("AI", response.subject());
        assertEquals(7L, response.teacherId());
        verify(exams).save(argThat(exam -> exam.getTeacher() == teacher && exam.getExamDate().equals(LocalDate.of(2026, 10, 1))));
    }

    @Test
    void findOrThrowReportsMissingExam() {
        when(exams.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.findOrThrow(99L));
    }

    @Test
    void listAllFiltersTeacherOwnedExams() {
        User teacher = user(7L, "Teacher", com.aes.common.Role.TEACHER);
        Exam owned = exam(1L, "Owned", teacher);
        Exam other = exam(2L, "Other", user(8L, "Other", com.aes.common.Role.TEACHER));
        UserPrincipal principal = new UserPrincipal(teacher);
        when(exams.findAll()).thenReturn(List.of(owned, other));
        when(authorization.isAdmin(principal)).thenReturn(false);
        when(authorization.isTeacher(principal)).thenReturn(true);

        var result = service.listAll(principal);

        assertEquals(List.of(1L), result.stream().map(response -> response.id()).toList());
    }

    private static User user(Long id, String name, com.aes.common.Role role) {
        User user = new User();
        user.setId(id);
        user.setName(name);
        user.setRole(role);
        return user;
    }

    private static Exam exam(Long id, String title, User teacher) {
        Exam exam = new Exam();
        exam.setId(id);
        exam.setTitle(title);
        exam.setSubject("AI");
        exam.setTeacher(teacher);
        return exam;
    }
}
