package com.aes.rubrics;

import com.aes.auth.UserPrincipal;
import com.aes.audit.AuditService;
import com.aes.common.BadRequestException;
import com.aes.common.ResourceAuthorizationService;
import com.aes.questions.Question;
import com.aes.questions.QuestionService;
import com.aes.rubrics.dto.RubricConceptRequest;
import com.aes.rubrics.dto.RubricCreateRequest;
import com.aes.users.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class RubricServiceTest {
    @Mock RubricRepository rubrics;
    @Mock QuestionService questions;
    @Mock ResourceAuthorizationService authorization;
    @Mock AuditService auditService;
    @InjectMocks RubricService service;

    @Test
    void createAppliesDefaultsAndMapsConcepts() {
        Question question = question(3L);
        when(questions.findOrThrow(3L)).thenReturn(question);
        when(rubrics.findByQuestionId(3L)).thenReturn(Optional.empty());
        when(rubrics.save(any(Rubric.class))).thenAnswer(invocation -> {
            Rubric rubric = invocation.getArgument(0);
            rubric.setId(15L);
            return rubric;
        });

        var request = new RubricCreateRequest(3L, "A reference answer", List.of(
            new RubricConceptRequest("OCR", 2.0, true, false),
            new RubricConceptRequest("Noise", 1.0, false, true)), null, null, null);
        var response = service.create(request, principal());

        assertEquals(15L, response.id());
        assertEquals(0, response.minRequiredConcepts());
        assertEquals(2, response.concepts().size());
        assertEquals("OCR", response.concepts().get(0).concept());
        verify(rubrics).save(argThat(rubric -> rubric.getConcepts().size() == 2
                && rubric.getConcepts().get(0).isRequired()
                && rubric.getConcepts().get(1).isNegative()));
    }

    @Test
    void duplicateRubricIsRejected() {
        Question question = question(3L);
        when(questions.findOrThrow(3L)).thenReturn(question);
        when(rubrics.findByQuestionId(3L)).thenReturn(Optional.of(new Rubric()));

        var request = new RubricCreateRequest(3L, "Reference", List.of(
            new RubricConceptRequest("Concept", 1.0, false, false)), null, null, null);

        assertThrows(BadRequestException.class, () -> service.create(request, principal()));
        verify(rubrics, never()).save(any());
    }

    private static Question question(Long id) {
        Question question = new Question();
        question.setId(id);
        return question;
    }

    private static UserPrincipal principal() {
        User user = new User();
        user.setId(8L);
        user.setRole(com.aes.common.Role.TEACHER);
        return new UserPrincipal(user);
    }
}
