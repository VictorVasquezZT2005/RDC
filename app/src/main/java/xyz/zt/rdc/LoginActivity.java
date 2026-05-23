package xyz.zt.rdc;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import android.view.WindowManager;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText editTextEmail, editTextPassword;
    private Button buttonLogin, buttonForgotPassword;
    private FirebaseAuth mAuth;

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
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        String savedFont = prefs.getString("AppFont", "ubuntu");
        if ("caveat".equals(savedFont)) {
            setTheme(R.style.AppTheme_Caveat);
        } else {
            setTheme(R.style.AppTheme_Ubuntu);
        }
        
        super.onCreate(savedInstanceState);
        
        // Soporte para Notch y Edge-to-Edge
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            getWindow().getAttributes().layoutInDisplayCutoutMode = 
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }

        setContentView(R.layout.activity_login);

        // Aplicar insets al layout raíz para evitar superposición con notch y barras de sistema
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(insets.left, insets.top, insets.right, insets.bottom);
            return windowInsets;
        });

        mAuth = FirebaseAuth.getInstance();

        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        buttonLogin = findViewById(R.id.buttonLogin);
        buttonForgotPassword = findViewById(R.id.buttonForgotPassword);

        buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginUser();
            }
        });

        buttonForgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                forgotPassword();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
        }
    }

    private void loginUser() {
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            editTextEmail.setError("Correo requerido");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            editTextPassword.setError("Contraseña requerida");
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                // Check if user is disabled in Firestore
                                FirebaseFirestore.getInstance().collection("users").document(user.getUid())
                                        .get().addOnSuccessListener(documentSnapshot -> {
                                            Boolean isDisabled = documentSnapshot.getBoolean("disabled");
                                            if (isDisabled != null && isDisabled) {
                                                mAuth.signOut();
                                                Toast.makeText(LoginActivity.this, "Esta cuenta ha sido deshabilitada.", Toast.LENGTH_LONG).show();
                                            } else {
                                                startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                                finish();
                                            }
                                        }).addOnFailureListener(e -> {
                                            // If check fails, allow entry but log error, or you can be stricter
                                            startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                            finish();
                                        });
                            }
                        } else {
                            Toast.makeText(LoginActivity.this, "Error de autenticación: " + (task.getException() != null ? task.getException().getMessage() : "Error desconocido"),
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void forgotPassword() {
        android.widget.EditText emailInput = new android.widget.EditText(this);
        emailInput.setHint("ejemplo@correo.com");
        emailInput.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        
        android.widget.FrameLayout container = new android.widget.FrameLayout(this);
        android.widget.FrameLayout.LayoutParams params = new android.widget.FrameLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT, 
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.leftMargin = getResources().getDimensionPixelSize(R.dimen.dialog_margin);
        params.rightMargin = getResources().getDimensionPixelSize(R.dimen.dialog_margin);
        emailInput.setLayoutParams(params);
        container.addView(emailInput);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Recuperar Contraseña")
                .setMessage("Ingresa tu correo electrónico para recibir un enlace de restablecimiento:")
                .setView(container)
                .setPositiveButton("Enviar Correo", (dialog, which) -> {
                    String email = emailInput.getText().toString().trim();
                    if (TextUtils.isEmpty(email)) {
                        Toast.makeText(LoginActivity.this, "Debes ingresar un correo", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    sendResetEmail(email);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void sendResetEmail(String email) {
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(LoginActivity.this, "Se ha enviado un correo para restablecer tu contraseña", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(LoginActivity.this, "Error: " + (task.getException() != null ? task.getException().getMessage() : "No se pudo enviar el correo"), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}