package com.example.mywallet;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * SplashActivity presents the visual landing screen of My Wallet Tracker.
 * It handles biometric authentication directly on the landing screen if App Lock is enabled,
 * then smoothly transitions to MainActivity.
 */
public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DURATION_MS = 1800;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean isNavigated = false;
    private boolean isAppLocked = false;

    private View btnUnlockFingerprint;

    private final Runnable navigateRunnable = this::navigateToMain;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.splashMain), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        View layoutCenter = findViewById(R.id.layoutCenterBrand);
        View tvFooter = findViewById(R.id.tvSplashFooter);
        btnUnlockFingerprint = findViewById(R.id.btnUnlockFingerprint);

        if (layoutCenter != null) {
            layoutCenter.setAlpha(0f);
            layoutCenter.setScaleX(0.88f);
            layoutCenter.setScaleY(0.88f);

            layoutCenter.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(900)
                    .setInterpolator(new DecelerateInterpolator())
                    .start();
        }

        if (tvFooter != null) {
            tvFooter.setAlpha(0f);
            tvFooter.animate()
                    .alpha(1f)
                    .setDuration(1100)
                    .setStartDelay(300)
                    .start();
        }

        // Check if biometric lock is configured
        SharedPreferences prefs = getSharedPreferences("my_wallet_prefs", MODE_PRIVATE);
        isAppLocked = prefs.getBoolean("is_app_locked", false);

        View root = findViewById(R.id.splashMain);

        if (isAppLocked) {
            if (btnUnlockFingerprint != null) {
                btnUnlockFingerprint.setVisibility(View.VISIBLE);
                btnUnlockFingerprint.setOnClickListener(v -> promptBiometricAuthentication());
            }

            if (root != null) {
                root.setOnClickListener(v -> promptBiometricAuthentication());
            }

            // Prompt for biometric unlock on the landing page after a brief entrance animation
            handler.postDelayed(this::promptBiometricAuthentication, 500);

        } else {
            if (btnUnlockFingerprint != null) {
                btnUnlockFingerprint.setVisibility(View.GONE);
            }

            if (root != null) {
                root.setOnClickListener(v -> navigateToMain());
            }

            // Automatic transition after normal splash duration
            handler.postDelayed(navigateRunnable, SPLASH_DURATION_MS);
        }
    }

    private void promptBiometricAuthentication() {
        if (isFinishing() || isDestroyed() || isNavigated) return;

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock My Wallet Tracker")
                .setSubtitle("Confirm your fingerprint or biometric credentials to continue")
                .setNegativeButtonText("Exit")
                .build();

        BiometricPrompt biometricPrompt = new BiometricPrompt(SplashActivity.this,
                ContextCompat.getMainExecutor(SplashActivity.this),
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                        super.onAuthenticationError(errorCode, errString);
                        if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON || errorCode == BiometricPrompt.ERROR_USER_CANCELED) {
                            Toast.makeText(SplashActivity.this, "Authentication required to open MyWallet", Toast.LENGTH_SHORT).show();
                            finishAffinity();
                        }
                    }

                    @Override
                    public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                        super.onAuthenticationSucceeded(result);
                        MainActivity.isSessionAuthenticated = true;
                        navigateToMain();
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        super.onAuthenticationFailed();
                        Toast.makeText(SplashActivity.this, "Fingerprint not recognized. Try again.", Toast.LENGTH_SHORT).show();
                    }
                });

        biometricPrompt.authenticate(promptInfo);
    }

    private synchronized void navigateToMain() {
        if (isNavigated) return;
        isNavigated = true;
        handler.removeCallbacks(navigateRunnable);

        Intent intent = new Intent(SplashActivity.this, MainActivity.class);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(navigateRunnable);
    }
}
