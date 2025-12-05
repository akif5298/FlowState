package com.flowstate.app.supabase;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/**
 * Instrumented tests for AuthService
 */
@RunWith(AndroidJUnit4.class)
public class AuthServiceTest {
    
    private Context context;
    private AuthService authService;
    
    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        authService = new AuthService(context);
    }
    
    @Test
    public void testServiceInitialization() {
        assertNotNull(authService);
    }
    
    @Test
    public void testGetCurrentUserWhenNotLoggedIn() {
        // When not logged in, should return null or empty
        String userId = authService.getCurrentUserId();
        // Either null or empty string is acceptable
        assertTrue(userId == null || userId.isEmpty());
    }
}

