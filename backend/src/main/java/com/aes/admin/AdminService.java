package com.aes.admin;

import com.aes.aiclient.AiServiceClient;
import com.aes.answersheets.AnswerSheetRepository;
import com.aes.answersheets.AnswerSheetStatus;
import com.aes.audit.AuditLog;
import com.aes.audit.AuditLogRepository;
import com.aes.audit.AuditService;
import com.aes.auth.UserPrincipal;
import com.aes.common.ResourceAuthorizationService;
import com.aes.common.Role;
import com.aes.consistency.ConsistencyFlagRepository;
import com.aes.evaluations.EvaluationRepository;
import com.aes.evaluations.EvaluationStatus;
import com.aes.exams.ExamRepository;
import com.aes.questions.QuestionRepository;
import com.aes.similarity.SimilarAnswerResultRepository;
import com.aes.users.User;
import com.aes.users.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

@Service
@SuppressWarnings("null")
public class AdminService {
	private final UserRepository userRepository;
	private final ExamRepository examRepository;
	private final QuestionRepository questionRepository;
	private final AnswerSheetRepository answerSheetRepository;
	private final EvaluationRepository evaluationRepository;
	private final SimilarAnswerResultRepository similarityRepository;
	private final ConsistencyFlagRepository consistencyRepository;
	private final AuditLogRepository auditRepository;
	private final AuditService auditService;
	private final AiServiceClient aiServiceClient;
	private final ResourceAuthorizationService authorizationService;
	private final Path storagePath;
	private final String applicationVersion;
	private final String environment;

	public AdminService(UserRepository userRepository, ExamRepository examRepository,
						QuestionRepository questionRepository, AnswerSheetRepository answerSheetRepository,
						EvaluationRepository evaluationRepository, SimilarAnswerResultRepository similarityRepository,
						ConsistencyFlagRepository consistencyRepository, AuditLogRepository auditRepository,
						AuditService auditService, AiServiceClient aiServiceClient,
						ResourceAuthorizationService authorizationService,
						@Value("${app.upload.storage-dir:./uploads}") String storageDir,
						@Value("${app.version:1.0.0}") String applicationVersion,
						@Value("${spring.profiles.active:h2}") String environment) {
		this.userRepository = userRepository;
		this.examRepository = examRepository;
		this.questionRepository = questionRepository;
		this.answerSheetRepository = answerSheetRepository;
		this.evaluationRepository = evaluationRepository;
		this.similarityRepository = similarityRepository;
		this.consistencyRepository = consistencyRepository;
		this.auditRepository = auditRepository;
		this.auditService = auditService;
		this.aiServiceClient = aiServiceClient;
		this.authorizationService = authorizationService;
		this.storagePath = Path.of(storageDir);
		this.applicationVersion = applicationVersion;
		this.environment = environment;
	}

	public AdminDashboardResponse dashboard(UserPrincipal principal) {
		requireAdmin(principal);
		return new AdminDashboardResponse(
				userRepository.count(), userRepository.countByRole(Role.STUDENT), userRepository.countByRole(Role.TEACHER),
				userRepository.countByRole(Role.ADMIN), examRepository.count(), questionRepository.count(),
				answerSheetRepository.count(), evaluationRepository.count(), evaluationRepository.countByStatus(EvaluationStatus.NEEDS_REVIEW),
				answerSheetRepository.countByStatus(AnswerSheetStatus.FAILED), similarityRepository.count(), consistencyRepository.count(), null);
	}

	public List<AdminUserResponse> users(String search, int page, int size, UserPrincipal principal) {
		requireAdmin(principal);
		PageRequest request = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 100));
		List<User> users = search == null || search.isBlank()
			? userRepository.findAll(request).getContent()
				: userRepository.findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(search, search, request);
		return users.stream().map(user -> new AdminUserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole(), user.getStatus(), user.getCreatedAt())).toList();
	}

	public List<AdminAuditResponse> auditLogs(String search, int page, int size, UserPrincipal principal) {
		requireAdmin(principal);
		PageRequest request = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 100));
		List<AuditLog> logs = search == null || search.isBlank()
			? auditRepository.findAll(request).getContent()
				: auditRepository.findByActionContainingIgnoreCaseOrEntityTypeContainingIgnoreCase(search, search, request);
		return logs.stream().map(log -> new AdminAuditResponse(log.getId(), log.getActorUserId(), log.getAction(),
				log.getEntityType(), log.getEntityId(), log.getDetails(), log.getTimestamp())).toList();
	}

	@Transactional
	public AdminUserResponse changeRole(Long userId, Role role, UserPrincipal principal) {
		requireAdmin(principal);
		User user = userRepository.findById(userId).orElseThrow();
		Role oldRole = user.getRole();
		user.setRole(role);
		User saved = userRepository.save(user);
		auditService.log(principal.getId(), "USER_ROLE_CHANGED", "User", userId, oldRole + " -> " + role);
		return new AdminUserResponse(saved.getId(), saved.getName(), saved.getEmail(), saved.getRole(), saved.getStatus(), saved.getCreatedAt());
	}

	@Transactional
	public AdminUserResponse changeStatus(Long userId, com.aes.users.UserStatus status, UserPrincipal principal) {
		requireAdmin(principal);
		User user = userRepository.findById(userId).orElseThrow();
		com.aes.users.UserStatus oldStatus = user.getStatus();
		user.setStatus(status);
		User saved = userRepository.save(user);
		auditService.log(principal.getId(), "USER_STATUS_CHANGED", "User", userId, oldStatus + " -> " + status);
		return new AdminUserResponse(saved.getId(), saved.getName(), saved.getEmail(), saved.getRole(), saved.getStatus(), saved.getCreatedAt());
	}

	public SystemHealthResponse health(UserPrincipal principal) {
		requireAdmin(principal);
		String database;
		try {
			userRepository.count();
			database = "ONLINE";
		} catch (Exception ignored) {
			database = "OFFLINE";
		}
		boolean aiOnline = aiServiceClient.isHealthy();
		boolean storageOnline = Files.isDirectory(storagePath) && Files.isWritable(storagePath);
		return new SystemHealthResponse("ONLINE", database, aiOnline ? "ONLINE" : "OFFLINE",
				aiOnline ? "AVAILABLE" : "UNAVAILABLE", aiOnline ? "AVAILABLE" : "UNAVAILABLE",
				storageOnline ? "AVAILABLE" : "UNAVAILABLE", applicationVersion, environment, Instant.now());
	}

	private void requireAdmin(UserPrincipal principal) {
		if (!authorizationService.isAdmin(principal)) {
			throw new org.springframework.security.access.AccessDeniedException("Administrator access required");
		}
	}
}
