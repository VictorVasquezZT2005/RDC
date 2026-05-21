package xyz.zt.rdc;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SchoolsFragment extends Fragment {

    private RecyclerView rvSchools;
    private ExtendedFloatingActionButton btnAddSchool;
    private TextInputEditText etSearchSchools;
    private GenericAdapter adapter;
    private List<DocumentSnapshot> schoolList = new ArrayList<>();
    private List<DocumentSnapshot> filteredList = new ArrayList<>();
    private Map<String, Integer> schoolTeacherCounts = new HashMap<>();
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_schools, container, false);

        db = FirebaseFirestore.getInstance();
        rvSchools = view.findViewById(R.id.rvSchools);
        btnAddSchool = view.findViewById(R.id.btnAddSchool);
        etSearchSchools = view.findViewById(R.id.etSearchSchools);

        setupSearchView();
        setupRecyclerView();
        loadSchools();

        btnAddSchool.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), CreateSchoolActivity.class));
        });

        return view;
    }

    private void setupSearchView() {
        etSearchSchools.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilter();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupRecyclerView() {
        adapter = new GenericAdapter(filteredList, "name", "code", new GenericAdapter.OnItemClickListener() {
            @Override
            public void onEdit(DocumentSnapshot doc) {
                Intent intent = new Intent(getActivity(), CreateSchoolActivity.class);
                intent.putExtra("docId", doc.getId());
                startActivity(intent);
            }

            @Override
            public void onDelete(DocumentSnapshot doc) {
                db.collection("schools").document(doc.getId()).delete()
                        .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Eliminado", Toast.LENGTH_SHORT).show());
            }
        });
        rvSchools.setAdapter(adapter);
    }

    private void loadSchools() {
        db.collection("schools").addSnapshotListener((value, error) -> {
            if (error != null || getContext() == null) return;
            if (value != null) {
                schoolList.clear();
                schoolList.addAll(value.getDocuments());
                loadTeacherCounts();
            }
        });
    }

    private void loadTeacherCounts() {
        db.collection("users")
                .whereEqualTo("role", "teacher")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    schoolTeacherCounts.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        String schoolCode = doc.getString("schoolCode");
                        if (schoolCode != null) {
                            int currentCount = schoolTeacherCounts.getOrDefault(schoolCode, 0);
                            schoolTeacherCounts.put(schoolCode, currentCount + 1);
                        }
                    }
                    applyFilter();
                });
    }

    private void applyFilter() {
        String searchText = etSearchSchools.getText().toString().toLowerCase().trim();
        filteredList.clear();

        for (DocumentSnapshot doc : schoolList) {
            String name = doc.getString("name");
            String code = doc.getString("code");

            if (code == null) code = doc.getId();

            boolean matchesSearch = searchText.isEmpty() ||
                    (name != null && name.toLowerCase().contains(searchText)) ||
                    (code != null && code.toLowerCase().contains(searchText));

            if (matchesSearch) {
                filteredList.add(doc);
            }
        }

        adapter.setSubtitleMapper(doc -> {
            String code = doc.getString("code");
            if (code == null) code = doc.getId();
            int count = schoolTeacherCounts.getOrDefault(code, 0);
            return "Código: " + code + " | Maestros: " + count;
        });

        adapter.notifyDataSetChanged();
    }
}