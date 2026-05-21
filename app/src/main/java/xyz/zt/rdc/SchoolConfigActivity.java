package xyz.zt.rdc;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SchoolConfigActivity extends AppCompatActivity {

    private LinearLayout layoutGradesContainer;
    private TextView tvSchoolNameHeader;
    private Button btnSaveConfig;
    private FirebaseFirestore db;
    private String docId;

    private String[] standardGrades = {
            "Kinder 4", "Kinder 5", "Kinder 6",
            "1er Grado", "2do Grado", "3er Grado",
            "4to Grado", "5to Grado", "6to Grado",
            "7mo Grado", "8vo Grado", "9no Grado"
    };

    private String[] standardSections = {"Sección A", "Sección B", "Sección C", "Sección D", "Sección E"};
    private String[] standardTurns = {"Matutino", "Vespertino", "Nocturno"};

    private Map<String, List<String>> currentConfig = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_school_config);

        db = FirebaseFirestore.getInstance();
        docId = getIntent().getStringExtra("docId");
        String schoolName = getIntent().getStringExtra("schoolName");

        layoutGradesContainer = findViewById(R.id.layoutGradesContainer);
        tvSchoolNameHeader = findViewById(R.id.tvSchoolNameHeader);
        btnSaveConfig = findViewById(R.id.btnSaveConfig);

        if (schoolName != null) {
            tvSchoolNameHeader.setText(schoolName);
        }

        if (docId == null) {
            Toast.makeText(this, "Error: No se proporcionó ID de escuela", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadConfig();

        btnSaveConfig.setOnClickListener(v -> saveConfig());
    }

    private void loadConfig() {
        db.collection("schools").document(docId).get().addOnSuccessListener(doc -> {
            if (doc.exists() && doc.contains("config")) {
                Object configObj = doc.get("config");
                if (configObj instanceof Map) {
                    currentConfig = (Map<String, List<String>>) configObj;
                }
            }
            setupUI();
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Error al cargar configuración", Toast.LENGTH_SHORT).show();
            setupUI();
        });
    }

    private void setupUI() {
        layoutGradesContainer.removeAllViews();
        for (String grade : standardGrades) {
            addGradeItem(grade);
        }
    }

    private void addGradeItem(String gradeName) {
        LinearLayout gradeItem = new LinearLayout(this);
        gradeItem.setOrientation(LinearLayout.VERTICAL);
        gradeItem.setPadding(0, 8, 0, 16);

        MaterialCheckBox cbGrade = new MaterialCheckBox(this);
        cbGrade.setText(gradeName);
        cbGrade.setTextSize(18);
        cbGrade.setPadding(0, 16, 0, 16);

        LinearLayout sectionsContainer = new LinearLayout(this);
        sectionsContainer.setOrientation(LinearLayout.VERTICAL);
        sectionsContainer.setPadding(48, 0, 0, 0);
        sectionsContainer.setVisibility(View.GONE);

        List<String> selectedConfigs = currentConfig.get(gradeName);
        if (selectedConfigs != null) {
            cbGrade.setChecked(true);
            sectionsContainer.setVisibility(View.VISIBLE);
        }

        for (String section : standardSections) {
            LinearLayout sectionRow = new LinearLayout(this);
            sectionRow.setOrientation(LinearLayout.VERTICAL);
            sectionRow.setPadding(0, 0, 0, 8);

            MaterialCheckBox cbSection = new MaterialCheckBox(this);
            cbSection.setText(section);
            
            LinearLayout turnsContainer = new LinearLayout(this);
            turnsContainer.setOrientation(LinearLayout.HORIZONTAL);
            turnsContainer.setPadding(48, 0, 0, 0);
            turnsContainer.setVisibility(View.GONE);

            boolean sectionHasTurnsSelected = false;
            for (String turn : standardTurns) {
                MaterialCheckBox cbTurn = new MaterialCheckBox(this);
                cbTurn.setText(turn);
                cbTurn.setTextSize(12);
                
                String fullKey = section + "|" + turn;
                if (selectedConfigs != null && selectedConfigs.contains(fullKey)) {
                    cbTurn.setChecked(true);
                    sectionHasTurnsSelected = true;
                }
                turnsContainer.addView(cbTurn);
            }

            if (sectionHasTurnsSelected) {
                cbSection.setChecked(true);
                turnsContainer.setVisibility(View.VISIBLE);
            }

            cbSection.setOnCheckedChangeListener((buttonView, isChecked) -> {
                turnsContainer.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            });

            sectionRow.addView(cbSection);
            sectionRow.addView(turnsContainer);
            sectionsContainer.addView(sectionRow);
        }

        cbGrade.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sectionsContainer.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });

        gradeItem.addView(cbGrade);
        gradeItem.addView(sectionsContainer);
        layoutGradesContainer.addView(gradeItem);
    }

    private void saveConfig() {
        Map<String, List<String>> newConfig = new HashMap<>();

        for (int i = 0; i < layoutGradesContainer.getChildCount(); i++) {
            View gradeView = layoutGradesContainer.getChildAt(i);
            if (gradeView instanceof LinearLayout) {
                LinearLayout gradeItem = (LinearLayout) gradeView;
                MaterialCheckBox cbGrade = (MaterialCheckBox) gradeItem.getChildAt(0);
                LinearLayout sectionsContainer = (LinearLayout) gradeItem.getChildAt(1);

                if (cbGrade.isChecked()) {
                    List<String> gradeConfigs = new ArrayList<>();
                    for (int j = 0; j < sectionsContainer.getChildCount(); j++) {
                        View sectionView = sectionsContainer.getChildAt(j);
                        if (sectionView instanceof LinearLayout) {
                            LinearLayout sectionRow = (LinearLayout) sectionView;
                            MaterialCheckBox cbSection = (MaterialCheckBox) sectionRow.getChildAt(0);
                            LinearLayout turnsContainer = (LinearLayout) sectionRow.getChildAt(1);

                            if (cbSection.isChecked()) {
                                for (int k = 0; k < turnsContainer.getChildCount(); k++) {
                                    View turnView = turnsContainer.getChildAt(k);
                                    if (turnView instanceof MaterialCheckBox) {
                                        MaterialCheckBox cbTurn = (MaterialCheckBox) turnView;
                                        if (cbTurn.isChecked()) {
                                            gradeConfigs.add(cbSection.getText().toString() + "|" + cbTurn.getText().toString());
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (!gradeConfigs.isEmpty()) {
                        newConfig.put(cbGrade.getText().toString(), gradeConfigs);
                    }
                }
            }
        }

        if (newConfig.isEmpty()) {
            Toast.makeText(this, "Selecciona al menos un grado y una sección", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("schools").document(docId).update("config", newConfig)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Configuración guardada", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error al guardar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}