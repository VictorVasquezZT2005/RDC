package xyz.zt.rdc;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.slider.Slider;
import com.google.firebase.auth.FirebaseAuth;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class OnboardingActivity extends BaseActivity {

    private MaterialCheckBox cbTerms, cbNotifications;
    private MaterialButton btnNext;
    private TextView tvInstruction, tvFontSizeLabel;
    private SharedPreferences sharedPreferences;
    private RadioGroup radioGroupFont, radioGroupTheme;
    private Slider sliderFontSize;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Toast.makeText(this, "Notificaciones habilitadas", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Las notificaciones están desactivadas", Toast.LENGTH_SHORT).show();
                }
                finishOnboarding();
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        sharedPreferences = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);

        // Verificar si ya se mostró el onboarding
        boolean isFirstRun = sharedPreferences.getBoolean("FirstRun", true);
        if (!isFirstRun) {
            proceedToNextActivity();
            return;
        }

        super.onCreate(savedInstanceState);

        String savedFont = sharedPreferences.getString("AppFont", "ubuntu");
        int themeMode = sharedPreferences.getInt("ThemeMode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);

        setContentView(R.layout.activity_onboarding);

        // Apply insets to avoid overlapping with status bar/notch
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(insets.left, insets.top, insets.right, insets.bottom);
            return windowInsets;
        });

        // Views
        cbTerms = findViewById(R.id.cbTerms);
        cbNotifications = findViewById(R.id.cbNotifications);
        btnNext = findViewById(R.id.btnOnboardingNext);
        tvInstruction = findViewById(R.id.tvInstruction);
        tvFontSizeLabel = findViewById(R.id.tvFontSizeLabelOnboarding);
        radioGroupFont = findViewById(R.id.radioGroupFontOnboarding);
        radioGroupTheme = findViewById(R.id.radioGroupThemeOnboarding);
        sliderFontSize = findViewById(R.id.sliderFontSizeOnboarding);

        // Initial State Font
        if ("caveat".equals(savedFont)) {
            ((RadioButton) findViewById(R.id.radioFontCaveatOnboarding)).setChecked(true);
        } else {
            ((RadioButton) findViewById(R.id.radioFontUbuntuOnboarding)).setChecked(true);
        }

        radioGroupFont.setOnCheckedChangeListener((group, checkedId) -> {
            String font = (checkedId == R.id.radioFontCaveatOnboarding) ? "caveat" : "ubuntu";
            sharedPreferences.edit().putString("AppFont", font).apply();
            recreate();
        });

        // Initial State Theme
        if (themeMode == AppCompatDelegate.MODE_NIGHT_NO) {
            ((RadioButton) findViewById(R.id.radioLightOnboarding)).setChecked(true);
        } else if (themeMode == AppCompatDelegate.MODE_NIGHT_YES) {
            ((RadioButton) findViewById(R.id.radioDarkOnboarding)).setChecked(true);
        } else {
            ((RadioButton) findViewById(R.id.radioSystemOnboarding)).setChecked(true);
        }

        radioGroupTheme.setOnCheckedChangeListener((group, checkedId) -> {
            int mode;
            if (checkedId == R.id.radioLightOnboarding) {
                mode = AppCompatDelegate.MODE_NIGHT_NO;
            } else if (checkedId == R.id.radioDarkOnboarding) {
                mode = AppCompatDelegate.MODE_NIGHT_YES;
            } else {
                mode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
            }
            sharedPreferences.edit().putInt("ThemeMode", mode).apply();
            AppCompatDelegate.setDefaultNightMode(mode);
        });

        // Initial State Font Size
        float savedFontScale = sharedPreferences.getFloat("FontScale", 1.0f);
        sliderFontSize.setValue(savedFontScale);
        updateFontSizeLabel(savedFontScale);

        sliderFontSize.addOnChangeListener((slider, value, fromUser) -> {
            if (fromUser) {
                updateFontSizeLabel(value);
                sharedPreferences.edit().putFloat("FontScale", value).apply();
                recreate();
            }
        });

        cbTerms.setOnCheckedChangeListener((buttonView, isChecked) -> {
            btnNext.setEnabled(isChecked);
            tvInstruction.setVisibility(isChecked ? View.GONE : View.VISIBLE);
        });

        btnNext.setOnClickListener(v -> {
            if (cbNotifications.isChecked() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                } else {
                    finishOnboarding();
                }
            } else {
                finishOnboarding();
            }
        });
    }

    private void updateFontSizeLabel(float scale) {
        int percentage = Math.round(scale * 100);
        String label = percentage + "%";
        if (scale == 1.0f) label = "Normal (100%)";
        else if (scale < 1.0f) label = "Pequeña (" + percentage + "%)";
        else if (scale > 1.2f) label = "Grande (" + percentage + "%)";
        tvFontSizeLabel.setText(label);
    }

    private void finishOnboarding() {
        sharedPreferences.edit().putBoolean("FirstRun", false).apply();
        proceedToNextActivity();
    }

    private void proceedToNextActivity() {
        Intent intent;
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            intent = new Intent(this, MainActivity.class);
        } else {
            intent = new Intent(this, LoginActivity.class);
        }
        startActivity(intent);
        finish();
    }
}