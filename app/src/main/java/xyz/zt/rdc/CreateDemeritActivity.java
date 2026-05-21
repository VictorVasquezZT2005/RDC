package xyz.zt.rdc;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.Toast;

import androidx.annotation.NonNull;
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

import android.view.WindowManager;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

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
    private String teacherName = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Soporte para Notch y Edge-to-Edge
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            getWindow().getAttributes().layoutInDisplayCutoutMode = 
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_demerit);

        // Aplicar insets al layout raíz para evitar superposición con notch y barras de sistema
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(insets.left, insets.top, insets.right, insets.bottom);
            return windowInsets;
        });

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        initViews();
        spinnerStudentName.setThreshold(1);
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

    private Map<String, DocumentSnapshot> studentMap = new HashMap<>();

    private void loadTeacherData() {
        if (mAuth.getCurrentUser() == null) return;
        
        db.collection("users").document(mAuth.getCurrentUser().getUid()).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String role = doc.getString("role");
                        teacherName = doc.getString("name");
                        teacherSchoolCode = doc.getString("schoolCode");
                        if (teacherSchoolCode != null) {
                            loadStudentsFromSchool(teacherSchoolCode);
                        } else if ("admin".equals(role)) {
                            loadAllStudents();
                        } else {
                            Toast.makeText(this, "No tienes una escuela asignada", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void loadAllStudents() {
        db.collection("users")
                .whereEqualTo("role", "student")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    studentList.clear();
                    studentDisplayNames.clear();
                    studentMap.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        studentList.add(doc);
                        String name = doc.getString("name");
                        String nie = doc.getString("nie");
                        String turn = doc.getString("turn");
                        String displayName = name + " (NIE: " + (nie != null ? nie : "N/A") + (turn != null ? " - " + turn : "") + ")";
                        studentDisplayNames.add(displayName);
                        studentMap.put(displayName, doc);
                    }
                    updateStudentAdapter();
                    if (studentDisplayNames.isEmpty()) {
                        Toast.makeText(this, "No se encontraron alumnos", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Se cargaron " + studentDisplayNames.size() + " alumnos", Toast.LENGTH_SHORT).show();
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
                    studentMap.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        studentList.add(doc);
                        String name = doc.getString("name");
                        String nie = doc.getString("nie");
                        String turn = doc.getString("turn");
                        String displayName = name + " (NIE: " + (nie != null ? nie : "N/A") + (turn != null ? " - " + turn : "") + ")";
                        studentDisplayNames.add(displayName);
                        studentMap.put(displayName, doc);
                    }
                    updateStudentAdapter();
                    if (studentDisplayNames.isEmpty()) {
                        Toast.makeText(this, "No se encontraron alumnos en tu escuela", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Se cargaron " + studentDisplayNames.size() + " alumnos de tu escuela", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setupStudentSearch() {
        spinnerStudentName.setOnItemClickListener((parent, view, position, id) -> {
            String selectedSelection = (String) parent.getItemAtPosition(position);
            selectedStudentDoc = studentMap.get(selectedSelection);
            if (selectedStudentDoc != null) {
                fillStudentData(selectedStudentDoc);
                loadSchoolData(selectedStudentDoc.getString("schoolCode"));
            }
        });
    }

    private void updateStudentAdapter() {
        StudentSearchAdapter adapter = new StudentSearchAdapter(this,
                android.R.layout.simple_dropdown_item_1line, studentDisplayNames);
        spinnerStudentName.setAdapter(adapter);
    }

    private class StudentSearchAdapter extends ArrayAdapter<String> implements Filterable {
        private List<String> originalList;
        private List<String> filteredList;

        public StudentSearchAdapter(android.content.Context context, int resource, List<String> objects) {
            super(context, resource, objects);
            this.originalList = new ArrayList<>(objects);
            this.filteredList = new ArrayList<>(objects);
        }

        @Override
        public int getCount() {
            return filteredList.size();
        }

        @Override
        public String getItem(int position) {
            return filteredList.get(position);
        }

        @NonNull
        @Override
        public Filter getFilter() {
            return new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    FilterResults results = new FilterResults();
                    List<String> suggestions = new ArrayList<>();

                    if (constraint == null || constraint.length() == 0) {
                        suggestions.addAll(originalList);
                    } else {
                        String filterPattern = constraint.toString().toLowerCase().trim();
                        for (String item : originalList) {
                            if (item.toLowerCase().contains(filterPattern)) {
                                suggestions.add(item);
                            }
                        }
                    }

                    results.values = suggestions;
                    results.count = suggestions.size();
                    return results;
                }

                @Override
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    filteredList.clear();
                    if (results != null && results.count > 0) {
                        filteredList.addAll((List) results.values);
                    }
                    notifyDataSetChanged();
                }

                @Override
                public CharSequence convertResultToString(Object resultValue) {
                    return (CharSequence) resultValue;
                }
            };
        }
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
        demerit.put("teacherName", teacherName != null ? teacherName : mAuth.getCurrentUser().getEmail());
        
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