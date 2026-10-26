package com.aes.common;

import com.aes.auth.UserPrincipal;
import com.aes.exams.Exam;
import com.aes.users.User;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ResourceAuthorizationServiceTest {

    private final ResourceAuthorizationService authorizationService = new ResourceAuthorizationService();

    @Test
    void ownerTeacherCanWriteOwnExam() {
        Exam exam = examOwnedBy(10L);

        assertDoesNotThrow(() -> authorizationService.requireExamWrite(exam, principal(Role.TEACHER, 10L)));
    }

    @Test
    void otherTeacherCannotWriteAnotherTeachersExam() {
        Exam exam = examOwnedBy(10L);

        assertThrows(AccessDeniedException.class,
                () -> authorizationService.requireExamWrite(exam, principal(Role.TEACHER, 11L)));
    }

    @Test
    void studentCannotAccessAnotherStudentsAnalytics() {
        UserPrincipal student = principal(Role.STUDENT, 20L);

        assertThrows(AccessDeniedException.class,
                () -> authorizationService.requireStudentSelfOrAdmin(21L, student));
    }

    @Test
    void adminCanAccessAnotherStudentsAnalytics() {
        UserPrincipal admin = principal(Role.ADMIN, 1L);

        assertDoesNotThrow(() -> authorizationService.requireStudentSelfOrAdmin(21L, admin));
    }

    private Exam examOwnedBy(Long teacherId) {
        User teacher = new User();
        teacher.setId(teacherId);
        Exam exam = new Exam();
        exam.setTeacher(teacher);
        return exam;
    }

    private UserPrincipal principal(Role role, Long userId) {
        User user = new User();
        user.setId(userId);
        user.setRole(role);
        return new UserPrincipal(user);
    }
}
