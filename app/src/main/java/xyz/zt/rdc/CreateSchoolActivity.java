package xyz.zt.rdc;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class CreateSchoolActivity extends AppCompatActivity {

    private TextInputEditText etSchoolName, etSchoolCode, etDepartment, etMunicipality, etDistrict;
    private Button btnSaveSchool, btnConfigGrades;
    private TextView tvTitle;
    private FirebaseFirestore db;
    private String editingDocId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_school);

        db = FirebaseFirestore.getInstance();

        tvTitle = findViewById(R.id.tvTitle);
        etSchoolName = findViewById(R.id.etSchoolName);
        etSchoolCode = findViewById(R.id.etSchoolCode);
        etDepartment = findViewById(R.id.etDepartment);
        etMunicipality = findViewById(R.id.etMunicipality);
        etDistrict = findViewById(R.id.etDistrict);
        btnSaveSchool = findViewById(R.id.btnSaveSchool);
        btnConfigGrades = findViewById(R.id.btnConfigGrades);

        editingDocId = getIntent().getStringExtra("docId");
        if (editingDocId != null) {
            loadSchoolData();
        }

        btnSaveSchool.setOnClickListener(v -> saveSchool());

        btnConfigGrades.setOnClickListener(v -> {
            Intent intent = new Intent(this, SchoolConfigActivity.class);
            intent.putExtra("docId", editingDocId);
            intent.putExtra("schoolName", etSchoolName.getText().toString());
            startActivity(intent);
        });
    }

    private void loadSchoolData() {
        db.collection("schools").document(editingDocId).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                tvTitle.setText("Editar Centro Educativo");
                etSchoolName.setText(doc.getString("name"));
                etSchoolCode.setText(doc.getString("code"));
                etDepartment.setText(doc.getString("department"));
                etMunicipality.setText(doc.getString("municipality"));
                etDistrict.setText(doc.getString("district"));
                etSchoolCode.setEnabled(false);
                btnSaveSchool.setText("Actualizar");
                btnConfigGrades.setVisibility(View.VISIBLE);
            }
        });
    }

    private void saveSchool() {
        String name = etSchoolName.getText().toString().trim();
        String code = etSchoolCode.getText().toString().trim();
        String dept = etDepartment.getText().toString().trim();
        String muni = etMunicipality.getText().toString().trim();
        String dist = etDistrict.getText().toString().trim();

        if (name.isEmpty() || code.isEmpty() || dept.isEmpty() || muni.isEmpty() || dist.isEmpty()) {
            Toast.makeText(this, "Todos los campos son obligatorios", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> school = new HashMap<>();
        school.put("name", name);
        school.put("code", code);
        school.put("department", dept);
        school.put("municipality", muni);
        school.put("district", dist);

        if (editingDocId != null) {
            db.collection("schools").document(editingDocId).update(school)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Actualizado", Toast.LENGTH_SHORT).show();
                        finish();
                    });
        } else {
            db.collection("schools").document(code).set(school)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Creado", Toast.LENGTH_SHORT).show();
                        finish();
                    });
        }
    }
}