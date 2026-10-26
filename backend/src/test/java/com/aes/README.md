# Backend tests

No concrete JUnit test classes were written for this build - the sandbox
this project was built in has no internet access to Maven Central, so
there was no way to compile-and-iterate against a real Spring context
(see `docs/LIMITATIONS.md`).

`spring-boot-starter-test` and `spring-security-test` are already on the
classpath (`pom.xml`), so you can start writing tests here immediately.
Recommended first tests, in priority order:

1. `AuthServiceTest` - registration, duplicate-email rejection, login with
   wrong password.
2. `RubricServiceTest` - duplicate-rubric-per-question rejection.
3. `EvaluationServiceTest` - mock `AiServiceClient` to verify orchestration
   (matching segmented answers to questions/rubrics) and the AI-service-down
   error path (`AiServiceException` -> `AnswerSheetStatus.FAILED`).
4. `FileStorageServiceTest` - MIME type rejection, oversized file
   rejection, path traversal guard.
5. A `@SpringBootTest` + `MockMvc` slice test for at least one full
   controller (e.g. `ExamController`) to catch wiring issues a purely
   unit-level test won't (security config, validation, JSON serialization).
