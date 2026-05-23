package xyz.zt.rdc;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.view.WindowCompat;

public class BaseActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(Context newBase) {
        SharedPreferences prefs = newBase.getSharedPreferences("AppPrefs", MODE_PRIVATE);
        float fontScale = prefs.getFloat("FontScale", 1.0f);
        
        Configuration configuration = newBase.getResources().getConfiguration();
        configuration.fontScale = fontScale;
        
        super.attachBaseContext(newBase);
        
        // Apply to the resources directly (compatible approach)
        newBase.getResources().updateConfiguration(configuration, newBase.getResources().getDisplayMetrics());
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        
        // Apply theme mode
        int themeMode = prefs.getInt("ThemeMode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        AppCompatDelegate.setDefaultNightMode(themeMode);
        
        // Apply font theme
        String savedFont = prefs.getString("AppFont", "ubuntu");
        if ("caveat".equals(savedFont)) {
            setTheme(R.style.AppTheme_Caveat);
        } else {
            setTheme(R.style.AppTheme_Ubuntu);
        }

        // Notch and Edge-to-Edge support
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            getWindow().getAttributes().layoutInDisplayCutoutMode = 
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }

        super.onCreate(savedInstanceState);
    }
}
