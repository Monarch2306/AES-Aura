package com.aes.analytics;

import com.aes.answersheets.AnswerSheetRepository;
import com.aes.auth.UserPrincipal;
import com.aes.common.ResourceAuthorizationService;
import com.aes.evaluations.EvaluationRepository;
import com.aes.exams.ExamService;
import com.aes.users.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {
    @Mock EvaluationRepository evaluations;
    @Mock AnswerSheetRepository sheets;
    @Mock ExamService exams;
    @Mock ResourceAuthorizationService authorization;

    @Test
    void emptyStudentHistoryReturnsEmptyLearningSignals() {
        when(sheets.findByStudentId(7L)).thenReturn(List.of());
        when(evaluations.findByAnswerSheet_Student_Id(7L)).thenReturn(List.of());
        AnalyticsService service = new AnalyticsService(evaluations, sheets, exams, authorization, 3);

        var result = service.studentAnalytics(7L, new UserPrincipal(user(7L)));

        assertEquals(0.0, result.averageMarksPercent());
        assertEquals(0.0, result.highestScorePercent());
        assertEquals(0.0, result.recentScorePercent());
        assertTrue(result.topWeakConcepts().isEmpty());
        assertTrue(result.recommendations().isEmpty());
        verify(authorization).requireStudentSelfOrAdmin(eq(7L), any(UserPrincipal.class));
    }

    private static User user(Long id) {
        User user = new User();
        user.setId(id);
        user.setRole(com.aes.common.Role.STUDENT);
        return user;
    }
}
