package xyz.zt.rdc;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CreateDemeritActivity extends AppCompatActivity {

    private MaterialAutoCompleteTextView spinnerStudentName;
    private TextInputEditText etStudentGrade, etStudentSection, etStudentNIE, etStudentGender, etStudentTurn;
    private TextInputEditText etSchoolName, etSchoolCode, etSchoolDistrict, etSchoolDept, etSchoolMuni;
    private TextInputEditText etDemeritReason;
    private MaterialButton btnSaveDemerit;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private List<DocumentSnapshot> studentList = new ArrayList<>();
    private List<String> studentDisplayNames = new ArrayList<>();
    private DocumentSnapshot selectedStudentDoc = null;
    private String teacherSchoolCode = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_demerit);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        initViews();
        loadTeacherData();
        setupStudentSearch();

        btnSaveDemerit.setOnClickListener(v -> saveDemerit());
    }

    private void initViews() {
        spinnerStudentName = findViewById(R.id.spinnerStudentName);
        etStudentGrade = findViewById(R.id.etStudentGrade);
        etStudentSection = findViewById(R.id.etStudentSection);
        etStudentNIE = findViewById(R.id.etStudentNIE);
        etStudentGender = findViewById(R.id.etStudentGender);
        etStudentTurn = findViewById(R.id.etStudentTurn);

        etSchoolName = findViewById(R.id.etSchoolName);
        etSchoolCode = findViewById(R.id.etSchoolCode);
        etSchoolDistrict = findViewById(R.id.etSchoolDistrict);
        etSchoolDept = findViewById(R.id.etSchoolDept);
        etSchoolMuni = findViewById(R.id.etSchoolMuni);

        etDemeritReason = findViewById(R.id.etDemeritReason);
        btnSaveDemerit = findViewById(R.id.btnSaveDemerit);
    }

    private void loadTeacherData() {
        if (mAuth.getCurrentUser() == null) return;
        
        db.collection("users").document(mAuth.getCurrentUser().getUid()).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        teacherSchoolCode = doc.getString("schoolCode");
                        if (teacherSchoolCode != null) {
                            loadStudentsFromSchool(teacherSchoolCode);
                        }
                    }
                });
    }

    private void loadStudentsFromSchool(String schoolCode) {
        db.collection("users")
                .whereEqualTo("schoolCode", schoolCode)
                .whereEqualTo("role", "student")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    studentList.clear();
                    studentDisplayNames.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        studentList.add(doc);
                        String name = doc.getString("name");
                        String nie = doc.getString("nie");
                        studentDisplayNames.add(name + " (NIE: " + (nie != null ? nie : "N/A") + ")");
                    }
                    updateStudentAdapter();
                });
    }

    private void setupStudentSearch() {
        spinnerStudentName.setOnItemClickListener((parent, view, position, id) -> {
            String selectedSelection = (String) parent.getItemAtPosition(position);
            int index = studentDisplayNames.indexOf(selectedSelection);
            if (index != -1) {
                selectedStudentDoc = studentList.get(index);
                fillStudentData(selectedStudentDoc);
                loadSchoolData(selectedStudentDoc.getString("schoolCode"));
            }
        });
    }

    private void updateStudentAdapter() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, studentDisplayNames);
        spinnerStudentName.setAdapter(adapter);
    }

    private void fillStudentData(DocumentSnapshot doc) {
        etStudentGrade.setText(doc.getString("grade"));
        etStudentSection.setText(doc.getString("section"));
        etStudentNIE.setText(doc.getString("nie"));
        etStudentGender.setText(doc.getString("gender"));
        etStudentTurn.setText(doc.getString("turn")); // Assuming 'turn' exists
    }

    private void loadSchoolData(String schoolCode) {
        if (schoolCode == null) return;
        
        db.collection("schools").document(schoolCode).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        etSchoolName.setText(doc.getString("name"));
                        etSchoolCode.setText(doc.getId());
                        etSchoolDistrict.setText(doc.getString("district"));
                        etSchoolDept.setText(doc.getString("department"));
                        etSchoolMuni.setText(doc.getString("municipality"));
                    }
                });
    }

    private void saveDemerit() {
        if (selectedStudentDoc == null) {
            Toast.makeText(this, "Por favor selecciona un alumno", Toast.LENGTH_SHORT).show();
            return;
        }

        String reason = etDemeritReason.getText().toString().trim();
        if (reason.isEmpty()) {
            etDemeritReason.setError("El motivo es obligatorio");
            return;
        }

        Map<String, Object> demerit = new HashMap<>();
        demerit.put("studentId", selectedStudentDoc.getId());
        demerit.put("studentName", selectedStudentDoc.getString("name"));
        demerit.put("studentEmail", selectedStudentDoc.getString("email"));
        demerit.put("studentNIE", selectedStudentDoc.getString("nie"));
        demerit.put("studentGrade", selectedStudentDoc.getString("grade"));
        demerit.put("studentSection", selectedStudentDoc.getString("section"));
        
        demerit.put("teacherId", mAuth.getCurrentUser().getUid());
        demerit.put("teacherName", mAuth.getCurrentUser().getDisplayName()); // Should ideally fetch from Firestore if null
        
        demerit.put("schoolCode", etSchoolCode.getText().toString());
        demerit.put("schoolName", etSchoolName.getText().toString());
        
        demerit.put("reason", reason);
        demerit.put("timestamp", com.google.firebase.Timestamp.now());

        db.collection("demerits").add(demerit)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Demérito registrado correctamente", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error al guardar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}