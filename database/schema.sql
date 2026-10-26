-- AI Smart Answer Sheet Evaluation & Learning Analytics System
-- PostgreSQL schema. Spring Boot (spring.jpa.hibernate.ddl-auto=update) will
-- also create/update these tables automatically on startup; this file is
-- provided for (a) documentation, (b) manual DB provisioning, and (c)
-- reproducibility outside the Spring context.

CREATE TABLE IF NOT EXISTS users (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(255) NOT NULL,
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    role            VARCHAR(20) NOT NULL CHECK (role IN ('TEACHER','STUDENT','ADMIN')),
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','DEACTIVATED')),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS exams (
    id              BIGSERIAL PRIMARY KEY,
    title           VARCHAR(500) NOT NULL,
    subject         VARCHAR(255) NOT NULL,
    exam_date       DATE,
    teacher_id      BIGINT NOT NULL REFERENCES users(id),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_exams_teacher ON exams(teacher_id);

CREATE TABLE IF NOT EXISTS questions (
    id              BIGSERIAL PRIMARY KEY,
    exam_id         BIGINT NOT NULL REFERENCES exams(id) ON DELETE CASCADE,
    question_number INTEGER NOT NULL,
    text            TEXT NOT NULL,
    max_marks       DOUBLE PRECISION NOT NULL CHECK (max_marks > 0),
    UNIQUE (exam_id, question_number)
);
CREATE INDEX IF NOT EXISTS idx_questions_exam ON questions(exam_id);

CREATE TABLE IF NOT EXISTS rubrics (
    id                      BIGSERIAL PRIMARY KEY,
    question_id             BIGINT NOT NULL UNIQUE REFERENCES questions(id) ON DELETE CASCADE,
    reference_answer        TEXT NOT NULL,
    min_required_concepts   INTEGER NOT NULL DEFAULT 0,
    weight_semantic         DOUBLE PRECISION,
    weight_concept_coverage DOUBLE PRECISION
);

CREATE TABLE IF NOT EXISTS rubric_concepts (
    id          BIGSERIAL PRIMARY KEY,
    rubric_id   BIGINT NOT NULL REFERENCES rubrics(id) ON DELETE CASCADE,
    concept     VARCHAR(500) NOT NULL,
    weight      DOUBLE PRECISION NOT NULL DEFAULT 1.0,
    required    BOOLEAN NOT NULL DEFAULT FALSE,
    negative    BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE INDEX IF NOT EXISTS idx_rubric_concepts_rubric ON rubric_concepts(rubric_id);

CREATE TABLE IF NOT EXISTS answer_sheets (
    id                  BIGSERIAL PRIMARY KEY,
    exam_id             BIGINT NOT NULL REFERENCES exams(id) ON DELETE CASCADE,
    student_id          BIGINT NOT NULL REFERENCES users(id),
    original_file_name  VARCHAR(500) NOT NULL,
    stored_file_path    VARCHAR(1000) NOT NULL,
    mime_type           VARCHAR(100) NOT NULL,
    file_size_bytes     BIGINT NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'UPLOADED'
                        CHECK (status IN ('UPLOADED','PROCESSING','PROCESSED','FAILED')),
    raw_ocr_text        TEXT,
    ocr_confidence      DOUBLE PRECISION,
    image_quality_score DOUBLE PRECISION,
    image_blur_score    DOUBLE PRECISION,
    image_brightness_score DOUBLE PRECISION,
    image_contrast_score DOUBLE PRECISION,
    image_resolution_width INTEGER,
    image_resolution_height INTEGER,
    image_rotation_detected BOOLEAN,
    image_quality_warnings TEXT,
    processing_error    TEXT,
    total_pages         INTEGER DEFAULT 1,
    uploaded_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    processed_at        TIMESTAMPTZ
);
CREATE INDEX IF NOT EXISTS idx_answer_sheets_exam ON answer_sheets(exam_id);
CREATE INDEX IF NOT EXISTS idx_answer_sheets_student ON answer_sheets(student_id);

CREATE TABLE IF NOT EXISTS answer_pages (
    id                  BIGSERIAL PRIMARY KEY,
    answer_sheet_id     BIGINT NOT NULL REFERENCES answer_sheets(id) ON DELETE CASCADE,
    page_number         INTEGER NOT NULL,
    file_path           VARCHAR(1000) NOT NULL,
    raw_ocr_text        TEXT,
    ocr_confidence      DOUBLE PRECISION,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (answer_sheet_id, page_number)
);
CREATE INDEX IF NOT EXISTS idx_answer_pages_sheet ON answer_pages(answer_sheet_id);

CREATE TABLE IF NOT EXISTS evaluations (
    id                          BIGSERIAL PRIMARY KEY,
    answer_sheet_id             BIGINT NOT NULL REFERENCES answer_sheets(id) ON DELETE CASCADE,
    question_id                 BIGINT NOT NULL REFERENCES questions(id),
    student_answer_text         TEXT,
    marks_awarded               DOUBLE PRECISION NOT NULL,
    max_marks                   DOUBLE PRECISION NOT NULL,
    normalized_score            DOUBLE PRECISION,
    tfidf_similarity            DOUBLE PRECISION,
    semantic_similarity         DOUBLE PRECISION,
    concept_coverage            DOUBLE PRECISION,
    matched_concepts            TEXT,
    missing_concepts            TEXT,
    contradicted_concepts       TEXT,
    ocr_confidence               DOUBLE PRECISION,
    segmentation_confidence     DOUBLE PRECISION,
    overall_confidence          DOUBLE PRECISION,
    confidence_level            VARCHAR(10) CHECK (confidence_level IN ('HIGH','MEDIUM','LOW')),
    status                      VARCHAR(20) NOT NULL CHECK (status IN ('AUTO_EVALUATED','NEEDS_REVIEW','REVIEWED')),
    feedback                    TEXT,
    explanation_json            TEXT,
    original_ai_marks           DOUBLE PRECISION,
    final_marks                 DOUBLE PRECISION,
    override_reason             TEXT,
    reviewed_by_user_id         BIGINT REFERENCES users(id),
    reviewed_at                 TIMESTAMPTZ,
    evaluated_at                TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (answer_sheet_id, question_id)
);
CREATE INDEX IF NOT EXISTS idx_evaluations_sheet ON evaluations(answer_sheet_id);
CREATE INDEX IF NOT EXISTS idx_evaluations_question ON evaluations(question_id);
CREATE INDEX IF NOT EXISTS idx_evaluations_status ON evaluations(status);

CREATE TABLE IF NOT EXISTS audit_logs (
    id              BIGSERIAL PRIMARY KEY,
    actor_user_id   BIGINT REFERENCES users(id),
    action          VARCHAR(100) NOT NULL,
    entity_type     VARCHAR(100) NOT NULL,
    entity_id       BIGINT,
    details         TEXT,
    timestamp       TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_audit_logs_entity ON audit_logs(entity_type, entity_id);

CREATE TABLE IF NOT EXISTS similar_answer_results (
    id BIGSERIAL PRIMARY KEY,
    exam_id BIGINT NOT NULL REFERENCES exams(id) ON DELETE CASCADE,
    question_id BIGINT NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
    first_evaluation_id BIGINT NOT NULL REFERENCES evaluations(id) ON DELETE CASCADE,
    first_student_id BIGINT NOT NULL REFERENCES users(id),
    second_evaluation_id BIGINT NOT NULL REFERENCES evaluations(id) ON DELETE CASCADE,
    second_student_id BIGINT NOT NULL REFERENCES users(id),
    text_similarity DOUBLE PRECISION NOT NULL,
    concept_similarity DOUBLE PRECISION NOT NULL,
    combined_similarity DOUBLE PRECISION NOT NULL,
    severity VARCHAR(20) NOT NULL,
    status VARCHAR(30) NOT NULL,
    reviewed_at TIMESTAMPTZ,
    reviewed_by BIGINT REFERENCES users(id),
    review_notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(first_evaluation_id, second_evaluation_id)
);
CREATE INDEX IF NOT EXISTS idx_similar_answer_exam ON similar_answer_results(exam_id);
CREATE INDEX IF NOT EXISTS idx_similar_answer_question ON similar_answer_results(question_id);
CREATE INDEX IF NOT EXISTS idx_similar_answer_student ON similar_answer_results(first_student_id);
CREATE INDEX IF NOT EXISTS idx_similar_answer_compared_student ON similar_answer_results(second_student_id);
CREATE INDEX IF NOT EXISTS idx_similar_answer_severity ON similar_answer_results(severity);
CREATE INDEX IF NOT EXISTS idx_similar_answer_status ON similar_answer_results(status);

CREATE TABLE IF NOT EXISTS evaluation_consistency_flags (
    id BIGSERIAL PRIMARY KEY,
    exam_id BIGINT NOT NULL REFERENCES exams(id) ON DELETE CASCADE,
    question_id BIGINT NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
    evaluation_id BIGINT NOT NULL REFERENCES evaluations(id) ON DELETE CASCADE,
    compared_evaluation_id BIGINT NOT NULL REFERENCES evaluations(id) ON DELETE CASCADE,
    semantic_similarity DOUBLE PRECISION NOT NULL,
    concept_coverage_difference DOUBLE PRECISION NOT NULL,
    marks_difference DOUBLE PRECISION NOT NULL,
    severity VARCHAR(20) NOT NULL,
    reason TEXT NOT NULL,
    status VARCHAR(30) NOT NULL,
    reviewed_by BIGINT REFERENCES users(id),
    reviewed_at TIMESTAMPTZ,
    review_notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(evaluation_id, compared_evaluation_id)
);
CREATE INDEX IF NOT EXISTS idx_consistency_exam ON evaluation_consistency_flags(exam_id);
CREATE INDEX IF NOT EXISTS idx_consistency_question ON evaluation_consistency_flags(question_id);
CREATE INDEX IF NOT EXISTS idx_consistency_status ON evaluation_consistency_flags(status);

-- NOTE on schema scope: the original spec additionally names `subjects`,
-- `teachers`, `students`, `answer_pages`, `ocr_results`, `feedback` and
-- `teacher_reviews` as separate tables. For this build, `subjects` is
-- collapsed into `exams.subject` (a string), `teachers`/`students` are
-- represented by `users.role`, `answer_pages`/`ocr_results` are collapsed
-- into `answer_sheets` (single-image sheets, one OCR result each), and
-- `teacher_reviews`/`feedback` are collapsed into columns directly on
-- `evaluations` (original_ai_marks, final_marks, override_reason,
-- reviewed_by_user_id, reviewed_at, feedback). This keeps the schema
-- normalized enough to support every required query while avoiding
-- unnecessary joins for a system this size. Splitting these back out is
-- straightforward if the team needs multi-page sheets or multiple review
-- rounds per evaluation.
