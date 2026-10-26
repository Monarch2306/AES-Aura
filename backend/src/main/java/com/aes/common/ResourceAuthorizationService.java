package com.aes.common;

import com.aes.answersheets.AnswerSheet;
import com.aes.auth.UserPrincipal;
import com.aes.exams.Exam;
import com.aes.questions.Question;
import com.aes.evaluations.Evaluation;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class ResourceAuthorizationService {

    public boolean isAdmin(UserPrincipal principal) {
        return principal != null && principal.getUser().getRole() == Role.ADMIN;
    }

    public boolean isTeacher(UserPrincipal principal) {
        return principal != null && principal.getUser().getRole() == Role.TEACHER;
    }

    public void requireExamRead(Exam exam, UserPrincipal principal) {
        if (isAdmin(principal) || isTeacherOwner(exam, principal)) {
            return;
        }
        if (principal != null && principal.getUser().getRole() == Role.STUDENT) {
            return;
        }
        deny();
    }

    public void requireExamWrite(Exam exam, UserPrincipal principal) {
        if (!isAdmin(principal) && !isTeacherOwner(exam, principal)) {
            deny();
        }
    }

    public void requireQuestionWrite(Question question, UserPrincipal principal) {
        requireExamWrite(question.getExam(), principal);
    }

    public void requireRubricRead(Question question, UserPrincipal principal) {
        requireExamWrite(question.getExam(), principal);
    }

    public void requireAnswerSheetRead(AnswerSheet sheet, UserPrincipal principal) {
        if (isAdmin(principal) || isTeacherOwner(sheet.getExam(), principal)
                || isStudentOwner(sheet, principal)) {
            return;
        }
        deny();
    }

    public void requireEvaluationRead(Evaluation evaluation, UserPrincipal principal) {
        requireAnswerSheetRead(evaluation.getAnswerSheet(), principal);
    }

    public void requireEvaluationWrite(Evaluation evaluation, UserPrincipal principal) {
        requireEvaluationWrite(evaluation.getAnswerSheet(), principal);
    }

    public void requireEvaluationWrite(AnswerSheet sheet, UserPrincipal principal) {
        if (!isAdmin(principal) && !isTeacherOwner(sheet.getExam(), principal)) {
            deny();
        }
    }

    public void requireStudentSelfOrAdmin(Long studentId, UserPrincipal principal) {
        if (!isAdmin(principal) && (principal == null || !studentId.equals(principal.getId()))) {
            deny();
        }
    }

    private boolean isTeacherOwner(Exam exam, UserPrincipal principal) {
        return isTeacher(principal) && exam.getTeacher().getId().equals(principal.getId());
    }

    private boolean isStudentOwner(AnswerSheet sheet, UserPrincipal principal) {
        return principal != null && principal.getUser().getRole() == Role.STUDENT
                && sheet.getStudent().getId().equals(principal.getId());
    }

    private void deny() {
        throw new AccessDeniedException("You are not authorized to access this resource");
    }
}
