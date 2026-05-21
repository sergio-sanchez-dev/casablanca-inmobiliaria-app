package com.example.https;


import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import android.content.Intent;
import android.provider.CalendarContract;
import android.util.Log;
import java.util.*;

public class CalendarioActivity extends AppCompatActivity {

    CalendarView calendarView;
    ListView lvEventos;
    Button btnAgregarEvento;
    Button btnEstadisticas;
    TextView tvFechaSeleccionada;

    ArrayAdapter<String> adapter;
    List<String> listaEventos;
    List<String> listaIds;

    FirebaseFirestore db;
    String userId;
    String userEmail;
    String rol = "user";
    String fechaSeleccionada = "";

    String[] asesores  = {"Adri","Alex","Javi","David"};
    String[] tiposCita = {"Miguel","Aceptacion","Adquisicion","Asesoramiento financiero",
            "Bancos","Certificado energetico","Coger encargo","Contrato de alquiler",
            "Entrevista","Escritura publica","Gestion de encargo","Propuesta",
            "Re-Adquisicion","Reportaje Fotografico","Tasacion","Tasks",
            "Visita con cb hipotecas","Zona"};
    String[] horas    = {"10:00","11:00","12:00","13:00","14:00","15:00",
            "16:00","17:00","18:00","19:00","20:00","21:00"};
    String[] tiposVia = {"Calle","Avenida","Plaza","Paseo"};
    String[] estados  = {"pendiente","confirmada","cancelada","realizada"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendario);

        db        = FirebaseFirestore.getInstance();
        userId    = FirebaseAuth.getInstance().getCurrentUser().getUid();
        userEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail();

        calendarView        = findViewById(R.id.calendarView);
        lvEventos           = findViewById(R.id.lvEventos);
        tvFechaSeleccionada = findViewById(R.id.tvFechaSeleccionada);
        btnAgregarEvento    = findViewById(R.id.btnAgregarEvento);
        btnEstadisticas     = findViewById(R.id.btnEstadisticas);

        btnEstadisticas.setVisibility(View.GONE);

        listaEventos = new ArrayList<>();
        listaIds     = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, listaEventos);
        lvEventos.setAdapter(adapter);

        Calendar hoy = Calendar.getInstance();
        fechaSeleccionada = String.format("%02d/%02d/%04d",
                hoy.get(Calendar.DAY_OF_MONTH),
                hoy.get(Calendar.MONTH) + 1,
                hoy.get(Calendar.YEAR));
        tvFechaSeleccionada.setText("Citas del " + fechaSeleccionada);

        listaEventos.add("⏳ Cargando...");
        listaIds.add("");
        adapter.notifyDataSetChanged();

        cargarRolYDatos();

        calendarView.setOnDateChangeListener((view, year, month, day) -> {
            fechaSeleccionada = String.format("%02d/%02d/%04d", day, month + 1, year);
            tvFechaSeleccionada.setText("Citas del " + fechaSeleccionada);
            cargarDatos();
        });

        btnAgregarEvento.setOnClickListener(v -> mostrarDialogoCita());
        btnEstadisticas.setOnClickListener(v -> cargarEstadisticas());

        lvEventos.setOnItemLongClickListener((parent, view, position, id) -> {
            if (position >= listaIds.size()) return true;
            String docId = listaIds.get(position);
            if (docId == null || docId.isEmpty()) return true;
            String texto = listaEventos.get(position);

            new AlertDialog.Builder(this)
                    .setTitle("¿Qué quieres hacer?")
                    .setMessage(texto)
                    .setPositiveButton("✏️ Editar", (d, w) -> mostrarEditarCita(docId))
                    .setNeutralButton("🗑 Eliminar", (d, w) -> eliminarCita(docId, position))
                    .setNegativeButton("Cancelar", null)
                    .show();
            return true;
        });
    }

    // ─── ROL ─────────────────────────────────────────────────────────────────

    private void cargarRolYDatos() {
        db.collection("usuarios")
                .whereEqualTo("uid", userId)
                .get()
                .addOnSuccessListener(snapshots -> {
                    if (!snapshots.isEmpty()) {
                        DocumentSnapshot doc = snapshots.getDocuments().get(0);
                        String rolDoc = doc.getString("rol");
                        rol = (rolDoc != null) ? rolDoc : "user";
                    } else {
                        rol = "user";
                    }
                    btnEstadisticas.setVisibility("admin".equals(rol) ? View.VISIBLE : View.GONE);
                    cargarDatos();
                })
                .addOnFailureListener(e -> {
                    rol = "user";
                    btnEstadisticas.setVisibility(View.GONE);
                    cargarDatos();
                });
    }

    private void buscarRolPorEmail() {
        db.collection("usuarios")
                .whereEqualTo("email", userEmail)
                .get()
                .addOnSuccessListener(snapshots -> {
                    if (!snapshots.isEmpty()) {
                        String rolDoc = snapshots.getDocuments().get(0).getString("rol");
                        rol = (rolDoc != null) ? rolDoc : "user";
                        Log.d("CALENDARIO", "Rol encontrado por email: " + rol);
                        snapshots.getDocuments().get(0).getReference()
                                .update("uid_actualizado", userId);
                    } else {
                        rol = "user";
                    }
                    btnEstadisticas.setVisibility("admin".equals(rol) ? View.VISIBLE : View.GONE);
                    cargarDatos();
                })
                .addOnFailureListener(e -> {
                    rol = "user";
                    btnEstadisticas.setVisibility(View.GONE);
                    cargarDatos();
                });
    }

    // ─── CARGA ───────────────────────────────────────────────────────────────

    private void cargarDatos() {
        Log.d("CALENDARIO", "cargarDatos() rol=" + rol + " fecha=" + fechaSeleccionada);

        tvFechaSeleccionada.setText(
                "admin".equals(rol)
                        ? "👑 ADMIN — Citas del " + fechaSeleccionada
                        : "Citas del " + fechaSeleccionada
        );

        if ("admin".equals(rol)) {
            db.collection("citas")
                    .get()
                    .addOnSuccessListener(snapshots -> {
                        Log.d("CALENDARIO", "Admin: total docs=" + snapshots.size());
                        filtrarYMostrar(snapshots);
                    })
                    .addOnFailureListener(e -> mostrarError(e.getMessage()));
        } else {
            db.collection("citas")
                    .whereEqualTo("userId", userId)
                    .get()
                    .addOnSuccessListener(snapshots -> {
                        Log.d("CALENDARIO", "User: docs propios=" + snapshots.size());
                        filtrarYMostrar(snapshots);
                    })
                    .addOnFailureListener(e -> mostrarError(e.getMessage()));
        }
    }

    private void filtrarYMostrar(QuerySnapshot snapshots) {
        listaEventos.clear();
        listaIds.clear();

        for (QueryDocumentSnapshot doc : snapshots) {
            String fechaDoc = doc.getString("fecha");

            if (!fechaSeleccionada.isEmpty() &&
                    (fechaDoc == null || !fechaDoc.equals(fechaSeleccionada))) {
                continue;
            }

            String linea = construirLinea(doc);
            if (linea != null) {
                listaEventos.add(linea);
                listaIds.add(doc.getId());
            }
        }

        if (listaEventos.isEmpty()) {
            listaEventos.add("📭 Sin citas para este día");
            listaIds.add("");
        }

        adapter.notifyDataSetChanged();
    }

    private String construirLinea(QueryDocumentSnapshot doc) {
        String fecha  = doc.getString("fecha");
        String hora   = doc.getString("hora");
        String tipo   = doc.getString("tipo");
        String asesor = doc.getString("asesor");
        String dir    = doc.getString("direccion");
        if (tipo == null) return null;
        String estado = doc.getString("estado");
        if (estado == null) estado = "pendiente";

        String iconoEstado =
                estado.equals("confirmada") ? "🟢 " :
                        estado.equals("cancelada")  ? "🔴 " :
                                estado.equals("realizada")  ? "✅ " : "🟡 ";

        return iconoEstado +
                "📅 " + (fecha  != null ? fecha  : "") +
                "  🕒 " + (hora != null ? hora   : "") +
                "\n📌 " + tipo +
                "  👤 " + (asesor != null ? asesor : "") +
                "\n📍 " + (dir    != null ? dir    : "") +
                "\n⚙️ " + estado;
    }

    private void mostrarError(String msg) {
        listaEventos.clear();
        listaIds.clear();
        listaEventos.add("❌ Error: " + msg);
        listaIds.add("");
        adapter.notifyDataSetChanged();
        Log.e("CALENDARIO", "Error Firestore: " + msg);
    }

    // ─── NUEVA CITA ──────────────────────────────────────────────────────────

    private void mostrarDialogoCita() {
        View dialogView = LayoutInflater.from(this)
                .inflate(R.layout.dialogevento, null);

        Spinner  spAsesor   = dialogView.findViewById(R.id.spAsesorEvento);
        Spinner  spTipoCita = dialogView.findViewById(R.id.spTipoCitaEvento);
        Spinner  spHora     = dialogView.findViewById(R.id.spHoraEvento);
        Spinner  spTipoVia  = dialogView.findViewById(R.id.spTipoViaEvento);
        EditText etCalle    = dialogView.findViewById(R.id.etNombreCalleEvento);
        EditText etCiudad   = dialogView.findViewById(R.id.etCiudadEvento);
        EditText etFecha    = dialogView.findViewById(R.id.etFechaEvento);

        spAsesor.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, asesores));
        spTipoCita.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, tiposCita));
        spHora.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, horas));
        spTipoVia.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, tiposVia));

        etFecha.setText(fechaSeleccionada);
        etFecha.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            new DatePickerDialog(this, (dp, y, m, d) ->
                    etFecha.setText(String.format("%02d/%02d/%04d", d, m + 1, y)),
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)).show();
        });

        new AlertDialog.Builder(this)
                .setTitle("Nueva Cita")
                .setView(dialogView)
                .setPositiveButton("Guardar", (dialog, which) -> {
                    String asesor = spAsesor.getSelectedItem().toString();
                    String tipo   = spTipoCita.getSelectedItem().toString();
                    String hora   = spHora.getSelectedItem().toString();
                    String via    = spTipoVia.getSelectedItem().toString();
                    String calle  = etCalle.getText().toString().trim();
                    String ciudad = etCiudad.getText().toString().trim();
                    String fecha  = etFecha.getText().toString().trim();

                    if (fecha.isEmpty()) {
                        Toast.makeText(this, "Elige una fecha", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    guardarCita(asesor, tipo, hora, via + " " + calle + ", " + ciudad, fecha);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void guardarCita(String asesor, String tipo, String hora,
                             String direccion, String fecha) {
        Map<String, Object> cita = new HashMap<>();
        cita.put("asesor",    asesor);
        cita.put("tipo",      tipo);
        cita.put("hora",      hora);
        cita.put("direccion", direccion);
        cita.put("fecha",     fecha);
        cita.put("userId",    userId);
        cita.put("timestamp", new Date());
        cita.put("estado",    "pendiente");

        db.collection("citas").add(cita)
                .addOnSuccessListener(ref -> {
                    Toast.makeText(this, "✅ Cita guardada", Toast.LENGTH_SHORT).show();
                    abrirGoogleCalendar(tipo, asesor, hora, direccion, fecha);
                    cargarDatos();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "❌ Error: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
    }

    // ─── ELIMINAR ────────────────────────────────────────────────────────────

    private void eliminarCita(String docId, int position) {
        db.collection("citas").document(docId).delete()
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "🗑 Eliminada", Toast.LENGTH_SHORT).show();
                    listaEventos.remove(position);
                    listaIds.remove(position);
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "❌ Error al eliminar",
                                Toast.LENGTH_SHORT).show());
    }

    // ─── GOOGLE CALENDAR ─────────────────────────────────────────────────────

    private void abrirGoogleCalendar(String tipo, String asesor,
                                     String hora, String direccion, String fecha) {
        try {
            String[] f = fecha.split("/");
            String[] h = hora.split(":");
            Calendar start = Calendar.getInstance();
            start.set(Integer.parseInt(f[2]), Integer.parseInt(f[1]) - 1,
                    Integer.parseInt(f[0]), Integer.parseInt(h[0]),
                    Integer.parseInt(h[1]), 0);
            Calendar end = (Calendar) start.clone();
            end.add(Calendar.HOUR_OF_DAY, 1);

            startActivity(new Intent(Intent.ACTION_INSERT)
                    .setData(CalendarContract.Events.CONTENT_URI)
                    .putExtra(CalendarContract.Events.TITLE, tipo)
                    .putExtra(CalendarContract.Events.DESCRIPTION, "Asesor: " + asesor)
                    .putExtra(CalendarContract.Events.EVENT_LOCATION, direccion)
                    .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME,  start.getTimeInMillis())
                    .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, end.getTimeInMillis()));
        } catch (Exception ignored) {}
    }

    // ─── EDITAR CITA COMPLETA ────────────────────────────────────────────────
    // FIX: ya NO casteamos dialogView a LinearLayout (el XML raíz es ScrollView).
    // Construimos el diálogo con el formulario existente + un LinearLayout propio
    // para el spinner de estado, envuelto todo en un ScrollView nuevo.

    private void mostrarEditarCita(String docId) {

        db.collection("citas").document(docId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;

                    // Inflamos el formulario original (raíz = ScrollView o lo que sea)
                    View formView = LayoutInflater.from(this)
                            .inflate(R.layout.dialogevento, null);

                    Spinner  spAsesor   = formView.findViewById(R.id.spAsesorEvento);
                    Spinner  spTipoCita = formView.findViewById(R.id.spTipoCitaEvento);
                    Spinner  spHora     = formView.findViewById(R.id.spHoraEvento);
                    Spinner  spTipoVia  = formView.findViewById(R.id.spTipoViaEvento);
                    EditText etCalle    = formView.findViewById(R.id.etNombreCalleEvento);
                    EditText etCiudad   = formView.findViewById(R.id.etCiudadEvento);
                    EditText etFecha    = formView.findViewById(R.id.etFechaEvento);

                    // Contenedor raíz que SÍ es LinearLayout (creado en código)
                    // Así evitamos el ClassCastException del XML
                    LinearLayout contenedor = new LinearLayout(this);
                    contenedor.setOrientation(LinearLayout.VERTICAL);

                    // Añadimos el formulario al contenedor
                    contenedor.addView(formView);

                    // Añadimos label + spinner de estado debajo
                    TextView tvLabel = new TextView(this);
                    tvLabel.setText("Estado de la cita:");
                    tvLabel.setPadding(32, 24, 32, 8);
                    tvLabel.setTextSize(14f);
                    contenedor.addView(tvLabel);

                    Spinner spEstado = new Spinner(this);
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT);
                    lp.setMargins(32, 0, 32, 24);
                    spEstado.setLayoutParams(lp);
                    spEstado.setAdapter(new ArrayAdapter<>(this,
                            android.R.layout.simple_spinner_dropdown_item, estados));
                    contenedor.addView(spEstado);

                    // Envolvemos en ScrollView para que quepa en pantalla
                    ScrollView scrollView = new ScrollView(this);
                    scrollView.addView(contenedor);

                    // ── Adapters ──
                    spAsesor.setAdapter(new ArrayAdapter<>(this,
                            android.R.layout.simple_spinner_dropdown_item, asesores));
                    spTipoCita.setAdapter(new ArrayAdapter<>(this,
                            android.R.layout.simple_spinner_dropdown_item, tiposCita));
                    spHora.setAdapter(new ArrayAdapter<>(this,
                            android.R.layout.simple_spinner_dropdown_item, horas));
                    spTipoVia.setAdapter(new ArrayAdapter<>(this,
                            android.R.layout.simple_spinner_dropdown_item, tiposVia));

                    // ── Pre-rellenar con datos actuales ──
                    setSpinner(spAsesor,   asesores,  doc.getString("asesor"));
                    setSpinner(spTipoCita, tiposCita, doc.getString("tipo"));
                    setSpinner(spHora,     horas,     doc.getString("hora"));
                    setSpinner(spEstado,   estados,   doc.getString("estado"));

                    // Dirección: separar "TipoVia Calle, Ciudad"
                    String dirCompleta = doc.getString("direccion");
                    if (dirCompleta != null && !dirCompleta.isEmpty()) {
                        boolean encontrada = false;
                        for (String via : tiposVia) {
                            if (dirCompleta.startsWith(via + " ")) {
                                setSpinner(spTipoVia, tiposVia, via);
                                String sinVia = dirCompleta.substring(via.length() + 1);
                                int coma = sinVia.lastIndexOf(",");
                                if (coma >= 0) {
                                    etCalle.setText(sinVia.substring(0, coma).trim());
                                    etCiudad.setText(sinVia.substring(coma + 1).trim());
                                } else {
                                    etCalle.setText(sinVia.trim());
                                }
                                encontrada = true;
                                break;
                            }
                        }
                        if (!encontrada) {
                            etCalle.setText(dirCompleta);
                        }
                    }

                    // Fecha con DatePicker
                    String fechaDoc = doc.getString("fecha");
                    etFecha.setText(fechaDoc != null ? fechaDoc : fechaSeleccionada);
                    etFecha.setOnClickListener(v -> {
                        Calendar cal = Calendar.getInstance();
                        new DatePickerDialog(this, (dp, y, m, d) ->
                                etFecha.setText(String.format("%02d/%02d/%04d", d, m + 1, y)),
                                cal.get(Calendar.YEAR),
                                cal.get(Calendar.MONTH),
                                cal.get(Calendar.DAY_OF_MONTH)).show();
                    });

                    // ── Diálogo ──
                    new AlertDialog.Builder(this)
                            .setTitle("✏️ Editar Cita")
                            .setView(scrollView)  // <-- usamos el ScrollView wrapper
                            .setPositiveButton("💾 Guardar cambios", (d, w) -> {

                                String via    = spTipoVia.getSelectedItem().toString();
                                String calle  = etCalle.getText().toString().trim();
                                String ciudad = etCiudad.getText().toString().trim();
                                String fecha  = etFecha.getText().toString().trim();

                                if (fecha.isEmpty()) {
                                    Toast.makeText(this, "Elige una fecha",
                                            Toast.LENGTH_SHORT).show();
                                    return;
                                }

                                Map<String, Object> update = new HashMap<>();
                                update.put("asesor",    spAsesor.getSelectedItem().toString());
                                update.put("tipo",      spTipoCita.getSelectedItem().toString());
                                update.put("hora",      spHora.getSelectedItem().toString());
                                update.put("estado",    spEstado.getSelectedItem().toString());
                                update.put("fecha",     fecha);
                                update.put("direccion", via + " " + calle + ", " + ciudad);

                                db.collection("citas").document(docId)
                                        .update(update)
                                        .addOnSuccessListener(a -> {
                                            Toast.makeText(this, "✔ Cita actualizada",
                                                    Toast.LENGTH_SHORT).show();
                                            cargarDatos();
                                        })
                                        .addOnFailureListener(e ->
                                                Toast.makeText(this, "❌ Error al guardar",
                                                        Toast.LENGTH_SHORT).show());
                            })
                            .setNegativeButton("Cancelar", null)
                            .show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "❌ No se pudo cargar la cita",
                                Toast.LENGTH_SHORT).show());
    }

    /** Helper: selecciona el item del spinner que coincida con el valor guardado */
    private void setSpinner(Spinner spinner, String[] opciones, String valor) {
        if (valor == null) return;
        int idx = Arrays.asList(opciones).indexOf(valor);
        if (idx >= 0) spinner.setSelection(idx);
    }

    // ─── ESTADÍSTICAS — admin ve TODOS, user ve solo las suyas ───────────────

    private void cargarEstadisticas() {
        if (!"admin".equals(rol)) {
            Toast.makeText(this, "⛔ Solo disponible para administradores",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        String[] partes   = fechaSeleccionada.split("/");
        String mesActual  = partes.length == 3 ? partes[1] : "";
        String anioActual = partes.length == 3 ? partes[2] : "";

        // Admin descarga todo; user solo lo suyo (aquí siempre admin, pero dejamos
        // la lógica preparada para extenderlo)
        CollectionReference ref = db.collection("citas");

        ref.get().addOnSuccessListener(snapshots -> {

            // Contadores DÍA — por usuario
            Map<String, int[]> diaUsuario = new LinkedHashMap<>();
            // Contadores MES — por usuario
            Map<String, int[]> mesUsuario = new LinkedHashMap<>();

            // Totales globales DÍA
            int dTotal = 0, dPend = 0, dConf = 0, dCanc = 0, dReal = 0;
            // Totales globales MES
            int mTotal = 0, mPend = 0, mConf = 0, mCanc = 0, mReal = 0;

            for (DocumentSnapshot doc : snapshots) {
                String fecha   = doc.getString("fecha");
                String estado  = doc.getString("estado");
                String uidCita = doc.getString("userId");
                if (fecha == null) continue;

                String[] fp = fecha.split("/");
                if (fp.length != 3) continue;

                String fMes  = fp[1];
                String fAnio = fp[2];

                // Nombre del asesor para agrupar (usamos userId como clave,
                // mostramos email o asesor si existe)
                String asesor = doc.getString("asesor");
                String claveUsuario = (asesor != null && !asesor.isEmpty())
                        ? asesor : (uidCita != null ? uidCita.substring(0, 6) : "?");

                // ── DÍA ──
                if (fecha.equals(fechaSeleccionada)) {
                    dTotal++;
                    if ("pendiente".equals(estado))       dPend++;
                    else if ("confirmada".equals(estado)) dConf++;
                    else if ("cancelada".equals(estado))  dCanc++;
                    else if ("realizada".equals(estado))  dReal++;

                    // Por usuario/asesor
                    if (!diaUsuario.containsKey(claveUsuario))
                        diaUsuario.put(claveUsuario, new int[5]); // total,pend,conf,canc,real
                    int[] cu = diaUsuario.get(claveUsuario);
                    cu[0]++;
                    if ("pendiente".equals(estado))       cu[1]++;
                    else if ("confirmada".equals(estado)) cu[2]++;
                    else if ("cancelada".equals(estado))  cu[3]++;
                    else if ("realizada".equals(estado))  cu[4]++;
                }

                // ── MES ──
                if (fMes.equals(mesActual) && fAnio.equals(anioActual)) {
                    mTotal++;
                    if ("pendiente".equals(estado))       mPend++;
                    else if ("confirmada".equals(estado)) mConf++;
                    else if ("cancelada".equals(estado))  mCanc++;
                    else if ("realizada".equals(estado))  mReal++;

                    // Por usuario/asesor
                    if (!mesUsuario.containsKey(claveUsuario))
                        mesUsuario.put(claveUsuario, new int[5]);
                    int[] cu = mesUsuario.get(claveUsuario);
                    cu[0]++;
                    if ("pendiente".equals(estado))       cu[1]++;
                    else if ("confirmada".equals(estado)) cu[2]++;
                    else if ("cancelada".equals(estado))  cu[3]++;
                    else if ("realizada".equals(estado))  cu[4]++;
                }
            }

            mostrarDashboard(
                    dTotal, dPend, dConf, dCanc, dReal, diaUsuario,
                    mTotal, mPend, mConf, mCanc, mReal, mesUsuario,
                    mesActual, anioActual
            );

        }).addOnFailureListener(e -> mostrarError(e.getMessage()));
    }

    private void mostrarDashboard(
            int dTotal, int dPen, int dCon, int dCan, int dRea, Map<String,int[]> diaU,
            int mTotal, int mPen, int mCon, int mCan, int mRea, Map<String,int[]> mesU,
            String mes, String anio) {

        listaEventos.clear();
        listaIds.clear();

        String[] meses = {"","Enero","Febrero","Marzo","Abril","Mayo","Junio",
                "Julio","Agosto","Septiembre","Octubre","Noviembre","Diciembre"};
        int mesNum = 0;
        try { mesNum = Integer.parseInt(mes); } catch (Exception ignored) {}
        String nombreMes = (mesNum >= 1 && mesNum <= 12) ? meses[mesNum] : mes;

        // ── CABECERA ──
        addFila("━━━━━━━━━━━━━━━━━━━━━━━━");
        addFila("📊  DASHBOARD ADMIN");
        addFila("━━━━━━━━━━━━━━━━━━━━━━━━");
        addFila("");

        // ── SECCIÓN DÍA GLOBAL ──
        addFila("📅  DÍA: " + fechaSeleccionada);
        addFila("────────────────────────");
        addFila("📌  Total:          " + dTotal);
        addFila("🟡  Pendientes:     " + dPen);
        addFila("🟢  Confirmadas:    " + dCon);
        addFila("🔴  Canceladas:     " + dCan);
        addFila("✅  Realizadas:     " + dRea);

        // Desglose por asesor/usuario ese día
        if (!diaU.isEmpty()) {
            addFila("");
            addFila("  👥 Por asesor ese día:");
            for (Map.Entry<String, int[]> e : diaU.entrySet()) {
                int[] v = e.getValue();
                addFila("  • " + e.getKey() + ": " + v[0]
                        + "  🟡" + v[1] + " 🟢" + v[2] + " 🔴" + v[3] + " ✅" + v[4]);
            }
        }

        addFila("");

        // ── SECCIÓN MES GLOBAL ──
        addFila("🗓  MES: " + nombreMes + " " + anio);
        addFila("────────────────────────");
        addFila("📌  Total:          " + mTotal);
        addFila("🟡  Pendientes:     " + mPen);
        addFila("🟢  Confirmadas:    " + mCon);
        addFila("🔴  Canceladas:     " + mCan);
        addFila("✅  Realizadas:     " + mRea);

        // Desglose por asesor/usuario ese mes
        if (!mesU.isEmpty()) {
            addFila("");
            addFila("  👥 Por asesor ese mes:");
            for (Map.Entry<String, int[]> e : mesU.entrySet()) {
                int[] v = e.getValue();
                addFila("  • " + e.getKey() + ": " + v[0]
                        + "  🟡" + v[1] + " 🟢" + v[2] + " 🔴" + v[3] + " ✅" + v[4]);
            }
        }

        addFila("");
        addFila("━━━━━━━━━━━━━━━━━━━━━━━━");
        addFila("↩  Pulsa una fecha para volver");

        adapter.notifyDataSetChanged();
    }

    private void addFila(String texto) {
        listaEventos.add(texto);
        listaIds.add("");
    }
}


