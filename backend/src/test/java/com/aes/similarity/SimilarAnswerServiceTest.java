package com.aes.similarity;

import com.aes.answersheets.AnswerSheet;
import com.aes.auth.UserPrincipal;
import com.aes.common.ResourceAuthorizationService;
import com.aes.evaluations.Evaluation;
import com.aes.evaluations.EvaluationRepository;
import com.aes.exams.Exam;
import com.aes.exams.ExamService;
import com.aes.questions.Question;
import com.aes.users.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.anyLong;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class SimilarAnswerServiceTest {
    @Mock EvaluationRepository evaluations;
    @Mock SimilarAnswerResultRepository results;
    @Mock ExamService exams;
    @Mock ResourceAuthorizationService authorization;

    @Test
    void returnsOnlyPairsAboveReviewThreshold() {
        Exam exam = new Exam();
        exam.setId(1L);
        User teacher = user(7L, com.aes.common.Role.TEACHER);
        exam.setTeacher(teacher);
        Question question = new Question();
        question.setId(3L);
        question.setQuestionNumber(1);
        AnswerSheet firstSheet = sheet(10L, exam, user(20L, com.aes.common.Role.STUDENT));
        AnswerSheet secondSheet = sheet(11L, exam, user(21L, com.aes.common.Role.STUDENT));
        AnswerSheet thirdSheet = sheet(12L, exam, user(22L, com.aes.common.Role.STUDENT));
        Evaluation first = evaluation(100L, firstSheet, question, "Cloud computing enables remote access to shared servers and storage.");
        Evaluation second = evaluation(101L, secondSheet, question, "Cloud computing enables remote access to shared servers and storage.");
        Evaluation third = evaluation(102L, thirdSheet, question, "Photosynthesis uses sunlight to produce oxygen.");
        first.setMatchedConcepts("cloud, servers");
        second.setMatchedConcepts("cloud, servers");
        when(exams.findOrThrow(1L)).thenReturn(exam);
        when(evaluations.findByAnswerSheet_Exam_Id(1L)).thenReturn(List.of(first, second, third));
        when(results.findByExamIdOrderByCombinedSimilarityDesc(1L)).thenReturn(List.of());
        when(results.findByFirstEvaluationIdAndSecondEvaluationId(anyLong(), anyLong())).thenReturn(Optional.empty());
        SimilarAnswerService service = new SimilarAnswerService(evaluations, results, exams, authorization, 0.85);

        List<SimilarAnswerResponse> result = service.findSimilarAnswers(1L, new UserPrincipal(teacher));

        assertTrue(result.isEmpty());
        verify(results).save(argThat(saved -> saved.getCombinedSimilarity() >= 0.85
            && saved.getStatus() == SimilarityReviewStatus.REVIEW_REQUIRED));
    }

    private static Evaluation evaluation(Long id, AnswerSheet sheet, Question question, String answer) {
        Evaluation evaluation = new Evaluation();
        evaluation.setId(id);
        evaluation.setAnswerSheet(sheet);
        evaluation.setQuestion(question);
        evaluation.setStudentAnswerText(answer);
        return evaluation;
    }

    private static AnswerSheet sheet(Long id, Exam exam, User student) {
        AnswerSheet sheet = new AnswerSheet();
        sheet.setId(id);
        sheet.setExam(exam);
        sheet.setStudent(student);
        return sheet;
    }

    private static User user(Long id, com.aes.common.Role role) {
        User user = new User();
        user.setId(id);
        user.setRole(role);
        return user;
    }
}
