package xyz.zt.rdc;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

public class UserDetailActivity extends AppCompatActivity {

    private TextView tvName, tvRole;
    private View rowEmail, rowSchool, rowGender, rowGrade, rowSection, rowNIE, rowAssignments;
    private Button btnBack;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_detail);

        db = FirebaseFirestore.getInstance();

        tvName = findViewById(R.id.tvDetailName);
        tvRole = findViewById(R.id.tvDetailRole);
        rowEmail = findViewById(R.id.rowEmail);
        rowSchool = findViewById(R.id.rowSchool);
        rowAssignments = findViewById(R.id.rowAssignments);
        rowGender = findViewById(R.id.rowGender);
        rowGrade = findViewById(R.id.rowGrade);
        rowSection = findViewById(R.id.rowSection);
        rowNIE = findViewById(R.id.rowNIE);
        btnBack = findViewById(R.id.btnBack);

        String docId = getIntent().getStringExtra("docId");
        if (docId != null) {
            loadUserData(docId);
        } else {
            Toast.makeText(this, "Error: No se encontró el ID del usuario", Toast.LENGTH_SHORT).show();
            finish();
        }

        btnBack.setOnClickListener(v -> finish());
    }

    private void loadUserData(String docId) {
        db.collection("users").document(docId).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                String name = doc.getString("name");
                String role = doc.getString("role");
                String email = doc.getString("email");
                String school = doc.getString("schoolName");
                String gender = doc.getString("gender");
                String grade = doc.getString("grade");
                String section = doc.getString("section");
                String nie = doc.getString("nie");

                String assignmentsStr = null;
                if ("teacher".equals(role)) {
                    java.util.List<String> assignments = (java.util.List<String>) doc.get("assignments");
                    if (assignments != null && !assignments.isEmpty()) {
                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i < assignments.size(); i++) {
                            sb.append(assignments.get(i));
                            if (i < assignments.size() - 1) sb.append(", ");
                        }
                        assignmentsStr = sb.toString();
                    }
                }

                tvName.setText(name != null ? name : "Sin nombre");
                tvRole.setText(getFriendlyRole(role));

                setupRow(rowEmail, "Correo Electrónico", email);
                setupRow(rowSchool, "Centro Educativo", school);
                setupRow(rowAssignments, "Asignaciones", assignmentsStr);
                setupRow(rowGender, "Sexo", gender);
                setupRow(rowGrade, "Grado", grade);
                setupRow(rowSection, "Sección", section);
                setupRow(rowNIE, "NIE", nie);

            } else {
                Toast.makeText(this, "El usuario no existe", Toast.LENGTH_SHORT).show();
                finish();
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Error al cargar datos", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void setupRow(View row, String label, String value) {
        if (value == null || value.isEmpty()) {
            row.setVisibility(View.GONE);
        } else {
            row.setVisibility(View.VISIBLE);
            TextView tvLabel = row.findViewById(R.id.tvRowLabel);
            TextView tvValue = row.findViewById(R.id.tvRowValue);
            tvLabel.setText(label);
            tvValue.setText(value);
        }
    }

    private String getFriendlyRole(String role) {
        if (role == null) return "Sin rol";
        switch (role) {
            case "teacher": return "Maestro";
            case "admin": return "Administrador (Dev)";
            case "principal": return "Director";
            case "sub_principal": return "Sub-Director";
            case "administrative_assistant": return "Asistente Administrativo";
            case "student": return "Alumno";
            default: return role;
        }
    }
}