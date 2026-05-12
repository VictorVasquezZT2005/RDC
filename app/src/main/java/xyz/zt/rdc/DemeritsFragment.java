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

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class DemeritsFragment extends Fragment {

    private RecyclerView rvDemerits;
    private ExtendedFloatingActionButton btnAddDemerit;
    private TextInputEditText etSearchDemerits;
    private GenericAdapter adapter;
    private List<DocumentSnapshot> demeritList = new ArrayList<>();
    private List<DocumentSnapshot> filteredList = new ArrayList<>();
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String userSchoolCode = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_demerits, container, false);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        rvDemerits = view.findViewById(R.id.rvDemerits);
        btnAddDemerit = view.findViewById(R.id.btnAddDemerit);
        etSearchDemerits = view.findViewById(R.id.etSearchDemerits);

        // Ocultar BottomNav al entrar
        toggleBottomNav(false);

        setupSearchView();
        setupRecyclerView();
        loadUserInfo();

        btnAddDemerit.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), CreateDemeritActivity.class);
            startActivity(intent);
        });

        return view;
    }

    private void toggleBottomNav(boolean show) {
        if (getActivity() != null) {
            BottomNavigationView bottomNav = getActivity().findViewById(R.id.bottom_navigation);
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

    private void setupSearchView() {
        etSearchDemerits.addTextChangedListener(new TextWatcher() {
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
        adapter = new GenericAdapter(filteredList, "studentName", "reason", new GenericAdapter.OnItemClickListener() {
            @Override
            public void onEdit(DocumentSnapshot doc) {
                Toast.makeText(getContext(), "Edición en desarrollo", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onDelete(DocumentSnapshot doc) {
                db.collection("demerits").document(doc.getId()).delete()
                        .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Eliminado", Toast.LENGTH_SHORT).show());
            }
        });
        rvDemerits.setAdapter(adapter);
    }

    private void loadUserInfo() {
        String email = mAuth.getCurrentUser().getEmail();
        if (email == null) return;

        db.collection("users").whereEqualTo("email", email).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot userDoc = queryDocumentSnapshots.getDocuments().get(0);
                        String role = userDoc.getString("role");
                        userSchoolCode = userDoc.getString("schoolCode");
                        
                        // Admin can see everything, or specific logic
                        loadDemerits(role);
                    }
                });
    }

    private void loadDemerits(String role) {
        Query query = db.collection("demerits");
        
        // If not admin, filter by school
        if (!"admin".equals(role) && userSchoolCode != null) {
            query = query.whereEqualTo("schoolCode", userSchoolCode);
        }

        query.addSnapshotListener((value, error) -> {
            if (error != null || getContext() == null) return;
            if (value != null) {
                demeritList.clear();
                demeritList.addAll(value.getDocuments());
                applyFilter();
            }
        });
    }

    private void applyFilter() {
        String searchText = etSearchDemerits.getText().toString().toLowerCase().trim();
        filteredList.clear();

        for (DocumentSnapshot doc : demeritList) {
            String studentName = doc.getString("studentName");
            String reason = doc.getString("reason");

            boolean matchesSearch = searchText.isEmpty() ||
                    (studentName != null && studentName.toLowerCase().contains(searchText)) ||
                    (reason != null && reason.toLowerCase().contains(searchText));

            if (matchesSearch) {
                filteredList.add(doc);
            }
        }
        adapter.notifyDataSetChanged();
    }
}