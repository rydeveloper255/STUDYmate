-- ============================================================================
-- STUDY MATE — SUPABASE AUTH & RLS SECURITY HARDENING MIGRATION
-- Target: PostgreSQL 15+ / Supabase
-- Type: Safe, Non-Destructive, Idempotent
-- ============================================================================

-- ----------------------------------------------------------------------------
-- STEP 1: SAFE PROFILE EXTENSIONS & SANITIZATION
-- ----------------------------------------------------------------------------

-- Add mobile_number to public.profiles if missing
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_schema = 'public' 
          AND table_name = 'profiles' 
          AND column_name = 'mobile_number'
    ) THEN
        ALTER TABLE public.profiles ADD COLUMN mobile_number TEXT;
    END IF;
END $$;

-- Create partial unique index on non-null mobile numbers to prevent collisions
CREATE UNIQUE INDEX IF NOT EXISTS idx_profiles_mobile_number_unique 
    ON public.profiles(mobile_number) 
    WHERE mobile_number IS NOT NULL AND mobile_number <> '';

-- ----------------------------------------------------------------------------
-- STEP 2: AUTOMATIC PROFILE & SETTINGS BOOTSTRAP TRIGGER
-- ----------------------------------------------------------------------------

CREATE OR REPLACE FUNCTION public.handle_new_user_bootstrap()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, auth, pg_temp
AS $$
DECLARE
    v_raw_name TEXT;
    v_raw_mobile TEXT;
BEGIN
    -- Extract optional metadata safely
    v_raw_name := COALESCE(NEW.raw_user_meta_data->>'full_name', NEW.raw_user_meta_data->>'name', 'Student');
    v_raw_mobile := NULLIF(TRIM(NEW.raw_user_meta_data->>'mobile_number'), '');

    -- Insert Default Profile if not present
    INSERT INTO public.profiles (
        id,
        email,
        name,
        mobile_number,
        created_at,
        updated_at
    ) VALUES (
        NEW.id,
        COALESCE(NEW.email, ''),
        v_raw_name,
        v_raw_mobile,
        timezone('utc'::text, now()),
        timezone('utc'::text, now())
    )
    ON CONFLICT (id) DO UPDATE
    SET email = EXCLUDED.email,
        mobile_number = COALESCE(public.profiles.mobile_number, EXCLUDED.mobile_number),
        updated_at = timezone('utc'::text, now());

    -- Insert Default User Settings
    INSERT INTO public.user_settings (
        user_id,
        theme,
        sound_enabled,
        vibration_enabled,
        created_at,
        updated_at
    ) VALUES (
        NEW.id,
        'dark',
        true,
        true,
        timezone('utc'::text, now()),
        timezone('utc'::text, now())
    )
    ON CONFLICT (user_id) DO NOTHING;

    -- Assign default 'user' role
    INSERT INTO public.user_roles (
        user_id,
        role,
        granted_at
    ) VALUES (
        NEW.id,
        'user',
        timezone('utc'::text, now())
    )
    ON CONFLICT (user_id, role) DO NOTHING;

    RETURN NEW;
END;
$$;

-- Bind Trigger to auth.users safely
DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW
    EXECUTE FUNCTION public.handle_new_user_bootstrap();

-- ----------------------------------------------------------------------------
-- STEP 3: SECURITY-DEFINER SECURE RPC FUNCTIONS
-- ----------------------------------------------------------------------------

-- 3.1 Secure Mobile-to-Email Resolver for Auth Login
CREATE OR REPLACE FUNCTION public.resolve_email_from_mobile(p_mobile_number TEXT)
RETURNS JSONB
LANGUAGE plpgsql
STABLE
SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
DECLARE
    v_email TEXT;
    v_sanitized_mobile TEXT;
BEGIN
    v_sanitized_mobile := TRIM(p_mobile_number);
    
    IF v_sanitized_mobile IS NULL OR v_sanitized_mobile = '' THEN
        RETURN jsonb_build_object('success', false, 'message', 'Invalid mobile number format');
    END IF;

    SELECT email INTO v_email
    FROM public.profiles
    WHERE mobile_number = v_sanitized_mobile
    LIMIT 1;

    IF v_email IS NULL THEN
        -- Generic error to mitigate enumeration
        RETURN jsonb_build_object('success', false, 'message', 'User account not found');
    END IF;

    RETURN jsonb_build_object('success', true, 'email', v_email);
END;
$$;

REVOKE ALL ON FUNCTION public.resolve_email_from_mobile(TEXT) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.resolve_email_from_mobile(TEXT) TO anon, authenticated, service_role;

-- 3.2 Hardened Immutable Test Submission RPC
CREATE OR REPLACE FUNCTION public.submit_test_atomic(
    p_session_id UUID,
    p_attempt_json JSONB,
    p_answers_json JSONB DEFAULT '[]'::jsonb
)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
DECLARE
    v_user_id UUID;
    v_is_completed BOOLEAN;
    v_attempt_id UUID;
    v_answer JSONB;
BEGIN
    v_user_id := auth.uid();
    IF v_user_id IS NULL THEN
        RAISE EXCEPTION 'Unauthorized: Caller is not authenticated.';
    END IF;

    -- Validate Session Ownership and Finalization State
    SELECT is_completed INTO v_is_completed
    FROM public.test_sessions
    WHERE id = p_session_id AND user_id = v_user_id;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Session % not found or ownership mismatch.', p_session_id;
    END IF;

    IF v_is_completed THEN
        SELECT id INTO v_attempt_id FROM public.test_attempts WHERE session_id = p_session_id;
        RETURN jsonb_build_object(
            'status', 'ALREADY_SUBMITTED',
            'session_id', p_session_id,
            'attempt_id', v_attempt_id,
            'message', 'Test session is already completed and immutable.'
        );
    END IF;

    -- Lock & Mark Session Complete
    UPDATE public.test_sessions
    SET is_completed = true,
        completed_at = timezone('utc'::text, now())
    WHERE id = p_session_id AND user_id = v_user_id;

    -- Insert Final Immutable Test Attempt
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
        COALESCE(p_attempt_json->>'title', 'Untitled Attempt'),
        COALESCE(p_attempt_json->>'subject', 'General'),
        COALESCE(p_attempt_json->>'exam_name', 'General Exam'),
        COALESCE(p_attempt_json->>'topic', 'All Topics'),
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
        COALESCE((p_attempt_json->>'timestamp')::bigint, (extract(epoch from now()) * 1000)::bigint)
    )
    ON CONFLICT (session_id) DO NOTHING
    RETURNING id INTO v_attempt_id;

    -- Upsert Individual Question Snapshots (Enforcing caller user_id)
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
        ON CONFLICT (session_id, question_id) DO NOTHING;
    END LOOP;

    -- Record Audit Event
    INSERT INTO public.sync_audit_log (user_id, operation, table_name, record_id, status)
    VALUES (v_user_id, 'SUBMIT_TEST_ATOMIC', 'test_attempts', p_session_id::text, 'SUCCESS');

    RETURN jsonb_build_object(
        'status', 'SUCCESS',
        'session_id', p_session_id,
        'attempt_id', v_attempt_id,
        'message', 'Test evaluated and finalized.'
    );
END;
$$;

REVOKE ALL ON FUNCTION public.submit_test_atomic(UUID, JSONB, JSONB) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.submit_test_atomic(UUID, JSONB, JSONB) TO authenticated, service_role;

-- 3.3 Protected Client Audit Logger (Prevents user_id spoofing)
CREATE OR REPLACE FUNCTION public.log_sync_event(
    p_operation TEXT,
    p_table_name TEXT,
    p_record_id TEXT,
    p_status TEXT,
    p_error_details TEXT DEFAULT NULL,
    p_client_request_id TEXT DEFAULT NULL
)
RETURNS UUID
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, pg_temp
AS $$
DECLARE
    v_log_id UUID;
    v_user_id UUID;
BEGIN
    v_user_id := auth.uid();
    
    INSERT INTO public.sync_audit_log (
        user_id,
        operation,
        table_name,
        record_id,
        status,
        error_details,
        client_request_id,
        created_at
    ) VALUES (
        v_user_id,
        p_operation,
        p_table_name,
        p_record_id,
        p_status,
        p_error_details,
        p_client_request_id,
        timezone('utc'::text, now())
    )
    RETURNING id INTO v_log_id;

    RETURN v_log_id;
END;
$$;

REVOKE ALL ON FUNCTION public.log_sync_event(TEXT, TEXT, TEXT, TEXT, TEXT, TEXT) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.log_sync_event(TEXT, TEXT, TEXT, TEXT, TEXT, TEXT) TO authenticated, anon, service_role;

-- ----------------------------------------------------------------------------
-- STEP 4: RLS POLICY HARDENING ACROSS ALL USER TABLES
-- ----------------------------------------------------------------------------

-- 4.1 public.profiles
DROP POLICY IF EXISTS "Users can read own profile" ON public.profiles;
DROP POLICY IF EXISTS "Users can insert own profile" ON public.profiles;
DROP POLICY IF EXISTS "Users can update own profile" ON public.profiles;

CREATE POLICY "profiles_select_policy" ON public.profiles FOR SELECT USING (auth.uid() = id OR public.is_admin());
CREATE POLICY "profiles_insert_policy" ON public.profiles FOR INSERT WITH CHECK (auth.uid() = id);
CREATE POLICY "profiles_update_policy" ON public.profiles FOR UPDATE USING (auth.uid() = id) WITH CHECK (auth.uid() = id);

-- 4.2 public.test_sessions (Immutable once completed)
DROP POLICY IF EXISTS "Users can manage own test sessions" ON public.test_sessions;
DROP POLICY IF EXISTS "Users can create own test sessions" ON public.test_sessions;
DROP POLICY IF EXISTS "Users can update only uncompleted sessions" ON public.test_sessions;

CREATE POLICY "test_sessions_select" ON public.test_sessions FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY "test_sessions_insert" ON public.test_sessions FOR INSERT WITH CHECK (auth.uid() = user_id);
CREATE POLICY "test_sessions_update" ON public.test_sessions FOR UPDATE USING (auth.uid() = user_id AND is_completed = false) WITH CHECK (auth.uid() = user_id);

-- 4.3 public.test_attempts & test_answers (Append-only & read-only for users)
DROP POLICY IF EXISTS "Users can read own test attempts" ON public.test_attempts;
DROP POLICY IF EXISTS "Users can insert own test attempts" ON public.test_attempts;
CREATE POLICY "test_attempts_select" ON public.test_attempts FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY "test_attempts_insert" ON public.test_attempts FOR INSERT WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "Users can read own test answers" ON public.test_answers;
DROP POLICY IF EXISTS "Users can insert own test answers" ON public.test_answers;
CREATE POLICY "test_answers_select" ON public.test_answers FOR SELECT USING (auth.uid() = user_id);
CREATE POLICY "test_answers_insert" ON public.test_answers FOR INSERT WITH CHECK (auth.uid() = user_id);

-- 4.4 public.sync_audit_log
DROP POLICY IF EXISTS "Users can insert sync audit logs" ON public.sync_audit_log;
DROP POLICY IF EXISTS "Admins can view all audit logs" ON public.sync_audit_log;

CREATE POLICY "audit_log_insert_strict" ON public.sync_audit_log FOR INSERT WITH CHECK (auth.uid() = user_id OR user_id IS NULL);
CREATE POLICY "audit_log_select_admin" ON public.sync_audit_log FOR SELECT USING (public.is_admin());

-- 4.5 Protect Question Bank Answers & Drafts
DROP POLICY IF EXISTS "Public read question versions" ON public.question_versions;
CREATE POLICY "question_versions_authenticated_read" ON public.question_versions 
    FOR SELECT USING (auth.role() = 'authenticated' OR public.is_admin());

-- ----------------------------------------------------------------------------
-- STEP 5: STORAGE BUCKET ISOLATION POLICIES (note-attachments)
-- ----------------------------------------------------------------------------

-- Ensure private bucket exists
INSERT INTO storage.buckets (id, name, public)
VALUES ('note-attachments', 'note-attachments', false)
ON CONFLICT (id) DO UPDATE SET public = false;

-- Clean existing bucket policies
DROP POLICY IF EXISTS "Allow user individual storage upload" ON storage.objects;
DROP POLICY IF EXISTS "Allow user individual storage read" ON storage.objects;
DROP POLICY IF EXISTS "Allow user individual storage delete" ON storage.objects;

-- Enforce prefix matching based on auth.uid()
CREATE POLICY "Allow user individual storage upload"
ON storage.objects FOR INSERT
TO authenticated
WITH CHECK (
    bucket_id = 'note-attachments' AND 
    (storage.foldername(name))[1] = auth.uid()::text
);

CREATE POLICY "Allow user individual storage read"
ON storage.objects FOR SELECT
TO authenticated
USING (
    bucket_id = 'note-attachments' AND 
    (storage.foldername(name))[1] = auth.uid()::text
);

CREATE POLICY "Allow user individual storage delete"
ON storage.objects FOR DELETE
TO authenticated
USING (
    bucket_id = 'note-attachments' AND 
    (storage.foldername(name))[1] = auth.uid()::text
);
