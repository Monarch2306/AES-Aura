package com.aes.questions;

import com.aes.auth.UserPrincipal;
import com.aes.audit.AuditService;
import com.aes.common.BadRequestException;
import com.aes.common.ResourceAuthorizationService;
import com.aes.exams.Exam;
import com.aes.exams.ExamService;
import com.aes.questions.dto.QuestionCreateRequest;
import com.aes.rubrics.Rubric;
import com.aes.rubrics.RubricRepository;
import com.aes.users.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class QuestionServiceTest {
    @Mock QuestionRepository questions;
    @Mock ExamService exams;
    @Mock RubricRepository rubrics;
    @Mock ResourceAuthorizationService authorization;
    @Mock AuditService auditService;
    @InjectMocks QuestionService service;

    @Test
    void duplicateQuestionNumberIsRejectedBeforeSave() {
        Exam exam = exam(4L);
        when(exams.findOrThrow(4L)).thenReturn(exam);
        when(questions.findByExamIdAndQuestionNumber(4L, 2)).thenReturn(Optional.of(new Question()));

        assertThrows(BadRequestException.class, () -> service.create(
                new QuestionCreateRequest(4L, 2, "Explain OCR", 5.0), principal()));
        verify(questions, never()).save(any());
    }

    @Test
    void createMapsQuestionAndRubricPresence() {
        Exam exam = exam(4L);
        when(exams.findOrThrow(4L)).thenReturn(exam);
        when(questions.findByExamIdAndQuestionNumber(4L, 2)).thenReturn(Optional.empty());
        when(questions.save(any(Question.class))).thenAnswer(invocation -> {
            Question question = invocation.getArgument(0);
            question.setId(11L);
            return question;
        });
        when(rubrics.findByQuestionId(11L)).thenReturn(Optional.of(new Rubric()));

        var response = service.create(new QuestionCreateRequest(4L, 2, "Explain OCR", 5.0), principal());

        assertEquals(11L, response.id());
        assertEquals(4L, response.examId());
        assertEquals(2, response.questionNumber());
        assertTrue(response.hasRubric());
    }

    private static Exam exam(Long id) {
        Exam exam = new Exam();
        exam.setId(id);
        exam.setTitle("Assessment");
        exam.setSubject("AI");
        User teacher = new User();
        teacher.setId(8L);
        exam.setTeacher(teacher);
        return exam;
    }

    private static UserPrincipal principal() {
        User user = new User();
        user.setId(8L);
        user.setRole(com.aes.common.Role.TEACHER);
        return new UserPrincipal(user);
    }
}
