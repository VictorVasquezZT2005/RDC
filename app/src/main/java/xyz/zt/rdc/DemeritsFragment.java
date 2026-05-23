package xyz.zt.rdc;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DemeritsFragment extends Fragment {

    private MaterialAutoCompleteTextView spinnerStudentName;
    private TextInputEditText etStudentGrade, etStudentSection, etStudentNIE, etStudentGender, etStudentTurn;
    private TextInputEditText etSchoolName, etSchoolCode, etSchoolDistrict, etSchoolDept, etSchoolMuni;
    private TextInputEditText etPrincipalName, etPrincipalSignature;

    private LinearLayout layoutEntriesContainer;
    private MaterialButton btnAddEntry, btnSaveDemerit;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private List<DocumentSnapshot> studentList = new ArrayList<>();
    private List<String> studentDisplayNames = new ArrayList<>();
    private Map<String, DocumentSnapshot> studentMap = new HashMap<>();
    private DocumentSnapshot selectedStudentDoc = null;
    private String teacherSchoolCode = null;
    private String teacherName = null;
    
    private String currentCardId = null;

    private class EntryBlock {
        View view;
        TextView tvTitle;
        EditText etDateTime;
        AutoCompleteTextView spinnerD, spinnerR, spinnerRC;
        View layoutItem15, layoutItem16, layoutGroupRC, layoutGroupDR;
        EditText etName15, etSig15, etName16, etSig16, etSigStudent;
        ImageButton btnRemove;
        boolean isSaved = false;

        EntryBlock(View v, int index, boolean saved) {
            this.view = v;
            this.isSaved = saved;
            tvTitle = v.findViewById(R.id.tvEntryTitle);
            tvTitle.setText("Registro #" + index);

            etDateTime = v.findViewById(R.id.etDateTime);
            spinnerD = v.findViewById(R.id.spinnerD);
            spinnerR = v.findViewById(R.id.spinnerR);
            spinnerRC = v.findViewById(R.id.spinnerRC);
            layoutItem15 = v.findViewById(R.id.layoutItem15);
            layoutItem16 = v.findViewById(R.id.layoutItem16);
            layoutGroupRC = v.findViewById(R.id.layoutGroupRC);
            layoutGroupDR = v.findViewById(R.id.layoutGroupDR);
            
            etName15 = v.findViewById(R.id.etName15);
            etSig15 = v.findViewById(R.id.etSig15);
            etName16 = v.findViewById(R.id.etName16);
            etSig16 = v.findViewById(R.id.etSig16);
            etSigStudent = v.findViewById(R.id.etSigStudent);
            btnRemove = v.findViewById(R.id.btnRemoveEntry);

            setupDropdowns();
            
            if (saved) {
                disableFields();
                btnRemove.setVisibility(View.GONE);
            } else {
                setCurrentDateTime();
                btnRemove.setVisibility(View.VISIBLE);
                btnRemove.setOnClickListener(v1 -> {
                    layoutEntriesContainer.removeView(view);
                    entryBlocks.remove(this);
                    updateAddButtonVisibility();
                });
                
                if (teacherName != null) {
                    etName15.setText(teacherName);
                    etName16.setText(teacherName);
                }
            }
        }

        void disableFields() {
            spinnerD.setEnabled(false);
            spinnerR.setEnabled(false);
            spinnerRC.setEnabled(false);
            etSig15.setEnabled(false);
            etSig16.setEnabled(false);
            etSigStudent.setEnabled(false);
            if (spinnerD.getParent().getParent() instanceof com.google.android.material.textfield.TextInputLayout) {
                ((com.google.android.material.textfield.TextInputLayout) spinnerD.getParent().getParent()).setEnabled(false);
                ((com.google.android.material.textfield.TextInputLayout) spinnerR.getParent().getParent()).setEnabled(false);
                ((com.google.android.material.textfield.TextInputLayout) spinnerRC.getParent().getParent()).setEnabled(false);
            }
        }

        void fillData(Map<String, Object> data) {
            etDateTime.setText((String) data.get("dateTime"));
            spinnerD.setText((String) data.get("selectedD"), false);
            spinnerR.setText((String) data.get("selectedR"), false);
            spinnerRC.setText((String) data.get("selectedRC"), false);
            etName15.setText((String) data.get("registrarName"));
            etName16.setText((String) data.get("responsableName"));
            etSig15.setText((String) data.get("sigRegistrar"));
            etSig16.setText((String) data.get("sigResponsable"));
            etSigStudent.setText((String) data.get("sigStudent"));
            
            String d = (String) data.get("selectedD");
            String r = (String) data.get("selectedR");
            String rc = (String) data.get("selectedRC");
            
            if ((d != null && !d.isEmpty()) || (rc != null && !rc.isEmpty())) {
                layoutItem15.setVisibility(View.VISIBLE);
                layoutItem16.setVisibility(View.GONE);
                if (rc != null && !rc.isEmpty()) layoutGroupDR.setVisibility(View.GONE);
                else layoutGroupRC.setVisibility(View.GONE);
            } else if (r != null && !r.isEmpty()) {
                layoutItem15.setVisibility(View.GONE);
                layoutItem16.setVisibility(View.VISIBLE);
                layoutGroupRC.setVisibility(View.GONE);
            }
        }

        void setCurrentDateTime() {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
            etDateTime.setText(sdf.format(Calendar.getInstance().getTime()));
        }

        void setupDropdowns() {
            String[] optionsD = {"A", "B", "C", "D"};
            String[] optionsR = {"A", "B", "C"};
            String[] optionsRC = {"A", "B"};

            spinnerD.setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_dropdown_item_1line, optionsD));
            spinnerR.setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_dropdown_item_1line, optionsR));
            spinnerRC.setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_dropdown_item_1line, optionsRC));

            if (!isSaved) {
                spinnerD.setOnItemClickListener((parent, view, position, id) -> {
                    spinnerRC.setText("", false);
                    spinnerR.setText("", false);
                    layoutGroupRC.setVisibility(View.GONE);
                    layoutItem15.setVisibility(View.VISIBLE);
                    layoutItem16.setVisibility(View.GONE);
                });

                spinnerR.setOnItemClickListener((parent, view, position, id) -> {
                    spinnerRC.setText("", false);
                    spinnerD.setText("", false);
                    layoutGroupRC.setVisibility(View.GONE);
                    layoutItem15.setVisibility(View.GONE);
                    layoutItem16.setVisibility(View.VISIBLE);
                });

                spinnerRC.setOnItemClickListener((parent, view, position, id) -> {
                    spinnerD.setText("", false);
                    spinnerR.setText("", false);
                    layoutGroupDR.setVisibility(View.GONE);
                    layoutItem15.setVisibility(View.VISIBLE);
                    layoutItem16.setVisibility(View.GONE);
                });
            }
        }

        Map<String, Object> getData() {
            Map<String, Object> data = new HashMap<>();
            data.put("dateTime", etDateTime.getText().toString());
            data.put("selectedD", spinnerD.getText().toString());
            data.put("selectedR", spinnerR.getText().toString());
            data.put("selectedRC", spinnerRC.getText().toString());
            data.put("registrarName", etName15.getText().toString());
            data.put("responsableName", etName16.getText().toString());
            data.put("sigRegistrar", etSig15.getText().toString());
            data.put("sigResponsable", etSig16.getText().toString());
            data.put("sigStudent", etSigStudent.getText().toString());
            return data;
        }
    }

    private List<EntryBlock> entryBlocks = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_demerits, container, false);
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        initViews(view);
        spinnerStudentName.setThreshold(1);
        loadTeacherData();
        setupStudentSearch();
        btnAddEntry.setOnClickListener(v -> addNewEntry());
        btnSaveDemerit.setOnClickListener(v -> saveAllRecords());
        view.findViewById(R.id.btnViewRules).setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), DemeritNormativaActivity.class));
        });
        view.findViewById(R.id.btnViewInstructivo).setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), DemeritInstructivoActivity.class));
        });
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setBottomNavVisibility(false);
        }
        return view;
    }

    private void initViews(View view) {
        spinnerStudentName = view.findViewById(R.id.spinnerStudentName);
        etStudentGrade = view.findViewById(R.id.etStudentGrade);
        etStudentSection = view.findViewById(R.id.etStudentSection);
        etStudentNIE = view.findViewById(R.id.etStudentNIE);
        etStudentGender = view.findViewById(R.id.etStudentGender);
        etStudentTurn = view.findViewById(R.id.etStudentTurn);
        etSchoolName = view.findViewById(R.id.etSchoolName);
        etSchoolCode = view.findViewById(R.id.etSchoolCode);
        etSchoolDistrict = view.findViewById(R.id.etSchoolDistrict);
        etSchoolDept = view.findViewById(R.id.etSchoolDept);
        etSchoolMuni = view.findViewById(R.id.etSchoolMuni);
        etPrincipalName = view.findViewById(R.id.etPrincipalName);
        etPrincipalSignature = view.findViewById(R.id.etPrincipalSignature);
        layoutEntriesContainer = view.findViewById(R.id.layoutEntriesContainer);
        btnAddEntry = view.findViewById(R.id.btnAddEntry);
        btnSaveDemerit = view.findViewById(R.id.btnSaveDemerit);
    }

    private void addNewEntry() {
        if (entryBlocks.size() >= 15) {
            Toast.makeText(getContext(), "Límite de 15 registros alcanzado", Toast.LENGTH_SHORT).show();
            return;
        }
        View entryView = getLayoutInflater().inflate(R.layout.item_conduct_entry, layoutEntriesContainer, false);
        EntryBlock block = new EntryBlock(entryView, entryBlocks.size() + 1, false);
        entryBlocks.add(block);
        layoutEntriesContainer.addView(entryView);
        updateAddButtonVisibility();
    }

    private void updateAddButtonVisibility() {
        btnAddEntry.setVisibility(entryBlocks.size() >= 15 ? View.GONE : View.VISIBLE);
    }

    private void loadTeacherData() {
        if (mAuth.getCurrentUser() == null || !isAdded()) return;
        db.collection("users").document(mAuth.getCurrentUser().getUid()).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists() && isAdded()) {
                        String role = doc.getString("role");
                        teacherName = doc.getString("name");
                        teacherSchoolCode = doc.getString("schoolCode");
                        for (EntryBlock block : entryBlocks) {
                            if (!block.isSaved) {
                                block.etName15.setText(teacherName);
                                block.etName16.setText(teacherName);
                            }
                        }
                        
                        if (teacherSchoolCode != null) {
                            loadStudentsFromSchool(teacherSchoolCode);
                        } else if ("admin".equals(role)) {
                            loadAllStudents();
                        }
                    }
                });
    }

    private void loadAllStudents() {
        db.collection("users")
                .whereEqualTo("role", "student")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!isAdded()) return;
                    studentList.clear(); studentDisplayNames.clear(); studentMap.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String name = doc.getString("name");
                        String nie = doc.getString("nie");
                        String displayName = name + " (NIE: " + (nie != null ? nie : "N/A") + ")";
                        studentDisplayNames.add(displayName);
                        studentMap.put(displayName, doc);
                    }
                    updateStudentAdapter();
                });
    }

    private void loadStudentsFromSchool(String schoolCode) {
        db.collection("users").whereEqualTo("schoolCode", schoolCode).whereEqualTo("role", "student").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!isAdded()) return;
                    studentList.clear(); studentDisplayNames.clear(); studentMap.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String name = doc.getString("name");
                        String nie = doc.getString("nie");
                        String displayName = name + " (NIE: " + (nie != null ? nie : "N/A") + ")";
                        studentDisplayNames.add(displayName);
                        studentMap.put(displayName, doc);
                    }
                    updateStudentAdapter();
                });
    }

    private void setupStudentSearch() {
        spinnerStudentName.setOnItemClickListener((parent, view, position, id) -> {
            String selectedSelection = (String) parent.getItemAtPosition(position);
            selectedStudentDoc = studentMap.get(selectedSelection);
            if (selectedStudentDoc != null) {
                fillStudentData(selectedStudentDoc);
                loadSchoolData(selectedStudentDoc.getString("schoolCode"));
                loadExistingConductCard(selectedStudentDoc.getId());
            }
        });
    }

    private void loadExistingConductCard(String studentId) {
        db.collection("conduct_cards").whereEqualTo("studentId", studentId).limit(1).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    layoutEntriesContainer.removeAllViews();
                    entryBlocks.clear();
                    currentCardId = null;
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot doc = queryDocumentSnapshots.getDocuments().get(0);
                        currentCardId = doc.getId();
                        List<Map<String, Object>> entries = (List<Map<String, Object>>) doc.get("entries");
                        if (entries != null) {
                            for (Map<String, Object> entry : entries) addSavedEntry(entry);
                        }
                    }
                    if (entryBlocks.size() < 15) addNewEntry();
                    updateAddButtonVisibility();
                });
    }

    private void addSavedEntry(Map<String, Object> data) {
        View entryView = getLayoutInflater().inflate(R.layout.item_conduct_entry, layoutEntriesContainer, false);
        EntryBlock block = new EntryBlock(entryView, entryBlocks.size() + 1, true);
        block.fillData(data);
        entryBlocks.add(block);
        layoutEntriesContainer.addView(entryView);
    }

    private void fillStudentData(DocumentSnapshot doc) {
        etStudentGrade.setText(doc.getString("grade"));
        etStudentSection.setText(doc.getString("section"));
        etStudentNIE.setText(doc.getString("nie"));
        etStudentGender.setText(doc.getString("gender"));
        etStudentTurn.setText(doc.getString("turn"));
    }

    private void updateStudentAdapter() {
        if (getContext() == null) return;
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), 
                android.R.layout.simple_dropdown_item_1line, studentDisplayNames);
        spinnerStudentName.setAdapter(adapter);
    }

    private void loadSchoolData(String schoolCode) {
        if (schoolCode == null || !isAdded()) return;
        etPrincipalName.setText("Buscando Director...");
        db.collection("schools").document(schoolCode).get().addOnSuccessListener(doc -> {
            if (doc.exists() && isAdded()) {
                etSchoolName.setText(doc.getString("name")); etSchoolCode.setText(doc.getId());
                etSchoolDistrict.setText(doc.getString("district")); etSchoolDept.setText(doc.getString("department"));
                etSchoolMuni.setText(doc.getString("municipality"));
                String pName = doc.getString("principalName");
                if (pName != null && !pName.isEmpty()) etPrincipalName.setText(pName);
                else loadPrincipalNameByQuery(doc.getId(), doc.getString("name"));
            }
        });
    }

    private void loadPrincipalNameByQuery(String schoolCode, String schoolName) {
        db.collection("users").whereEqualTo("schoolCode", schoolCode).whereEqualTo("role", "principal").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty() && isAdded()) etPrincipalName.setText(queryDocumentSnapshots.getDocuments().get(0).getString("name"));
                    else {
                        db.collection("users").whereEqualTo("schoolName", schoolName).whereEqualTo("role", "principal").get().addOnSuccessListener(q2 -> {
                            if (!q2.isEmpty() && isAdded()) etPrincipalName.setText(q2.getDocuments().get(0).getString("name"));
                            else if (isAdded()) etPrincipalName.setText("Director no asignado");
                        });
                    }
                });
    }

    private void saveAllRecords() {
        if (selectedStudentDoc == null) {
            Toast.makeText(getContext(), "Selecciona un alumno", Toast.LENGTH_SHORT).show();
            return;
        }
        List<Map<String, Object>> allEntries = new ArrayList<>();
        for (EntryBlock block : entryBlocks) allEntries.add(block.getData());
        Map<String, Object> cardData = new HashMap<>();
        cardData.put("studentId", selectedStudentDoc.getId());
        cardData.put("studentName", selectedStudentDoc.getString("name"));
        cardData.put("schoolCode", etSchoolCode.getText().toString());
        cardData.put("principalName", etPrincipalName.getText().toString());
        cardData.put("entries", allEntries);
        cardData.put("timestamp", com.google.firebase.Timestamp.now());
        if (currentCardId != null) {
            db.collection("conduct_cards").document(currentCardId).set(cardData).addOnSuccessListener(aVoid -> {
                Toast.makeText(getContext(), "Tarjeta actualizada", Toast.LENGTH_SHORT).show();
                loadExistingConductCard(selectedStudentDoc.getId());
            });
        } else {
            db.collection("conduct_cards").add(cardData).addOnSuccessListener(ref -> {
                currentCardId = ref.getId();
                Toast.makeText(getContext(), "Tarjeta creada", Toast.LENGTH_SHORT).show();
                loadExistingConductCard(selectedStudentDoc.getId());
            });
        }
    }

    @Override public void onDestroyView() {
        super.onDestroyView();
        if (getActivity() instanceof MainActivity) ((MainActivity) getActivity()).setBottomNavVisibility(true);
    }
}
