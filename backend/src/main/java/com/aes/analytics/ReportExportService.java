package com.aes.analytics;

import com.aes.analytics.dto.ExamAnalyticsResponse;
import com.aes.analytics.dto.StudentAnalyticsResponse;
import com.aes.answersheets.AnswerSheet;
import com.aes.answersheets.AnswerSheetRepository;
import com.aes.auth.UserPrincipal;
import com.aes.evaluations.Evaluation;
import com.aes.evaluations.EvaluationRepository;
import com.aes.exams.Exam;
import com.aes.exams.ExamService;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
@SuppressWarnings("null")
public class ReportExportService {

    private final AnalyticsService analyticsService;
    private final ExamService examService;
    private final AnswerSheetRepository answerSheetRepository;
    private final EvaluationRepository evaluationRepository;

    public ReportExportService(AnalyticsService analyticsService, ExamService examService,
                               AnswerSheetRepository answerSheetRepository, EvaluationRepository evaluationRepository) {
        this.analyticsService = analyticsService;
        this.examService = examService;
        this.answerSheetRepository = answerSheetRepository;
        this.evaluationRepository = evaluationRepository;
    }

    public byte[] generateExamCsv(Long examId, UserPrincipal principal) {
        Exam exam = examService.findOrThrow(examId);
        List<AnswerSheet> sheets = answerSheetRepository.findByExamId(examId);

        StringBuilder sb = new StringBuilder();
        sb.append("Exam ID,Exam Title,Subject,Student ID,Student Name,Answer Sheet Status,Total Marks,Evaluated Qs,Quality Score\n");

        for (AnswerSheet sheet : sheets) {
            List<Evaluation> evals = evaluationRepository.findByAnswerSheetId(sheet.getId());
            double totalMarks = evals.stream().mapToDouble(Evaluation::getMarksAwarded).sum();
            int evaluatedQs = evals.size();
            Double qualityScore = sheet.getImageQualityScore();

            sb.append(exam.getId()).append(",")
              .append(escapeCsv(exam.getTitle())).append(",")
              .append(escapeCsv(exam.getSubject())).append(",")
              .append(sheet.getStudent().getId()).append(",")
              .append(escapeCsv(sheet.getStudent().getName())).append(",")
              .append(sheet.getStatus()).append(",")
              .append(String.format("%.2f", totalMarks)).append(",")
              .append(evaluatedQs).append(",")
              .append(qualityScore != null ? String.format("%.2f", qualityScore) : "N/A")
              .append("\n");
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    public byte[] generateExamReportTxt(Long examId, UserPrincipal principal) {
        ExamAnalyticsResponse analytics = analyticsService.examAnalytics(examId, principal);
        Exam exam = examService.findOrThrow(examId);

        StringBuilder sb = new StringBuilder();
        sb.append("====================================================\n");
        sb.append("      AES-QA ACADEMIC EXAM ANALYTICS REPORT         \n");
        sb.append("====================================================\n\n");
        sb.append("Exam ID: ").append(analytics.examId()).append("\n");
        sb.append("Title: ").append(exam.getTitle()).append("\n");
        sb.append("Subject: ").append(exam.getSubject()).append("\n");
        sb.append("Total Students: ").append(analytics.studentCount()).append("\n");
        sb.append("Evaluated Answer Sheets: ").append(analytics.evaluatedAnswerCount()).append("\n");
        sb.append("Average Percentage: ").append(formatPercent(analytics.averagePercent())).append("\n");
        sb.append("Median Percentage: ").append(formatPercent(analytics.medianPercent())).append("\n");
        sb.append("Highest Percentage: ").append(formatPercent(analytics.highestPercent())).append("\n");
        sb.append("Lowest Percentage: ").append(formatPercent(analytics.lowestPercent())).append("\n\n");

        sb.append("--- Question Performance & Difficulty ---\n");
        if (analytics.questions() != null) {
            analytics.questions().forEach(q -> {
                sb.append(String.format("Q%d (%s): Avg Marks = %s/%s (Difficulty: %s)\n",
                        q.questionNumber(), q.questionText(), formatDouble(q.averageMarks()), formatDouble(q.maxMarks()), q.difficulty()));
            });
        }

        sb.append("\n--- Pedagogical Insights ---\n");
        if (analytics.insights() != null) {
            analytics.insights().forEach(obs -> sb.append("- ").append(obs).append("\n"));
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    public byte[] generateStudentReportTxt(Long studentId, UserPrincipal principal) {
        StudentAnalyticsResponse analytics = analyticsService.studentAnalytics(studentId, principal);
        StringBuilder sb = new StringBuilder();
        sb.append("====================================================\n");
        sb.append("       AES-QA STUDENT PERFORMANCE REPORT            \n");
        sb.append("====================================================\n\n");
        sb.append("Student ID: ").append(analytics.studentId()).append("\n");
        sb.append("Total Submissions: ").append(analytics.totalAnswerSheets()).append("\n");
        sb.append("Average Percentage: ").append(formatPercent(analytics.averageMarksPercent())).append("\n");
        sb.append("Highest Percentage: ").append(formatPercent(analytics.highestScorePercent())).append("\n\n");

        sb.append("--- Strengths & Weaknesses ---\n");
        sb.append("Strong Concepts: ").append(analytics.strongConcepts() != null ? String.join(", ", analytics.strongConcepts()) : "None").append("\n");
        sb.append("Weak Concepts: ").append(analytics.topWeakConcepts() != null ? String.join(", ", analytics.topWeakConcepts()) : "None").append("\n\n");

        sb.append("--- Learning Recommendations ---\n");
        if (analytics.recommendations() != null) {
            analytics.recommendations().forEach(rec -> {
                sb.append(String.format("[%s] Concept: %s (Priority: %s, Mastery: %s)\n    Reason: %s\n",
                        rec.concept(), rec.concept(), rec.priority(), formatPercent(rec.masteryPercent()), rec.reason()));
            });
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String formatPercent(Double value) {
        return value != null ? String.format("%.2f%%", value) : "N/A";
    }

    private String formatDouble(Double value) {
        return value != null ? String.format("%.2f", value) : "0.00";
    }

    private String escapeCsv(String input) {
        if (input == null) return "";
        if (input.contains(",") || input.contains("\"") || input.contains("\n")) {
            return "\"" + input.replace("\"", "\"\"") + "\"";
        }
        return input;
    }
}
