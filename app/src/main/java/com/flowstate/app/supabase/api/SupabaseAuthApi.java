package com.flowstate.app.supabase.api;

import retrofit2.Call;
import retrofit2.http.*;

/**
 * Retrofit interface for Supabase Authentication API
 */
public interface SupabaseAuthApi {
    
    @POST("/auth/v1/signup")
    Call<AuthResponse> signUp(@Body SignUpRequest request);
    
    @POST("/auth/v1/token?grant_type=password")
    Call<AuthResponse> signIn(@Body SignInRequest request);
    
    @POST("/auth/v1/logout")
    Call<Void> signOut(@Header("Authorization") String authorization);
    
    @POST("/auth/v1/recover")
    Call<Void> resetPassword(@Body PasswordResetRequest request);
    
    @GET("/auth/v1/user")
    Call<UserResponse> getCurrentUser(@Header("Authorization") String authorization);
    
    @POST("/auth/v1/refresh")
    Call<AuthResponse> refreshToken(@Body RefreshTokenRequest request);
    
    // Request/Response models
    class SignUpRequest {
        public String email;
        public String password;
        public SignUpData data;
        
        public SignUpRequest(String email, String password, String username) {
            this.email = email;
            this.password = password;
            if (username != null) {
                this.data = new SignUpData();
                this.data.username = username;
            }
        }
    }
    
    class SignUpData {
        public String username;
    }
    
    class SignInRequest {
        public String email;
        public String password;
        
        public SignInRequest(String email, String password) {
            this.email = email;
            this.password = password;
        }
    }
    
    class PasswordResetRequest {
        public String email;
        
        public PasswordResetRequest(String email) {
            this.email = email;
        }
    }
    
    class RefreshTokenRequest {
        public String refresh_token;
        
        public RefreshTokenRequest(String refreshToken) {
            this.refresh_token = refreshToken;
        }
    }
    
    class AuthResponse {
        public String access_token;
        public String refresh_token;
        public UserResponse user;
        public String expires_in;
        public String token_type;
    }
    
    class UserResponse {
        public String id;
        public String email;
        public String phone;
        public String aud;
        public String role;
        public String created_at;
        public String updated_at;
        public UserMetadata user_metadata;
    }
    
    class UserMetadata {
        public String username;
    }
}

