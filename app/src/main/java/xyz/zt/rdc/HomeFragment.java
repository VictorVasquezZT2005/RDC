package xyz.zt.rdc;

import android.os.Bundle;
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

import com.github.mikephil.charting.charts.HorizontalBarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HomeFragment extends Fragment {

    private TextView tvWelcome, tvSchoolChartTitle;
    private android.widget.Button btnGoToDemerits, btnGoToStudents, btnSettings;
    private HorizontalBarChart chartSchool, chartGlobal;
    private TextInputLayout tilSchoolSelector;
    private AutoCompleteTextView spinnerSchoolSelector;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private String userRole = "";
    private String userSchoolCode = "";
    private List<String> allSchoolNames = new ArrayList<>();
    private List<String> allSchoolCodes = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        tvWelcome = view.findViewById(R.id.tvWelcomeMessage);
        tvSchoolChartTitle = view.findViewById(R.id.tvSchoolChartTitle);
        btnGoToDemerits = view.findViewById(R.id.btnGoToDemerits);
        btnGoToStudents = view.findViewById(R.id.btnGoToStudents);
        btnSettings = view.findViewById(R.id.btnSettings);
        
        chartSchool = view.findViewById(R.id.chartSchool);
        chartGlobal = view.findViewById(R.id.chartGlobal);
        tilSchoolSelector = view.findViewById(R.id.tilSchoolSelector);
        spinnerSchoolSelector = view.findViewById(R.id.spinnerSchoolSelector);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        
        setupCharts();
        setupNavigation();
        loadInitialData();

        return view;
    }

    private void setupCharts() {
        configureHorizontalBarChart(chartSchool);
        configureHorizontalBarChart(chartGlobal);
    }

    private void configureHorizontalBarChart(HorizontalBarChart chart) {
        chart.getDescription().setEnabled(false);
        chart.setDrawGridBackground(false);
        chart.setDrawBarShadow(false);
        chart.setDrawValueAboveBar(false);
        chart.setPinchZoom(false);
        chart.setDoubleTapToZoomEnabled(false);
        
        // Hide all axes and lines
        chart.getXAxis().setEnabled(false);
        chart.getAxisLeft().setEnabled(false);
        chart.getAxisRight().setEnabled(false);
        
        // Disable internal legend completely
        chart.getLegend().setEnabled(false);
        
        chart.setTouchEnabled(false);
        chart.setNoDataText("Cargando...");
        chart.setNoDataTextColor(getThemeColor(android.R.attr.textColorPrimary));
        
        chart.setExtraBottomOffset(0f);
    }

    private void setupNavigation() {
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

        btnSettings.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new SettingsFragment())
                        .addToBackStack(null)
                        .commit();
            }
        });

        spinnerSchoolSelector.setOnItemClickListener((parent, view, position, id) -> {
            String selectedSchoolCode = allSchoolCodes.get(position);
            String selectedSchoolName = allSchoolNames.get(position);
            tvSchoolChartTitle.setText("Escuela: " + selectedSchoolName);
            loadSchoolChartData(selectedSchoolCode);
        });
    }

    private void loadInitialData() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        tvWelcome.setText("Cargando...");
        
        db.collection("users").whereEqualTo("email", user.getEmail()).get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                if (!isAdded() || getContext() == null) return;
                
                if (!queryDocumentSnapshots.isEmpty()) {
                    DocumentSnapshot doc = queryDocumentSnapshots.getDocuments().get(0);
                    String name = doc.getString("name");
                    userRole = doc.getString("role");
                    userSchoolCode = doc.getString("schoolCode");
                    tvWelcome.setText("Hola,\n" + name);
                    
                    if ("admin".equals(userRole)) {
                        tilSchoolSelector.setVisibility(View.VISIBLE);
                        loadAllSchoolsForSelector();
                    } else if (userSchoolCode != null) {
                        tvSchoolChartTitle.setText("Mi Escuela");
                        loadSchoolChartData(userSchoolCode);
                    }
                } else {
                    tvWelcome.setText("Hola,\n" + user.getEmail());
                }
                
                loadGlobalChartData();
            });
    }

    private void loadAllSchoolsForSelector() {
        db.collection("schools").get().addOnSuccessListener(queryDocumentSnapshots -> {
            allSchoolNames.clear();
            allSchoolCodes.clear();
            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                allSchoolNames.add(doc.getString("name"));
                allSchoolCodes.add(doc.getId());
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                    android.R.layout.simple_dropdown_item_1line, allSchoolNames);
            spinnerSchoolSelector.setAdapter(adapter);
        });
    }

    private void loadSchoolChartData(String schoolCode) {
        db.collection("users")
            .whereEqualTo("role", "student")
            .whereEqualTo("schoolCode", schoolCode)
            .get()
            .addOnSuccessListener(students -> {
                db.collection("demerits")
                    .whereEqualTo("schoolCode", schoolCode)
                    .get()
                    .addOnSuccessListener(demerits -> {
                        processChartData(chartSchool, students, demerits);
                    });
            });
    }

    private void loadGlobalChartData() {
        db.collection("users")
            .whereEqualTo("role", "student")
            .get()
            .addOnSuccessListener(students -> {
                db.collection("demerits")
                    .get()
                    .addOnSuccessListener(demerits -> {
                        processChartData(chartGlobal, students, demerits);
                    });
            });
    }

    private void processChartData(HorizontalBarChart chart, com.google.firebase.firestore.QuerySnapshot studentDocs, com.google.firebase.firestore.QuerySnapshot demeritDocs) {
        if (!isAdded() || getContext() == null) return;

        Set<String> studentsWithDemerits = new HashSet<>();
        for (DocumentSnapshot doc : demeritDocs) {
            String studentEmail = doc.getString("studentEmail");
            if (studentEmail != null) {
                studentsWithDemerits.add(studentEmail);
            }
        }

        int totalStudents = studentDocs.size();
        int withDemerits = 0;
        
        for (DocumentSnapshot doc : studentDocs) {
            String email = doc.getString("email");
            if (email != null && studentsWithDemerits.contains(email)) {
                withDemerits++;
            }
        }

        int withoutDemerits = totalStudents - withDemerits;

        updateChart(chart, withDemerits, withoutDemerits);
    }

    private void updateChart(HorizontalBarChart chart, int withDemerits, int withoutDemerits) {
        if (withDemerits == 0 && withoutDemerits == 0) {
            chart.clear();
            chart.setNoDataText("No hay alumnos registrados");
            chart.invalidate();
            return;
        }

        // Inverted stacked entry: [Green (Sin Deméritos), Red (Con Deméritos)]
        ArrayList<BarEntry> entries = new ArrayList<>();
        entries.add(new BarEntry(0f, new float[]{withoutDemerits, withDemerits}));

        BarDataSet dataSet = new BarDataSet(entries, "");
        ArrayList<Integer> colors = new ArrayList<>();
        if (getContext() != null) {
            colors.add(androidx.core.content.ContextCompat.getColor(getContext(), R.color.brand_primary));  // Green
            colors.add(androidx.core.content.ContextCompat.getColor(getContext(), R.color.brand_tertiary)); // Red
        } else {
            colors.add(android.graphics.Color.GREEN);
            colors.add(android.graphics.Color.RED);
        }

        dataSet.setColors(colors);
        dataSet.setValueTextColor(android.graphics.Color.WHITE);
        dataSet.setValueTextSize(14f);

        BarData data = new BarData(dataSet);
        data.setBarWidth(0.8f);
        data.setValueFormatter(new com.github.mikephil.charting.formatter.DefaultValueFormatter(0));

        chart.setData(data);
        chart.setFitBars(true);
        chart.animateX(1000);
        chart.invalidate();
    }
    private int getThemeColor(int attr) {
        android.util.TypedValue typedValue = new android.util.TypedValue();
        if (getContext() != null && getContext().getTheme().resolveAttribute(attr, typedValue, true)) {
            return typedValue.data;
        }
        return android.graphics.Color.GRAY;
    }
}