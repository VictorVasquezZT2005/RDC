package xyz.zt.rdc;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class SettingsFragment extends Fragment {

    private RadioGroup radioGroupTheme;
    private RadioButton radioSystem, radioLight, radioDark;
    private com.google.android.material.slider.Slider sliderFontSize;
    private Button btnLogout, btnEditName, btnChangePassword;
    private TextView tvUserEmail, tvUserName, tvUserRole, tvUserSchool, tvUserAssignments, tvFontSizeLabel;
    private View layoutTeacherInfo;
    private SharedPreferences sharedPreferences;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        radioGroupTheme = view.findViewById(R.id.radioGroupTheme);
        radioSystem = view.findViewById(R.id.radioSystem);
        radioLight = view.findViewById(R.id.radioLight);
        radioDark = view.findViewById(R.id.radioDark);
        btnLogout = view.findViewById(R.id.btnLogout);
        btnEditName = view.findViewById(R.id.btnEditName);
        btnChangePassword = view.findViewById(R.id.btnChangePassword);
        tvUserEmail = view.findViewById(R.id.tvUserEmail);
        tvUserName = view.findViewById(R.id.tvUserName);
        tvUserRole = view.findViewById(R.id.tvUserRole);
        tvUserSchool = view.findViewById(R.id.tvUserSchool);
        tvUserAssignments = view.findViewById(R.id.tvUserAssignments);
        tvFontSizeLabel = view.findViewById(R.id.tvFontSizeLabel);
        sliderFontSize = view.findViewById(R.id.sliderFontSize);
        layoutTeacherInfo = view.findViewById(R.id.layoutTeacherInfo);

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            tvUserEmail.setText(user.getEmail());
            loadCurrentUserData(user.getEmail());
        }

        sharedPreferences = getActivity().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE);
        int savedTheme = sharedPreferences.getInt("ThemeMode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        float savedFontScale = sharedPreferences.getFloat("FontScale", 1.0f);

        sliderFontSize.setValue(savedFontScale);
        updateFontSizeLabel(savedFontScale);

        sliderFontSize.addOnChangeListener((slider, value, fromUser) -> {
            if (fromUser) {
                updateFontSizeLabel(value);
                sharedPreferences.edit().putFloat("FontScale", value).apply();
                if (getActivity() != null) {
                    getActivity().recreate();
                }
            }
        });

        if (savedTheme == AppCompatDelegate.MODE_NIGHT_NO) {
            radioLight.setChecked(true);
        } else if (savedTheme == AppCompatDelegate.MODE_NIGHT_YES) {
            radioDark.setChecked(true);
        } else {
            radioSystem.setChecked(true);
        }

        radioGroupTheme.setOnCheckedChangeListener((group, checkedId) -> {
            int mode;
            if (checkedId == R.id.radioLight) {
                mode = AppCompatDelegate.MODE_NIGHT_NO;
            } else if (checkedId == R.id.radioDark) {
                mode = AppCompatDelegate.MODE_NIGHT_YES;
            } else {
                mode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
            }
            
            AppCompatDelegate.setDefaultNightMode(mode);
            sharedPreferences.edit().putInt("ThemeMode", mode).apply();
        });

        btnEditName.setOnClickListener(v -> showEditNameDialog());
        btnChangePassword.setOnClickListener(v -> showChangePasswordDialog());

        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            getActivity().finish();
        });

        // Ocultar BottomNav al entrar
        toggleBottomNav(false);

        return view;
    }

    private void toggleBottomNav(boolean show) {
        if (getActivity() != null) {
            com.google.android.material.bottomnavigation.BottomNavigationView bottomNav = getActivity().findViewById(R.id.bottom_navigation);
            if (bottomNav != null) {
                bottomNav.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Mostrar BottomNav al salir
        toggleBottomNav(true);
    }

    private void updateFontSizeLabel(float scale) {
        int percentage = Math.round(scale * 100);
        String label = percentage + "%";
        if (scale == 1.0f) label = "Normal (100%)";
        else if (scale < 1.0f) label = "Pequeña (" + percentage + "%)";
        else if (scale > 1.2f) label = "Grande (" + percentage + "%)";
        tvFontSizeLabel.setText(label);
    }

    private void showEditNameDialog() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        TextInputEditText input = new TextInputEditText(getContext());
        input.setHint("Nombre Completo");

        // Pre-cargar nombre actual
        db.collection("users").whereEqualTo("email", user.getEmail()).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        input.setText(queryDocumentSnapshots.getDocuments().get(0).getString("name"));
                    }
                });

        android.widget.FrameLayout container = new android.widget.FrameLayout(getContext());
        android.widget.FrameLayout.LayoutParams params = new android.widget.FrameLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.leftMargin = getResources().getDimensionPixelSize(R.dimen.dialog_margin);
        params.rightMargin = getResources().getDimensionPixelSize(R.dimen.dialog_margin);
        input.setLayoutParams(params);
        container.addView(input);

        new MaterialAlertDialogBuilder(getContext())
                .setTitle("Cambiar Nombre")
                .setView(container)
                .setPositiveButton("Actualizar", (dialog, which) -> {
                    String newName = input.getText().toString().trim();
                    if (!newName.isEmpty()) {
                        updateUserName(user.getEmail(), newName);
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void loadCurrentUserData(String email) {
        db.collection("users").whereEqualTo("email", email).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        com.google.firebase.firestore.DocumentSnapshot doc = queryDocumentSnapshots.getDocuments().get(0);
                        String name = doc.getString("name");
                        String role = doc.getString("role");
                        String schoolCode = doc.getString("schoolCode");
                        java.util.List<String> assignments = (java.util.List<String>) doc.get("assignments");
                        
                        tvUserName.setText(name);
                        
                        if (schoolCode != null) {
                            loadSchoolName(schoolCode);
                        } else {
                            tvUserSchool.setVisibility(View.GONE);
                        }

                        if (role != null) {
                            String displayRole = role;
                            // Map internal keys to friendly labels
                            switch (role) {
                                case "teacher": 
                                    displayRole = "Maestro"; 
                                    showTeacherInfo(assignments);
                                    break;
                                case "admin": displayRole = "Administrador (Dev)"; break;
                                case "principal": displayRole = "Director"; break;
                                case "sub_principal": displayRole = "Sub-Director"; break;
                                case "administrative_assistant": displayRole = "Asistente Administrativo"; break;
                                case "student": displayRole = "Alumno"; break;
                            }
                            tvUserRole.setText("Rol: " + displayRole);
                        }
                    }
                });
    }

    private void loadSchoolName(String schoolCode) {
        db.collection("schools").whereEqualTo("code", schoolCode).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        String schoolName = queryDocumentSnapshots.getDocuments().get(0).getString("name");
                        tvUserSchool.setText("Escuela: " + schoolName);
                        tvUserSchool.setVisibility(View.VISIBLE);
                    } else {
                        tvUserSchool.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> tvUserSchool.setVisibility(View.GONE));
    }

    private void showTeacherInfo(java.util.List<String> assignments) {
        if (assignments != null && !assignments.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < assignments.size(); i++) {
                sb.append("• ").append(assignments.get(i));
                if (i < assignments.size() - 1) sb.append("\n");
            }
            tvUserAssignments.setText(sb.toString());
            layoutTeacherInfo.setVisibility(View.VISIBLE);
        } else {
            layoutTeacherInfo.setVisibility(View.GONE);
        }
    }

    private void updateUserName(String email, String newName) {
        db.collection("users").whereEqualTo("email", email).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        String docId = queryDocumentSnapshots.getDocuments().get(0).getId();
                        db.collection("users").document(docId).update("name", newName)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(getContext(), "Nombre actualizado", Toast.LENGTH_SHORT).show();
                                    tvUserName.setText(newName);
                                })
                                .addOnFailureListener(e -> Toast.makeText(getContext(), "Error al actualizar", Toast.LENGTH_SHORT).show());
                    }
                });
    }

    private void showChangePasswordDialog() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        TextInputEditText input = new TextInputEditText(getContext());
        input.setHint("Nueva Contraseña");
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);

        android.widget.FrameLayout container = new android.widget.FrameLayout(getContext());
        android.widget.FrameLayout.LayoutParams params = new android.widget.FrameLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.leftMargin = getResources().getDimensionPixelSize(R.dimen.dialog_margin);
        params.rightMargin = getResources().getDimensionPixelSize(R.dimen.dialog_margin);
        input.setLayoutParams(params);
        container.addView(input);

        new MaterialAlertDialogBuilder(getContext())
                .setTitle("Cambiar Contraseña")
                .setMessage("Ingresa tu nueva contraseña:")
                .setView(container)
                .setPositiveButton("Cambiar", (dialog, which) -> {
                    String newPassword = input.getText().toString().trim();
                    if (newPassword.length() >= 6) {
                        user.updatePassword(newPassword)
                                .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Contraseña actualizada", Toast.LENGTH_SHORT).show())
                                .addOnFailureListener(e -> Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
                    } else {
                        Toast.makeText(getContext(), "Mínimo 6 caracteres", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}