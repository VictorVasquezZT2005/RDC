package xyz.zt.rdc;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class UsersFragment extends Fragment {

    private RecyclerView rvUsers;
    private ExtendedFloatingActionButton btnAddUser;
    private AutoCompleteTextView spinnerFilterRole;
    private TextInputEditText etSearchUsers;
    private GenericAdapter adapter;
    private List<DocumentSnapshot> userList = new ArrayList<>();
    private List<DocumentSnapshot> filteredList = new ArrayList<>();
    private List<String> teacherAssignments = new ArrayList<>();
    private java.util.Set<String> studentsWithDemerits = new java.util.HashSet<>();
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String mode = "all";
    private String userRole = "";
    private String userSchoolCode = "";

    private String[] filterLabels = {"Todos", "Maestro", "Administrador", "Director", "Sub-Director", "Asistente Administrativo", "Alumno"};
    private String[] filterKeys = {"all", "teacher", "admin", "principal", "sub_principal", "administrative_assistant", "student"};

    private String[] roleLabels = {"Maestro", "Administrador (Dev)", "Director", "Sub-Director", "Asistente Administrativo", "Alumno"};
    private String[] roleKeys = {"teacher", "admin", "principal", "sub_principal", "administrative_assistant", "student"};

    public UsersFragment() {
        // Required empty public constructor
    }

    public UsersFragment(String mode) {
        this.mode = mode;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mode = getArguments().getString("mode", "all");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_users, container, false);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        rvUsers = view.findViewById(R.id.rvUsers);
        btnAddUser = view.findViewById(R.id.btnAddUser);
        spinnerFilterRole = view.findViewById(R.id.spinnerFilterRole);
        etSearchUsers = view.findViewById(R.id.etSearchUsers);
        TextView tvTitle = view.findViewById(R.id.tvTitle);

        if ("student_filter".equals(mode)) {
            if (tvTitle != null) tvTitle.setText("Alumnos");
            if (etSearchUsers != null) etSearchUsers.setHint("Buscar alumno...");
            
            // Hide parent container of filter spinner
            if (spinnerFilterRole.getParent() != null && spinnerFilterRole.getParent().getParent() instanceof View) {
                ((View) spinnerFilterRole.getParent().getParent()).setVisibility(View.GONE);
            }
            btnAddUser.setVisibility(View.GONE);
        }

        setupSearchView();
        setupFilterSpinner();
        setupRecyclerView();
        loadCurrentUserAndUsers();

        // Hide BottomNav if in Alumnos mode
        if ("student_filter".equals(mode) && getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setBottomNavVisibility(false);
        }

        btnAddUser.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), CreateUserActivity.class));
        });

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Restore BottomNav if it was hidden
        if ("student_filter".equals(mode) && getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setBottomNavVisibility(true);
        }
    }

    private void setupSearchView() {
        etSearchUsers.addTextChangedListener(new TextWatcher() {
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

    private void setupFilterSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_dropdown_item_1line, filterLabels);
        spinnerFilterRole.setAdapter(adapter);

        spinnerFilterRole.setOnItemClickListener((parent, view, position, id) -> {
            applyFilter();
        });
    }

    private void setupRecyclerView() {
        adapter = new GenericAdapter(filteredList, "name", "role", new GenericAdapter.OnItemClickListener() {
            @Override
            public void onEdit(DocumentSnapshot doc) {
                if (isCurrentUser(doc)) {
                    Toast.makeText(getContext(), "No puedes editar tu propio usuario", Toast.LENGTH_SHORT).show();
                    return;
                }
                Intent intent = new Intent(getActivity(), CreateUserActivity.class);
                intent.putExtra("docId", doc.getId());
                startActivity(intent);
            }

            @Override
            public void onDelete(DocumentSnapshot doc) {
                if (isCurrentUser(doc)) {
                    Toast.makeText(getContext(), "No puedes eliminar tu propio usuario", Toast.LENGTH_SHORT).show();
                    return;
                }
                db.collection("users").document(doc.getId()).delete()
                        .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Eliminado permanentemente", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onToggleStatus(DocumentSnapshot doc, boolean currentlyDisabled) {
                if (isCurrentUser(doc)) {
                    Toast.makeText(getContext(), "No puedes deshabilitar tu propio usuario", Toast.LENGTH_SHORT).show();
                    return;
                }
                db.collection("users").document(doc.getId()).update("disabled", !currentlyDisabled)
                        .addOnSuccessListener(aVoid -> {
                            String msg = currentlyDisabled ? "Usuario habilitado" : "Usuario deshabilitado";
                            Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
                        });
            }

            @Override
            public void onItemClick(DocumentSnapshot doc) {
                Intent intent = new Intent(getActivity(), UserDetailActivity.class);
                intent.putExtra("docId", doc.getId());
                startActivity(intent);
            }

            @Override
            public void onChangeRole(DocumentSnapshot doc) {
                showChangeRoleDialog(doc);
            }
        });
        if ("student_filter".equals(mode)) {
            adapter.setReadOnly(true);
        }
        rvUsers.setAdapter(adapter);
    }

    private boolean isCurrentUser(DocumentSnapshot doc) {
        if (mAuth.getCurrentUser() == null) return false;
        
        String currentUid = mAuth.getCurrentUser().getUid();
        String currentUserEmail = mAuth.getCurrentUser().getEmail();
        
        // Check by document ID (if ID is the UID)
        if (doc.getId().equals(currentUid)) return true;
        
        // Check by email field
        String docEmail = doc.getString("email");
        return currentUserEmail != null && currentUserEmail.equalsIgnoreCase(docEmail);
    }

    private void loadCurrentUserAndUsers() {
        if (mAuth.getCurrentUser() == null) return;
        String email = mAuth.getCurrentUser().getEmail();
        
        db.collection("users").whereEqualTo("email", email).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot doc = queryDocumentSnapshots.getDocuments().get(0);
                        userRole = doc.getString("role");
                        userSchoolCode = doc.getString("schoolCode");
                        List<String> assignments = (List<String>) doc.get("assignments");
                        if (assignments != null) {
                            teacherAssignments.clear();
                            teacherAssignments.addAll(assignments);
                        }
                        
                        if ("teacher".equals(userRole)) {
                            loadStudentsWithDemerits(email);
                        } else {
                            loadUsers();
                        }
                    } else {
                        loadUsers();
                    }
                })
                .addOnFailureListener(e -> loadUsers());
    }

    private void loadStudentsWithDemerits(String teacherEmail) {
        db.collection("demerits").whereEqualTo("teacherEmail", teacherEmail).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    studentsWithDemerits.clear();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        String studentEmail = doc.getString("studentEmail");
                        if (studentEmail != null) studentsWithDemerits.add(studentEmail);
                    }
                    loadUsers();
                })
                .addOnFailureListener(e -> loadUsers());
    }

    private void loadUsers() {
        db.collection("users").addSnapshotListener((value, error) -> {
            if (error != null || getContext() == null) return;
            if (value != null) {
                userList.clear();
                userList.addAll(value.getDocuments());
                
                // Update restricted email in case current user changed
                if (mAuth.getCurrentUser() != null) {
                    adapter.setRestrictedEmail(mAuth.getCurrentUser().getEmail());
                }
                
                applyFilter();
            }
        });
    }

    private void applyFilter() {
        String searchText = etSearchUsers.getText().toString().toLowerCase().trim();
        filteredList.clear();

        String selectedRoleLabel = spinnerFilterRole.getText().toString();
        int position = -1;
        for (int i = 0; i < filterLabels.length; i++) {
            if (filterLabels[i].equals(selectedRoleLabel)) {
                position = i;
                break;
            }
        }
        String selectedKey = (position >= 0) ? filterKeys[position] : "all";

        for (DocumentSnapshot doc : userList) {
            String role = doc.getString("role");
            String name = doc.getString("name");
            String email = doc.getString("email");
            String grade = doc.getString("grade");
            String section = doc.getString("section");
            
            boolean matchesRole = selectedKey.equals("all") || selectedKey.equals(role);
            
            if ("student_filter".equals(mode)) {
                matchesRole = "student".equals(role);
                
                if ("teacher".equals(userRole) && matchesRole) {
                    // Strictly filter for teachers:
                    // 1. Same school
                    String studentSchoolCode = doc.getString("schoolCode");
                    if (studentSchoolCode != null && studentSchoolCode.equals(userSchoolCode)) {
                        
                        // 2. Either belongs to teacher's assignments OR has a conduct record
                        boolean inAssignments = false;
                        if (grade != null && section != null) {
                            String assignmentKey = grade + " - " + section;
                            for (String assignment : teacherAssignments) {
                                if (assignment.contains(assignmentKey)) {
                                    inAssignments = true;
                                    break;
                                }
                            }
                        }
                        
                        boolean hasDemerits = email != null && studentsWithDemerits.contains(email);
                        
                        if (!inAssignments && !hasDemerits) {
                            matchesRole = false;
                        }
                    } else {
                        matchesRole = false;
                    }
                }
            }

            boolean matchesSearch = searchText.isEmpty() || 
                                    (name != null && name.toLowerCase().contains(searchText)) ||
                                    (email != null && email.toLowerCase().contains(searchText));

            if (matchesRole && matchesSearch) {
                filteredList.add(doc);
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void showChangeRoleDialog(DocumentSnapshot doc) {
        String currentRole = doc.getString("role");
        int currentSelection = -1;
        for (int i = 0; i < roleKeys.length; i++) {
            if (roleKeys[i].equals(currentRole)) {
                currentSelection = i;
                break;
            }
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), R.layout.item_dialog_toggle, roleLabels);

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(getContext())
                .setTitle("Cambiar Rol de Usuario")
                .setSingleChoiceItems(adapter, currentSelection, (dialog, which) -> {
                    String newRole = roleKeys[which];
                    if (!newRole.equals(currentRole)) {
                        db.collection("users").document(doc.getId()).update("role", newRole)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(getContext(), "Rol actualizado correctamente", Toast.LENGTH_SHORT).show();
                                    dialog.dismiss();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        dialog.dismiss();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}