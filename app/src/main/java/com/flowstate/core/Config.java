package com.flowstate.core;

import com.flowstate.app.BuildConfig;

/**
 * Global constants for the application
 * 
 * All API credentials are loaded from BuildConfig, which reads from local.properties
 * 
 * To add your credentials, add them to local.properties:
 *   SUPABASE_URL=https://your-project.supabase.co
 *   SUPABASE_ANON_KEY=your-anon-key
 *   OPENAI_API_KEY=your-openai-key (optional)
 */
public final class Config {
    
    /**
     * Supabase project URL
     * Get this from your Supabase project: Settings -> API -> Project URL
     */
    public static final String SUPABASE_URL = BuildConfig.SUPABASE_URL;
    
    /**
     * Supabase anonymous/public API key
     * Get this from your Supabase project: Settings -> API -> Project API keys -> anon/public
     */
    public static final String SUPABASE_ANON_KEY = BuildConfig.SUPABASE_ANON_KEY;
    
    /**
     * OpenAI API key (may be null if not configured)
     * Get this from https://platform.openai.com/api-keys
     * Add to local.properties: OPENAI_API_KEY=your-key
     */
    public static final String OPENAI_API_KEY = BuildConfig.OPENAI_API_KEY;
    
    /**
     * HIBP Password Checker Service URL
     * URL of your deployed HIBP password checking service
     * Add to local.properties: HIBP_SERVICE_URL=https://your-hibp-service.com
     * If not set, defaults to placeholder (password checking will fail gracefully)
     */
    public static final String HIBP_SERVICE_URL = BuildConfig.HIBP_SERVICE_URL != null 
            ? BuildConfig.HIBP_SERVICE_URL 
            : "https://your-hibp-service.com";
    
    /**
     * Remote Model API URL for energy level predictions
     * Add to local.properties: REMOTE_MODEL_URL=https://router.huggingface.co/models/google/timesfm-1.0-200m
     * Defaults to HuggingFace TimesFM model endpoint
     */
    public static final String REMOTE_MODEL_URL = BuildConfig.REMOTE_MODEL_URL;
    
    /**
     * Remote Model API Key (optional, for authenticated endpoints)
     * Add to local.properties: REMOTE_MODEL_API_KEY=your-api-key
     */
    public static final String REMOTE_MODEL_API_KEY = BuildConfig.REMOTE_MODEL_API_KEY != null 
            ? BuildConfig.REMOTE_MODEL_API_KEY 
            : null;
    
    /**
     * Google Gemini API key for energy level predictions
     * Get this from https://aistudio.google.com/app/apikey
     * Add to local.properties: GEMINI_API_KEY=your-gemini-api-key
     */
    public static final String GEMINI_API_KEY = BuildConfig.GEMINI_API_KEY != null 
            ? BuildConfig.GEMINI_API_KEY 
            : null;
    
    // Prevent instantiation
    private Config() {
        throw new AssertionError("Cannot instantiate Config class");
    }
}

