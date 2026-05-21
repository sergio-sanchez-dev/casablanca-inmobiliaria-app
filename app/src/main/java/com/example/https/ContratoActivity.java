package com.example.https;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ContratoActivity extends AppCompatActivity {

    // ── Medidas A4 ────────────────────────────────────────────────────────────
    private static final float PW = 595f, PH = 842f;
    private static final float ML = 36f, MR = 36f;
    private static final float TW = PW - ML - MR;
    private static final float MB = 40f;
    private static final float TS_BODY = 11.5f;

    // ── Estado PDF ────────────────────────────────────────────────────────────
    private PdfDocument      mDoc;
    private PdfDocument.Page mPage;
    private Canvas           mCv;
    private int              mPageNum;
    private float            mY;
    private Bitmap           mLogo;
    private Bitmap           mSello;

    // ── EditTexts ─────────────────────────────────────────────────────────────
    EditText etC1Nombre, etC1NIF, etC1Domicilio;
    EditText etC2Nombre, etC2NIF, etC2Domicilio;
    CheckBox cbPropiaNombre;
    EditText etRepNombre, etRepDomicilio, etRepNIF, etRepCalidad;
    EditText etPlazo;
    EditText etV1Nombre, etV1NIF;
    EditText etV2Nombre, etV2NIF;
    EditText etV3Nombre, etV3NIF;
    EditText etV4Nombre, etV4NIF;
    EditText etDireccion, etRefCatastral, etOtros;
    EditText etPrecio, etArras, etContratoPrivado, etEscritura;
    EditText etCargas;
    EditText etFechaContratoPrivado, etFechaEscritura;
    EditText etLugar, etFirmaDia, etFirmaMes, etFirmaAnio;
    EditText etClausula10;
    Spinner  spEstado;
    SignatureView signatureView;
    Button   btnBorrarFirma, btnVerFirma;
    Button   btnGuardar;

    // ── Lista ─────────────────────────────────────────────────────────────────
    Button btnFiltroHoy, btnFiltroAyer, btnFiltroMes, btnFiltroTodas;
    Spinner spFiltroAsesor;
    RecyclerView rvContratos;
    GenericListAdapter listAdapter;
    List<GenericItem> listaContratos = new ArrayList<>();

    // ── Firebase ──────────────────────────────────────────────────────────────
    FirebaseFirestore db;
    String userId;
    boolean esAdmin = false;
    String docIdEditando = null;
    Map<String, String> mapaAsesores = new HashMap<>();

    // ─────────────────────────────────────────────────────────────────────────
    // LIFECYCLE
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contrato);
        db     = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        cargarAsesores();
        bindFormulario();
        bindLista();
        comprobarRol();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BIND
    // ─────────────────────────────────────────────────────────────────────────
    private void bindFormulario() {
        etC1Nombre    = findViewById(R.id.etC1Nombre);
        etC1NIF       = findViewById(R.id.etC1NIF);
        etC1Domicilio = findViewById(R.id.etC1Domicilio);
        etC2Nombre    = findViewById(R.id.etC2Nombre);
        etC2NIF       = findViewById(R.id.etC2NIF);
        etC2Domicilio = findViewById(R.id.etC2Domicilio);
        cbPropiaNombre  = findViewById(R.id.cbPropiaNombre);
        etRepNombre     = findViewById(R.id.etRepNombre);
        etRepDomicilio  = findViewById(R.id.etRepDomicilio);
        etRepNIF        = findViewById(R.id.etRepNIF);
        etRepCalidad    = findViewById(R.id.etRepCalidad);
        etPlazo         = findViewById(R.id.etPlazo);
        etV1Nombre = findViewById(R.id.etV1Nombre);
        etV1NIF = findViewById(R.id.etV1NIF);
        etV2Nombre = findViewById(R.id.etV2Nombre);
        etV2NIF = findViewById(R.id.etV2NIF);
        etV3Nombre = findViewById(R.id.etV3Nombre);
        etV3NIF = findViewById(R.id.etV3NIF);
        etV4Nombre = findViewById(R.id.etV4Nombre);
        etV4NIF = findViewById(R.id.etV4NIF);
        etDireccion    = findViewById(R.id.etDireccion);
        etRefCatastral = findViewById(R.id.etRefCatastral);
        etOtros        = findViewById(R.id.etOtros);
        etPrecio          = findViewById(R.id.etPrecio);
        etArras           = findViewById(R.id.etArras);
        etContratoPrivado = findViewById(R.id.etContratoPrivado);
        etEscritura       = findViewById(R.id.etEscritura);
        etCargas          = findViewById(R.id.etCargas);
        etFechaContratoPrivado = findViewById(R.id.etFechaContratoPrivado);
        etFechaEscritura       = findViewById(R.id.etFechaEscritura);
        etLugar     = findViewById(R.id.etLugar);
        etFirmaDia  = findViewById(R.id.etFirmaDia);
        etFirmaMes  = findViewById(R.id.etFirmaMes);
        etFirmaAnio = findViewById(R.id.etFirmaAnio);
        etClausula10 = findViewById(R.id.etClausula10);
        signatureView  = findViewById(R.id.signatureView);
        btnBorrarFirma = findViewById(R.id.btnBorrarFirma);
        btnVerFirma    = findViewById(R.id.btnVerFirma);
        spEstado = findViewById(R.id.spEstado);
        btnGuardar = findViewById(R.id.btnGuardar);

        if (btnBorrarFirma != null && signatureView != null) {
            btnBorrarFirma.setOnClickListener(v -> signatureView.clear());
        }

        if (btnVerFirma != null && signatureView != null) {
            btnVerFirma.setOnClickListener(v -> Toast.makeText(this,
                    signatureView.hasFirma() ? "Firma capturada ✓" : "Sin firma todavía",
                    Toast.LENGTH_SHORT).show());
        }

        if (spEstado != null) {
            spEstado.setAdapter(new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_dropdown_item,
                    new String[]{"En proceso", "Firmado", "Cancelado", "Pendiente escritura"}));
        }

        if (btnGuardar != null) {
            btnGuardar.setOnClickListener(v -> guardarOActualizar());
        }
    }

    private void bindLista() {
        btnFiltroHoy   = findViewById(R.id.btnFiltroHoy);
        btnFiltroAyer  = findViewById(R.id.btnFiltroAyer);
        btnFiltroMes   = findViewById(R.id.btnFiltroMes);
        btnFiltroTodas = findViewById(R.id.btnFiltroTodas);
        spFiltroAsesor = findViewById(R.id.spFiltroAsesor);
        rvContratos    = findViewById(R.id.rvVisitas);

        listAdapter = new GenericListAdapter(listaContratos, new GenericListAdapter.OnItemListener() {
            @Override public void onEditar(GenericItem item, int pos)   { cargarEnFormulario(item); }
            @Override public void onEliminar(GenericItem item, int pos) { confirmarEliminacion(item, pos); }
        });

        if (rvContratos != null) {
            rvContratos.setLayoutManager(new LinearLayoutManager(this));
            rvContratos.setAdapter(listAdapter);
            rvContratos.setNestedScrollingEnabled(false);
        }

        if (btnFiltroHoy   != null) btnFiltroHoy.setOnClickListener(v   -> cargarContratos("hoy"));
        if (btnFiltroAyer  != null) btnFiltroAyer.setOnClickListener(v  -> cargarContratos("ayer"));
        if (btnFiltroMes   != null) btnFiltroMes.setOnClickListener(v   -> cargarContratos("mes"));
        if (btnFiltroTodas != null) btnFiltroTodas.setOnClickListener(v -> cargarContratos("todas"));

        if (spFiltroAsesor != null) {
            spFiltroAsesor.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> p, View v, int pos, long id) { cargarContratos("todas"); }
                @Override
                public void onNothingSelected(AdapterView<?> p) {}
            });
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ROL Y ASESORES
    // ─────────────────────────────────────────────────────────────────────────
    private void comprobarRol() {
        db.collection("usuarios").whereEqualTo("uid", userId).get()
                .addOnSuccessListener(q -> {
                    if (!q.isEmpty()) {
                        String rol = q.getDocuments().get(0).getString("rol");
                        esAdmin = "admin".equals(rol);
                    }
                    if (spFiltroAsesor != null)
                        spFiltroAsesor.setVisibility(esAdmin ? View.VISIBLE : View.GONE);
                    cargarContratos("hoy");
                })
                .addOnFailureListener(e -> cargarContratos("hoy"));
    }

    private void cargarAsesores() {
        db.collection("usuarios").get().addOnSuccessListener(q -> {
            mapaAsesores.clear();
            List<String> lista = new ArrayList<>();
            lista.add("Todos");
            for (QueryDocumentSnapshot doc : q) {
                String uid      = doc.getString("uid");
                String telefono = doc.getString("telefono");
                if (uid != null && telefono != null) {
                    mapaAsesores.put(uid, telefono);
                    lista.add(telefono);
                }
            }
            if (esAdmin && spFiltroAsesor != null) {
                ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                        android.R.layout.simple_spinner_item, lista);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spFiltroAsesor.setAdapter(adapter);
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GUARDAR / ACTUALIZAR
    // ─────────────────────────────────────────────────────────────────────────
    private void guardarOActualizar() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(this, "Usuario no autenticado", Toast.LENGTH_SHORT).show();
            return;
        }
        if (val(etC1Nombre).isEmpty()) {
            etC1Nombre.setError("Obligatorio");
            return;
        }
        if (docIdEditando == null && !signatureView.hasFirma()) {
            Toast.makeText(this, "Añade la firma del cliente antes de guardar", Toast.LENGTH_LONG).show();
            return;
        }

        Map<String, Object> data = construirData();

        if (docIdEditando != null) {
            db.collection("contratos").document(docIdEditando)
                    .update(data)
                    .addOnSuccessListener(u -> {
                        Toast.makeText(this, "✔ Contrato actualizado", Toast.LENGTH_SHORT).show();
                        limpiarFormulario();
                        cargarContratos("todas");
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "❌ " + e.getMessage(), Toast.LENGTH_LONG).show());
        } else {
            db.collection("contratos").add(data)
                    .addOnSuccessListener(ref -> {
                        File pdf = buildPDF(data);
                        if (pdf != null) {
                            compartirPDF(pdf);
                        }
                        Toast.makeText(this, "Contrato guardado ✓", Toast.LENGTH_SHORT).show();
                        limpiarFormulario();
                        cargarContratos("hoy");
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
        }
    }

    private Map<String, Object> construirData() {
        Map<String, Object> data = new HashMap<>();
        data.put("userId",    userId);
        data.put("timestamp", Timestamp.now());
        data.put("estado",    spEstado.getSelectedItem().toString());
        data.put("c1_nombre",    val(etC1Nombre));
        data.put("c1_nif",       val(etC1NIF));
        data.put("c1_domicilio", val(etC1Domicilio));
        data.put("c2_nombre",    val(etC2Nombre));
        data.put("c2_nif",       val(etC2NIF));
        data.put("c2_domicilio", val(etC2Domicilio));
        data.put("propio_nombre",   cbPropiaNombre.isChecked());
        data.put("rep_nombre",      val(etRepNombre));
        data.put("rep_domicilio",   val(etRepDomicilio));
        data.put("rep_nif",         val(etRepNIF));
        data.put("rep_calidad",     val(etRepCalidad));
        data.put("plazo",            val(etPlazo));
        data.put("v1_nombre", val(etV1Nombre)); data.put("v1_nif", val(etV1NIF));
        data.put("v2_nombre", val(etV2Nombre)); data.put("v2_nif", val(etV2NIF));
        data.put("v3_nombre", val(etV3Nombre)); data.put("v3_nif", val(etV3NIF));
        data.put("v4_nombre", val(etV4Nombre)); data.put("v4_nif", val(etV4NIF));
        data.put("direccion",     val(etDireccion));
        data.put("ref_catastral", val(etRefCatastral));
        data.put("otros",         val(etOtros));
        data.put("precio",           val(etPrecio));
        data.put("arras",            val(etArras));
        data.put("contrato_privado", val(etContratoPrivado));
        data.put("escritura",        val(etEscritura));
        data.put("cargas",           val(etCargas));
        data.put("fecha_contrato_privado", val(etFechaContratoPrivado));
        data.put("fecha_escritura",        val(etFechaEscritura));
        data.put("lugar",      val(etLugar));
        data.put("firma_dia",  val(etFirmaDia));
        data.put("firma_mes",  val(etFirmaMes));
        data.put("firma_anio", val(etFirmaAnio));
        data.put("clausula_10", val(etClausula10));
        return data;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CARGA CON FILTROS
    // ─────────────────────────────────────────────────────────────────────────
    private void cargarContratos(String filtro) {
        Query query = esAdmin
                ? db.collection("contratos")
                : db.collection("contratos").whereEqualTo("userId", userId);

        query.get().addOnSuccessListener(snapshots -> {
            listaContratos.clear();
            Calendar calInicio = Calendar.getInstance();
            Calendar calFin    = Calendar.getInstance();

            switch (filtro) {
                case "hoy":
                    calInicio.set(Calendar.HOUR_OF_DAY, 0);
                    calInicio.set(Calendar.MINUTE, 0);
                    calInicio.set(Calendar.SECOND, 0);
                    calFin.set(Calendar.HOUR_OF_DAY, 23);
                    calFin.set(Calendar.MINUTE, 59);
                    calFin.set(Calendar.SECOND, 59);
                    break;
                case "ayer":
                    calInicio.add(Calendar.DAY_OF_YEAR, -1);
                    calInicio.set(Calendar.HOUR_OF_DAY, 0);
                    calInicio.set(Calendar.MINUTE, 0);
                    calInicio.set(Calendar.SECOND, 0);
                    calFin.add(Calendar.DAY_OF_YEAR, -1);
                    calFin.set(Calendar.HOUR_OF_DAY, 23);
                    calFin.set(Calendar.MINUTE, 59);
                    calFin.set(Calendar.SECOND, 59);
                    break;
                case "mes":
                    calInicio.set(Calendar.DAY_OF_MONTH, 1);
                    calInicio.set(Calendar.HOUR_OF_DAY, 0);
                    calInicio.set(Calendar.MINUTE, 0);
                    calInicio.set(Calendar.SECOND, 0);
                    calFin.set(Calendar.DAY_OF_MONTH, calFin.getActualMaximum(Calendar.DAY_OF_MONTH));
                    calFin.set(Calendar.HOUR_OF_DAY, 23);
                    calFin.set(Calendar.MINUTE, 59);
                    calFin.set(Calendar.SECOND, 59);
                    break;
            }

            Date dInicio = calInicio.getTime();
            Date dFin    = calFin.getTime();

            for (QueryDocumentSnapshot doc : snapshots) {
                Date ts = doc.getDate("timestamp");
                if (ts == null) continue;

                boolean pasaFecha = "todas".equals(filtro) || (ts.after(dInicio) && ts.before(dFin));
                if (!pasaFecha) continue;

                String uidCreador    = doc.getString("userId");
                String asesorReal    = mapaAsesores.get(uidCreador);
                String asesorVisible = asesorReal != null ? asesorReal : "Sin asesor";
                String titulo        = doc.getString("c1_nombre");
                String direccion     = doc.getString("direccion");
                String estado        = doc.getString("estado");

                listaContratos.add(new GenericItem(
                        doc.getId(),
                        titulo   != null ? titulo   : "—",
                        direccion != null ? direccion : "",
                        estado   != null ? estado   : "",
                        uidCreador,
                        doc.getData(),
                        ts,
                        asesorVisible
                ));
            }

            if (esAdmin && spFiltroAsesor != null && spFiltroAsesor.getSelectedItem() != null) {
                String sel = spFiltroAsesor.getSelectedItem().toString();
                if (!sel.equals("Todos")) {
                    List<GenericItem> filtradas = new ArrayList<>();
                    for (GenericItem item : listaContratos)
                        if (sel.equalsIgnoreCase(item.asesor)) filtradas.add(item);
                    listaContratos.clear();
                    listaContratos.addAll(filtradas);
                }
            }

            Collections.sort(listaContratos, (a, b) -> {
                if (a.timestamp == null || b.timestamp == null) return 0;
                return b.timestamp.compareTo(a.timestamp);
            });

            listAdapter.notifyDataSetChanged();
            if (listaContratos.isEmpty())
                Toast.makeText(this, "Sin contratos para este filtro", Toast.LENGTH_SHORT).show();

        }).addOnFailureListener(e ->
                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CARGAR EN FORMULARIO
    // ─────────────────────────────────────────────────────────────────────────
    private void cargarEnFormulario(GenericItem item) {
        docIdEditando = item.docId;
        db.collection("contratos").document(item.docId).get()
                .addOnSuccessListener(doc -> {
                    etC1Nombre.setText(s(doc, "c1_nombre"));
                    etC1NIF.setText(s(doc, "c1_nif"));
                    etC1Domicilio.setText(s(doc, "c1_domicilio"));
                    etC2Nombre.setText(s(doc, "c2_nombre"));
                    etC2NIF.setText(s(doc, "c2_nif"));
                    etC2Domicilio.setText(s(doc, "c2_domicilio"));
                    Boolean propio = doc.getBoolean("propio_nombre");
                    cbPropiaNombre.setChecked(propio != null && propio);
                    etRepNombre.setText(s(doc, "rep_nombre"));
                    etRepDomicilio.setText(s(doc, "rep_domicilio"));
                    etRepNIF.setText(s(doc, "rep_nif"));
                    etRepCalidad.setText(s(doc, "rep_calidad"));
                    etPlazo.setText(s(doc, "plazo"));
                    etV1Nombre.setText(s(doc, "v1_nombre"));
                    etV1NIF.setText(s(doc, "v1_nif"));
                    etV2Nombre.setText(s(doc, "v2_nombre"));
                    etV2NIF.setText(s(doc, "v2_nif"));
                    etV3Nombre.setText(s(doc, "v3_nombre"));
                    etV3NIF.setText(s(doc, "v3_nif"));
                    etV4Nombre.setText(s(doc, "v4_nombre"));
                    etV4NIF.setText(s(doc, "v4_nif"));
                    etDireccion.setText(s(doc, "direccion"));
                    etRefCatastral.setText(s(doc, "ref_catastral"));
                    etOtros.setText(s(doc, "otros"));
                    etPrecio.setText(s(doc, "precio"));
                    etArras.setText(s(doc, "arras"));
                    etContratoPrivado.setText(s(doc, "contrato_privado"));
                    etEscritura.setText(s(doc, "escritura"));
                    etCargas.setText(s(doc, "cargas"));
                    etFechaContratoPrivado.setText(s(doc, "fecha_contrato_privado"));
                    etFechaEscritura.setText(s(doc, "fecha_escritura"));
                    etLugar.setText(s(doc, "lugar"));
                    etFirmaDia.setText(s(doc, "firma_dia"));
                    etFirmaMes.setText(s(doc, "firma_mes"));
                    etFirmaAnio.setText(s(doc, "firma_anio"));
                    etClausula10.setText(s(doc, "clausula_10"));

                    String estado = doc.getString("estado");
                    if (estado != null) {
                        ArrayAdapter<CharSequence> adapter = (ArrayAdapter<CharSequence>) spEstado.getAdapter();
                        int pos = adapter.getPosition(estado);
                        if (pos >= 0) spEstado.setSelection(pos);
                    }

                    btnGuardar.setText("💾 Actualizar contrato");
                    if (findViewById(R.id.scrollContrato) != null)
                        findViewById(R.id.scrollContrato).scrollTo(0, 0);
                    Toast.makeText(this, "Editando contrato de " + item.titulo, Toast.LENGTH_SHORT).show();
                });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ELIMINAR
    // ─────────────────────────────────────────────────────────────────────────
    private void confirmarEliminacion(GenericItem item, int pos) {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar contrato")
                .setMessage("¿Eliminar el contrato de " + item.titulo + "?\nEsta acción no se puede deshacer.")
                .setPositiveButton("🗑 Eliminar", (d, w) ->
                        db.collection("contratos").document(item.docId).delete()
                                .addOnSuccessListener(u -> {
                                    listAdapter.remove(pos);
                                    Toast.makeText(this, "Contrato eliminado", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(this, "Error al eliminar", Toast.LENGTH_SHORT).show()))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS FORMULARIO
    // ─────────────────────────────────────────────────────────────────────────
    private void limpiarFormulario() {
        docIdEditando = null;
        btnGuardar.setText("GUARDAR Y GENERAR PDF");
        etC1Nombre.setText("");
        etC1NIF.setText("");
        etC1Domicilio.setText("");
        etC2Nombre.setText("");
        etC2NIF.setText("");
        etC2Domicilio.setText("");
        cbPropiaNombre.setChecked(true);
        etRepNombre.setText("");
        etRepDomicilio.setText("");
        etRepNIF.setText("");
        etRepCalidad.setText("");
        etPlazo.setText("");
        etV1Nombre.setText("");
        etV1NIF.setText("");
        etV2Nombre.setText("");
        etV2NIF.setText("");
        etV3Nombre.setText("");
        etV3NIF.setText("");
        etV4Nombre.setText("");
        etV4NIF.setText("");
        etDireccion.setText("");
        etRefCatastral.setText("");
        etOtros.setText("");
        etPrecio.setText("");
        etArras.setText("");
        etContratoPrivado.setText("");
        etEscritura.setText("");
        etCargas.setText("");
        etFechaContratoPrivado.setText("");
        etFechaEscritura.setText("");
        etLugar.setText("");
        etFirmaDia.setText("");
        etFirmaMes.setText("");
        etFirmaAnio.setText("");
        etClausula10.setText("");
        spEstado.setSelection(0);
        signatureView.clear();
    }

    private String val(EditText et) {
        return et != null ? et.getText().toString().trim() : "";
    }

    private String s(com.google.firebase.firestore.DocumentSnapshot doc, String key) {
        Object v = doc.get(key);
        return v != null ? String.valueOf(v) : "";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PDF — canvas unificado en mCv / mY
    // ─────────────────────────────────────────────────────────────────────────
    private File buildPDF(Map<String, Object> raw) {
        // ── Carga de recursos gráficos igual que EncargoActivity ─────────────
        try {
            mLogo = BitmapFactory.decodeResource(getResources(), R.drawable.logo_casablanca);
        } catch (Exception e) {
            mLogo = null;
        }
        try {
            mSello = BitmapFactory.decodeResource(getResources(), R.drawable.sello_empresa);
        } catch (Exception e) {
            mSello = null;
        }

        mDoc = new PdfDocument();
        mPageNum = 1;
        abrirPagina();

        // ── Extraer datos ────────────────────────────────────────────────────
        final String c1n  = s2(raw,"c1_nombre");
        final String c1f  = s2(raw,"c1_nif");
        final String c1d  = s2(raw,"c1_domicilio");
        final String c2n  = s2(raw,"c2_nombre");
        final String c2f  = s2(raw,"c2_nif");
        final String c2d  = s2(raw,"c2_domicilio");
        final boolean propioNombre = Boolean.TRUE.equals(raw.get("propio_nombre"));
        final String repN  = s2(raw,"rep_nombre");
        final String repD  = s2(raw,"rep_domicilio");
        final String repF  = s2(raw,"rep_nif");
        final String repC  = s2(raw,"rep_calidad");
        final String plazo = s2(raw,"plazo");
        final String v1n   = s2(raw,"v1_nombre");
        final String v1f   = s2(raw,"v1_nif");
        final String v2n   = s2(raw,"v2_nombre");
        final String v2f   = s2(raw,"v2_nif");
        final String v3n   = s2(raw,"v3_nombre");
        final String v3f   = s2(raw,"v3_nif");
        final String v4n   = s2(raw,"v4_nombre");
        final String v4f   = s2(raw,"v4_nif");
        final String dir   = s2(raw,"direccion");
        final String cat   = s2(raw,"ref_catastral");
        final String otros = s2(raw,"otros");
        final String precio = s2(raw,"precio");
        final String arras  = s2(raw,"arras");
        final String cp     = s2(raw,"contrato_privado");
        final String esc    = s2(raw,"escritura");
        final String cargas = s2(raw,"cargas");
        final String fcp    = s2(raw,"fecha_contrato_privado");
        final String fesc   = s2(raw,"fecha_escritura");
        final String lugar  = s2(raw,"lugar");
        final String dia    = s2(raw,"firma_dia");
        final String mes    = s2(raw,"firma_mes");
        final String anio   = s2(raw,"firma_anio");
        final String cl10   = s2(raw,"clausula_10");

        // ── Cabecera ─────────────────────────────────────────────────────────
        cabecera();
        gap(10f);

        // Título
        Paint pTit = paint(10f, Color.BLACK, true, Paint.Align.LEFT);
        textLine("PROPUESTA DE CONTRATO DE COMPRAVENTA DE INMUEBLE", pTit, ML);
        gap(6f);

        // ── Cláusula 1 ───────────────────────────────────────────────────────
        Paint pB = paint(9f, Color.BLACK, false, Paint.Align.LEFT);
        parrafo("1) D./Dª " + fill(c1n,48) + " (C1), Mayor de edad, con domicilio en "
                + fill(c1d,38) + "  N.I.F. " + fill(c1f,18)
                + "  Y D./Dª " + fill(c2n,48) + " (C2), mayor de edad, con domicilio en "
                + fill(c2d,38) + "  N.I.F. " + fill(c2f,18), pB);
        gap(6f);

        textLine("Actuando:", paint(9f, Color.BLACK, true, Paint.Align.LEFT), ML);
        String mP = propioNombre ? "✓" : " ";
        String mR = !propioNombre ? "✓" : " ";
        parrafo("( " + mP + " )  en su propio nombre y representación (en adelante, el Proponente o comprador)", pB);
        gap(4f);
        parrafo("( " + mR + " )  en nombre y representación de " + fill(repN,40)
                + " (en adelante, el proponente o comprador)  con domicilio en " + fill(repD,35)
                + " provisto de N.I.F./C.I.F. " + fill(repF,18) + "  en calidad de "
                + fill(repC,25) + ", Según acredita documentalmente.", pB);
        gap(6f);

        parrafo("SE COMPROMETE, durante un plazo de " + fill(plazo,8)
                + " días atendiendo a las condiciones y plazos establecidos en el presente"
                + " documento, a comprar como cuerpo cierto y en el estado de conservación"
                + " en el que se encuentra, el inmueble propiedad", pB);
        gap(6f);

        filaVendedor(v1n, v1f);
        filaVendedor(v2n, v2f);
        filaVendedor(v3n, v3f);
        filaVendedor(v4n, v4f);

        parrafo("en adelante, el vendedor de la vivienda identificándose el mismo como sigue:", pB);
        gap(3f);
        etiquetaValor("Dirección",            fill(dir,65));
        etiquetaValor("Referencia catastral", fill(cat,58));
        etiquetaValor("Otros",                fill(otros,65));
        gap(8f);

        // ── Cláusula 2 ───────────────────────────────────────────────────────
        parrafo("2) El precio propuesto para la compra del inmueble queda fijada en "
                + fill(precio,22) + " Euros.", pB);
        gap(3f);
        parrafo("El pago se hará del siguiente modo: " + fill(arras,18)
                + " Euros, a la firma del presente documento por el Proponente, como prueba de"
                + " su voluntad de suscribir un contrato de compraventa de dicho inmueble."
                + " En el supuesto de que la presente propuesta fuese conforme con el Encargo de"
                + " Venta suscrito por el Vendedor, dicha cantidad, a cuenta del precio del"
                + " inmueble, constituirá arras o señal según lo establecido en el art. 1454 del"
                + " Código Civil.", pB);
        gap(6f);

        // ── Cláusula 3 ───────────────────────────────────────────────────────
        parrafo("3) El inmueble en objeto será entregado libre de cargas, gravámenes, vicios,"
                + " evicciones a excepción de " + fill(cargas,22) + ".", pB);
        gap(6f);

        // ── Cláusula 4 ───────────────────────────────────────────────────────
        parrafo("4) El proponente se compromete a suscribir el contrato privado de compraventa"
                + " antes del día " + fill(fcp,10)
                + " y la escritura pública de compraventa antes del día " + fill(fesc,10) + ".", pB);
        gap(6f);

        // ── Cláusulas 5–9 ────────────────────────────────────────────────────
        parrafo("5) En el supuesto de que la presente propuesta fuese conforme con el encargo"
                + " suscrito por el vendedor será vinculante por ambas partes una vez firmado por"
                + " el comprador.", pB);
        gap(5f);

        parrafo("6) La Falta de suscripción del contrato de compraventa en los plazos y condiciones"
                + " establecidos comportará la pérdida de las sumas abonadas por él en concepto de arras"
                + " en favor del Vendedor.", pB);
        gap(5f);

        parrafo("7) Tanto el Comprador como el Vendedor declaran conocer y aceptar que Soluciones"
                + " Villasan S.L. por su actividad de mediación reciba los honorarios convenidos.", pB);
        gap(5f);

        parrafo("8) El Proponente declara conocer y aceptar la situación urbanística, el estado del"
                + " inmueble y su calificación energética.", pB);
        gap(5f);

        parrafo("9) Todos los gastos van por cuenta compradora excepto plusvalía municipal y"
                + " cancelación hipotecaria que van por cuenta vendedora.", pB);
        gap(8f);

        // ── Cláusula 10 (opcional) ────────────────────────────────────────────
        if (!cl10.isEmpty()) {
            parrafo("10) " + cl10, pB);
            gap(6f);
        }

        // ── Bloque de firmas ──────────────────────────────────────────────────
        checkPage(130f);

        Paint pTitFirma = paint(10f, Color.BLACK, true, Paint.Align.LEFT);
        textLine("Firma de las partes", pTitFirma, ML);
        gap(6f);

        String fechaTxt = "En " + fill(lugar,18) + ", a " + fill(dia,4)
                + " de " + fill(mes,8) + " de 20"
                + (anio.length() >= 2 ? anio.substring(anio.length()-2) : "..");
        textLine(fechaTxt, paint(9f, Color.BLACK, false, Paint.Align.LEFT), ML);
        gap(14f);

        bloquesFirmas(c1n, v1n);

        // Cerrar última página
        mDoc.finishPage(mPage);

        File file = new File(getExternalFilesDir(null),
                "contrato_" + System.currentTimeMillis() + ".pdf");
        try (FileOutputStream fos = new FileOutputStream(file)) {
            mDoc.writeTo(fos);
            mDoc.close();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return file;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BLOQUE DE FIRMAS
    // ─────────────────────────────────────────────────────────────────────────
    private void bloquesFirmas(String c1Nombre, String v1Nombre) {
        checkPage(150f);

        Paint pFL  = paint(TS_BODY, Color.BLACK, false, Paint.Align.LEFT);
        float colW = TW / 2f - 6f;
        float col2X = ML + TW / 2f + 5f;

        mCv.drawText("Por el Comprador", ML,    mY + TS_BODY, pFL);
        mCv.drawText("Por el Vendedor",  col2X, mY + TS_BODY, pFL);
        mY += TS_BODY * 1.8f;

        float boxH = 70f;
        Paint boxP = new Paint();
        boxP.setColor(Color.parseColor("#AAAAAA"));
        boxP.setStyle(Paint.Style.STROKE);
        boxP.setStrokeWidth(0.7f);

        // Columna izquierda: sello empresa
        if (mSello != null) {
            float ratio = (float) mSello.getWidth() / mSello.getHeight();
            float sW = colW * 0.9f;
            float sH = sW / ratio;
            if (sH > boxH * 0.8f) {
                sH = boxH * 0.8f;
                sW = sH * ratio;
            }
            float sX = ML + (colW - sW) / 2f;
            float sY = mY + (boxH - sH) / 2f;
            mCv.drawBitmap(mSello, null,
                    new RectF(sX, sY, sX + sW, sY + sH),
                    new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG));
        } else {
            mCv.drawRect(ML, mY, ML + colW, mY + boxH, boxP);
        }

        // Columna derecha: firma cliente
        if (signatureView != null && signatureView.hasFirma()) {
            Bitmap sig = signatureView.toBitmap();
            if (sig != null && sig.getWidth() > 0) {
                mCv.drawBitmap(sig, null,
                        new RectF(col2X, mY, col2X + colW, mY + boxH),
                        new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG));
            }
        } else {
            mCv.drawRect(col2X, mY, col2X + colW, mY + boxH, boxP);
        }

        mY += boxH + 8f;

        mCv.drawText("Soluciones Villasan S.L.",          ML,    mY + TS_BODY, pFL);
        mCv.drawText("Nombre: " + c1Nombre,               col2X, mY + TS_BODY, pFL);
        mY += TS_BODY * 1.8f;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS PDF
    // ─────────────────────────────────────────────────────────────────────────

    private void abrirPagina() {
        PdfDocument.PageInfo info = new PdfDocument.PageInfo.Builder((int)PW, (int)PH, mPageNum++).create();
        mPage = mDoc.startPage(info);
        mCv   = mPage.getCanvas();
        mY    = 36f;
        if (mPageNum > 2) {
            cabecera();
            gap(4f);
        }
    }

    private void checkPage(float needed) {
        if (mY + needed > PH - MB) {
            mDoc.finishPage(mPage);
            abrirPagina();
        }
    }

    private void cabecera() {
        // ── Logo cargado desde drawable, igual que EncargoActivity ───────────
        float logoW = 77f;
        float logoH = 58f;
        if (mLogo != null) {
            mCv.drawBitmap(mLogo, null, new RectF(ML, mY, ML + logoW, mY + logoH), null);
        }
        float hx = ML + logoW + 8f;
        Paint b  = paint(10f, Color.BLACK, true,  Paint.Align.LEFT);
        Paint n  = paint(9f,  Color.BLACK, false, Paint.Align.LEFT);
        Paint lk = paint(9f,  Color.parseColor("#1155CC"), false, Paint.Align.LEFT);
        mCv.drawText("SOLUCIONES VILLASAN S.L.        N.I.F.  B75484741", hx, mY + 6f,  b);
        mCv.drawText("Avenida de Beleña Nº16 local 1 - Guadalajara - Tlf.: 949410344",  hx, mY + 18f, n);
        mCv.drawText("casablancaguasvivas@gmail.com   www.casablancainmobiliarias.com",  hx, mY + 30f, lk);
        mY += logoH + 4f;
        Paint sep = new Paint();
        sep.setColor(Color.parseColor("#CCCCCC"));
        sep.setStrokeWidth(0.5f);
        mCv.drawLine(ML, mY, ML + TW, mY, sep);
        mY += 8f;
    }

    private void textLine(String txt, Paint p, float x) {
        float h = p.getTextSize() * 1.3f;
        checkPage(h);
        mCv.drawText(txt, x, mY + p.getTextSize(), p);
        mY += h;
    }

    private void parrafo(String texto, Paint p) {
        if (texto == null || texto.isEmpty()) return;
        float lineH = p.getTextSize() * 1.4f;
        for (String l : wrapText(texto, p, TW)) {
            checkPage(lineH);
            mCv.drawText(l, ML, mY + p.getTextSize(), p);
            mY += lineH;
        }
    }

    private void gap(float pts) {
        mY += pts;
    }

    private List<String> wrapText(String texto, Paint p, float maxW) {
        List<String> r = new ArrayList<>();
        if (texto == null || texto.isEmpty()) return r;
        String[] words = texto.split(" ", -1);
        StringBuilder cur = new StringBuilder();
        for (String w : words) {
            String test = cur.length() == 0 ? w : cur + " " + w;
            if (p.measureText(test) <= maxW) {
                cur = new StringBuilder(test);
            } else {
                if (cur.length() > 0) r.add(cur.toString());
                cur = new StringBuilder(w);
            }
        }
        if (cur.length() > 0) r.add(cur.toString());
        return r;
    }

    private void filaVendedor(String nombre, String nif) {
        Paint p = paint(9f, Color.BLACK, false, Paint.Align.LEFT);
        float h = p.getTextSize() * 1.4f;
        checkPage(h);
        mCv.drawText("de D./Dª " + fill(nombre,44) + " provisto de N.I.F./C.I.F. " + fill(nif,22), ML, mY + p.getTextSize(), p);
        mY += h;
    }

    private void etiquetaValor(String lbl, String valor) {
        Paint pb = paint(9f, Color.BLACK, true,  Paint.Align.LEFT);
        Paint pn = paint(9f, Color.BLACK, false, Paint.Align.LEFT);
        float h = pb.getTextSize() * 1.4f;
        checkPage(h);
        float lw = pb.measureText(lbl + ": ");
        mCv.drawText(lbl + ": ", ML,      mY + pb.getTextSize(), pb);
        mCv.drawText(valor,       ML + lw, mY + pn.getTextSize(), pn);
        mY += h;
    }

    private String fill(String val, int dots) {
        if (val != null && !val.isEmpty()) return val;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < dots; i++) sb.append('…');
        return sb.toString();
    }

    private Paint paint(float size, int color, boolean bold, Paint.Align align) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setTextSize(size);
        p.setColor(color);
        p.setFakeBoldText(bold);
        p.setTextAlign(align);
        p.setTypeface(Typeface.create(Typeface.SERIF, bold ? Typeface.BOLD : Typeface.NORMAL));
        return p;
    }

    private String s2(Map<String, Object> m, String k) {
        Object v = m.get(k);
        return v != null ? String.valueOf(v) : "";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // COMPARTIR PDF
    // ─────────────────────────────────────────────────────────────────────────
    private void compartirPDF(File file) {
        Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".provider", file);
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("application/pdf");
        intent.putExtra(Intent.EXTRA_SUBJECT, "Propuesta de Contrato de Compraventa");
        intent.putExtra(Intent.EXTRA_TEXT,    "Adjunto propuesta firmada de compraventa de inmueble.");
        intent.putExtra(Intent.EXTRA_STREAM,  uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(intent, "Compartir PDF"));
    }
}