package com.personaleenergy.app.util;

import org.junit.AssumptionViolatedException;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * Test rule to skip tests on Windows when Robolectric has issues
 * This is a workaround for Robolectric's POSIX permissions issue on Windows
 */
public class WindowsSkipRule implements TestRule {
    
    private static boolean isWindows() {
        String osName = System.getProperty("os.name", "").toLowerCase();
        return osName.contains("win");
    }
    
    @Override
    public Statement apply(Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                // Check if we're on Windows
                if (isWindows()) {
                    // Skip test on Windows if it uses Robolectric
                    // (Robolectric tests are annotated with @RunWith(RobolectricTestRunner.class))
                    throw new AssumptionViolatedException(
                        "Skipping Robolectric test on Windows due to POSIX permissions issue. " +
                        "Run tests on Linux/Mac or use WSL for full test coverage."
                    );
                }
                
                // Otherwise, run the test
                try {
                    base.evaluate();
                } catch (UnsupportedOperationException e) {
                    if (e.getMessage() != null && 
                        e.getMessage().contains("posix:permissions")) {
                        // Skip if it's the POSIX permissions error
                        throw new AssumptionViolatedException(
                            "Skipping test on Windows: " + e.getMessage(), e
                        );
                    }
                    throw e;
                }
            }
        };
    }
    
    /**
     * Static helper to check if we should skip Robolectric tests
     */
    public static boolean shouldSkipRobolectricTests() {
        return isWindows();
    }
}

