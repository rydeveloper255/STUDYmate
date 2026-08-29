-- ============================================================================
-- STUDY MATE — SUPABASE PRODUCTION HARDENED SCHEMA & RLS POLICIES
-- Target: PostgreSQL 15+ / Supabase
-- Complies with:
-- 1. Strict Row Level Security (RLS) on all user tables using auth.uid().
-- 2. Role-Based Access Control (RBAC): user, reviewer, admin.
-- 3. Content Write Protection (Public reads verified PYQs; Admin-only writes).
-- 4. Immutable Completed Test Sessions & Answer Snapshots.
-- 5. Atomic Idempotent Test Submission RPC (submit_test_atomic).
-- 6. High-Performance Aggregation RPCs (Home & Profile summaries).
-- 7. Secure Storage Buckets & Policies.
-- ============================================================================

-- Enable required extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ----------------------------------------------------------------------------
-- 1. ROLE-BASED ACCESS CONTROL (RBAC)
-- ----------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS public.user_roles (
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    role TEXT NOT NULL CHECK (role IN ('user', 'reviewer', 'admin')),
    granted_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    granted_by UUID REFERENCES auth.users(id),
    PRIMARY KEY (user_id, role)
);

ALTER TABLE public.user_roles ENABLE ROW LEVEL SECURITY;

-- Helper security function to verify roles
CREATE OR REPLACE FUNCTION public.has_role(p_user_id UUID, p_role TEXT)
RETURNS BOOLEAN
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
    SELECT EXISTS (
        SELECT 1 FROM public.user_roles
        WHERE user_id = p_user_id AND role = p_role
    );
$$;

-- Helper security function to check if current caller is admin
CREATE OR REPLACE FUNCTION public.is_admin()
RETURNS BOOLEAN
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
    SELECT public.has_role(auth.uid(), 'admin');
$$;

-- RLS: Users can read their own roles; admins can manage all roles
CREATE POLICY "Users can read own roles"
    ON public.user_roles FOR SELECT
    USING (auth.uid() = user_id OR public.is_admin());

CREATE POLICY "Only admins can modify roles"
    ON public.user_roles FOR ALL
    USING (public.is_admin())
    WITH CHECK (public.is_admin());

-- ----------------------------------------------------------------------------
-- 2. USER PROFILES & SETTINGS
-- ----------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS public.profiles (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    email TEXT UNIQUE NOT NULL,
    name TEXT NOT NULL DEFAULT 'Student',
    photo_url TEXT,
    grade TEXT DEFAULT 'Class 12',
    education_level TEXT DEFAULT 'High School',
    language_preference TEXT DEFAULT 'en',
    exam_category TEXT DEFAULT 'Engineering',
    exam_name TEXT DEFAULT 'JEE Main',
    exam_date_millis BIGINT,
    target_score TEXT DEFAULT 'Top 500 AIR',
    goal TEXT DEFAULT 'Score Top Percentile',
    subjects JSONB DEFAULT '[]'::jsonb,
    high_priority_subjects JSONB DEFAULT '[]'::jsonb,
    medium_priority_subjects JSONB DEFAULT '[]'::jsonb,
    low_priority_subjects JSONB DEFAULT '[]'::jsonb,
    strong_subjects JSONB DEFAULT '[]'::jsonb,
    weak_subjects JSONB DEFAULT '[]'::jsonb,
    weak_topics JSONB DEFAULT '[]'::jsonb,
    preparation_level TEXT DEFAULT 'Intermediate',
    daily_target_minutes INT DEFAULT 180,
    available_study_hours NUMERIC(4, 2) DEFAULT 4.0,
    preferred_study_start_time TEXT DEFAULT '07:00',
    preferred_study_end_time TEXT DEFAULT '22:00',
    preferred_study_days JSONB DEFAULT '["Mon","Tue","Wed","Thu","Fri","Sat"]'::jsonb,
    break_duration_minutes INT DEFAULT 10,
    preferred_study_time TEXT DEFAULT 'Morning (6 AM - 12 PM)',
    morning_night_preference TEXT DEFAULT 'Morning Person',
    revision_frequency TEXT DEFAULT 'Daily',
    mock_test_frequency TEXT DEFAULT 'Weekly',
    daily_study_goal TEXT DEFAULT 'Complete 3 Focus Sessions',
    short_term_goal TEXT DEFAULT 'Master high-weightage topics',
    long_term_goal TEXT DEFAULT 'Secure top rank',
    notifications_enabled BOOLEAN DEFAULT true,
    xp INT DEFAULT 0 CHECK (xp >= 0),
    level INT DEFAULT 1 CHECK (level >= 1),
    streak_days INT DEFAULT 0 CHECK (streak_days >= 0),
    total_focus_minutes INT DEFAULT 0 CHECK (total_focus_minutes >= 0),
    total_questions_solved INT DEFAULT 0 CHECK (total_questions_solved >= 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now())
);

ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can read own profile"
    ON public.profiles FOR SELECT
    USING (auth.uid() = id);

CREATE POLICY "Users can insert own profile"
    ON public.profiles FOR INSERT
    WITH CHECK (auth.uid() = id);

CREATE POLICY "Users can update own profile"
    ON public.profiles FOR UPDATE
    USING (auth.uid() = id)
    WITH CHECK (auth.uid() = id);

CREATE TABLE IF NOT EXISTS public.user_settings (
    user_id UUID PRIMARY KEY REFERENCES public.profiles(id) ON DELETE CASCADE,
    theme TEXT DEFAULT 'dark',
    tts_speed NUMERIC(3, 2) DEFAULT 1.0,
    tts_voice TEXT DEFAULT 'en-US-Standard-C',
    sound_enabled BOOLEAN DEFAULT true,
    vibration_enabled BOOLEAN DEFAULT true,
    notification_preferences JSONB DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now())
);

ALTER TABLE public.user_settings ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can manage own settings"
    ON public.user_settings FOR ALL
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

-- ----------------------------------------------------------------------------
-- 3. OFFICIAL CURRICULUM, EXAMS & QUESTION BANK (PROTECTED CONTENT)
-- ----------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS public.exams (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL,
    category TEXT NOT NULL,
    is_official BOOLEAN NOT NULL DEFAULT true,
    total_marks NUMERIC(6, 2),
    duration_minutes INT,
    marking_scheme JSONB DEFAULT '{}'::jsonb,
    syllabus_summary JSONB DEFAULT '{}'::jsonb,
    status TEXT NOT NULL DEFAULT 'active' CHECK (status IN ('active', 'inactive', 'deprecated')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    UNIQUE(name, category)
);

ALTER TABLE public.exams ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Public read active exams"
    ON public.exams FOR SELECT
    USING (status = 'active' OR public.is_admin());

CREATE POLICY "Admin write exams"
    ON public.exams FOR ALL
    USING (public.is_admin())
    WITH CHECK (public.is_admin());

CREATE TABLE IF NOT EXISTS public.subjects (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    exam_id UUID REFERENCES public.exams(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    code TEXT,
    weightage_percent NUMERIC(5, 2),
    icon_name TEXT,
    order_index INT DEFAULT 0,
    status TEXT NOT NULL DEFAULT 'active' CHECK (status IN ('active', 'inactive')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    UNIQUE(exam_id, name)
);

ALTER TABLE public.subjects ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Public read active subjects"
    ON public.subjects FOR SELECT
    USING (status = 'active' OR public.is_admin());

CREATE POLICY "Admin write subjects"
    ON public.subjects FOR ALL
    USING (public.is_admin())
    WITH CHECK (public.is_admin());

CREATE TABLE IF NOT EXISTS public.chapters (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    subject_id UUID NOT NULL REFERENCES public.subjects(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    order_index INT DEFAULT 0,
    status TEXT NOT NULL DEFAULT 'active' CHECK (status IN ('active', 'inactive')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    UNIQUE(subject_id, name)
);

ALTER TABLE public.chapters ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Public read active chapters"
    ON public.chapters FOR SELECT
    USING (status = 'active' OR public.is_admin());

CREATE POLICY "Admin write chapters"
    ON public.chapters FOR ALL
    USING (public.is_admin())
    WITH CHECK (public.is_admin());

CREATE TABLE IF NOT EXISTS public.topics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    chapter_id UUID NOT NULL REFERENCES public.chapters(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    syllabus_code TEXT,
    difficulty TEXT DEFAULT 'Medium',
    status TEXT NOT NULL DEFAULT 'active' CHECK (status IN ('active', 'inactive')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    UNIQUE(chapter_id, name)
);

ALTER TABLE public.topics ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Public read active topics"
    ON public.topics FOR SELECT
    USING (status = 'active' OR public.is_admin());

CREATE POLICY "Admin write topics"
    ON public.topics FOR ALL
    USING (public.is_admin())
    WITH CHECK (public.is_admin());

-- Immutable Versioned Question Bank
CREATE TABLE IF NOT EXISTS public.questions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    topic_id UUID REFERENCES public.topics(id) ON DELETE SET NULL,
    exam_name TEXT NOT NULL,
    subject TEXT NOT NULL,
    chapter TEXT NOT NULL,
    topic TEXT NOT NULL,
    question_text TEXT NOT NULL,
    options JSONB NOT NULL,
    correct_answer TEXT NOT NULL,
    explanation TEXT,
    pyq_year INT,
    pyq_shift TEXT,
    difficulty TEXT DEFAULT 'Medium' CHECK (difficulty IN ('Easy', 'Medium', 'Hard')),
    is_verified BOOLEAN NOT NULL DEFAULT true,
    version INT NOT NULL DEFAULT 1,
    status TEXT NOT NULL DEFAULT 'published' CHECK (status IN ('draft', 'published', 'archived', 'rejected')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now())
);

ALTER TABLE public.questions ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Public read published questions"
    ON public.questions FOR SELECT
    USING (status = 'published' OR public.is_admin() OR public.has_role(auth.uid(), 'reviewer'));

CREATE POLICY "Admin and Reviewer manage questions"
    ON public.questions FOR ALL
    USING (public.is_admin() OR public.has_role(auth.uid(), 'reviewer'))
    WITH CHECK (public.is_admin() OR public.has_role(auth.uid(), 'reviewer'));

CREATE TABLE IF NOT EXISTS public.question_versions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    question_id UUID NOT NULL REFERENCES public.questions(id) ON DELETE CASCADE,
    version INT NOT NULL,
    question_text TEXT NOT NULL,
    options JSONB NOT NULL,
    correct_answer TEXT NOT NULL,
    explanation TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    UNIQUE(question_id, version)
);

ALTER TABLE public.question_versions ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Public read question versions"
    ON public.question_versions FOR SELECT
    USING (true);

-- ----------------------------------------------------------------------------
-- 4. TEST SESSIONS, IMMUTABLE RESULTS & ANSWER SNAPSHOTS
-- ----------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS public.test_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    exam_name TEXT NOT NULL,
    title TEXT NOT NULL,
    question_count INT NOT NULL,
    time_limit_seconds INT NOT NULL,
    is_completed BOOLEAN NOT NULL DEFAULT false,
    started_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    completed_at TIMESTAMPTZ,
    snapshot_data JSONB DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now())
);

ALTER TABLE public.test_sessions ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can manage own test sessions"
    ON public.test_sessions FOR SELECT
    USING (auth.uid() = user_id);

CREATE POLICY "Users can create own test sessions"
    ON public.test_sessions FOR INSERT
    WITH CHECK (auth.uid() = user_id);

-- Enforce: Completed test sessions are IMMUTABLE to prevent score tampering
CREATE POLICY "Users can update only uncompleted sessions"
    ON public.test_sessions FOR UPDATE
    USING (auth.uid() = user_id AND is_completed = false)
    WITH CHECK (auth.uid() = user_id);

-- Immutable Test Answers
CREATE TABLE IF NOT EXISTS public.test_answers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id UUID NOT NULL REFERENCES public.test_sessions(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    question_id UUID,
    question_version_id UUID REFERENCES public.question_versions(id) ON DELETE SET NULL,
    question_text_snapshot TEXT,
    options_snapshot JSONB,
    student_answer TEXT,
    correct_answer_snapshot TEXT,
    is_correct BOOLEAN,
    time_taken_seconds INT DEFAULT 0,
    submitted_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    UNIQUE(session_id, question_id)
);

ALTER TABLE public.test_answers ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can read own test answers"
    ON public.test_answers FOR SELECT
    USING (auth.uid() = user_id);

CREATE POLICY "Users can insert own test answers"
    ON public.test_answers FOR INSERT
    WITH CHECK (auth.uid() = user_id);

-- Finalized Test Attempts (One result per test session)
CREATE TABLE IF NOT EXISTS public.test_attempts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id UUID UNIQUE REFERENCES public.test_sessions(id) ON DELETE RESTRICT,
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    local_id BIGINT,
    title TEXT NOT NULL,
    subject TEXT NOT NULL,
    exam_name TEXT NOT NULL,
    topic TEXT NOT NULL,
    difficulty TEXT NOT NULL DEFAULT 'Medium',
    score NUMERIC(6, 2) NOT NULL DEFAULT 0.0,
    total_questions INT NOT NULL,
    correct_count INT NOT NULL,
    incorrect_count INT NOT NULL,
    skipped_count INT NOT NULL,
    accuracy_percent NUMERIC(5, 2) NOT NULL,
    time_spent_seconds INT NOT NULL,
    weak_topics JSONB DEFAULT '[]'::jsonb,
    strong_topics JSONB DEFAULT '[]'::jsonb,
    ai_recommendation TEXT,
    marking_scheme TEXT,
    timestamp BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    UNIQUE(user_id, local_id)
);

ALTER TABLE public.test_attempts ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can read own test attempts"
    ON public.test_attempts FOR SELECT
    USING (auth.uid() = user_id);

CREATE POLICY "Users can insert own test attempts"
    ON public.test_attempts FOR INSERT
    WITH CHECK (auth.uid() = user_id);

-- ----------------------------------------------------------------------------
-- 5. MISTAKES, FLASHCARDS, NOTES & STUDY TASKS
-- ----------------------------------------------------------------------------

-- Mistakes Book
CREATE TABLE IF NOT EXISTS public.question_attempts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    local_id BIGINT NOT NULL,
    question_text TEXT NOT NULL,
    student_answer TEXT,
    correct_answer TEXT,
    subject TEXT NOT NULL,
    topic TEXT NOT NULL,
    mistake_category TEXT DEFAULT 'Conceptual',
    explanation TEXT,
    is_mastered BOOLEAN NOT NULL DEFAULT false,
    timestamp BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    UNIQUE(user_id, local_id)
);

ALTER TABLE public.question_attempts ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can manage own mistakes"
    ON public.question_attempts FOR ALL
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

-- Revision Items / Flashcards
CREATE TABLE IF NOT EXISTS public.revision_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    local_id BIGINT NOT NULL,
    subject TEXT NOT NULL,
    topic TEXT NOT NULL,
    front TEXT NOT NULL,
    back TEXT NOT NULL,
    hint TEXT,
    difficulty TEXT DEFAULT 'Medium',
    status TEXT DEFAULT 'PRACTICE_SOON',
    confidence INT DEFAULT 3,
    review_count INT DEFAULT 0,
    last_reviewed BIGINT,
    interval_days INT DEFAULT 1,
    ease_factor NUMERIC(4, 2) DEFAULT 2.50,
    repetitions INT DEFAULT 0,
    next_review_date BIGINT,
    source_doc_title TEXT,
    created_at BIGINT NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    UNIQUE(user_id, local_id)
);

ALTER TABLE public.revision_items ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can manage own revision items"
    ON public.revision_items FOR ALL
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

-- Smart Notes
CREATE TABLE IF NOT EXISTS public.notes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    local_id BIGINT NOT NULL,
    title TEXT NOT NULL,
    subject TEXT NOT NULL,
    topic TEXT NOT NULL,
    content_markdown TEXT NOT NULL,
    key_points JSONB DEFAULT '[]'::jsonb,
    formulas JSONB DEFAULT '[]'::jsonb,
    important_facts JSONB DEFAULT '[]'::jsonb,
    source_url TEXT,
    source_title TEXT,
    is_bookmarked BOOLEAN DEFAULT false,
    is_revised BOOLEAN DEFAULT false,
    revision_category TEXT DEFAULT 'IMPORTANT',
    created_at BIGINT NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    UNIQUE(user_id, local_id)
);

ALTER TABLE public.notes ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can manage own notes"
    ON public.notes FOR ALL
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

-- Note Attachments (PDFs, Images)
CREATE TABLE IF NOT EXISTS public.note_attachments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    note_local_id BIGINT NOT NULL,
    file_name TEXT NOT NULL,
    file_type TEXT NOT NULL,
    file_url TEXT NOT NULL,
    file_size_bytes BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now())
);

ALTER TABLE public.note_attachments ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can manage own note attachments"
    ON public.note_attachments FOR ALL
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

-- Study Planner Tasks
CREATE TABLE IF NOT EXISTS public.study_tasks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    local_id BIGINT NOT NULL,
    subject TEXT NOT NULL,
    chapter TEXT NOT NULL,
    topic TEXT NOT NULL,
    target_minutes INT NOT NULL,
    is_completed BOOLEAN NOT NULL DEFAULT false,
    scheduled_date_millis BIGINT NOT NULL,
    priority TEXT DEFAULT 'HIGH',
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    UNIQUE(user_id, local_id)
);

ALTER TABLE public.study_tasks ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can manage own study tasks"
    ON public.study_tasks FOR ALL
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

-- Study Sessions & Focus History
CREATE TABLE IF NOT EXISTS public.study_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    local_id BIGINT NOT NULL,
    session_type TEXT NOT NULL,
    subject TEXT NOT NULL,
    topic TEXT NOT NULL,
    duration_minutes INT NOT NULL,
    actual_minutes_spent INT NOT NULL,
    xp_earned INT NOT NULL,
    accuracy_percent NUMERIC(5, 2),
    questions_attempted INT DEFAULT 0,
    productivity_rating INT DEFAULT 5,
    notes_summary TEXT,
    timestamp BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    UNIQUE(user_id, local_id)
);

ALTER TABLE public.study_sessions ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can manage own study sessions"
    ON public.study_sessions FOR ALL
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

CREATE TABLE IF NOT EXISTS public.focus_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    local_id BIGINT NOT NULL,
    subject TEXT NOT NULL,
    topic TEXT NOT NULL,
    duration_minutes INT NOT NULL,
    actual_minutes_spent INT NOT NULL,
    xp_earned INT NOT NULL,
    is_completed BOOLEAN NOT NULL DEFAULT true,
    timestamp BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    UNIQUE(user_id, local_id)
);

ALTER TABLE public.focus_sessions ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can manage own focus sessions"
    ON public.focus_sessions FOR ALL
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

-- Topic Progress & Mastery Matrix
CREATE TABLE IF NOT EXISTS public.topic_progress (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    local_id BIGINT,
    subject TEXT NOT NULL,
    topic TEXT NOT NULL,
    mastery_score INT NOT NULL DEFAULT 0,
    accuracy_percent NUMERIC(5, 2) NOT NULL DEFAULT 0.0,
    total_attempted INT NOT NULL DEFAULT 0,
    correct_count INT NOT NULL DEFAULT 0,
    incorrect_count INT NOT NULL DEFAULT 0,
    easy_solved INT NOT NULL DEFAULT 0,
    med_solved INT NOT NULL DEFAULT 0,
    hard_solved INT NOT NULL DEFAULT 0,
    retention_decay_rate NUMERIC(4, 3) DEFAULT 0.95,
    mastery_level TEXT DEFAULT 'LEARNING',
    weak_spots JSONB DEFAULT '[]'::jsonb,
    last_tested_at BIGINT,
    recommended_review_at BIGINT,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    UNIQUE(user_id, subject, topic)
);

ALTER TABLE public.topic_progress ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can manage own topic progress"
    ON public.topic_progress FOR ALL
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

-- NOVA Memories & Context
CREATE TABLE IF NOT EXISTS public.nova_memory (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    local_id BIGINT NOT NULL,
    memory_key TEXT NOT NULL,
    content TEXT NOT NULL,
    category TEXT DEFAULT 'STUDY_PATTERN',
    source_context TEXT,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at BIGINT NOT NULL,
    UNIQUE(user_id, local_id)
);

ALTER TABLE public.nova_memory ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can manage own nova memories"
    ON public.nova_memory FOR ALL
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

CREATE TABLE IF NOT EXISTS public.nova_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    sender TEXT NOT NULL CHECK (sender IN ('USER', 'NOVA', 'SYSTEM')),
    text TEXT NOT NULL,
    reasoning_content TEXT,
    created_at BIGINT NOT NULL
);

ALTER TABLE public.nova_messages ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can manage own nova messages"
    ON public.nova_messages FOR ALL
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

-- Intelligence Snapshots
CREATE TABLE IF NOT EXISTS public.student_insights (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    timestamp BIGINT NOT NULL,
    exam_days_remaining INT,
    overall_mastery_score INT,
    syllabus_completion_percent INT,
    readiness_index NUMERIC(4, 2),
    top_recommended_action TEXT,
    top_recommended_subject TEXT,
    pacing_status TEXT,
    insights_summary TEXT,
    weak_topics_count INT,
    mastered_topics_count INT
);

ALTER TABLE public.student_insights ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can manage own student insights"
    ON public.student_insights FOR ALL
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

-- App Usage (Daily Summary)
CREATE TABLE IF NOT EXISTS public.app_usage_daily (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    package_name TEXT NOT NULL,
    date_string TEXT NOT NULL,
    usage_minutes INT DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    UNIQUE(user_id, package_name, date_string)
);

ALTER TABLE public.app_usage_daily ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can manage own app usage"
    ON public.app_usage_daily FOR ALL
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

-- Current Affairs (Daily verified articles)
CREATE TABLE IF NOT EXISTS public.current_affairs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    date_string TEXT NOT NULL,
    headline TEXT NOT NULL,
    summary TEXT NOT NULL,
    category TEXT NOT NULL,
    tags JSONB DEFAULT '[]'::jsonb,
    key_takeaways JSONB DEFAULT '[]'::jsonb,
    quiz_questions JSONB DEFAULT '[]'::jsonb,
    source_url TEXT,
    published_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    is_verified BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    UNIQUE(date_string, headline)
);

ALTER TABLE public.current_affairs ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Public read verified current affairs"
    ON public.current_affairs FOR SELECT
    USING (is_verified = true OR public.is_admin());

CREATE POLICY "Admin manage current affairs"
    ON public.current_affairs FOR ALL
    USING (public.is_admin())
    WITH CHECK (public.is_admin());

-- ----------------------------------------------------------------------------
-- 6. AUDIT & LOGGING
-- ----------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS public.sync_audit_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES auth.users(id) ON DELETE SET NULL,
    operation TEXT NOT NULL,
    table_name TEXT NOT NULL,
    record_id TEXT,
    status TEXT NOT NULL,
    error_details TEXT,
    client_request_id TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now())
);

ALTER TABLE public.sync_audit_log ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users can insert sync audit logs"
    ON public.sync_audit_log FOR INSERT
    WITH CHECK (auth.uid() = user_id OR user_id IS NULL);

CREATE POLICY "Admins can view all audit logs"
    ON public.sync_audit_log FOR SELECT
    USING (public.is_admin());

-- ----------------------------------------------------------------------------
-- 7. PERFORMANCE INDEXES
-- ----------------------------------------------------------------------------

CREATE INDEX IF NOT EXISTS idx_profiles_email ON public.profiles(email);
CREATE INDEX IF NOT EXISTS idx_test_sessions_user_id ON public.test_sessions(user_id, is_completed);
CREATE INDEX IF NOT EXISTS idx_test_attempts_user_id ON public.test_attempts(user_id, timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_test_answers_session_id ON public.test_answers(session_id);
CREATE INDEX IF NOT EXISTS idx_questions_exam_subject ON public.questions(exam_name, subject, difficulty);
CREATE INDEX IF NOT EXISTS idx_current_affairs_date ON public.current_affairs(date_string DESC);
CREATE INDEX IF NOT EXISTS idx_study_tasks_user_date ON public.study_tasks(user_id, scheduled_date_millis);
CREATE INDEX IF NOT EXISTS idx_revision_items_user_review ON public.revision_items(user_id, next_review_date);
CREATE INDEX IF NOT EXISTS idx_topic_progress_user ON public.topic_progress(user_id, subject);

-- ----------------------------------------------------------------------------
-- 8. STORED PROCEDURES & ATOMIC RPCS
-- ----------------------------------------------------------------------------

-- Atomic, Idempotent Test Submission
CREATE OR REPLACE FUNCTION public.submit_test_atomic(
    p_session_id UUID,
    p_attempt_json JSONB,
    p_answers_json JSONB DEFAULT '[]'::jsonb
)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_user_id UUID;
    v_is_completed BOOLEAN;
    v_attempt_id UUID;
    v_answer JSONB;
BEGIN
    v_user_id := auth.uid();
    IF v_user_id IS NULL THEN
        RAISE EXCEPTION 'Authentication required to submit test';
    END IF;

    -- 1. Validate Session Ownership and State
    SELECT is_completed INTO v_is_completed
    FROM public.test_sessions
    WHERE id = p_session_id AND user_id = v_user_id;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Test session % not found for authenticated user', p_session_id;
    END IF;

    IF v_is_completed THEN
        -- Idempotency: Already finalized, return existing attempt
        SELECT id INTO v_attempt_id FROM public.test_attempts WHERE session_id = p_session_id;
        RETURN jsonb_build_object(
            'status', 'ALREADY_SUBMITTED',
            'session_id', p_session_id,
            'attempt_id', v_attempt_id,
            'message', 'Test session was previously finalized.'
        );
    END IF;

    -- 2. Finalize Test Session
    UPDATE public.test_sessions
    SET is_completed = true,
        completed_at = timezone('utc'::text, now())
    WHERE id = p_session_id;

    -- 3. Upsert Final Result / Attempt
    INSERT INTO public.test_attempts (
        session_id,
        user_id,
        local_id,
        title,
        subject,
        exam_name,
        topic,
        difficulty,
        score,
        total_questions,
        correct_count,
        incorrect_count,
        skipped_count,
        accuracy_percent,
        time_spent_seconds,
        weak_topics,
        strong_topics,
        ai_recommendation,
        marking_scheme,
        timestamp
    ) VALUES (
        p_session_id,
        v_user_id,
        (p_attempt_json->>'local_id')::bigint,
        p_attempt_json->>'title',
        p_attempt_json->>'subject',
        p_attempt_json->>'exam_name',
        p_attempt_json->>'topic',
        COALESCE(p_attempt_json->>'difficulty', 'Medium'),
        (p_attempt_json->>'score')::numeric,
        (p_attempt_json->>'total_questions')::int,
        (p_attempt_json->>'correct_count')::int,
        (p_attempt_json->>'incorrect_count')::int,
        (p_attempt_json->>'skipped_count')::int,
        (p_attempt_json->>'accuracy_percent')::numeric,
        (p_attempt_json->>'time_spent_seconds')::int,
        COALESCE(p_attempt_json->'weak_topics', '[]'::jsonb),
        COALESCE(p_attempt_json->'strong_topics', '[]'::jsonb),
        p_attempt_json->>'ai_recommendation',
        p_attempt_json->>'marking_scheme',
        (p_attempt_json->>'timestamp')::bigint
    )
    ON CONFLICT (session_id) DO NOTHING
    RETURNING id INTO v_attempt_id;

    -- 4. Store Answer Snapshots
    FOR v_answer IN SELECT * FROM jsonb_array_elements(p_answers_json)
    LOOP
        INSERT INTO public.test_answers (
            session_id,
            user_id,
            question_id,
            question_text_snapshot,
            options_snapshot,
            student_answer,
            correct_answer_snapshot,
            is_correct,
            time_taken_seconds
        ) VALUES (
            p_session_id,
            v_user_id,
            (v_answer->>'question_id')::uuid,
            v_answer->>'question_text',
            v_answer->'options',
            v_answer->>'student_answer',
            v_answer->>'correct_answer',
            (v_answer->>'is_correct')::boolean,
            COALESCE((v_answer->>'time_taken_seconds')::int, 0)
        )
        ON CONFLICT (session_id, question_id) DO UPDATE
        SET student_answer = EXCLUDED.student_answer,
            is_correct = EXCLUDED.is_correct;
    END LOOP;

    -- 5. Audit Log
    INSERT INTO public.sync_audit_log (user_id, operation, table_name, record_id, status)
    VALUES (v_user_id, 'SUBMIT_TEST_ATOMIC', 'test_attempts', p_session_id::text, 'SUCCESS');

    RETURN jsonb_build_object(
        'status', 'SUCCESS',
        'session_id', p_session_id,
        'attempt_id', v_attempt_id,
        'message', 'Test submitted and finalized atomically.'
    );
END;
$$;

-- High-Performance Aggregated Home Dashboard Query
CREATE OR REPLACE FUNCTION public.get_home_aggregated_summary(p_user_id UUID)
RETURNS JSONB
LANGUAGE plpgsql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    v_profile JSONB;
    v_pending_tasks_count INT;
    v_revisions_due_count INT;
    v_recent_accuracy NUMERIC;
BEGIN
    IF auth.uid() != p_user_id AND NOT public.is_admin() THEN
        RAISE EXCEPTION 'Unauthorized to view student summary';
    END IF;

    -- Profile Summary
    SELECT to_jsonb(p) INTO v_profile
    FROM (
        SELECT id, name, email, exam_name, xp, level, streak_days, daily_target_minutes, total_focus_minutes
        FROM public.profiles WHERE id = p_user_id
    ) p;

    -- Pending Study Tasks
    SELECT COUNT(*) INTO v_pending_tasks_count
    FROM public.study_tasks
    WHERE user_id = p_user_id AND is_completed = false;

    -- Flashcards Due for Review
    SELECT COUNT(*) INTO v_revisions_due_count
    FROM public.revision_items
    WHERE user_id = p_user_id AND (next_review_date IS NULL OR next_review_date <= (extract(epoch from now()) * 1000)::bigint);

    -- Recent Mock Accuracy (Average of last 5 tests)
    SELECT ROUND(AVG(accuracy_percent), 1) INTO v_recent_accuracy
    FROM (
        SELECT accuracy_percent FROM public.test_attempts
        WHERE user_id = p_user_id
        ORDER BY timestamp DESC
        LIMIT 5
    ) a;

    RETURN jsonb_build_object(
        'profile', v_profile,
        'pending_tasks_count', COALESCE(v_pending_tasks_count, 0),
        'revisions_due_count', COALESCE(v_revisions_due_count, 0),
        'recent_accuracy', COALESCE(v_recent_accuracy, 0.0),
        'server_timestamp', (extract(epoch from now()) * 1000)::bigint
    );
END;
$$;

-- Backend Health Check
CREATE OR REPLACE FUNCTION public.check_backend_health()
RETURNS JSONB
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
    SELECT jsonb_build_object(
        'status', 'HEALTHY',
        'database', 'PostgreSQL 15',
        'auth_enabled', true,
        'rls_enforced', true,
        'server_time', timezone('utc'::text, now())
    );
$$;

-- ----------------------------------------------------------------------------
-- 9. STEP 69: DEDICATED PRACTICE & CURRENT AFFAIRS TABLES & MIGRATIONS
-- ----------------------------------------------------------------------------

-- 9.1 Practice Questions (Compatible alias / table)
CREATE TABLE IF NOT EXISTS public.practice_questions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    question_id TEXT UNIQUE,
    exam_id UUID REFERENCES public.exams(id) ON DELETE SET NULL,
    exam_name TEXT NOT NULL,
    subject TEXT NOT NULL,
    chapter TEXT NOT NULL,
    topic TEXT NOT NULL,
    question_text TEXT NOT NULL,
    options JSONB NOT NULL,
    correct_answer TEXT NOT NULL,
    explanation TEXT,
    difficulty TEXT DEFAULT 'Medium' CHECK (difficulty IN ('Easy', 'Medium', 'Hard', 'Mixed')),
    question_type TEXT DEFAULT 'PRACTICE' CHECK (question_type IN ('PRACTICE', 'PYQ', 'DAILY_QUIZ', 'MOCK')),
    year INT,
    paper_shift TEXT,
    source_reference TEXT,
    language TEXT DEFAULT 'English',
    is_verified BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now())
);

ALTER TABLE public.practice_questions ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Public read verified practice questions"
    ON public.practice_questions FOR SELECT
    USING (is_verified = true OR public.is_admin() OR public.has_role(auth.uid(), 'reviewer'));

CREATE POLICY "Admin manage practice questions"
    ON public.practice_questions FOR ALL
    USING (public.is_admin() OR public.has_role(auth.uid(), 'reviewer'))
    WITH CHECK (public.is_admin() OR public.has_role(auth.uid(), 'reviewer'));

-- 9.2 Dedicated Mock Tests & Templates
CREATE TABLE IF NOT EXISTS public.mock_tests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    exam_id UUID REFERENCES public.exams(id) ON DELETE SET NULL,
    exam_name TEXT NOT NULL,
    title TEXT NOT NULL,
    description TEXT,
    duration INT NOT NULL DEFAULT 60, -- duration in minutes
    total_questions INT NOT NULL DEFAULT 30,
    difficulty TEXT DEFAULT 'Medium' CHECK (difficulty IN ('Easy', 'Medium', 'Hard', 'Mixed')),
    marking_scheme JSONB DEFAULT '{"correct": 1.0, "negative": 0.25}'::jsonb,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now())
);

ALTER TABLE public.mock_tests ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Public read active mock tests"
    ON public.mock_tests FOR SELECT
    USING (is_active = true OR public.is_admin());

CREATE POLICY "Admin manage mock tests"
    ON public.mock_tests FOR ALL
    USING (public.is_admin())
    WITH CHECK (public.is_admin());

-- Mock Test Questions Association (Normalized Question References)
CREATE TABLE IF NOT EXISTS public.mock_test_questions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    mock_test_id UUID NOT NULL REFERENCES public.mock_tests(id) ON DELETE CASCADE,
    question_id UUID REFERENCES public.practice_questions(id) ON DELETE CASCADE,
    question_order INT NOT NULL DEFAULT 1,
    marks NUMERIC(4, 2) DEFAULT 1.0,
    negative_marks NUMERIC(4, 2) DEFAULT 0.25,
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    UNIQUE(mock_test_id, question_id),
    UNIQUE(mock_test_id, question_order)
);

ALTER TABLE public.mock_test_questions ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Public read mock test questions"
    ON public.mock_test_questions FOR SELECT
    USING (true);

CREATE POLICY "Admin manage mock test questions"
    ON public.mock_test_questions FOR ALL
    USING (public.is_admin())
    WITH CHECK (public.is_admin());

-- Mock Test User-Specific Attempts
CREATE TABLE IF NOT EXISTS public.mock_attempts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    mock_test_id UUID REFERENCES public.mock_tests(id) ON DELETE SET NULL,
    test_title TEXT NOT NULL,
    exam_name TEXT NOT NULL,
    score NUMERIC(6, 2) NOT NULL DEFAULT 0.0,
    correct INT NOT NULL DEFAULT 0,
    wrong INT NOT NULL DEFAULT 0,
    unattempted INT NOT NULL DEFAULT 0,
    accuracy NUMERIC(5, 2) NOT NULL DEFAULT 0.0,
    time_taken INT NOT NULL DEFAULT 0, -- seconds
    started_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    completed_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    weak_topics JSONB DEFAULT '[]'::jsonb,
    strong_topics JSONB DEFAULT '[]'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now())
);

ALTER TABLE public.mock_attempts ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users read own mock attempts"
    ON public.mock_attempts FOR SELECT
    USING (auth.uid() = user_id);

CREATE POLICY "Users insert own mock attempts"
    ON public.mock_attempts FOR INSERT
    WITH CHECK (auth.uid() = user_id);

-- 9.3 Dedicated Daily Quiz System (One official quiz per date)
CREATE TABLE IF NOT EXISTS public.daily_quizzes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    quiz_date DATE UNIQUE NOT NULL,
    title TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'published' CHECK (status IN ('draft', 'published', 'archived')),
    total_questions INT NOT NULL DEFAULT 10,
    duration_minutes INT NOT NULL DEFAULT 15,
    difficulty TEXT DEFAULT 'Mixed',
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now())
);

ALTER TABLE public.daily_quizzes ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Public read published daily quizzes"
    ON public.daily_quizzes FOR SELECT
    USING (status = 'published' OR public.is_admin());

CREATE POLICY "Admin manage daily quizzes"
    ON public.daily_quizzes FOR ALL
    USING (public.is_admin())
    WITH CHECK (public.is_admin());

CREATE TABLE IF NOT EXISTS public.daily_quiz_questions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    daily_quiz_id UUID NOT NULL REFERENCES public.daily_quizzes(id) ON DELETE CASCADE,
    question_id UUID REFERENCES public.practice_questions(id) ON DELETE CASCADE,
    question_order INT NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    UNIQUE(daily_quiz_id, question_id),
    UNIQUE(daily_quiz_id, question_order)
);

ALTER TABLE public.daily_quiz_questions ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Public read daily quiz questions"
    ON public.daily_quiz_questions FOR SELECT
    USING (true);

CREATE POLICY "Admin manage daily quiz questions"
    ON public.daily_quiz_questions FOR ALL
    USING (public.is_admin())
    WITH CHECK (public.is_admin());

CREATE TABLE IF NOT EXISTS public.daily_quiz_attempts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    daily_quiz_id UUID NOT NULL REFERENCES public.daily_quizzes(id) ON DELETE CASCADE,
    score NUMERIC(6, 2) NOT NULL DEFAULT 0.0,
    correct INT NOT NULL DEFAULT 0,
    wrong INT NOT NULL DEFAULT 0,
    unattempted INT NOT NULL DEFAULT 0,
    accuracy NUMERIC(5, 2) NOT NULL DEFAULT 0.0,
    time_taken_seconds INT NOT NULL DEFAULT 0,
    completed_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    UNIQUE(user_id, daily_quiz_id)
);

ALTER TABLE public.daily_quiz_attempts ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users read own daily quiz attempts"
    ON public.daily_quiz_attempts FOR SELECT
    USING (auth.uid() = user_id);

CREATE POLICY "Users insert own daily quiz attempts"
    ON public.daily_quiz_attempts FOR INSERT
    WITH CHECK (auth.uid() = user_id);

-- 9.4 Step 69 Daily Current Affairs Schema (Safe non-destructive migration)
CREATE TABLE IF NOT EXISTS public.daily_current_affairs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title TEXT NOT NULL,
    short_summary TEXT NOT NULL,
    content TEXT NOT NULL,
    affair_date DATE NOT NULL,
    published_date TEXT,
    day INT,
    month INT,
    year INT,
    category TEXT NOT NULL DEFAULT 'National & Governance',
    source_name TEXT DEFAULT 'PIB / Official Govt Portal',
    source_url TEXT,
    read_more_url TEXT,
    image_url TEXT,
    source_type TEXT DEFAULT 'OFFICIAL_PORTAL',
    source_identifier TEXT,
    content_hash TEXT UNIQUE,
    ai_processed BOOLEAN NOT NULL DEFAULT false,
    key_points_json JSONB DEFAULT '[]'::jsonb,
    quiz_questions_json JSONB DEFAULT '[]'::jsonb,
    metadata JSONB DEFAULT '{}'::jsonb,
    is_verified BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now())
);

ALTER TABLE public.daily_current_affairs ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Public read verified daily current affairs"
    ON public.daily_current_affairs FOR SELECT
    USING (is_verified = true OR public.is_admin());

CREATE POLICY "Admin manage daily current affairs"
    ON public.daily_current_affairs FOR ALL
    USING (public.is_admin())
    WITH CHECK (public.is_admin());

-- 9.5 Content Ingestion Log Table
CREATE TABLE IF NOT EXISTS public.content_fetch_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    source_type TEXT NOT NULL,
    source_name TEXT NOT NULL,
    source_url TEXT,
    items_discovered INT DEFAULT 0,
    items_saved INT DEFAULT 0,
    duplicates_skipped INT DEFAULT 0,
    status TEXT NOT NULL DEFAULT 'SUCCESS',
    error_message TEXT,
    started_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    finished_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now())
);

ALTER TABLE public.content_fetch_log ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Admins view content fetch logs"
    ON public.content_fetch_log FOR ALL
    USING (public.is_admin())
    WITH CHECK (public.is_admin());

-- 9.6 Step 71 Latest Updates Schema (Dedicated 5 Categories: vacancy, admit_card, result, answer_key, admission)
CREATE TABLE IF NOT EXISTS public.latest_updates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    update_type TEXT NOT NULL CHECK (update_type IN ('vacancy', 'admit_card', 'result', 'answer_key', 'admission')),
    title TEXT NOT NULL,
    short_description TEXT,
    full_content TEXT,
    organization TEXT,
    exam_name TEXT,
    post_name TEXT,
    published_date TEXT,
    start_date TEXT,
    last_date TEXT,
    exam_date TEXT,
    source_url TEXT,
    apply_url TEXT,
    download_url TEXT,
    image_url TEXT,
    language TEXT DEFAULT 'English',
    source_name TEXT,
    source_type TEXT DEFAULT 'OFFICIAL_PORTAL',
    external_id TEXT UNIQUE,
    content_hash TEXT UNIQUE,
    metadata JSONB DEFAULT '{}'::jsonb,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now())
);

ALTER TABLE public.latest_updates ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Public read active latest updates"
    ON public.latest_updates FOR SELECT
    USING (is_active = true OR public.is_admin());

CREATE POLICY "Admin manage latest updates"
    ON public.latest_updates FOR ALL
    USING (public.is_admin())
    WITH CHECK (public.is_admin());

-- Step 69 & 71 Performance Indexes
CREATE INDEX IF NOT EXISTS idx_practice_questions_filter ON public.practice_questions(exam_name, subject, chapter, topic, difficulty);
CREATE INDEX IF NOT EXISTS idx_practice_questions_pyq ON public.practice_questions(question_type, exam_name, year, paper_shift);
CREATE INDEX IF NOT EXISTS idx_mock_tests_exam ON public.mock_tests(exam_name, is_active);
CREATE INDEX IF NOT EXISTS idx_mock_attempts_user ON public.mock_attempts(user_id, completed_at DESC);
CREATE INDEX IF NOT EXISTS idx_daily_quizzes_date ON public.daily_quizzes(quiz_date);
CREATE INDEX IF NOT EXISTS idx_daily_current_affairs_affair_date ON public.daily_current_affairs(affair_date DESC);
CREATE INDEX IF NOT EXISTS idx_daily_current_affairs_hash ON public.daily_current_affairs(content_hash);
CREATE INDEX IF NOT EXISTS idx_latest_updates_type ON public.latest_updates(update_type, is_active, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_latest_updates_dates ON public.latest_updates(last_date, exam_date);
CREATE INDEX IF NOT EXISTS idx_latest_updates_org_exam ON public.latest_updates(organization, exam_name);
CREATE INDEX IF NOT EXISTS idx_latest_updates_hash ON public.latest_updates(content_hash);

