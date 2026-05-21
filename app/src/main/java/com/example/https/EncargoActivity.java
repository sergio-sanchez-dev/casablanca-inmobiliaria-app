package com.example.https;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
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

public class EncargoActivity extends AppCompatActivity {

    // ── Medidas A4 ────────────────────────────────────────────
    private static final float PW = 595f, PH = 842f;
    private static final float ML = 36f, MR = 36f;
    private static final float TW = PW - ML - MR;
    private static final float MB = 40f;
    private static final float LOGO_W = 77f, LOGO_H = 58f;
    private static final float TS_CAB = 12f, TS_WEB = 9f, TS_TIT = 14f;
    private static final float TS_CAMPO = 12f, TS_BODY = 11.5f, TS_PIE = 8f;
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

    // ── Formulario ────────────────────────────────────────────
    EditText etC1Nombre, etC1Direccion, etC1DNI, etC1Telefono1, etC1Telefono2, etC1Email;
    EditText etC2Nombre, etC2Direccion, etC2DNI, etC2Telefono1, etC2Telefono2, etC2Email;
    EditText etDireccionInmueble, etCargas, etPrecio, etHonorarios;
    EditText etFechaInicio, etFechaFin;
    EditText etLocalidad, etFechaDia, etFechaMes, etFechaAnio;
    EditText etClausula11, etObservaciones;
    RadioGroup rgLlaves;
    Button btnGuardar, btnGuardarYPDF, btnBorrarFirma, btnVerFirma;
    SignatureView signatureView;

    // ── Lista ─────────────────────────────────────────────────
    Button btnFiltroHoy, btnFiltroAyer, btnFiltroMes, btnFiltroTodas;
    Spinner spFiltroAsesor;
    RecyclerView rvEncargos;
    GenericListAdapter listAdapter;
    List<GenericItem> listaEncargos = new ArrayList<>();

    // ── Firebase ──────────────────────────────────────────────
    FirebaseFirestore db;
    String userId;
    boolean esAdmin = false;

    // ── Edición ───────────────────────────────────────────────
    String docIdEditando = null;
    Map<String, String> mapaAsesores = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_encargo);
        db     = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        cargarAsesores();
        bindFormulario();
        bindLista();
        comprobarRol();
    }

    // ═════════════════════════════════════════════════════════
    // BIND
    // ═════════════════════════════════════════════════════════

    private void bindFormulario() {
        etC1Nombre    = findViewById(R.id.etC1Nombre);
        etC1Direccion = findViewById(R.id.etC1Direccion);
        etC1DNI       = findViewById(R.id.etC1DNI);
        etC1Telefono1 = findViewById(R.id.etC1Telefono1);
        etC1Telefono2 = findViewById(R.id.etC1Telefono2);
        etC1Email     = findViewById(R.id.etC1Email);
        etC2Nombre    = findViewById(R.id.etC2Nombre);
        etC2Direccion = findViewById(R.id.etC2Direccion);
        etC2DNI       = findViewById(R.id.etC2DNI);
        etC2Telefono1 = findViewById(R.id.etC2Telefono1);
        etC2Telefono2 = findViewById(R.id.etC2Telefono2);
        etC2Email     = findViewById(R.id.etC2Email);
        etDireccionInmueble = findViewById(R.id.etDireccionInmueble);
        etCargas            = findViewById(R.id.etCargas);
        etPrecio            = findViewById(R.id.etPrecio);
        etHonorarios        = findViewById(R.id.etHonorarios);
        etFechaInicio       = findViewById(R.id.etFechaInicio);
        etFechaFin          = findViewById(R.id.etFechaFin);
        etClausula11        = findViewById(R.id.etClausula11);
        etObservaciones     = findViewById(R.id.etObservaciones);
        rgLlaves            = findViewById(R.id.rgLlaves);
        etLocalidad  = findViewById(R.id.etLocalidad);
        etFechaDia   = findViewById(R.id.etFechaDia);
        etFechaMes   = findViewById(R.id.etFechaMes);
        etFechaAnio  = findViewById(R.id.etFechaAnio);
        signatureView  = findViewById(R.id.signatureView);
        btnBorrarFirma = findViewById(R.id.btnBorrarFirma);
        btnVerFirma    = findViewById(R.id.btnVerFirma);
        btnGuardar     = findViewById(R.id.btnGuardar);
        btnGuardarYPDF = findViewById(R.id.btnGuardarYPDF);

        btnBorrarFirma.setOnClickListener(v -> signatureView.clear());
        btnVerFirma.setOnClickListener(v -> Toast.makeText(this,
                signatureView.hasFirma() ? "Firma capturada ✓" : "Todavía no hay firma",
                Toast.LENGTH_SHORT).show());
        btnGuardar.setOnClickListener(v -> guardarEncargo(false));
        btnGuardarYPDF.setOnClickListener(v -> guardarEncargo(true));
    }

    private void bindLista() {
        btnFiltroHoy   = findViewById(R.id.btnFiltroHoy);
        btnFiltroAyer  = findViewById(R.id.btnFiltroAyer);
        btnFiltroMes   = findViewById(R.id.btnFiltroMes);
        btnFiltroTodas = findViewById(R.id.btnFiltroTodas);
        spFiltroAsesor = findViewById(R.id.spFiltroAsesor);
        rvEncargos     = findViewById(R.id.rvEncargos);

        listAdapter = new GenericListAdapter(listaEncargos, new GenericListAdapter.OnItemListener() {
            @Override public void onEditar(GenericItem item, int position) { cargarEnFormulario(item); }
            @Override public void onEliminar(GenericItem item, int position) { confirmarEliminacion(item, position); }
        });

        rvEncargos.setLayoutManager(new LinearLayoutManager(this));
        rvEncargos.setAdapter(listAdapter);
        rvEncargos.setNestedScrollingEnabled(false);

        btnFiltroHoy.setOnClickListener(v   -> cargarEncargos("hoy"));
        btnFiltroAyer.setOnClickListener(v  -> cargarEncargos("ayer"));
        btnFiltroMes.setOnClickListener(v   -> cargarEncargos("mes"));
        btnFiltroTodas.setOnClickListener(v -> cargarEncargos("todas"));
    }

    // ═════════════════════════════════════════════════════════
    // ROL
    // ═════════════════════════════════════════════════════════

    private void comprobarRol() {
        db.collection("usuarios")
                .whereEqualTo("uid", userId)
                .get()
                .addOnSuccessListener(query -> {
                    if (!query.isEmpty()) {
                        String rol = query.getDocuments().get(0).getString("rol");
                        esAdmin = "admin".equals(rol);
                    }
                    spFiltroAsesor.setVisibility(esAdmin ? View.VISIBLE : View.GONE);
                    cargarEncargos("hoy");
                });
    }

    // ═════════════════════════════════════════════════════════
    // GUARDAR / ACTUALIZAR
    // ═════════════════════════════════════════════════════════

    private void guardarEncargo(boolean generarPDF) {
        if (val(etC1Nombre).isEmpty()) {
            etC1Nombre.setError("Obligatorio"); etC1Nombre.requestFocus(); return;
        }
        if (val(etDireccionInmueble).isEmpty()) {
            etDireccionInmueble.setError("Obligatorio"); etDireccionInmueble.requestFocus(); return;
        }
        if (generarPDF && docIdEditando == null && !signatureView.hasFirma()) {
            Toast.makeText(this, "Añade la firma del cliente antes de generar el PDF", Toast.LENGTH_LONG).show();
            return;
        }

        String llaves = (rgLlaves.getCheckedRadioButtonId() == R.id.rbLlavesSi) ? "SI" : "NO";

        Map<String, Object> data = new HashMap<>();
        data.put("userId",    userId);
        data.put("timestamp", Timestamp.now());
        data.put("c1_nombre",    val(etC1Nombre));
        data.put("c1_direccion", val(etC1Direccion));
        data.put("c1_dni",       val(etC1DNI));
        data.put("c1_telefono1", val(etC1Telefono1));
        data.put("c1_telefono2", val(etC1Telefono2));
        data.put("c1_email",     val(etC1Email));
        data.put("c2_nombre",    val(etC2Nombre));
        data.put("c2_direccion", val(etC2Direccion));
        data.put("c2_dni",       val(etC2DNI));
        data.put("c2_telefono1", val(etC2Telefono1));
        data.put("c2_telefono2", val(etC2Telefono2));
        data.put("c2_email",     val(etC2Email));
        data.put("direccion_inmueble", val(etDireccionInmueble));
        data.put("cargas",             val(etCargas));
        data.put("precio",             val(etPrecio));
        data.put("honorarios",         val(etHonorarios));
        data.put("fecha_inicio",       val(etFechaInicio));
        data.put("fecha_fin",          val(etFechaFin));
        data.put("llaves_en_oficina",  llaves);
        data.put("clausula_11",        val(etClausula11));
        data.put("observaciones",      val(etObservaciones));
        data.put("localidad",  val(etLocalidad));
        data.put("fecha_dia",  val(etFechaDia));
        data.put("fecha_mes",  val(etFechaMes));
        data.put("fecha_anio", val(etFechaAnio));

        if (docIdEditando != null) {
            db.collection("encargos").document(docIdEditando)
                    .update(data)
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(this, "✔ Encargo actualizado", Toast.LENGTH_SHORT).show();
                        limpiarFormulario();
                        cargarEncargos("todas");
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "❌ " + e.getMessage(), Toast.LENGTH_LONG).show());
        } else {
            db.collection("encargos").add(data)
                    .addOnSuccessListener(ref -> {
                        if (generarPDF) compartirPDF(buildPDF(data));
                        Toast.makeText(this, "✅ Encargo guardado", Toast.LENGTH_SHORT).show();
                        limpiarFormulario();
                        cargarEncargos("hoy");
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "❌ " + e.getMessage(), Toast.LENGTH_LONG).show());
        }
    }

    // ═════════════════════════════════════════════════════════
    // CARGA LISTA CON FILTROS
    // ═════════════════════════════════════════════════════════

    private void cargarEncargos(String filtro) {
        Query query = esAdmin
                ? db.collection("encargos")
                : db.collection("encargos").whereEqualTo("userId", userId);

        query.get().addOnSuccessListener(snapshots -> {
            listaEncargos.clear();

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
                String inmueble = doc.getString("direccion_inmueble");

                String titulo    = cliente != null ? cliente : "—";
                String subtitulo = (inmueble != null ? inmueble : "") + " · " + asesorVisible;

                GenericItem item = new GenericItem(
                        doc.getId(), titulo, subtitulo,
                        doc.getString("honorarios"),
                        uidCreador, doc.getData(), ts, asesorVisible);
                listaEncargos.add(item);
            }

            // Filtro por asesor (solo admin)
            if (esAdmin && spFiltroAsesor.getSelectedItem() != null) {
                String sel = spFiltroAsesor.getSelectedItem().toString();
                if (!sel.equals("Todos")) {
                    List<GenericItem> filtradas = new ArrayList<>();
                    for (GenericItem v : listaEncargos)
                        if (sel.equalsIgnoreCase(v.asesor)) filtradas.add(v);
                    listaEncargos.clear();
                    listaEncargos.addAll(filtradas);
                }
            }

            Collections.sort(listaEncargos, (a, b) -> {
                if (a.timestamp == null || b.timestamp == null) return 0;
                return b.timestamp.compareTo(a.timestamp);
            });

            listAdapter.notifyDataSetChanged();
            if (listaEncargos.isEmpty())
                Toast.makeText(this, "Sin encargos para este filtro", Toast.LENGTH_SHORT).show();

        }).addOnFailureListener(e ->
                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // ═════════════════════════════════════════════════════════
    // CARGAR EN FORMULARIO
    // ═════════════════════════════════════════════════════════

    private void cargarEnFormulario(GenericItem item) {
        docIdEditando = item.docId;
        db.collection("encargos").document(item.docId).get()
                .addOnSuccessListener(doc -> {
                    etC1Nombre.setText(s(doc,"c1_nombre"));
                    etC1Direccion.setText(s(doc,"c1_direccion"));
                    etC1DNI.setText(s(doc,"c1_dni"));
                    etC1Telefono1.setText(s(doc,"c1_telefono1"));
                    etC1Telefono2.setText(s(doc,"c1_telefono2"));
                    etC1Email.setText(s(doc,"c1_email"));
                    etC2Nombre.setText(s(doc,"c2_nombre"));
                    etC2Direccion.setText(s(doc,"c2_direccion"));
                    etC2DNI.setText(s(doc,"c2_dni"));
                    etC2Telefono1.setText(s(doc,"c2_telefono1"));
                    etC2Telefono2.setText(s(doc,"c2_telefono2"));
                    etC2Email.setText(s(doc,"c2_email"));
                    etDireccionInmueble.setText(s(doc,"direccion_inmueble"));
                    etCargas.setText(s(doc,"cargas"));
                    etPrecio.setText(s(doc,"precio"));
                    etHonorarios.setText(s(doc,"honorarios"));
                    etFechaInicio.setText(s(doc,"fecha_inicio"));
                    etFechaFin.setText(s(doc,"fecha_fin"));
                    etClausula11.setText(s(doc,"clausula_11"));
                    etObservaciones.setText(s(doc,"observaciones"));
                    etLocalidad.setText(s(doc,"localidad"));
                    etFechaDia.setText(s(doc,"fecha_dia"));
                    etFechaMes.setText(s(doc,"fecha_mes"));
                    etFechaAnio.setText(s(doc,"fecha_anio"));

                    String llaves = s(doc,"llaves_en_oficina");
                    if ("SI".equals(llaves)) rgLlaves.check(R.id.rbLlavesSi);
                    else rgLlaves.check(R.id.rbLlavesNo);

                    btnGuardar.setText("💾 Actualizar encargo");
                    btnGuardarYPDF.setText("💾 Actualizar y PDF");
                    findViewById(R.id.scrollencargo).scrollTo(0, 0);
                    Toast.makeText(this, "Editando encargo de " + item.titulo, Toast.LENGTH_SHORT).show();
                });
    }

    // ═════════════════════════════════════════════════════════
    // ELIMINAR
    // ═════════════════════════════════════════════════════════

    private void confirmarEliminacion(GenericItem item, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar encargo")
                .setMessage("¿Eliminar el encargo de " + item.titulo + "?\nEsta acción no se puede deshacer.")
                .setPositiveButton("🗑 Eliminar", (d, w) -> {
                    db.collection("encargos").document(item.docId)
                            .delete()
                            .addOnSuccessListener(unused -> {
                                listAdapter.remove(position);
                                Toast.makeText(this, "Encargo eliminado", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Error al eliminar", Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // ═════════════════════════════════════════════════════════
    // ASESORES
    // ═════════════════════════════════════════════════════════

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

    // ═════════════════════════════════════════════════════════
    // HELPERS
    // ═════════════════════════════════════════════════════════

    private void limpiarFormulario() {
        docIdEditando = null;
        btnGuardar.setText("GUARDAR ENCARGO");
        btnGuardarYPDF.setText("GUARDAR Y GENERAR PDF");
        etC1Nombre.setText(""); etC1Direccion.setText(""); etC1DNI.setText("");
        etC1Telefono1.setText(""); etC1Telefono2.setText(""); etC1Email.setText("");
        etC2Nombre.setText(""); etC2Direccion.setText(""); etC2DNI.setText("");
        etC2Telefono1.setText(""); etC2Telefono2.setText(""); etC2Email.setText("");
        etDireccionInmueble.setText(""); etCargas.setText(""); etPrecio.setText("");
        etHonorarios.setText(""); etFechaInicio.setText(""); etFechaFin.setText("");
        etClausula11.setText(""); etObservaciones.setText("");
        etLocalidad.setText(""); etFechaDia.setText(""); etFechaMes.setText(""); etFechaAnio.setText("");
        rgLlaves.check(R.id.rbLlavesSi);
        signatureView.clear();
    }

    private String val(EditText et) { return et != null ? et.getText().toString().trim() : ""; }

    private String s(com.google.firebase.firestore.DocumentSnapshot doc, String key) {
        Object v = doc.get(key); return v != null ? String.valueOf(v) : "";
    }

    // ═════════════════════════════════════════════════════════
    // PDF
    // ═════════════════════════════════════════════════════════

    private File buildPDF(Map<String, Object> data) {
        try { mLogo  = BitmapFactory.decodeResource(getResources(), R.drawable.logo_casablanca); } catch (Exception e) { mLogo = null; }
        try { mSello = BitmapFactory.decodeResource(getResources(), R.drawable.sello_empresa);  } catch (Exception e) { mSello = null; }

        mDoc = new PdfDocument(); mPageNum = 1; abrirPagina();

        String c1    = s2(data,"c1_nombre");    String c1dir = s2(data,"c1_direccion");
        String c1dni = s2(data,"c1_dni");        String c1t1  = s2(data,"c1_telefono1");
        String c1t2  = s2(data,"c1_telefono2"); String c1mail= s2(data,"c1_email");
        String c2    = s2(data,"c2_nombre");    String c2dir = s2(data,"c2_direccion");
        String c2dni = s2(data,"c2_dni");        String c2t1  = s2(data,"c2_telefono1");
        String c2t2  = s2(data,"c2_telefono2"); String c2mail= s2(data,"c2_email");
        String inm   = s2(data,"direccion_inmueble");
        String cargas= s2(data,"cargas");       String precio = s2(data,"precio");
        String honor = s2(data,"honorarios");   String fIni   = s2(data,"fecha_inicio");
        String fFin  = s2(data,"fecha_fin");    String llaves = s2(data,"llaves_en_oficina");
        String cl11  = s2(data,"clausula_11");  String obs    = s2(data,"observaciones");

        if (cargas.isEmpty()) cargas = ".....................................................................";
        if (precio.isEmpty()) precio = ".....................................................................";
        if (honor.isEmpty())  honor  = "……………………..";
        if (fIni.isEmpty())   fIni   = "………………………………";
        if (fFin.isEmpty())   fFin   = "………………………………";
        if (inm.isEmpty())    inm    = ".................................................................................................................................................";
        if (cl11.isEmpty())   cl11   = "………………………………………………………………………………………………………………………………………….............................\n………………………………………………………………………………………………………………………………………………………………………………………..";

        cabecera(); gap(8f);

        Paint pTit = paint(TS_TIT, C_BLACK, false, Paint.Align.CENTER);
        textLine("NOTA DE ENCARGO DE COMPRAVENTA DE INMUEBLE", pTit, PW / 2f);
        gap(10f);

        Paint pC = paint(TS_CAMPO, C_BLACK, false, Paint.Align.LEFT);
        textLine("D./Dª.(C1):" + fill(c1,"_______________________________________________________________________________"), pC, ML);
        gap(2f); textLine("dirección:" + fill(c1dir,"______________________________________________") + "  D.N.I.: " + fill(c1dni,"___________________________"), pC, ML);
        gap(2f); textLine("Teléfono 1:" + fill(c1t1,"_________________") + "                Teléfono 2:" + fill(c1t2,"________________"), pC, ML);
        gap(2f); textLine("E-mail 1:" + fill(c1mail,"_________________________________________________________________________________"), pC, ML);
        gap(6f);
        textLine("D./Dª.(C2): " + fill(c2,"_______________________________________________________________________________"), pC, ML);
        gap(2f); textLine("dirección:" + fill(c2dir,"_________________________________________________________") + "D.N.I.: " + fill(c2dni,"_________________") + " Teléfono 1:" + fill(c2t1,"_____________"), pC, ML);
        gap(2f); textLine("E-mail 1: " + fill(c2mail,"_________________________________________________________________________________"), pC, ML);
        gap(6f);

        Paint pB = paint(TS_BODY, C_BLACK, false, Paint.Align.LEFT);
        parrafo("Actuando:      en su propio nombre y en representación (en adelante, el Cliente)", pB);
        parrafo("                          en nombre y representación de.............................................................................adelante, el Cliente), provisto de N.I.F./C.I.F.......................... en calidad de................................................ según acredita documentalmente.", pB);
        gap(4f);
        parrafo("ENCARGA, de forma exclusiva, a SOLUCIONES VILLASAN S.L. con NIF: B75484741, (en adelante, el Director), que acepta el encargo, la localización de un comprador para el inmueble identificado como sigue: Dirección:" + inm, pB);
        gap(5f);
        parrafo("2) El Cliente afirma que el citado inmueble se encuentra libre de cargas, gravámenes, vicios y evicciones a excepción de " + cargas + ".", pB);
        gap(3f); parrafo("3) El Cliente declara a tener la total y exclusiva disponibilidad del inmueble en objeto, en su afirmada condición de propietario, según deberá acreditar documentalmente.", pB);
        gap(3f); parrafo("4) El Cliente fija el precio del inmueble en " + precio + " Euros,", pB);
        gap(3f); parrafo("5) El Director se obliga a realizar las gestiones de mediación oportunas para la localización de un comprador, y a mantener informado al Cliente de tales gestiones.", pB);
        gap(3f); parrafo("6) Los honorarios a percibir por el Director del Cliente serán de " + honor + " +I.V.A. sobre el precio de venta, que se abonarán en el momento en el cual se considere aceptada por la parte vendedora la propuesta de Contrato de Compraventa.", pB);
        gap(2f); parrafo("El vendedor acepta, que el director cobre honorarios por los servicios prestados a la parte compradora.", pB);
        gap(3f); parrafo("7) El encargo tendrá validez desde el día " + fIni + " hasta el " + fFin + ". Este plazo se resumirá tácitamente renovado, de forma sucesiva, por idénticos períodos de tiempo, salvo que cualquiera de las dos partes notifique por escrito a la otra su voluntad en contrario con, al menos, 7 días de antelación respecto de la finalización del plazo o de cualquiera de sus prórrogas.", pB);
        gap(2f); parrafo("Expirado el plazo antes citado o cualquiera de sus prórrogas sin que el director, haya localizado un comprador conforme con el presente encargo, éste no tendrá derecho a percibir cantidad alguna en concepto de honorarios.", pB);
        gap(3f); parrafo("8) El Cliente autoriza al director a solicitar, recibir y retener arras o señal del Comprador por un importe máximo de\n" +
                "10%. En caso de que el comprador necesite hipoteca, el cliente autoriza a supeditar las arras a la concesión del\n" +
                "préstamo hipotecario con CB Hipotecas.", pB);
        gap(2f); parrafo("Recibidas las arras o señal por el director respecto a una Propuesta de Contrato de Compraventa conforme con el\n" +
                "presente encargo, la falta de suscripción del contrato de compraventa por el Proponente comportará la pérdida de\n" +
                "las sumas abonadas por él en tal concepto, a favor del Cliente. Si la causa de dicha falta de suscripción fuera\n" +
                "imputable al Cliente, éste deberá devolverlas dobladas al Proponente.", pB);
        parrafo("9) El Cliente autoriza, asimismo, al director a ofertar y publicar el inmueble. Del mismo modo, el Cliente autoriza que el director realice visitas comerciales al inmueble acompañado de potenciales compradores.", pB);
        gap(3f); parrafo("10) Los honorarios convenidos deberán ser abonados por el Cliente en su totalidad en el supuesto de que:", pB);
        itemLista("- La venta se lleve a cabo por el Cliente o terceros durante el plazo de vigencia del presente encargo;", pB);
        itemLista("- En el transcurso de un año desde la finalización del encargo, se realizase la venta a favor de personas presentadas al cliente por el director.", pB);
        itemLista("- El Cliente revoca el encargo antes de su caducidad sin mediar justa y adecuada causa.", pB);
        itemLista("- El Cliente se negará a suscribir un contrato de compraventa que trajera causa de una propuesta de la compra conforme con el presente encargo.", pB);
        gap(4f);
        for (String lin : cl11.split("\n")) parrafo(lin.trim().isEmpty() ? lin : lin, pB);
        gap(3f); parrafo("12) LA VIVIENDA SE ENTREGARÁ EL DIA DE ESCRITURA PUBLICA.", pB);
        gap(2f); parrafo("13) Las llaves del inmueble están en la dirección de la oficina que aparece en el encabezado " + (llaves.isEmpty() ? "SI / NO" : llaves) + ".", pB);
        if (!obs.isEmpty()) { gap(4f); parrafo(obs, pB); }

        bloquesFirma(data, pB);
        pieLOPD();

        mDoc.finishPage(mPage);
        File file = new File(getExternalFilesDir(null), "encargo_" + System.currentTimeMillis() + ".pdf");
        try (FileOutputStream fos = new FileOutputStream(file)) { mDoc.writeTo(fos); mDoc.close(); } catch (Exception e) { e.printStackTrace(); }
        return file;
    }

    private void bloquesFirma(Map<String, Object> data, Paint pB) {
        gap(14f); checkPage(140f);
        String loc  = s2(data,"localidad");  if (loc.isEmpty())  loc  = "..................................................";
        String dia  = s2(data,"fecha_dia");  if (dia.isEmpty())  dia  = "..............";
        String mes  = s2(data,"fecha_mes");  if (mes.isEmpty())  mes  = "………................................";
        String anio = s2(data,"fecha_anio"); if (anio.isEmpty()) anio = "…….….";
        String c1n  = s2(data,"c1_nombre");  if (c1n.isEmpty())  c1n  = "………………………………………………………………………..….";
        parrafo("Y para que así conste, lo firman, en " + loc + ", a " + dia + " de " + mes + " de " + anio + ".", pB);
        gap(14f);
        Paint pFL = paint(TS_BODY, C_BLACK, false, Paint.Align.LEFT);
        float colW = TW / 2f - 6f; float col2X = ML + TW / 2f + 5f;
        mCv.drawText("Por el Director", ML, mY + TS_BODY, pFL);
        mCv.drawText("El Cliente / El Representante", col2X, mY + TS_BODY, pFL);
        mY += TS_BODY * 1.8f;
        float selloMaxW = colW, selloMaxH = 90f;
        if (mSello != null) {
            float ratio = (float) mSello.getWidth() / mSello.getHeight();
            float sW, sH;
            if (ratio >= 1f) { sW = selloMaxW; sH = sW / ratio; if (sH > selloMaxH) { sH = selloMaxH; sW = sH * ratio; } }
            else { sH = selloMaxH; sW = sH * ratio; if (sW > selloMaxW) { sW = selloMaxW; sH = sW / ratio; } }
            RectF dest = new RectF(ML, mY, ML + sW, mY + sH);
            mCv.drawBitmap(mSello, null, dest, new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG));
            float selloBottom = mY + sH + 6f;
            mCv.drawText("Soluciones Villasan S.L.", ML, selloBottom + TS_BODY, pFL);
            float boxH = sH;
            Paint boxP = new Paint(); boxP.setColor(Color.parseColor("#AAAAAA")); boxP.setStyle(Paint.Style.STROKE); boxP.setStrokeWidth(0.7f);
            if (signatureView != null && signatureView.hasFirma()) {
                Bitmap sig = signatureView.toBitmap();
                if (sig != null && sig.getWidth() > 0) {
                    mCv.drawBitmap(sig, null, new RectF(col2X, mY, col2X + colW, mY + boxH), new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG));
                }
            }
            mCv.drawRect(col2X, mY, col2X + colW, mY + boxH, boxP);
            mY = selloBottom + TS_BODY * 1.8f;
            mCv.drawText("Nombre y Apellidos: " + c1n, col2X, mY, pFL);
            mY += TS_BODY * 1.8f;
        } else {
            float boxH = 70f;
            Paint boxP = new Paint(); boxP.setColor(Color.parseColor("#AAAAAA")); boxP.setStyle(Paint.Style.STROKE); boxP.setStrokeWidth(0.7f);
            mCv.drawRect(ML, mY, ML + colW, mY + boxH, boxP);
            if (signatureView != null && signatureView.hasFirma()) {
                Bitmap sig = signatureView.toBitmap();
                if (sig != null && sig.getWidth() > 0)
                    mCv.drawBitmap(sig, null, new RectF(col2X, mY, col2X + colW, mY + boxH), new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG));
            }
            mCv.drawRect(col2X, mY, col2X + colW, mY + boxH, boxP);
            mY += boxH + 8f;
            mCv.drawText("Soluciones Villasan S.L.", ML, mY + TS_BODY, pFL);
            mCv.drawText("Nombre y Apellidos: " + c1n, col2X, mY + TS_BODY, pFL);
            mY += TS_BODY * 1.8f;
        }
    }

    private void pieLOPD() {
        gap(8f); checkPage(20f);
        Paint sep = new Paint(); sep.setColor(C_BLACK); sep.setStrokeWidth(0.4f);
        mCv.drawLine(ML, mY, ML + TW, mY, sep); gap(4f);
        Paint p = paint(TS_PIE, C_BLACK, false, Paint.Align.LEFT);
        String lopd = "Para recibir información sobre los procedimientos de resolución de conflictos puede llamar a nuestro número de atención al cliente 949492903 o enviar un e-Mail a casablancatuhogar@gmail.com.  Los datos de carácter personal facilitados en este documento serán incluidos en un fichero del que es responsable la sociedad referenciada en el encabezamiento, los cuales serán utilizados para la gestión de servicios inherentes y complementarios a las actividades de intermediación inmobiliaria en franquicia y, en su caso, de intermediación para la celebración de contratos de préstamo o crédito, así como de seguro.";
        float lineH = TS_PIE * 1.35f;
        for (String l : wrapText(lopd, p, TW)) { checkPage(lineH); mCv.drawText(l, ML, mY + TS_PIE, p); mY += lineH; }
    }

    private void cabecera() {
        if (mLogo != null) mCv.drawBitmap(mLogo, null, new RectF(ML, mY, ML + LOGO_W, mY + LOGO_H), null);
        float cx = ML + LOGO_W + 8f + (TW - LOGO_W - 8f) / 2f;
        mCv.drawText("SOLUCIONES VILLASAN S.L.         N.I.F.  B75484741", cx, mY + TS_CAB + 2f, paint(TS_CAB, C_BLACK, false, Paint.Align.CENTER));
        mCv.drawText("Avenida de Beleña Nº16 local 1 - Guadalajara – Telf.: 949410344", cx, mY + TS_CAB + 14f, paint(10f, C_BLACK, false, Paint.Align.CENTER));
        mCv.drawText("casablancaaguasvivas@gmail.com   www.casablancainmobiliarias.com", cx, mY + TS_CAB + 25f, paint(TS_WEB, C_BLUE, false, Paint.Align.CENTER));
        mY += LOGO_H + 4f;
        Paint sep = new Paint(); sep.setColor(C_BLACK); sep.setStrokeWidth(0.5f);
        mCv.drawLine(ML, mY, ML + TW, mY, sep); mY += 6f;
    }

    private void abrirPagina() {
        PdfDocument.PageInfo info = new PdfDocument.PageInfo.Builder((int)PW, (int)PH, mPageNum++).create();
        mPage = mDoc.startPage(info); mCv = mPage.getCanvas(); mY = 36f;
        if (mPageNum > 2) { cabecera(); gap(4f); }
    }

    private void checkPage(float needed) {
        if (mY + needed > PH - MB) { mDoc.finishPage(mPage); abrirPagina(); }
    }

    private void textLine(String txt, Paint p, float x) {
        float h = p.getTextSize() * 1.3f; checkPage(h);
        mCv.drawText(txt, x, mY + p.getTextSize(), p); mY += h;
    }

    private void parrafo(String texto, Paint p) {
        if (texto == null || texto.isEmpty()) return;
        float lineH = p.getTextSize() * 1.4f;
        for (String l : wrapText(texto, p, TW)) { checkPage(lineH); mCv.drawText(l, ML, mY + p.getTextSize(), p); mY += lineH; }
    }

    private void itemLista(String texto, Paint p) {
        float lineH = p.getTextSize() * 1.4f;
        List<String> lineas = wrapText(texto, p, TW - 10f);
        boolean first = true;
        for (String l : lineas) { checkPage(lineH); mCv.drawText(l, ML + (first ? 0f : 10f), mY + p.getTextSize(), p); mY += lineH; first = false; }
    }

    private void gap(float pts) { mY += pts; }

    private List<String> wrapText(String texto, Paint p, float maxW) {
        List<String> r = new ArrayList<>();
        if (texto == null || texto.isEmpty()) return r;
        String[] words = texto.split(" ", -1); StringBuilder cur = new StringBuilder();
        for (String w : words) {
            String test = cur.length() == 0 ? w : cur + " " + w;
            if (p.measureText(test) <= maxW) { cur = new StringBuilder(test); }
            else { if (cur.length() > 0) r.add(cur.toString()); cur = new StringBuilder(w); }
        }
        if (cur.length() > 0) r.add(cur.toString());
        return r;
    }

    private Paint paint(float size, int color, boolean bold, Paint.Align align) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG); p.setTextSize(size); p.setColor(color); p.setFakeBoldText(bold); p.setTextAlign(align); return p;
    }

    private String fill(String val, String placeholder) { return (val == null || val.isEmpty()) ? placeholder : val; }

    private String s2(Map<String, Object> m, String k) { Object v = m.get(k); return v != null ? String.valueOf(v) : ""; }

    private void compartirPDF(File file) {
        Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".provider", file);
        Intent i = new Intent(Intent.ACTION_SEND); i.setType("application/pdf");
        i.putExtra(Intent.EXTRA_SUBJECT, "Nota de Encargo de Compraventa");
        i.putExtra(Intent.EXTRA_TEXT, "Adjunto nota de encargo firmada.");
        i.putExtra(Intent.EXTRA_STREAM, uri); i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(i, "Compartir PDF"));
    }

    private Bitmap scaleBitmapHQ(Bitmap src, int targetW, int targetH) {
        Bitmap current = src; int w = current.getWidth(), h = current.getHeight();
        while (w > targetW * 2 || h > targetH * 2) {
            w = Math.max(w / 2, targetW); h = Math.max(h / 2, targetH);
            Bitmap step = Bitmap.createScaledBitmap(current, w, h, true);
            if (current != src) current.recycle(); current = step;
        }
        Bitmap result = Bitmap.createScaledBitmap(current, targetW, targetH, true);
        if (current != src) current.recycle(); return result;
    }
}