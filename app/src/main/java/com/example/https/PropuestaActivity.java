package com.example.https;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
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

public class PropuestaActivity extends AppCompatActivity {

    // ── Medidas A4 ────────────────────────────────────────────
    private static final float PW = 595f, PH = 842f;
    private static final float ML = 36f, MR = 36f;
    private static final float TW = PW - ML - MR;
    private static final float MB = 40f;
    private static final float LOGO_W = 77f, LOGO_H = 58f;
    private static final float TS_CAB = 12f, TS_WEB = 9f, TS_BODY = 11.5f, TS_TIT = 14f;
    private static final int C_BLACK = Color.BLACK;
    private static final int C_BLUE  = Color.parseColor("#0000FF");

    // ── Estado PDF ────────────────────────────────────────────
    private PdfDocument      mDoc;
    private PdfDocument.Page mPage;
    private Canvas           mCv;
    private int              mPageNum;
    private float            mY;
    private Bitmap           mLogo;
    private Bitmap           mSello;

    // ── Formulario ────────────────────────────────────────────────────────────
    EditText etC1Nombre, etC1Domicilio, etC1Telf, etC1Email, etC1NIF;
    CheckBox cbSegundoComprador;
    LinearLayout layoutC2;
    EditText etC2Nombre, etC2Domicilio, etC2Telf, etC2Email, etC2NIF;
    RadioGroup  rgActuando;
    RadioButton rbPropio, rbRepresentacion;
    LinearLayout layoutRepresentacion;
    EditText etRepNombre, etRepDomicilio, etRepCalle, etRepNIF, etRepCalidad;
    EditText etFechaPropuesta, etInmueble, etHonorarios;
    EditText etLugar, etFirmaDia, etFirmaMes, etFirmaAnio;
    SignatureView signatureViewC1, signatureViewC2;
    Button btnBorrarFirmaC1, btnBorrarFirmaC2, btnGuardar, btnGuardarYPDF;

    // ── Lista ─────────────────────────────────────────────────────────────────
    Button btnFiltroHoy, btnFiltroAyer, btnFiltroMes, btnFiltroTodas;
    Spinner spFiltroAsesor;
    RecyclerView rvPropuestas;
    GenericListAdapter listAdapter;
    List<GenericItem> listaPropuestas = new ArrayList<>();

    // ── Firebase ──────────────────────────────────────────────────────────────
    FirebaseFirestore db;
    String userId;
    boolean esAdmin = false;

    // ── Edición ───────────────────────────────────────────────────────────────
    String docIdEditando = null;
    Map<String, String> mapaAsesores = new HashMap<>();

    // ─────────────────────────────────────────────────────────────────────────
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_propuesta);
        db     = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        cargarAsesores();
        bindFormulario();
        bindLista();
        comprobarRol();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // BIND
    // ═══════════════════════════════════════════════════════════════════════════

    private void bindFormulario() {
        etC1Nombre         = findViewById(R.id.etC1Nombre);
        etC1Domicilio      = findViewById(R.id.etC1Domicilio);
        etC1Telf           = findViewById(R.id.etC1Telf);
        etC1Email          = findViewById(R.id.etC1Email);
        etC1NIF            = findViewById(R.id.etC1NIF);
        cbSegundoComprador = findViewById(R.id.cbSegundoComprador);
        layoutC2           = findViewById(R.id.layoutC2);
        etC2Nombre         = findViewById(R.id.etC2Nombre);
        etC2Domicilio      = findViewById(R.id.etC2Domicilio);
        etC2Telf           = findViewById(R.id.etC2Telf);
        etC2Email          = findViewById(R.id.etC2Email);
        etC2NIF            = findViewById(R.id.etC2NIF);

        cbSegundoComprador.setOnCheckedChangeListener((btn, checked) ->
                layoutC2.setVisibility(checked ? View.VISIBLE : View.GONE));

        rgActuando           = findViewById(R.id.rgActuando);
        rbPropio             = findViewById(R.id.rbPropio);
        rbRepresentacion     = findViewById(R.id.rbRepresentacion);
        layoutRepresentacion = findViewById(R.id.layoutRepresentacion);
        etRepNombre          = findViewById(R.id.etRepNombre);
        etRepDomicilio       = findViewById(R.id.etRepDomicilio);
        etRepCalle           = findViewById(R.id.etRepCalle);
        etRepNIF             = findViewById(R.id.etRepNIF);
        etRepCalidad         = findViewById(R.id.etRepCalidad);

        rgActuando.setOnCheckedChangeListener((group, id) ->
                layoutRepresentacion.setVisibility(
                        id == R.id.rbRepresentacion ? View.VISIBLE : View.GONE));

        etFechaPropuesta = findViewById(R.id.etFechaPropuesta);
        etInmueble       = findViewById(R.id.etInmueble);
        etHonorarios     = findViewById(R.id.etHonorarios);
        etLugar          = findViewById(R.id.etLugar);
        etFirmaDia       = findViewById(R.id.etFirmaDia);
        etFirmaMes       = findViewById(R.id.etFirmaMes);
        etFirmaAnio      = findViewById(R.id.etFirmaAnio);
        signatureViewC1  = findViewById(R.id.signatureViewC1);
        signatureViewC2  = findViewById(R.id.signatureViewC2);
        btnBorrarFirmaC1 = findViewById(R.id.btnBorrarFirmaC1);
        btnBorrarFirmaC2 = findViewById(R.id.btnBorrarFirmaC2);
        btnGuardar       = findViewById(R.id.btnGuardar);
        btnGuardarYPDF   = findViewById(R.id.btnGuardarYPDF);

        btnBorrarFirmaC1.setOnClickListener(v -> signatureViewC1.clear());
        btnBorrarFirmaC2.setOnClickListener(v -> signatureViewC2.clear());
        btnGuardar.setOnClickListener(v -> guardar(false));
        btnGuardarYPDF.setOnClickListener(v -> guardar(true));
    }

    private void bindLista() {
        btnFiltroHoy   = findViewById(R.id.btnFiltroHoy);
        btnFiltroAyer  = findViewById(R.id.btnFiltroAyer);
        btnFiltroMes   = findViewById(R.id.btnFiltroMes);
        btnFiltroTodas = findViewById(R.id.btnFiltroTodas);
        spFiltroAsesor = findViewById(R.id.spFiltroAsesor);
        rvPropuestas   = findViewById(R.id.rvPropuestas);

        listAdapter = new GenericListAdapter(listaPropuestas, new GenericListAdapter.OnItemListener() {
            @Override public void onEditar(GenericItem item, int position)   { cargarEnFormulario(item); }
            @Override public void onEliminar(GenericItem item, int position) { confirmarEliminacion(item, position); }
        });
        rvPropuestas.setLayoutManager(new LinearLayoutManager(this));
        rvPropuestas.setAdapter(listAdapter);
        rvPropuestas.setNestedScrollingEnabled(false);

        btnFiltroHoy.setOnClickListener(v   -> cargarPropuestas("hoy"));
        btnFiltroAyer.setOnClickListener(v  -> cargarPropuestas("ayer"));
        btnFiltroMes.setOnClickListener(v   -> cargarPropuestas("mes"));
        btnFiltroTodas.setOnClickListener(v -> cargarPropuestas("todas"));
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ROL
    // ═══════════════════════════════════════════════════════════════════════════

    private void comprobarRol() {
        db.collection("usuarios").whereEqualTo("uid", userId).get()
                .addOnSuccessListener(query -> {
                    if (!query.isEmpty()) {
                        String rol = query.getDocuments().get(0).getString("rol");
                        esAdmin = "admin".equals(rol);
                    }
                    spFiltroAsesor.setVisibility(esAdmin ? View.VISIBLE : View.GONE);
                    cargarPropuestas("hoy");
                });
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // GUARDAR / ACTUALIZAR
    // ═══════════════════════════════════════════════════════════════════════════

    private void guardar(boolean generarPDF) {
        if (v(etC1Nombre).isEmpty()) {
            etC1Nombre.setError("Obligatorio"); etC1Nombre.requestFocus(); return;
        }
        if (v(etInmueble).isEmpty()) {
            etInmueble.setError("Obligatorio"); etInmueble.requestFocus(); return;
        }
        if (docIdEditando == null && !signatureViewC1.hasFirma()) {
            Toast.makeText(this, "Añade la firma del Comprador (C1) antes de guardar", Toast.LENGTH_LONG).show();
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("userId",    userId);
        data.put("timestamp", Timestamp.now());
        data.put("c1_nombre",    v(etC1Nombre));
        data.put("c1_domicilio", v(etC1Domicilio));
        data.put("c1_telf",      v(etC1Telf));
        data.put("c1_email",     v(etC1Email));
        data.put("c1_nif",       v(etC1NIF));

        boolean tieneC2 = cbSegundoComprador.isChecked();
        data.put("tiene_c2", tieneC2);
        if (tieneC2) {
            data.put("c2_nombre",    v(etC2Nombre));
            data.put("c2_domicilio", v(etC2Domicilio));
            data.put("c2_telf",      v(etC2Telf));
            data.put("c2_email",     v(etC2Email));
            data.put("c2_nif",       v(etC2NIF));
        }

        boolean enRep = rbRepresentacion.isChecked();
        data.put("en_representacion", enRep);
        if (enRep) {
            data.put("rep_nombre",    v(etRepNombre));
            data.put("rep_domicilio", v(etRepDomicilio));
            data.put("rep_calle",     v(etRepCalle));
            data.put("rep_nif",       v(etRepNIF));
            data.put("rep_calidad",   v(etRepCalidad));
        }

        data.put("fecha_propuesta", v(etFechaPropuesta));
        data.put("inmueble",        v(etInmueble));
        data.put("honorarios",      v(etHonorarios));
        data.put("lugar",           v(etLugar));
        data.put("firma_dia",       v(etFirmaDia));
        data.put("firma_mes",       v(etFirmaMes));
        data.put("firma_anio",      v(etFirmaAnio));

        if (docIdEditando != null) {
            db.collection("honorarios_comprador").document(docIdEditando)
                    .update(data)
                    .addOnSuccessListener(unused -> {
                        if (generarPDF) compartirPDF(generarPDF(data));
                        Toast.makeText(this, "✔ Propuesta actualizada", Toast.LENGTH_SHORT).show();
                        limpiarFormulario();
                        cargarPropuestas("todas");
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "❌ " + e.getMessage(), Toast.LENGTH_LONG).show());
        } else {
            db.collection("honorarios_comprador").add(data)
                    .addOnSuccessListener(ref -> {
                        if (generarPDF) compartirPDF(generarPDF(data));
                        Toast.makeText(this, "✅ Declaración guardada", Toast.LENGTH_SHORT).show();
                        limpiarFormulario();
                        cargarPropuestas("hoy");
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "❌ " + e.getMessage(), Toast.LENGTH_LONG).show());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // CARGA LISTA CON FILTROS
    // ═══════════════════════════════════════════════════════════════════════════

    private void cargarPropuestas(String filtro) {
        Query query = esAdmin
                ? db.collection("honorarios_comprador")
                : db.collection("honorarios_comprador").whereEqualTo("userId", userId);

        query.get().addOnSuccessListener(snapshots -> {
            listaPropuestas.clear();

            Calendar calInicio = Calendar.getInstance();
            Calendar calFin    = Calendar.getInstance();

            switch (filtro) {
                case "hoy":
                    calInicio.set(Calendar.HOUR_OF_DAY, 0);  calInicio.set(Calendar.MINUTE, 0);  calInicio.set(Calendar.SECOND, 0);
                    calFin.set(Calendar.HOUR_OF_DAY, 23);    calFin.set(Calendar.MINUTE, 59);    calFin.set(Calendar.SECOND, 59);
                    break;
                case "ayer":
                    calInicio.add(Calendar.DAY_OF_YEAR, -1);
                    calInicio.set(Calendar.HOUR_OF_DAY, 0);  calInicio.set(Calendar.MINUTE, 0);  calInicio.set(Calendar.SECOND, 0);
                    calFin.add(Calendar.DAY_OF_YEAR, -1);
                    calFin.set(Calendar.HOUR_OF_DAY, 23);    calFin.set(Calendar.MINUTE, 59);    calFin.set(Calendar.SECOND, 59);
                    break;
                case "mes":
                    calInicio.set(Calendar.DAY_OF_MONTH, 1);
                    calInicio.set(Calendar.HOUR_OF_DAY, 0);  calInicio.set(Calendar.MINUTE, 0);  calInicio.set(Calendar.SECOND, 0);
                    calFin.set(Calendar.DAY_OF_MONTH, calFin.getActualMaximum(Calendar.DAY_OF_MONTH));
                    calFin.set(Calendar.HOUR_OF_DAY, 23);    calFin.set(Calendar.MINUTE, 59);    calFin.set(Calendar.SECOND, 59);
                    break;
            }

            Date dInicio = calInicio.getTime();
            Date dFin    = calFin.getTime();

            for (QueryDocumentSnapshot doc : snapshots) {
                Date ts = doc.getDate("timestamp");
                if (ts == null) continue;

                boolean pasa = "todas".equals(filtro) || (ts.after(dInicio) && ts.before(dFin));
                if (!pasa) continue;

                String uidCreador    = doc.getString("userId");
                String asesorReal    = mapaAsesores.get(uidCreador);
                String asesorVisible = asesorReal != null ? asesorReal : "Sin asesor";

                String cliente  = doc.getString("c1_nombre");
                String inmueble = doc.getString("inmueble");
                String titulo    = cliente  != null ? cliente  : "—";
                String subtitulo = (inmueble != null ? inmueble : "") + " · " + asesorVisible;

                GenericItem item = new GenericItem(
                        doc.getId(), titulo, subtitulo,
                        doc.getString("honorarios"),
                        uidCreador, doc.getData(), ts, asesorVisible);
                listaPropuestas.add(item);
            }

            if (esAdmin && spFiltroAsesor.getSelectedItem() != null) {
                String sel = spFiltroAsesor.getSelectedItem().toString();
                if (!sel.equals("Todos")) {
                    List<GenericItem> filtradas = new ArrayList<>();
                    for (GenericItem vv : listaPropuestas)
                        if (sel.equalsIgnoreCase(vv.asesor)) filtradas.add(vv);
                    listaPropuestas.clear();
                    listaPropuestas.addAll(filtradas);
                }
            }

            Collections.sort(listaPropuestas, (a, b) -> {
                if (a.timestamp == null || b.timestamp == null) return 0;
                return b.timestamp.compareTo(a.timestamp);
            });

            listAdapter.notifyDataSetChanged();
            if (listaPropuestas.isEmpty())
                Toast.makeText(this, "Sin propuestas para este filtro", Toast.LENGTH_SHORT).show();

        }).addOnFailureListener(e ->
                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // CARGAR EN FORMULARIO (edición)
    // ═══════════════════════════════════════════════════════════════════════════

    private void cargarEnFormulario(GenericItem item) {
        docIdEditando = item.docId;
        db.collection("honorarios_comprador").document(item.docId).get()
                .addOnSuccessListener(doc -> {
                    etC1Nombre.setText(s(doc, "c1_nombre"));
                    etC1Domicilio.setText(s(doc, "c1_domicilio"));
                    etC1Telf.setText(s(doc, "c1_telf"));
                    etC1Email.setText(s(doc, "c1_email"));
                    etC1NIF.setText(s(doc, "c1_nif"));

                    Boolean tieneC2 = doc.getBoolean("tiene_c2");
                    cbSegundoComprador.setChecked(tieneC2 != null && tieneC2);
                    if (tieneC2 != null && tieneC2) {
                        etC2Nombre.setText(s(doc, "c2_nombre"));
                        etC2Domicilio.setText(s(doc, "c2_domicilio"));
                        etC2Telf.setText(s(doc, "c2_telf"));
                        etC2Email.setText(s(doc, "c2_email"));
                        etC2NIF.setText(s(doc, "c2_nif"));
                    }

                    Boolean enRep = doc.getBoolean("en_representacion");
                    if (enRep != null && enRep) {
                        rbRepresentacion.setChecked(true);
                        etRepNombre.setText(s(doc, "rep_nombre"));
                        etRepDomicilio.setText(s(doc, "rep_domicilio"));
                        etRepCalle.setText(s(doc, "rep_calle"));
                        etRepNIF.setText(s(doc, "rep_nif"));
                        etRepCalidad.setText(s(doc, "rep_calidad"));
                    } else {
                        rbPropio.setChecked(true);
                    }

                    etFechaPropuesta.setText(s(doc, "fecha_propuesta"));
                    etInmueble.setText(s(doc, "inmueble"));
                    etHonorarios.setText(s(doc, "honorarios"));
                    etLugar.setText(s(doc, "lugar"));
                    etFirmaDia.setText(s(doc, "firma_dia"));
                    etFirmaMes.setText(s(doc, "firma_mes"));
                    etFirmaAnio.setText(s(doc, "firma_anio"));

                    btnGuardar.setText("💾 Actualizar propuesta");
                    btnGuardarYPDF.setText("💾 Actualizar y PDF");

                    try { findViewById(R.id.scrollPropuesta).scrollTo(0, 0); } catch (Exception ignored) {}
                    Toast.makeText(this, "Editando propuesta de " + item.titulo, Toast.LENGTH_SHORT).show();
                });
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ELIMINAR
    // ═══════════════════════════════════════════════════════════════════════════

    private void confirmarEliminacion(GenericItem item, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar propuesta")
                .setMessage("¿Eliminar la propuesta de " + item.titulo + "?\nEsta acción no se puede deshacer.")
                .setPositiveButton("🗑 Eliminar", (d, w) -> {
                    db.collection("honorarios_comprador").document(item.docId)
                            .delete()
                            .addOnSuccessListener(unused -> {
                                listAdapter.remove(position);
                                Toast.makeText(this, "Propuesta eliminada", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Error al eliminar", Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // ASESORES
    // ═══════════════════════════════════════════════════════════════════════════

    private void cargarAsesores() {
        db.collection("usuarios").get().addOnSuccessListener(query -> {
            mapaAsesores.clear();
            for (QueryDocumentSnapshot doc : query) {
                String uid      = doc.getString("uid");
                String telefono = doc.getString("telefono");
                if (uid != null && telefono != null) mapaAsesores.put(uid, telefono);
            }
            if (esAdmin) {
                List<String> lista = new ArrayList<>();
                lista.add("Todos");
                lista.addAll(mapaAsesores.values());
                ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                        android.R.layout.simple_spinner_item, lista);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spFiltroAsesor.setAdapter(adapter);
            }
        });
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // HELPERS FORMULARIO
    // ═══════════════════════════════════════════════════════════════════════════

    private void limpiarFormulario() {
        docIdEditando = null;
        btnGuardar.setText("GUARDAR PROPUESTA");
        btnGuardarYPDF.setText("GUARDAR Y GENERAR PDF");
        etC1Nombre.setText(""); etC1Domicilio.setText(""); etC1Telf.setText("");
        etC1Email.setText(""); etC1NIF.setText("");
        cbSegundoComprador.setChecked(false);
        etC2Nombre.setText(""); etC2Domicilio.setText(""); etC2Telf.setText("");
        etC2Email.setText(""); etC2NIF.setText("");
        rbPropio.setChecked(true);
        etRepNombre.setText(""); etRepDomicilio.setText(""); etRepCalle.setText("");
        etRepNIF.setText(""); etRepCalidad.setText("");
        etFechaPropuesta.setText(""); etInmueble.setText(""); etHonorarios.setText("");
        etLugar.setText(""); etFirmaDia.setText(""); etFirmaMes.setText(""); etFirmaAnio.setText("");
        signatureViewC1.clear();
        signatureViewC2.clear();
    }

    private String v(EditText et) { return et != null ? et.getText().toString().trim() : ""; }

    private String s(com.google.firebase.firestore.DocumentSnapshot doc, String key) {
        Object val = doc.get(key); return val != null ? String.valueOf(val) : "";
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PDF — CABECERA + CONTENIDO (MULTIPÁGINA)
    // ═══════════════════════════════════════════════════════════════════════════

    private File generarPDF(Map<String, Object> raw) {
        try { mLogo  = BitmapFactory.decodeResource(getResources(), R.drawable.logo_casablanca); } catch (Exception e) { mLogo = null; }
        try { mSello = BitmapFactory.decodeResource(getResources(), R.drawable.sello_empresa);  } catch (Exception e) { mSello = null; }

        mDoc = new PdfDocument();
        mPageNum = 1;
        abrirPagina();
        cabecera();
        gap(8f);

        Paint pTit = paint(TS_TIT, C_BLACK, true, Paint.Align.CENTER);
        mCv.drawText("DECLARACION DE HONORARIOS - COMPRAVENTA", PW / 2f, mY, pTit);
        float tw = pTit.measureText("DECLARACION DE HONORARIOS - COMPRAVENTA");
        Paint ul = new Paint(); ul.setColor(C_BLACK); ul.setStrokeWidth(0.8f);
        mCv.drawLine((PW - tw) / 2f, mY + 2f, (PW + tw) / 2f, mY + 2f, ul);
        gap(12f);

        Paint pB    = paint(TS_BODY, C_BLACK, false, Paint.Align.LEFT);
        Paint pBold = paint(TS_BODY, C_BLACK, true,  Paint.Align.LEFT);

        String c1 = s2(raw, "c1_nombre");
        String c2 = s2(raw, "tiene_c2").equals("true") ? s2(raw, "c2_nombre") : "";

        // ── Datos C1 ──────────────────────────────────────────────────────────
        parrafo("De una parte, D./Dña. " + fill(c1, "___________________________________________")
                + " (C1), mayor de edad, con domicilio en "
                + fill(s2(raw, "c1_domicilio"), "___________________________________________") + ",", pB);
        gap(2f);
        parrafo("telf. " + fill(s2(raw, "c1_telf"), "_________________")
                + ", e-mail " + fill(s2(raw, "c1_email"), "___________________________________")
                + ", N.I.F: " + fill(s2(raw, "c1_nif"), "_______________"), pB);
        gap(5f);

        // ── Datos C2 (si tiene) ───────────────────────────────────────────────
        if (!c2.isEmpty()) {
            parrafo("y D./Dña. " + fill(c2, "___________________________________________")
                    + " (C2), mayor de edad, con domicilio en "
                    + fill(s2(raw, "c2_domicilio"), "___________________________________________")
                    + ", telf.: " + fill(s2(raw, "c2_telf"), "_________________")
                    + ", e-mail " + fill(s2(raw, "c2_email"), "__________________________")
                    + ", N.I.F: " + fill(s2(raw, "c2_nif"), "_______________"), pB);
            gap(5f);
        }

        parrafo("En adelante, el Proponente o Comprador, actuando:", pB);
        gap(6f);

        // ── Actuando (marca ✓) ────────────────────────────────────────────────
        boolean enRep       = s2(raw, "en_representacion").equals("true");
        String  marcaPropio = enRep ? "( )" : "(\u2713)";
        String  marcaRep    = enRep ? "(\u2713)" : "( )";

        mCv.drawText(marcaPropio + "  en su propio nombre y derecho.",
                ML + 12f, mY + TS_BODY, paint(TS_BODY, C_BLACK, false, Paint.Align.LEFT));
        mY += TS_BODY * 1.3f;

        parrafo(marcaRep + "  en nombre y representación de "
                + fill(s2(raw, "rep_nombre"), "___________________________________________")
                + ", con domicilio en " + fill(s2(raw, "rep_domicilio"), "________________")
                + ", C/ " + fill(s2(raw, "rep_calle"), "___________________________________________") + ",", pB);
        gap(2f);
        parrafo("provisto de N.I.F./C.I.F. " + fill(s2(raw, "rep_nif"), "__________________")
                + ", en calidad de " + fill(s2(raw, "rep_calidad"), "________________________________")
                + ", según acredita documentalmente.", pB);
        gap(6f);

        parrafo("Y, de otra parte, la sociedad que aparece en el encabezado, (en adelante, el director),", pB);
        gap(10f);

        // ── MANIFIESTAN Y ACUERDAN ────────────────────────────────────────────
        Paint pSec = paint(TS_BODY, C_BLACK, true, Paint.Align.CENTER);
        mCv.drawText("MANIFIESTAN Y ACUERDAN", PW / 2f, mY, pSec);
        float tw2 = pSec.measureText("MANIFIESTAN Y ACUERDAN");
        mCv.drawLine((PW - tw2) / 2f, mY + 2f, (PW + tw2) / 2f, mY + 2f, ul);
        gap(10f);

        parrafoConPrefijo("I.-",
                " Que el Proponente, en fecha " + fill(s2(raw, "fecha_propuesta"), "_________________")
                        + " suscribe una Propuesta de Contrato de Compraventa en relación con el inmueble sito en:",
                pBold, pB);
        gap(2f);
        parrafo(fill(s2(raw, "inmueble"), "________________________________________________________________________________"), pB);
        gap(5f);

        parrafoConPrefijo("II.-",
                " Que los honorarios a percibir por el director del Proponente por los servicios de intermediación"
                        + " inmobiliaria realizados, se fijan por mutuo acuerdo en "
                        + fill(s2(raw, "honorarios"), "___________")
                        + " + IVA, que se abonarán en el momento en el cual se considere aceptada por la parte vendedora"
                        + " la Propuesta de Contrato de Compraventa.",
                pBold, pB);
        gap(5f);

        parrafoConPrefijo("III.-",
                " En el supuesto que en fraude del intermediario y aprovechándose de la labor realizada por éste,"
                        + " el Proponente por sí mismo o valiéndose de terceros, adquiriera total o parcialmente la propiedad"
                        + " del citado inmueble o el uso y disfrute del mismo durante la vigencia del encargo de venta en"
                        + " exclusiva suscrito entre la Propiedad y el Director, o antes de haber trascurrido un año desde"
                        + " la fecha de la presente, el Proponente se obliga a satisfacer al director como penalización:"
                        + " 6 % + I.V.A. sobre el precio propuesto para la compra del inmueble con un mínimo de 6.000€ + I.V.A.",
                pBold, pB);
        gap(5f);

        parrafoConPrefijo("IIII.-",
                " En caso de que la venta no se lleve a cabo por causa imputable al vendedor Soluciones Villasan S.L."
                        + " se compromete a devolver los honorarios cobrados al cliente.",
                pBold, pB);
        gap(10f);

        parrafo("En " + fill(s2(raw, "lugar"), "_______________________") + ", a "
                + fill(s2(raw, "firma_dia"), "____") + " de "
                + fill(s2(raw, "firma_mes"), "_____________________") + " de 202"
                + fill(s2(raw, "firma_anio"), "__") + ".", pB);
        gap(15f);

        bloquesFirma(raw, c2);

        return cerrarPDF("honorarios_comprador_");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Párrafo con prefijo en negrita
    // ─────────────────────────────────────────────────────────────────────────
    private void parrafoConPrefijo(String prefijo, String resto, Paint pPref, Paint pRest) {
        float lineH = pRest.getTextSize() * 1.4f;
        float prefW = pPref.measureText(prefijo + " ");
        List<String> lineas = wrap(prefijo + resto, pRest, TW);
        boolean primera = true;
        for (String l : lineas) {
            checkPage(lineH);
            if (primera) {
                mCv.drawText(prefijo, ML, mY + pPref.getTextSize(), pPref);
                String restoLinea = l.length() > prefijo.length()
                        ? l.substring(prefijo.length()) : "";
                if (!restoLinea.isEmpty())
                    mCv.drawText(restoLinea, ML + prefW, mY + pRest.getTextSize(), pRest);
                primera = false;
            } else {
                mCv.drawText(l, ML, mY + pRest.getTextSize(), pRest);
            }
            mY += lineH;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Bloque de firmas  ← FIX: usa RectF directo igual que EncargoActivity
    // ─────────────────────────────────────────────────────────────────────────
    private void bloquesFirma(Map<String, Object> raw, String c2Nombre) {
        boolean tieneC2 = !c2Nombre.isEmpty();
        checkPage(95f);

        float selloMaxW = tieneC2 ? (TW / 3f - 5f) : (TW / 2f - 5f);
        float selloMaxH = 70f;
        float colW  = selloMaxW;
        int   ncols = tieneC2 ? 3 : 2;

        float[] xs = new float[ncols];
        xs[0] = ML;
        xs[1] = ML + colW + 7f;
        if (ncols == 3) xs[2] = ML + 2f * (colW + 7f);

        Paint pLabel = paint(TS_BODY - 1f, C_BLACK, false, Paint.Align.LEFT);
        Paint pName  = paint(TS_BODY - 1.5f, C_BLACK, false, Paint.Align.LEFT);

        // ── Paint del recuadro (declarado UNA vez, fuera del bucle) ───────────
        Paint boxP = new Paint();
        boxP.setColor(Color.parseColor("#AAAAAA"));
        boxP.setStyle(Paint.Style.STROKE);
        boxP.setStrokeWidth(0.7f);

        String[] labels = tieneC2
                ? new String[]{"Por el Director", "El Proponente", "El Proponente"}
                : new String[]{"Por el Director", "El Proponente"};
        String[] subNombres = tieneC2
                ? new String[]{"Soluciones Villasan S.L.", "(C1) " + s2(raw, "c1_nombre"), "(C2) " + c2Nombre}
                : new String[]{"Soluciones Villasan S.L.", "(C1) " + s2(raw, "c1_nombre")};

        for (int i = 0; i < ncols; i++)
            mCv.drawText(labels[i], xs[i], mY + TS_BODY - 1.5f, pLabel);
        mY += (TS_BODY - 1.5f) * 1.5f;

        float boxH = selloMaxH;

        // ── Columna 0: sello de empresa ───────────────────────────────────────
        if (mSello != null) {
            float ratio = (float) mSello.getWidth() / mSello.getHeight();
            float sW, sH;
            if (ratio >= 1f) {
                sW = selloMaxW; sH = sW / ratio;
                if (sH > selloMaxH) { sH = selloMaxH; sW = sH * ratio; }
            } else {
                sH = selloMaxH; sW = sH * ratio;
                if (sW > selloMaxW) { sW = selloMaxW; sH = sW / ratio; }
            }
            boxH = sH;
            RectF destSello = new RectF(xs[0], mY, xs[0] + sW, mY + sH);
            mCv.drawBitmap(mSello, null, destSello,
                    new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG));
        }

        // ── Columnas 1..n: firmas ─────────────────────────────────────────────
        SignatureView[] svs = tieneC2
                ? new SignatureView[]{signatureViewC1, signatureViewC2}
                : new SignatureView[]{signatureViewC1};

        for (int i = 1; i < ncols; i++) {
            SignatureView sv = svs[i - 1];
            if (sv != null && sv.hasFirma()) {
                Bitmap sig = sv.toBitmap();
                if (sig != null && sig.getWidth() > 0) {
                    // ✅ FIX: dibuja directamente con RectF como en EncargoActivity
                    mCv.drawBitmap(sig, null,
                            new RectF(xs[i], mY, xs[i] + colW, mY + boxH),
                            new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG));
                }
            }
            mCv.drawRect(xs[i], mY, xs[i] + colW, mY + boxH, boxP);
        }

        mY += boxH + 2f;

        for (int i = 0; i < ncols; i++)
            mCv.drawText(subNombres[i], xs[i], mY + (TS_BODY - 1.5f), pName);
        mY += (TS_BODY - 1.5f) * 1.6f;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // HELPERS PDF
    // ═══════════════════════════════════════════════════════════════════════════

    private void cabecera() {
        if (mLogo != null) mCv.drawBitmap(mLogo, null,
                new RectF(ML, mY, ML + LOGO_W, mY + LOGO_H), null);
        float cx = ML + LOGO_W + 8f + (TW - LOGO_W - 8f) / 2f;
        mCv.drawText("SOLUCIONES VILLASAN S.L.         N.I.F.  B75484741",
                cx, mY + TS_CAB + 2f,  paint(TS_CAB, C_BLACK, false, Paint.Align.CENTER));
        mCv.drawText("Avenida de Beleña Nº16 local 1 - Guadalajara – Telf.: 949410344",
                cx, mY + TS_CAB + 14f, paint(10f, C_BLACK, false, Paint.Align.CENTER));
        mCv.drawText("casablancaguasvivas@gmail.com   www.casablancainmobiliarias.com",
                cx, mY + TS_CAB + 25f, paint(TS_WEB, C_BLUE, false, Paint.Align.CENTER));
        mY += LOGO_H + 4f;
        Paint sep = new Paint(); sep.setColor(C_BLACK); sep.setStrokeWidth(0.5f);
        mCv.drawLine(ML, mY, ML + TW, mY, sep);
        mY += 6f;
    }

    private void abrirPagina() {
        PdfDocument.PageInfo info =
                new PdfDocument.PageInfo.Builder((int) PW, (int) PH, mPageNum++).create();
        mPage = mDoc.startPage(info);
        mCv   = mPage.getCanvas();
        mY    = 30f;
        if (mPageNum > 2) { cabecera(); gap(4f); }
    }

    private void checkPage(float needed) {
        if (mY + needed > PH - MB) { mDoc.finishPage(mPage); abrirPagina(); }
    }

    private void parrafo(String texto, Paint p) {
        if (texto == null || texto.isEmpty()) return;
        float lineH = p.getTextSize() * 1.4f;
        for (String l : wrap(texto, p, TW)) {
            checkPage(lineH);
            mCv.drawText(l, ML, mY + p.getTextSize(), p);
            mY += lineH;
        }
    }

    private void gap(float pts) { mY += pts; }

    private List<String> wrap(String texto, Paint p, float maxW) {
        List<String> result = new ArrayList<>();
        if (texto == null || texto.isEmpty()) return result;
        String[] words = texto.split(" ", -1);
        StringBuilder cur = new StringBuilder();
        for (String w : words) {
            String test = cur.length() == 0 ? w : cur + " " + w;
            if (p.measureText(test) <= maxW) {
                cur = new StringBuilder(test);
            } else {
                if (cur.length() > 0) result.add(cur.toString());
                cur = new StringBuilder(w);
            }
        }
        if (cur.length() > 0) result.add(cur.toString());
        return result;
    }

    private Paint paint(float size, int color, boolean bold, Paint.Align align) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setTextSize(size);
        p.setColor(color);
        p.setFakeBoldText(bold);
        p.setTextAlign(align);
        return p;
    }

    private String fill(String val, String placeholder) {
        return (val == null || val.isEmpty()) ? placeholder : val;
    }

    private String s2(Map<String, Object> m, String k) {
        Object v = m.get(k); return v != null ? String.valueOf(v) : "";
    }

    private File cerrarPDF(String prefix) {
        mDoc.finishPage(mPage);
        File file = new File(getExternalFilesDir(null),
                prefix + System.currentTimeMillis() + ".pdf");
        try (FileOutputStream fos = new FileOutputStream(file)) {
            mDoc.writeTo(fos);
            mDoc.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return file;
    }

    private void compartirPDF(File file) {
        Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".provider", file);
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("application/pdf");
        i.putExtra(Intent.EXTRA_SUBJECT, "Declaración de Honorarios - Compraventa");
        i.putExtra(Intent.EXTRA_TEXT, "Adjunto declaración de honorarios compraventa.");
        i.putExtra(Intent.EXTRA_STREAM, uri);
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(i, "Compartir PDF"));
    }
}