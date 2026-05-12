package xyz.zt.rdc;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CreateUserActivity extends AppCompatActivity {

    private AutoCompleteTextView spinnerUserRole, spinnerSchool, spinnerUserGrade, spinnerUserSection, spinnerUserGender;
    private TextInputEditText etUserName, etUserEmail, etUserPassword, etStudentNIE;
    private TextInputLayout tilPassword, tilNIE;
    private LinearLayout layoutGradeSectionFields, layoutSchoolSelection, layoutAdminPasswordControl, layoutTeacherAssignments;
    private Button btnSaveUser, btnSendResetEmail, btnSelectAssignments;
    private TextView tvTitle, tvSelectedAssignments;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private boolean isCurrentEditorAdmin = false;
    private List<String> schoolNames = new ArrayList<>();
    private List<String> schoolCodes = new ArrayList<>();
    private List<String> teacherSelectedAssignments = new ArrayList<>();
    private String editingDocId = null;
    private String pendingSchoolCode = null;
    private String pendingGrade = null;
    private String pendingSection = null;
    private String pendingGender = null;

    private static final String ROLE_TEACHER = "teacher";
    private static final String ROLE_ADMIN = "admin";
    private static final String ROLE_DIRECTOR = "principal";
    private static final String ROLE_SUB_DIRECTOR = "sub_principal";
    private static final String ROLE_ADMIN_ASSISTANT = "administrative_assistant";
    private static final String ROLE_STUDENT = "student";

    private String[] roleLabels = {"Maestro", "Administrador (Dev)", "Director", "Sub-Director", "Asistente Administrativo", "Alumno"};
    private String[] roleKeys = {ROLE_TEACHER, ROLE_ADMIN, ROLE_DIRECTOR, ROLE_SUB_DIRECTOR, ROLE_ADMIN_ASSISTANT, ROLE_STUDENT};
    private String[] genderLabels = {"Masculino", "Femenino"};

    private Map<String, List<String>> schoolConfig = new HashMap<>();
    private List<String> currentGrades = new ArrayList<>();
    private List<String> currentSections = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_user);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        tvTitle = findViewById(R.id.tvTitle);
        spinnerUserRole = findViewById(R.id.spinnerUserRole);
        spinnerSchool = findViewById(R.id.spinnerSchool);
        etUserName = findViewById(R.id.etUserName);
        etUserEmail = findViewById(R.id.etUserEmail);
        etUserPassword = findViewById(R.id.etUserPassword);
        tilPassword = findViewById(R.id.tilPassword);
        tilNIE = findViewById(R.id.tilNIE);
        spinnerUserGrade = findViewById(R.id.spinnerUserGrade);
        spinnerUserSection = findViewById(R.id.spinnerUserSection);
        spinnerUserGender = findViewById(R.id.spinnerUserGender);
        etStudentNIE = findViewById(R.id.etStudentNIE);
        layoutGradeSectionFields = findViewById(R.id.layoutGradeSectionFields);
        layoutTeacherAssignments = findViewById(R.id.layoutTeacherAssignments);
        layoutSchoolSelection = findViewById(R.id.layoutSchoolSelection);
        layoutAdminPasswordControl = findViewById(R.id.layoutAdminPasswordControl);
        btnSaveUser = findViewById(R.id.btnSaveUser);
        btnSendResetEmail = findViewById(R.id.btnSendResetEmail);
        btnSelectAssignments = findViewById(R.id.btnSelectAssignments);
        tvSelectedAssignments = findViewById(R.id.tvSelectedAssignments);

        checkCurrentEditorRole();
        setupRolesSpinner();
        setupGenderSpinner();
        loadSchools();

        spinnerUserRole.setOnItemClickListener((parent, view, position, id) -> {
            String roleKey = roleKeys[position];
            // Only Alumno (ROLE_STUDENT) has grade/section now
            boolean isStudent = roleKey.equals(ROLE_STUDENT);
            boolean isTeacher = roleKey.equals(ROLE_TEACHER);
            
            layoutGradeSectionFields.setVisibility(isStudent ? View.VISIBLE : View.GONE);
            layoutTeacherAssignments.setVisibility(isTeacher ? View.VISIBLE : View.GONE);
            tilNIE.setVisibility(isStudent ? View.VISIBLE : View.GONE);
            layoutSchoolSelection.setVisibility(roleKey.equals(ROLE_ADMIN) ? View.GONE : View.VISIBLE);
            
            if (isStudent || isTeacher) {
                refreshSchoolConfig();
            }
        });

        spinnerSchool.setOnItemClickListener((parent, view, position, id) -> {
            refreshSchoolConfig();
        });

        spinnerUserGrade.setOnItemClickListener((parent, view, position, id) -> {
            updateSectionsSpinner();
        });

        editingDocId = getIntent().getStringExtra("docId");
        if (editingDocId != null) {
            loadUserData();
        }

        btnSaveUser.setOnClickListener(v -> saveUser());

        btnSelectAssignments.setOnClickListener(v -> showTeacherAssignmentsDialog());

        btnSendResetEmail.setOnClickListener(v -> {
            String email = etUserEmail.getText().toString().trim();
            if (!email.isEmpty()) {
                mAuth.sendPasswordResetEmail(email)
                        .addOnSuccessListener(aVoid -> Toast.makeText(this, "Correo de restablecimiento enviado", Toast.LENGTH_SHORT).show())
                        .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void showTeacherAssignmentsDialog() {
        if (schoolConfig == null || schoolConfig.isEmpty()) {
            Toast.makeText(this, "Primero selecciona una escuela con configuración", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> allOptions = new ArrayList<>();
        // Using currentGrades to maintain order
        for (String grade : currentGrades) {
            List<String> sections = schoolConfig.get(grade);
            if (sections != null) {
                for (String section : sections) {
                    allOptions.add(grade + " - " + section);
                }
            }
        }

        String[] optionsArray = allOptions.toArray(new String[0]);
        boolean[] checkedItems = new boolean[optionsArray.length];
        
        for (int i = 0; i < optionsArray.length; i++) {
            if (teacherSelectedAssignments.contains(optionsArray[i])) {
                checkedItems[i] = true;
            }
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle("Seleccionar Asignaciones")
                .setMultiChoiceItems(optionsArray, checkedItems, (dialog, which, isChecked) -> {
                    if (isChecked) {
                        if (!teacherSelectedAssignments.contains(optionsArray[which])) {
                            teacherSelectedAssignments.add(optionsArray[which]);
                        }
                    } else {
                        teacherSelectedAssignments.remove(optionsArray[which]);
                    }
                })
                .setPositiveButton("Aceptar", (dialog, which) -> {
                    updateSelectedAssignmentsText();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void updateSelectedAssignmentsText() {
        if (teacherSelectedAssignments.isEmpty()) {
            tvSelectedAssignments.setText("Ninguna seleccionada");
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < teacherSelectedAssignments.size(); i++) {
                sb.append(teacherSelectedAssignments.get(i));
                if (i < teacherSelectedAssignments.size() - 1) sb.append("\n");
            }
            tvSelectedAssignments.setText(sb.toString());
        }
    }

    private void refreshSchoolConfig() {
        String selectedSchool = spinnerSchool.getText().toString();
        if (selectedSchool.isEmpty()) return;
        
        int index = -1;
        for (int i = 0; i < schoolNames.size(); i++) {
            if (schoolNames.get(i).equals(selectedSchool)) {
                index = i;
                break;
            }
        }
        if (index == -1) return;
        String schoolCode = schoolCodes.get(index);

        db.collection("schools").document(schoolCode).get().addOnSuccessListener(doc -> {
            if (doc.exists() && doc.contains("config")) {
                schoolConfig = (Map<String, List<String>>) doc.get("config");
                updateGradesSpinner();
            } else {
                schoolConfig.clear();
                updateGradesSpinner();
            }
        });
    }

    private void updateGradesSpinner() {
        currentGrades.clear();
        if (schoolConfig != null) {
            currentGrades.addAll(schoolConfig.keySet());
        }

        // Sort grades in a natural order (Kinder -> 1 -> 9)
        java.util.Collections.sort(currentGrades, new java.util.Comparator<String>() {
            @Override
            public int compare(String s1, String s2) {
                return Integer.compare(getGradeWeight(s1), getGradeWeight(s2));
            }

            private int getGradeWeight(String grade) {
                if (grade.startsWith("Kinder 4")) return 1;
                if (grade.startsWith("Kinder 5")) return 2;
                if (grade.startsWith("Kinder 6")) return 3;
                if (grade.startsWith("1er")) return 4;
                if (grade.startsWith("2do")) return 5;
                if (grade.startsWith("3er")) return 6;
                if (grade.startsWith("4to")) return 7;
                if (grade.startsWith("5to")) return 8;
                if (grade.startsWith("6to")) return 9;
                if (grade.startsWith("7mo")) return 10;
                if (grade.startsWith("8vo")) return 11;
                if (grade.startsWith("9no")) return 12;
                return 99; // Default for unknown
            }
        });
        
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, currentGrades);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerUserGrade.setAdapter(adapter);

        if (pendingGrade != null) {
            spinnerUserGrade.setText(pendingGrade, false);
            pendingGrade = null;
        }
        updateSectionsSpinner();
    }

    private void updateSectionsSpinner() {
        currentSections.clear();
        String selectedGrade = spinnerUserGrade.getText().toString();
        if (!selectedGrade.isEmpty()) {
            List<String> sections = schoolConfig.get(selectedGrade);
            if (sections != null) {
                currentSections.addAll(sections);
            }
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, currentSections);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerUserSection.setAdapter(adapter);

        if (pendingSection != null) {
            spinnerUserSection.setText(pendingSection, false);
            pendingSection = null;
        }
    }

    private void checkCurrentEditorRole() {
        if (mAuth.getCurrentUser() != null) {
            db.collection("users").document(mAuth.getCurrentUser().getUid()).get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            String role = doc.getString("role");
                            isCurrentEditorAdmin = ROLE_ADMIN.equals(role);
                            updateAdminUiVisibility();
                        }
                    });
        }
    }

    private void updateAdminUiVisibility() {
        if (editingDocId != null && isCurrentEditorAdmin) {
            layoutAdminPasswordControl.setVisibility(View.VISIBLE);
        }
    }

    private void setupRolesSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, roleLabels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerUserRole.setAdapter(adapter);
    }

    private void setupGenderSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, genderLabels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerUserGender.setAdapter(adapter);
    }

    private void loadSchools() {
        db.collection("schools").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                schoolNames.clear();
                schoolCodes.clear();
                for (QueryDocumentSnapshot document : task.getResult()) {
                    schoolNames.add(document.getString("name"));
                    schoolCodes.add(document.getId());
                }
                ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, schoolNames);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerSchool.setAdapter(adapter);

                if (pendingSchoolCode != null) {
                    for (int i = 0; i < schoolCodes.size(); i++) {
                        if (schoolCodes.get(i).equals(pendingSchoolCode)) {
                            spinnerSchool.setText(schoolNames.get(i), false);
                            break;
                        }
                    }
                }
            }
        });
    }

    private void loadUserData() {
        db.collection("users").document(editingDocId).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                FirebaseAuth auth = FirebaseAuth.getInstance();
                if (auth.getCurrentUser() != null) {
                    String currentUid = auth.getCurrentUser().getUid();
                    String currentUserEmail = auth.getCurrentUser().getEmail();
                    String docEmail = doc.getString("email");

                    boolean isSelf = doc.getId().equals(currentUid) || 
                                     (currentUserEmail != null && currentUserEmail.equalsIgnoreCase(docEmail));

                    if (isSelf) {
                        Toast.makeText(this, "No puedes editar tu propio usuario", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }
                }

                tvTitle.setText("Editar Usuario");
                etUserName.setText(doc.getString("name"));
                etUserEmail.setText(doc.getString("email"));
                tilPassword.setVisibility(View.GONE);
                
                String roleKey = doc.getString("role");
                for (int i = 0; i < roleKeys.length; i++) {
                    if (roleKeys[i].equals(roleKey)) {
                        spinnerUserRole.setText(roleLabels[i], false);
                        break;
                    }
                }

                // Force visibility based on role
                boolean isStudent = ROLE_STUDENT.equals(roleKey);
                boolean isTeacher = ROLE_TEACHER.equals(roleKey);
                layoutGradeSectionFields.setVisibility(isStudent ? View.VISIBLE : View.GONE);
                layoutTeacherAssignments.setVisibility(isTeacher ? View.VISIBLE : View.GONE);
                tilNIE.setVisibility(isStudent ? View.VISIBLE : View.GONE);

                if (!ROLE_ADMIN.equals(roleKey)) {
                    pendingSchoolCode = doc.getString("schoolCode");
                    
                    if (spinnerSchool.getAdapter() != null) {
                        for (int i = 0; i < schoolCodes.size(); i++) {
                            if (schoolCodes.get(i).equals(pendingSchoolCode)) {
                                spinnerSchool.setText(schoolNames.get(i), false);
                                break;
                            }
                        }
                    }
                }

                if (ROLE_TEACHER.equals(roleKey)) {
                    List<String> assignments = (List<String>) doc.get("assignments");
                    if (assignments != null) {
                        teacherSelectedAssignments.clear();
                        teacherSelectedAssignments.addAll(assignments);
                        updateSelectedAssignmentsText();
                    }
                }

                if (ROLE_STUDENT.equals(roleKey)) {
                    pendingGrade = doc.getString("grade");
                    pendingSection = doc.getString("section");
                    pendingGender = doc.getString("gender");

                    if (pendingGrade != null) {
                        spinnerUserGrade.setText(pendingGrade, false);
                        pendingGrade = null;
                    }

                    if (pendingSection != null) {
                        spinnerUserSection.setText(pendingSection, false);
                        pendingSection = null;
                    }

                    if (pendingGender != null) {
                        spinnerUserGender.setText(pendingGender, false);
                        pendingGender = null;
                    }

                    etStudentNIE.setText(doc.getString("nie"));
                }
                btnSaveUser.setText("Actualizar");
            }
        });
    }

    private void saveUser() {
        String name = etUserName.getText().toString().trim();
        String email = etUserEmail.getText().toString().trim();
        
        String selectedRoleLabel = spinnerUserRole.getText().toString();
        int selectedPos = -1;
        for (int i = 0; i < roleLabels.length; i++) {
            if (roleLabels[i].equals(selectedRoleLabel)) {
                selectedPos = i;
                break;
            }
        }
        
        if (selectedPos == -1) {
            Toast.makeText(this, "Debe seleccionar un rol", Toast.LENGTH_SHORT).show();
            return;
        }
        String roleKey = roleKeys[selectedPos];

        if (name.isEmpty() || email.isEmpty()) {
            Toast.makeText(this, "Nombre y correo obligatorios", Toast.LENGTH_SHORT).show();
            return;
        }

        if (roleKey.equals(ROLE_TEACHER) || roleKey.equals(ROLE_DIRECTOR) || roleKey.equals(ROLE_SUB_DIRECTOR)) {
            if (!email.endsWith("@clases.edu.sv")) {
                etUserEmail.setError("Debe usar correo @clases.edu.sv");
                return;
            }
        }

        Map<String, Object> user = new HashMap<>();
        user.put("name", name);
        user.put("email", email);
        user.put("role", roleKey);

        if (!roleKey.equals(ROLE_ADMIN)) {
            String selectedSchoolName = spinnerSchool.getText().toString();
            int index = -1;
            for (int i = 0; i < schoolNames.size(); i++) {
                if (schoolNames.get(i).equals(selectedSchoolName)) {
                    index = i;
                    break;
                }
            }
            
            if (index != -1) {
                user.put("schoolCode", schoolCodes.get(index));
                user.put("schoolName", schoolNames.get(index));
            } else {
                Toast.makeText(this, "Debe seleccionar una escuela", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        if (roleKey.equals(ROLE_TEACHER)) {
            if (teacherSelectedAssignments.isEmpty()) {
                Toast.makeText(this, "Debe seleccionar al menos una asignación", Toast.LENGTH_SHORT).show();
                return;
            }
            user.put("assignments", teacherSelectedAssignments);
        }

        if (roleKey.equals(ROLE_STUDENT)) {
            String grade = spinnerUserGrade.getText().toString();
            if (!grade.isEmpty()) {
                user.put("grade", grade);
            } else {
                Toast.makeText(this, "Debe seleccionar un grado", Toast.LENGTH_SHORT).show();
                return;
            }

            String section = spinnerUserSection.getText().toString();
            if (!section.isEmpty()) {
                user.put("section", section);
            } else {
                Toast.makeText(this, "Debe seleccionar una sección", Toast.LENGTH_SHORT).show();
                return;
            }
            
            user.put("gender", spinnerUserGender.getText().toString());
            user.put("nie", etStudentNIE.getText().toString());
        }

        if (editingDocId != null) {
            db.collection("users").document(editingDocId).update(user)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Actualizado", Toast.LENGTH_SHORT).show();
                        finish();
                    });
        } else {
            String password = etUserPassword.getText().toString().trim();
            if (password.length() < 6) {
                etUserPassword.setError("Mínimo 6 caracteres");
                return;
            }

            // Secondary app trick to prevent Admin logout - with unique name and cleanup
            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setApiKey("AIzaSyB5wQ5lhp0wkF__7I2xL47ghHk8iRukMek")
                    .setApplicationId("1:82327975341:android:2b4c705b42de2c9b53dd24")
                    .setProjectId("registro-de-cumplimiento")
                    .build();

            String secondaryAppName = "Secondary_" + System.currentTimeMillis();
            FirebaseApp secondaryApp = FirebaseApp.initializeApp(this, options, secondaryAppName);
            FirebaseAuth secondaryAuth = FirebaseAuth.getInstance(secondaryApp);

            // Create in Auth using secondary instance
            secondaryAuth.createUserWithEmailAndPassword(email, password)
                    .addOnSuccessListener(authResult -> {
                        String uid = authResult.getUser().getUid();
                        // Important: clean up secondary instance immediately
                        secondaryAuth.signOut();
                        secondaryApp.delete();
                        
                        db.collection("users").document(uid).set(user)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "Creado exitosamente", Toast.LENGTH_SHORT).show();
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, "Error en DB: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    })
                    .addOnFailureListener(e -> {
                        secondaryApp.delete(); // Cleanup on failure too
                        Toast.makeText(this, "Error en Auth: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }
}