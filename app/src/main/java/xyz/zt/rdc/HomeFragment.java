package xyz.zt.rdc;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class HomeFragment extends Fragment {

    private TextView tvWelcome;
    private android.widget.Button btnGoToDemerits, btnGoToStudents;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        tvWelcome = view.findViewById(R.id.tvWelcomeMessage);
        btnGoToDemerits = view.findViewById(R.id.btnGoToDemerits);
        btnGoToStudents = view.findViewById(R.id.btnGoToStudents);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        
        btnGoToDemerits.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new DemeritsFragment())
                        .addToBackStack(null)
                        .commit();
            }
        });

        btnGoToStudents.setOnClickListener(v -> {
            if (getActivity() != null) {
                UsersFragment fragment = new UsersFragment();
                Bundle args = new Bundle();
                args.putString("mode", "student_filter");
                fragment.setArguments(args);
                
                getActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, fragment)
                        .addToBackStack(null)
                        .commit();
            }
        });

        FirebaseUser user = mAuth.getCurrentUser();

        if (user != null) {
            tvWelcome.setText("Cargando...");
            db.collection("users").whereEqualTo("email", user.getEmail()).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (isAdded() && getContext() != null) {
                        if (!queryDocumentSnapshots.isEmpty()) {
                            String name = queryDocumentSnapshots.getDocuments().get(0).getString("name");
                            tvWelcome.setText("Hola,\n" + name);
                        } else {
                            tvWelcome.setText("Hola,\n" + user.getEmail());
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded() && getContext() != null) {
                        tvWelcome.setText("Hola,\n" + user.getEmail());
                    }
                });
        }

        return view;
    }
}