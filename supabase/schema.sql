-- Supabase Database Schema for FlowState
-- Run this SQL in your Supabase SQL Editor
-- This schema is normalized (3NF) and designed for optimal performance

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================================================================
-- USER MANAGEMENT
-- ============================================================================

-- User profiles table (extends Supabase auth.users)
CREATE TABLE IF NOT EXISTS public.profiles (
    id UUID REFERENCES auth.users(id) ON DELETE CASCADE PRIMARY KEY,
    username TEXT UNIQUE NOT NULL,
    email TEXT,
    full_name TEXT,
    timezone TEXT DEFAULT 'UTC',
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- User settings/preferences table
CREATE TABLE IF NOT EXISTS public.user_settings (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE NOT NULL UNIQUE,
    notification_enabled BOOLEAN DEFAULT true,
    notification_time TEXT, -- e.g., "09:00"
    google_fit_enabled BOOLEAN DEFAULT false,
    google_fit_account TEXT,
    ml_model_preference TEXT DEFAULT 'default', -- model version preference
    data_sync_enabled BOOLEAN DEFAULT true,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- ============================================================================
-- BIOMETRIC DATA (Normalized)
-- ============================================================================

-- Heart rate readings table
CREATE TABLE IF NOT EXISTS public.heart_rate_readings (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE NOT NULL,
    timestamp TIMESTAMPTZ NOT NULL,
    heart_rate_bpm INTEGER NOT NULL CHECK (heart_rate_bpm > 0 AND heart_rate_bpm <= 250),
    source TEXT DEFAULT 'google_fit', -- 'google_fit', 'manual', 'device'
    created_at TIMESTAMPTZ DEFAULT NOW(),
    CONSTRAINT unique_user_heartrate_timestamp UNIQUE (user_id, timestamp)
);

-- Sleep sessions table (normalized from biometric data)
CREATE TABLE IF NOT EXISTS public.sleep_sessions (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE NOT NULL,
    sleep_start TIMESTAMPTZ NOT NULL,
    sleep_end TIMESTAMPTZ,
    duration_minutes INTEGER, -- total sleep duration in minutes
    sleep_quality_score DOUBLE PRECISION CHECK (sleep_quality_score >= 0.0 AND sleep_quality_score <= 1.0),
    deep_sleep_minutes INTEGER,
    light_sleep_minutes INTEGER,
    rem_sleep_minutes INTEGER,
    awake_minutes INTEGER,
    source TEXT DEFAULT 'google_fit',
    created_at TIMESTAMPTZ DEFAULT NOW(),
    CONSTRAINT sleep_end_after_start CHECK (sleep_end IS NULL OR sleep_end >= sleep_start)
);

-- Body temperature readings table
CREATE TABLE IF NOT EXISTS public.temperature_readings (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE NOT NULL,
    timestamp TIMESTAMPTZ NOT NULL,
    temperature_celsius DOUBLE PRECISION NOT NULL CHECK (temperature_celsius >= 30.0 AND temperature_celsius <= 45.0),
    temperature_type TEXT DEFAULT 'skin' CHECK (temperature_type IN ('skin', 'body', 'ambient')),
    source TEXT DEFAULT 'google_fit',
    created_at TIMESTAMPTZ DEFAULT NOW(),
    CONSTRAINT unique_user_temp_timestamp UNIQUE (user_id, timestamp)
);

-- ============================================================================
-- COGNITIVE PERFORMANCE DATA
-- ============================================================================

-- Typing speed tests table
CREATE TABLE IF NOT EXISTS public.typing_speed_tests (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE NOT NULL,
    timestamp TIMESTAMPTZ NOT NULL,
    words_per_minute INTEGER NOT NULL CHECK (words_per_minute >= 0),
    accuracy_percentage DOUBLE PRECISION NOT NULL CHECK (accuracy_percentage >= 0.0 AND accuracy_percentage <= 100.0),
    total_characters INTEGER,
    errors INTEGER DEFAULT 0,
    sample_text TEXT,
    duration_seconds INTEGER,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Reaction time tests table
CREATE TABLE IF NOT EXISTS public.reaction_time_tests (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE NOT NULL,
    timestamp TIMESTAMPTZ NOT NULL,
    reaction_time_ms INTEGER NOT NULL CHECK (reaction_time_ms > 0 AND reaction_time_ms <= 5000),
    test_type TEXT DEFAULT 'visual' CHECK (test_type IN ('visual', 'audio', 'tactile')),
    attempts INTEGER DEFAULT 1,
    average_reaction_time_ms DOUBLE PRECISION,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Cognitive test sessions (aggregates multiple tests)
CREATE TABLE IF NOT EXISTS public.cognitive_test_sessions (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE NOT NULL,
    session_start TIMESTAMPTZ NOT NULL,
    session_end TIMESTAMPTZ,
    typing_test_id UUID REFERENCES public.typing_speed_tests(id) ON DELETE SET NULL,
    reaction_time_test_id UUID REFERENCES public.reaction_time_tests(id) ON DELETE SET NULL,
    overall_score DOUBLE PRECISION,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- ============================================================================
-- ENERGY PREDICTIONS & ML
-- ============================================================================

-- Energy predictions table
CREATE TABLE IF NOT EXISTS public.energy_predictions (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE NOT NULL,
    prediction_time TIMESTAMPTZ NOT NULL, -- when the prediction is for
    predicted_level TEXT NOT NULL CHECK (predicted_level IN ('HIGH', 'MEDIUM', 'LOW')),
    confidence_score DOUBLE PRECISION NOT NULL CHECK (confidence_score >= 0.0 AND confidence_score <= 1.0),
    ml_model_version TEXT DEFAULT 'v1.0',
    created_at TIMESTAMPTZ DEFAULT NOW(),
    CONSTRAINT unique_user_prediction_time UNIQUE (user_id, prediction_time)
);

-- Energy prediction factors (normalized - factors contributing to prediction)
CREATE TABLE IF NOT EXISTS public.energy_prediction_factors (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    prediction_id UUID REFERENCES public.energy_predictions(id) ON DELETE CASCADE NOT NULL,
    factor_type TEXT NOT NULL CHECK (factor_type IN ('biometric', 'cognitive', 'temporal', 'behavioral')),
    factor_name TEXT NOT NULL, -- e.g., 'heart_rate', 'sleep_quality', 'typing_speed'
    factor_value DOUBLE PRECISION NOT NULL,
    weight DOUBLE PRECISION, -- importance weight in prediction
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- ============================================================================
-- PRODUCTIVITY SUGGESTIONS & AI SCHEDULES
-- ============================================================================

-- Productivity suggestions table (LLM-generated)
CREATE TABLE IF NOT EXISTS public.productivity_suggestions (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE NOT NULL,
    prediction_id UUID REFERENCES public.energy_predictions(id) ON DELETE SET NULL,
    time_slot_start TIMESTAMPTZ NOT NULL,
    time_slot_end TIMESTAMPTZ NOT NULL,
    suggested_activity TEXT NOT NULL,
    activity_category TEXT CHECK (activity_category IN ('work', 'rest', 'exercise', 'social', 'creative', 'learning')),
    reasoning TEXT,
    estimated_energy_impact TEXT CHECK (estimated_energy_impact IN ('HIGH', 'MEDIUM', 'LOW')),
    priority INTEGER DEFAULT 5 CHECK (priority >= 1 AND priority <= 10),
    is_accepted BOOLEAN DEFAULT false,
    is_completed BOOLEAN DEFAULT false,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    CONSTRAINT time_slot_end_after_start CHECK (time_slot_end > time_slot_start)
);

-- AI-generated schedules (daily/weekly schedules)
CREATE TABLE IF NOT EXISTS public.ai_schedules (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE NOT NULL,
    schedule_date DATE NOT NULL,
    schedule_type TEXT DEFAULT 'daily' CHECK (schedule_type IN ('daily', 'weekly', 'custom')),
    is_active BOOLEAN DEFAULT true,
    generated_at TIMESTAMPTZ DEFAULT NOW(),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    CONSTRAINT unique_user_date_schedule UNIQUE (user_id, schedule_date, schedule_type)
);

-- Scheduled tasks/activities (linked to AI schedules)
CREATE TABLE IF NOT EXISTS public.scheduled_tasks (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    schedule_id UUID REFERENCES public.ai_schedules(id) ON DELETE CASCADE NOT NULL,
    suggestion_id UUID REFERENCES public.productivity_suggestions(id) ON DELETE SET NULL,
    task_name TEXT NOT NULL,
    task_description TEXT,
    start_time TIMESTAMPTZ NOT NULL,
    end_time TIMESTAMPTZ NOT NULL,
    task_type TEXT CHECK (task_type IN ('work', 'break', 'exercise', 'meal', 'social', 'other')),
    priority INTEGER DEFAULT 5 CHECK (priority >= 1 AND priority <= 10),
    is_completed BOOLEAN DEFAULT false,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    CONSTRAINT task_end_after_start CHECK (end_time > start_time)
);

-- ============================================================================
-- INSIGHTS & ANALYTICS
-- ============================================================================

-- Weekly insights table
CREATE TABLE IF NOT EXISTS public.weekly_insights (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE NOT NULL,
    week_start_date DATE NOT NULL,
    week_end_date DATE NOT NULL,
    average_energy_level TEXT CHECK (average_energy_level IN ('HIGH', 'MEDIUM', 'LOW')),
    average_heart_rate DOUBLE PRECISION,
    total_sleep_hours DOUBLE PRECISION,
    average_sleep_quality DOUBLE PRECISION,
    average_typing_speed DOUBLE PRECISION,
    average_reaction_time DOUBLE PRECISION,
    productivity_score DOUBLE PRECISION,
    insights_summary TEXT,
    generated_at TIMESTAMPTZ DEFAULT NOW(),
    CONSTRAINT unique_user_week_insight UNIQUE (user_id, week_start_date),
    CONSTRAINT week_end_after_start CHECK (week_end_date >= week_start_date)
);

-- Daily summaries table
CREATE TABLE IF NOT EXISTS public.daily_summaries (
    id UUID DEFAULT uuid_generate_v4() PRIMARY KEY,
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE NOT NULL,
    summary_date DATE NOT NULL,
    average_energy_level TEXT CHECK (average_energy_level IN ('HIGH', 'MEDIUM', 'LOW')),
    tasks_completed INTEGER DEFAULT 0,
    tasks_total INTEGER DEFAULT 0,
    productivity_score DOUBLE PRECISION,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    CONSTRAINT unique_user_date_summary UNIQUE (user_id, summary_date)
);

-- ============================================================================
-- INDEXES FOR PERFORMANCE
-- ============================================================================

-- User-specific indexes
CREATE INDEX IF NOT EXISTS idx_profiles_user_id ON public.profiles(id);
CREATE INDEX IF NOT EXISTS idx_user_settings_user_id ON public.user_settings(user_id);

-- Biometric data indexes
CREATE INDEX IF NOT EXISTS idx_heart_rate_user_timestamp ON public.heart_rate_readings(user_id, timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_sleep_sessions_user_start ON public.sleep_sessions(user_id, sleep_start DESC);
CREATE INDEX IF NOT EXISTS idx_temperature_user_timestamp ON public.temperature_readings(user_id, timestamp DESC);

-- Cognitive test indexes
CREATE INDEX IF NOT EXISTS idx_typing_tests_user_timestamp ON public.typing_speed_tests(user_id, timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_reaction_tests_user_timestamp ON public.reaction_time_tests(user_id, timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_cognitive_sessions_user_start ON public.cognitive_test_sessions(user_id, session_start DESC);

-- Energy prediction indexes
CREATE INDEX IF NOT EXISTS idx_energy_predictions_user_time ON public.energy_predictions(user_id, prediction_time DESC);
CREATE INDEX IF NOT EXISTS idx_prediction_factors_prediction_id ON public.energy_prediction_factors(prediction_id);

-- Productivity indexes
CREATE INDEX IF NOT EXISTS idx_suggestions_user_time ON public.productivity_suggestions(user_id, time_slot_start DESC);
CREATE INDEX IF NOT EXISTS idx_suggestions_prediction_id ON public.productivity_suggestions(prediction_id);
CREATE INDEX IF NOT EXISTS idx_ai_schedules_user_date ON public.ai_schedules(user_id, schedule_date DESC);
CREATE INDEX IF NOT EXISTS idx_scheduled_tasks_schedule_id ON public.scheduled_tasks(schedule_id);
CREATE INDEX IF NOT EXISTS idx_scheduled_tasks_start_time ON public.scheduled_tasks(start_time);

-- Insights indexes
CREATE INDEX IF NOT EXISTS idx_weekly_insights_user_week ON public.weekly_insights(user_id, week_start_date DESC);
CREATE INDEX IF NOT EXISTS idx_daily_summaries_user_date ON public.daily_summaries(user_id, summary_date DESC);

-- ============================================================================
-- ROW LEVEL SECURITY (RLS)
-- ============================================================================

-- Enable RLS on all tables
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.user_settings ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.heart_rate_readings ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.sleep_sessions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.temperature_readings ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.typing_speed_tests ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.reaction_time_tests ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.cognitive_test_sessions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.energy_predictions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.energy_prediction_factors ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.productivity_suggestions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.ai_schedules ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.scheduled_tasks ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.weekly_insights ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.daily_summaries ENABLE ROW LEVEL SECURITY;

-- ============================================================================
-- RLS POLICIES
-- ============================================================================

-- Profiles policies
CREATE POLICY "Users can view own profile" ON public.profiles
    FOR SELECT USING (auth.uid() = id);

CREATE POLICY "Users can update own profile" ON public.profiles
    FOR UPDATE USING (auth.uid() = id);

CREATE POLICY "Users can insert own profile" ON public.profiles
    FOR INSERT WITH CHECK (auth.uid() = id);

-- User settings policies
CREATE POLICY "Users can manage own settings" ON public.user_settings
    FOR ALL USING (auth.uid() = user_id);

-- Heart rate policies
CREATE POLICY "Users can manage own heart rate data" ON public.heart_rate_readings
    FOR ALL USING (auth.uid() = user_id);

-- Sleep sessions policies
CREATE POLICY "Users can manage own sleep data" ON public.sleep_sessions
    FOR ALL USING (auth.uid() = user_id);

-- Temperature policies
CREATE POLICY "Users can manage own temperature data" ON public.temperature_readings
    FOR ALL USING (auth.uid() = user_id);

-- Typing speed policies
CREATE POLICY "Users can manage own typing tests" ON public.typing_speed_tests
    FOR ALL USING (auth.uid() = user_id);

-- Reaction time policies
CREATE POLICY "Users can manage own reaction time tests" ON public.reaction_time_tests
    FOR ALL USING (auth.uid() = user_id);

-- Cognitive test sessions policies
CREATE POLICY "Users can manage own cognitive sessions" ON public.cognitive_test_sessions
    FOR ALL USING (auth.uid() = user_id);

-- Energy predictions policies
CREATE POLICY "Users can manage own energy predictions" ON public.energy_predictions
    FOR ALL USING (auth.uid() = user_id);

-- Energy prediction factors policies
CREATE POLICY "Users can view own prediction factors" ON public.energy_prediction_factors
    FOR SELECT USING (
        EXISTS (
            SELECT 1 FROM public.energy_predictions
            WHERE energy_predictions.id = energy_prediction_factors.prediction_id
            AND energy_predictions.user_id = auth.uid()
        )
    );

CREATE POLICY "Users can insert own prediction factors" ON public.energy_prediction_factors
    FOR INSERT WITH CHECK (
        EXISTS (
            SELECT 1 FROM public.energy_predictions
            WHERE energy_predictions.id = energy_prediction_factors.prediction_id
            AND energy_predictions.user_id = auth.uid()
        )
    );

-- Productivity suggestions policies
CREATE POLICY "Users can manage own suggestions" ON public.productivity_suggestions
    FOR ALL USING (auth.uid() = user_id);

-- AI schedules policies
CREATE POLICY "Users can manage own schedules" ON public.ai_schedules
    FOR ALL USING (auth.uid() = user_id);

-- Scheduled tasks policies
CREATE POLICY "Users can manage own scheduled tasks" ON public.scheduled_tasks
    FOR ALL USING (
        EXISTS (
            SELECT 1 FROM public.ai_schedules
            WHERE ai_schedules.id = scheduled_tasks.schedule_id
            AND ai_schedules.user_id = auth.uid()
        )
    );

-- Weekly insights policies
CREATE POLICY "Users can manage own weekly insights" ON public.weekly_insights
    FOR ALL USING (auth.uid() = user_id);

-- Daily summaries policies
CREATE POLICY "Users can manage own daily summaries" ON public.daily_summaries
    FOR ALL USING (auth.uid() = user_id);

-- ============================================================================
-- FUNCTIONS & TRIGGERS
-- ============================================================================

-- Function to automatically update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Triggers for updated_at
CREATE TRIGGER update_profiles_updated_at 
    BEFORE UPDATE ON public.profiles
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_user_settings_updated_at 
    BEFORE UPDATE ON public.user_settings
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Function to calculate sleep duration if not provided
CREATE OR REPLACE FUNCTION calculate_sleep_duration()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.sleep_end IS NOT NULL AND NEW.duration_minutes IS NULL THEN
        NEW.duration_minutes := EXTRACT(EPOCH FROM (NEW.sleep_end - NEW.sleep_start)) / 60;
    END IF;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER calculate_sleep_duration_trigger
    BEFORE INSERT OR UPDATE ON public.sleep_sessions
    FOR EACH ROW EXECUTE FUNCTION calculate_sleep_duration();

-- Function to create default user settings on profile creation
CREATE OR REPLACE FUNCTION create_default_user_settings()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO public.user_settings (user_id)
    VALUES (NEW.id)
    ON CONFLICT (user_id) DO NOTHING;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER create_user_settings_on_profile
    AFTER INSERT ON public.profiles
    FOR EACH ROW EXECUTE FUNCTION create_default_user_settings();
