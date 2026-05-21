package xyz.zt.rdc;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import android.content.res.Configuration;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.firebase.auth.FirebaseAuth;

public class OnboardingActivity extends AppCompatActivity {

    private MaterialCheckBox cbTerms;
    private MaterialButton btnNext;
    private TextView tvInstruction;
    private SharedPreferences sharedPreferences;

    @Override
    protected void attachBaseContext(Context newBase) {
        SharedPreferences prefs = newBase.getSharedPreferences("AppPrefs", MODE_PRIVATE);
        float fontScale = prefs.getFloat("FontScale", 1.0f);
        Configuration configuration = newBase.getResources().getConfiguration();
        configuration.fontScale = fontScale;
        super.attachBaseContext(newBase);
        newBase.getResources().updateConfiguration(configuration, newBase.getResources().getDisplayMetrics());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPreferences = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        
        // Verificar si ya se mostró el onboarding
        boolean isFirstRun = sharedPreferences.getBoolean("FirstRun", true);
        if (!isFirstRun) {
            proceedToNextActivity();
            return;
        }

        // Cargar tema
        int themeMode = sharedPreferences.getInt("ThemeMode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        AppCompatDelegate.setDefaultNightMode(themeMode);

        setContentView(R.layout.activity_onboarding);

        cbTerms = findViewById(R.id.cbTerms);
        btnNext = findViewById(R.id.btnOnboardingNext);
        tvInstruction = findViewById(R.id.tvInstruction);

        cbTerms.setOnCheckedChangeListener((buttonView, isChecked) -> {
            btnNext.setEnabled(isChecked);
            tvInstruction.setVisibility(isChecked ? View.GONE : View.VISIBLE);
        });

        btnNext.setOnClickListener(v -> {
            // Guardar que ya se vio el onboarding
            sharedPreferences.edit().putBoolean("FirstRun", false).apply();
            proceedToNextActivity();
        });
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