package com.example.https;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class DashboardActivity extends AppCompatActivity {

    // Navegación
    CardView btnCaptaciones, btnEncargos, btnVisitas, btnSenales,
            btnHonorarios, btnContratos, btnCalendario, btnSetup;

    // Stats grandes (header)
    TextView tvStatCaptaciones, tvStatCaptacionesSub;
    TextView tvStatVisitas, tvStatVisitasSub;

    // Subtítulos dinámicos de cada tarjeta
    TextView tvCountCaptaciones, tvCountEncargos, tvCountVisitas,
            tvCountSenales, tvCountHonorarios, tvCountContratos, tvCountCalendario;

    // Header
    TextView tvFecha, tvAvatar;

    // Citas (ex-Tareas)
    Button btnHoy, btnPendientes, btnCompletadas;
    RecyclerView rvTareas;
    TareasAdapter tareasAdapter;
    List<String> listaTareas;

    FirebaseFirestore db;
    String userId;

    // Timestamps inicio/fin del mes actual
    Timestamp tsInicioMes, tsFinMes;

    // Fecha de hoy en formato DD/MM/YYYY (para filtrar citas)
    String fechaHoy;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.drawer_layout);

        db     = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // ── Calcular fechas ────────────────────────────────────────────────
        Calendar cal = Calendar.getInstance();

        // Fecha hoy para filtrar citas (colección usa "DD/MM/YYYY")
        fechaHoy = String.format("%02d/%02d/%04d",
                cal.get(Calendar.DAY_OF_MONTH),
                cal.get(Calendar.MONTH) + 1,
                cal.get(Calendar.YEAR));

        // Inicio del mes (día 1, 00:00:00)
        Calendar inicio = Calendar.getInstance();
        inicio.set(Calendar.DAY_OF_MONTH, 1);
        inicio.set(Calendar.HOUR_OF_DAY, 0);
        inicio.set(Calendar.MINUTE, 0);
        inicio.set(Calendar.SECOND, 0);
        inicio.set(Calendar.MILLISECOND, 0);
        tsInicioMes = new Timestamp(inicio.getTime());

        // Fin del mes (último día, 23:59:59)
        Calendar fin = Calendar.getInstance();
        fin.set(Calendar.DAY_OF_MONTH, fin.getActualMaximum(Calendar.DAY_OF_MONTH));
        fin.set(Calendar.HOUR_OF_DAY, 23);
        fin.set(Calendar.MINUTE, 59);
        fin.set(Calendar.SECOND, 59);
        tsFinMes = new Timestamp(fin.getTime());

        // ── Header ─────────────────────────────────────────────────────────
        tvFecha  = findViewById(R.id.tvFecha);
        tvAvatar = findViewById(R.id.tvAvatar);

        String email = FirebaseAuth.getInstance().getCurrentUser().getEmail();
        if (email != null && email.length() >= 2) {
            tvAvatar.setText(email.substring(0, 2).toUpperCase());
        }

        String[] diasSemana  = {"Domingo","Lunes","Martes","Miércoles","Jueves","Viernes","Sábado"};
        String[] mesesNombre = {"enero","febrero","marzo","abril","mayo","junio",
                "julio","agosto","septiembre","octubre","noviembre","diciembre"};
        tvFecha.setText("Buenos días · "
                + diasSemana[cal.get(Calendar.DAY_OF_WEEK) - 1] + ", "
                + cal.get(Calendar.DAY_OF_MONTH) + " "
                + mesesNombre[cal.get(Calendar.MONTH)] + " "
                + cal.get(Calendar.YEAR));

        // ── Stats ──────────────────────────────────────────────────────────
        tvStatCaptaciones    = findViewById(R.id.tvStatCaptaciones);
        tvStatCaptacionesSub = findViewById(R.id.tvStatCaptacionesSub);
        tvStatVisitas        = findViewById(R.id.tvStatVisitas);
        tvStatVisitasSub     = findViewById(R.id.tvStatVisitasSub);

        // ── Subtítulos tarjetas ────────────────────────────────────────────
        tvCountCaptaciones = findViewById(R.id.tvCountCaptaciones);
        tvCountEncargos    = findViewById(R.id.tvCountEncargos);
        tvCountVisitas     = findViewById(R.id.tvCountVisitas);
        tvCountSenales     = findViewById(R.id.tvCountSenales);
        tvCountHonorarios  = findViewById(R.id.tvCountHonorarios);
        tvCountContratos   = findViewById(R.id.tvCountContratos);
        tvCountCalendario  = findViewById(R.id.tvCountCalendario);

        // ── Navegación ─────────────────────────────────────────────────────
        btnCaptaciones = findViewById(R.id.btnCaptaciones);
        btnEncargos    = findViewById(R.id.btnEncargos);
        btnVisitas     = findViewById(R.id.btnVisitas);
        btnSenales     = findViewById(R.id.btnSenales);
        btnHonorarios  = findViewById(R.id.btnHonorarios);
        btnContratos   = findViewById(R.id.btnContratos);
        btnCalendario  = findViewById(R.id.btnCalendario);
        btnSetup       = findViewById(R.id.btnSetup);

        btnCaptaciones.setOnClickListener(v -> startActivity(new Intent(this, CaptacionActivity.class)));
        btnEncargos.setOnClickListener(v   -> startActivity(new Intent(this, EncargoActivity.class)));
        btnVisitas.setOnClickListener(v    -> startActivity(new Intent(this, VisitasActivity.class)));
        btnSenales.setOnClickListener(v    -> startActivity(new Intent(this, SenalActivity.class)));
        btnHonorarios.setOnClickListener(v -> startActivity(new Intent(this, PropuestaActivity.class)));
        btnContratos.setOnClickListener(v  -> startActivity(new Intent(this, ContratoActivity.class)));
        btnCalendario.setOnClickListener(v -> startActivity(new Intent(this, CalendarioActivity.class)));
        btnSetup.setOnClickListener(v      -> startActivity(new Intent(this, SetupActivity.class)));

        comprobarRolYMostrarBoton();

        // ── Citas ──────────────────────────────────────────────────────────
        btnHoy         = findViewById(R.id.btnHoy);
        btnPendientes  = findViewById(R.id.btnPendientes);
        btnCompletadas = findViewById(R.id.btnCompletadas);
        rvTareas       = findViewById(R.id.rvTareas);

        listaTareas   = new ArrayList<>();
        tareasAdapter = new TareasAdapter(listaTareas);
        rvTareas.setLayoutManager(new LinearLayoutManager(this));
        rvTareas.setAdapter(tareasAdapter);
        rvTareas.setNestedScrollingEnabled(false);

        cargarCitasHoy();
        btnHoy.setOnClickListener(v        -> cargarCitasHoy());
        btnPendientes.setOnClickListener(v -> cargarCitasPorEstado("pendiente"));
        btnCompletadas.setOnClickListener(v-> cargarCitasPorEstado("realizada"));

        // ── Contadores reales de Firestore ─────────────────────────────────
        cargarContadores();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // ROL ADMIN
    // ═════════════════════════════════════════════════════════════════════════

    private void comprobarRolYMostrarBoton() {
        db.collection("usuarios")
                .whereEqualTo("uid", userId)
                .get()
                .addOnSuccessListener(query -> {
                    if (!query.isEmpty()) {
                        String rol = query.getDocuments().get(0).getString("rol");
                        btnSetup.setVisibility("admin".equals(rol) ? View.VISIBLE : View.GONE);
                    } else {
                        btnSetup.setVisibility(View.GONE);
                    }
                });
    }

    // ═════════════════════════════════════════════════════════════════════════
    // CITAS (colección "citas", campo "fecha" = "DD/MM/YYYY", campo "estado")
    // ═════════════════════════════════════════════════════════════════════════

    private void cargarCitasHoy() {
        db.collection("citas")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(snapshots -> {
                    listaTareas.clear();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        if (fechaHoy.equals(doc.getString("fecha"))) {
                            String tipo   = doc.getString("tipo");
                            String hora   = doc.getString("hora");
                            String estado = doc.getString("estado");
                            if (tipo != null) {
                                String display = tipo + (hora != null ? " · " + hora : "");
                                listaTareas.add(display + "|" + (estado != null ? estado : "pendiente"));
                            }
                        }
                    }
                    if (listaTareas.isEmpty()) listaTareas.add("Sin citas para hoy|");
                    tareasAdapter.notifyDataSetChanged();

                    // Actualizar stat "Visitas hoy" con citas de hoy
                    tvStatVisitas.setText(String.valueOf(listaTareas.size() == 1
                            && listaTareas.get(0).startsWith("Sin citas") ? 0 : listaTareas.size()));
                    long pendientes = listaTareas.stream()
                            .filter(s -> s.contains("|pendiente")).count();
                    tvStatVisitasSub.setText(pendientes > 0 ? pendientes + " pendientes" : "Al día");
                });
    }

    private void cargarCitasPorEstado(String estado) {
        db.collection("citas")
                .whereEqualTo("userId", userId)
                .whereEqualTo("estado", estado)
                .get()
                .addOnSuccessListener(snapshots -> {
                    listaTareas.clear();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        String tipo  = doc.getString("tipo");
                        String fecha = doc.getString("fecha");
                        if (tipo != null) {
                            listaTareas.add(tipo + (fecha != null ? " · " + fecha : "") + "|" + estado);
                        }
                    }
                    if (listaTareas.isEmpty()) listaTareas.add("Sin citas · " + estado + "|");
                    tareasAdapter.notifyDataSetChanged();
                });
    }

    // ═════════════════════════════════════════════════════════════════════════
    // CONTADORES MENSUALES
    // Todas las colecciones usan "timestamp" (Firestore Timestamp) y "userId"
    // ═════════════════════════════════════════════════════════════════════════

    private void cargarContadores() {

        // CAPTACIONES
        db.collection("captaciones")
                .whereEqualTo("userId", userId)
                .whereGreaterThanOrEqualTo("timestamp", tsInicioMes)
                .whereLessThanOrEqualTo("timestamp", tsFinMes)
                .get()
                .addOnSuccessListener(snap -> {
                    int n = snap.size();
                    tvStatCaptaciones.setText(String.valueOf(n));
                    tvCountCaptaciones.setText(n + " este mes");
                })
                .addOnFailureListener(e -> {
                    tvStatCaptaciones.setText("0");
                    tvCountCaptaciones.setText("0 este mes");
                });

        // ENCARGOS
        db.collection("encargos")
                .whereEqualTo("userId", userId)
                .whereGreaterThanOrEqualTo("timestamp", tsInicioMes)
                .whereLessThanOrEqualTo("timestamp", tsFinMes)
                .get()
                .addOnSuccessListener(snap ->
                        tvCountEncargos.setText(snap.size() + " este mes"))
                .addOnFailureListener(e ->
                        tvCountEncargos.setText("0 este mes"));

        // VISITAS (colección "visitas" con timestamp)
        db.collection("visitas")
                .whereEqualTo("userId", userId)
                .whereGreaterThanOrEqualTo("timestamp", tsInicioMes)
                .whereLessThanOrEqualTo("timestamp", tsFinMes)
                .get()
                .addOnSuccessListener(snap ->
                        tvCountVisitas.setText(snap.size() + " este mes"))
                .addOnFailureListener(e ->
                        tvCountVisitas.setText("0 este mes"));

        // SEÑALES: suma senales + devoluciones
        db.collection("senales")
                .whereEqualTo("userId", userId)
                .whereGreaterThanOrEqualTo("timestamp", tsInicioMes)
                .whereLessThanOrEqualTo("timestamp", tsFinMes)
                .get()
                .addOnSuccessListener(snapSenales -> {
                    int countSenales = snapSenales.size();
                    db.collection("devoluciones")
                            .whereEqualTo("userId", userId)
                            .whereGreaterThanOrEqualTo("timestamp", tsInicioMes)
                            .whereLessThanOrEqualTo("timestamp", tsFinMes)
                            .get()
                            .addOnSuccessListener(snapDev ->
                                    tvCountSenales.setText((countSenales + snapDev.size()) + " este mes"))
                            .addOnFailureListener(e ->
                                    tvCountSenales.setText(countSenales + " este mes"));
                })
                .addOnFailureListener(e ->
                        tvCountSenales.setText("0 este mes"));

        // PROPUESTAS / HONORARIOS
        db.collection("honorarios_comprador")
                .whereEqualTo("userId", userId)
                .whereGreaterThanOrEqualTo("timestamp", tsInicioMes)
                .whereLessThanOrEqualTo("timestamp", tsFinMes)
                .get()
                .addOnSuccessListener(snap ->
                        tvCountHonorarios.setText(snap.size() + " este mes"))
                .addOnFailureListener(e ->
                        tvCountHonorarios.setText("0 este mes"));

        // CONTRATOS
        db.collection("contratos")
                .whereEqualTo("userId", userId)
                .whereGreaterThanOrEqualTo("timestamp", tsInicioMes)
                .whereLessThanOrEqualTo("timestamp", tsFinMes)
                .get()
                .addOnSuccessListener(snap ->
                        tvCountContratos.setText(snap.size() + " este mes"))
                .addOnFailureListener(e ->
                        tvCountContratos.setText("0 este mes"));

        // CITAS del mes (para el subtítulo del calendario)
        db.collection("citas")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(snap -> {
                    // Filtramos por mes/año manualmente porque "fecha" es String "DD/MM/YYYY"
                    Calendar cal = Calendar.getInstance();
                    String mesAnio = String.format("%02d/%04d",
                            cal.get(Calendar.MONTH) + 1,
                            cal.get(Calendar.YEAR));
                    long citasMes = 0;
                    for (QueryDocumentSnapshot doc : snap) {
                        String fecha = doc.getString("fecha");
                        if (fecha != null && fecha.length() == 10
                                && fecha.substring(3).equals(mesAnio)) {
                            citasMes++;
                        }
                    }
                    tvCountCalendario.setText(citasMes + " citas este mes");
                })
                .addOnFailureListener(e ->
                        tvCountCalendario.setText("Ver agenda completa"));
    }
}