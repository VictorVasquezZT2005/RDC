package xyz.zt.rdc;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Actividad que implementa el formulario escolar con tabla dinámica y persistencia Room.
 */
public class SchoolFormActivity extends BaseActivity {

    private TableLayout tableForm;
    private EditText etDirectorInfo;
    private List<RegistroFila> listaFilas = new ArrayList<>();
    private AppDatabase db;
    
    // TextViews para los totales
    private TextView tvTotalDA, tvTotalDB, tvTotalDC, tvTotalDD;
    private TextView tvTotalRA, tvTotalRB, tvTotalRC;
    private TextView tvTotalRCA, tvTotalRCB;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_school_form);

        db = AppDatabase.getInstance(this);
        tableForm = findViewById(R.id.tableForm);
        etDirectorInfo = findViewById(R.id.etDirectorInfo);
        
        initTotalTextViews();
        setupTableRows();

        findViewById(R.id.btnSaveForm).setOnClickListener(v -> saveFormulario());
    }

    private void initTotalTextViews() {
        tvTotalDA = findViewById(R.id.tvTotalDA);
        tvTotalDB = findViewById(R.id.tvTotalDB);
        tvTotalDC = findViewById(R.id.tvTotalDC);
        tvTotalDD = findViewById(R.id.tvTotalDD);
        tvTotalRA = findViewById(R.id.tvTotalRA);
        tvTotalRB = findViewById(R.id.tvTotalRB);
        tvTotalRC = findViewById(R.id.tvTotalRC);
        tvTotalRCA = findViewById(R.id.tvTotalRCA);
        tvTotalRCB = findViewById(R.id.tvTotalRCB);
    }

    private void setupTableRows() {
        // Generamos las 8 filas numeradas
        for (int i = 1; i <= 8; i++) {
            final RegistroFila fila = new RegistroFila();
            fila.numeroFila = i;
            listaFilas.add(fila);

            TableRow tr = new TableRow(this);
            tr.setBackgroundColor(getResources().getColor(android.R.color.white));

            // 1. Índice
            tr.addView(createTextView(String.valueOf(i)));

            // 2. Fecha (11)
            final TextView tvFecha = createTextView("Toque...");
            tvFecha.setOnClickListener(v -> showDatePicker(tvFecha, fila));
            tr.addView(tvFecha);

            // 3. Deméritos (12: A, B, C, D)
            tr.addView(createNumericEditText(val -> { fila.dA = val; calculateTotals(); }));
            tr.addView(createNumericEditText(val -> { fila.dB = val; calculateTotals(); }));
            tr.addView(createNumericEditText(val -> { fila.dC = val; calculateTotals(); }));
            tr.addView(createNumericEditText(val -> { fila.dD = val; calculateTotals(); }));

            // 4. Redención (13: A, B, C)
            tr.addView(createNumericEditText(val -> { fila.rA = val; calculateTotals(); }));
            tr.addView(createNumericEditText(val -> { fila.rB = val; calculateTotals(); }));
            tr.addView(createNumericEditText(val -> { fila.rC = val; calculateTotals(); }));

            // 5. Reconocimiento (14: A, B)
            tr.addView(createNumericEditText(val -> { fila.rcA = val; calculateTotals(); }));
            tr.addView(createNumericEditText(val -> { fila.rcB = val; calculateTotals(); }));

            // 6. Registra (15)
            tr.addView(createStringEditText(s -> fila.nombreFirmaRegistra = s, 120));

            // 7. Responsable Redención (16)
            tr.addView(createStringEditText(s -> fila.responsableRedencion = s, 120));

            // 8. Firma Estudiante (17)
            tr.addView(createStringEditText(s -> fila.firmaEstudiante = s, 100));

            // Insertamos la fila antes de la fila de Totales (que es la última en el XML)
            tableForm.addView(tr, i + 1); 
        }
    }

    private TextView createTextView(String text) {
        TextView tv = new TextView(this, null, 0, R.style.TableCell);
        tv.setText(text);
        tv.setPadding(8, 8, 8, 8);
        return tv;
    }

    private EditText createNumericEditText(OnValueChangeListener listener) {
        EditText et = new EditText(this, null, 0, R.style.TableCell);
        et.setInputType(InputType.TYPE_CLASS_NUMBER);
        et.setGravity(Gravity.CENTER);
        et.setMinWidth(dpToPx(40));
        et.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                try {
                    int val = s.toString().isEmpty() ? 0 : Integer.parseInt(s.toString());
                    listener.onChanged(val);
                } catch (Exception e) { listener.onChanged(0); }
            }
        });
        return et;
    }

    private EditText createStringEditText(OnStringChangeListener listener, int minWidthDp) {
        EditText et = new EditText(this, null, 0, R.style.TableCell);
        et.setInputType(InputType.TYPE_CLASS_TEXT);
        et.setMinWidth(dpToPx(minWidthDp));
        et.setPadding(8, 8, 8, 8);
        et.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                listener.onChanged(s.toString());
            }
        });
        return et;
    }

    private void showDatePicker(TextView tv, RegistroFila fila) {
        Calendar c = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            String date = dayOfMonth + "/" + (month + 1) + "/" + year;
            tv.setText(date);
            fila.fecha = date;
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void calculateTotals() {
        int totalDA = 0, totalDB = 0, totalDC = 0, totalDD = 0;
        int totalRA = 0, totalRB = 0, totalRC = 0;
        int totalRCA = 0, totalRCB = 0;

        for (RegistroFila f : listaFilas) {
            totalDA += f.dA; totalDB += f.dB; totalDC += f.dC; totalDD += f.dD;
            totalRA += f.rA; totalRB += f.rB; totalRC += f.rC;
            totalRCA += f.rcA; totalRCB += f.rcB;
        }

        tvTotalDA.setText(String.valueOf(totalDA));
        tvTotalDB.setText(String.valueOf(totalDB));
        tvTotalDC.setText(String.valueOf(totalDC));
        tvTotalDD.setText(String.valueOf(totalDD));
        tvTotalRA.setText(String.valueOf(totalRA));
        tvTotalRB.setText(String.valueOf(totalRB));
        tvTotalRC.setText(String.valueOf(totalRC));
        tvTotalRCA.setText(String.valueOf(totalRCA));
        tvTotalRCB.setText(String.valueOf(totalRCB));
    }

    private void saveFormulario() {
        new Thread(() -> {
            FormularioData form = new FormularioData();
            form.nombreDirectorSello = etDirectorInfo.getText().toString();
            form.fechaCreacion = Calendar.getInstance().getTime().toString();
            
            long formId = db.formularioDao().insertFormulario(form);
            
            for (RegistroFila f : listaFilas) {
                f.formularioId = (int) formId;
            }
            db.formularioDao().insertFilas(listaFilas);

            runOnUiThread(() -> {
                Toast.makeText(this, "Formulario guardado localmente", Toast.LENGTH_SHORT).show();
                finish();
            });
        }).start();
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    interface OnValueChangeListener { void onChanged(int val); }
    interface OnStringChangeListener { void onChanged(String s); }
}
