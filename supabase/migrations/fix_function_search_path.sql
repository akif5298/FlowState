-- Migration: Fix function search_path security warnings
-- Run this in your Supabase SQL Editor to fix the security warnings

-- Fix update_updated_at_column function
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER
SET search_path = public
AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Fix calculate_sleep_duration function
CREATE OR REPLACE FUNCTION calculate_sleep_duration()
RETURNS TRIGGER
SET search_path = public
AS $$
BEGIN
    IF NEW.sleep_end IS NOT NULL AND NEW.duration_minutes IS NULL THEN
        NEW.duration_minutes := EXTRACT(EPOCH FROM (NEW.sleep_end - NEW.sleep_start)) / 60;
    END IF;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Fix create_default_user_settings function
CREATE OR REPLACE FUNCTION create_default_user_settings()
RETURNS TRIGGER
SET search_path = public
AS $$
BEGIN
    INSERT INTO public.user_settings (user_id)
    VALUES (NEW.id)
    ON CONFLICT (user_id) DO NOTHING;
    RETURN NEW;
END;
$$ language 'plpgsql';

