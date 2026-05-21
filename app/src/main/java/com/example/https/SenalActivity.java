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
import android.util.Base64;
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

public class SenalActivity extends AppCompatActivity {

    // ── Logo Base64 ──────────────────────────────────────────────────────────
    private static final String[] LOGO_B64 = {
            "iVBORw0KGgoAAAANSUhEUgAAAIIAAABFCAYAAACG7j7xAABDCklEQVR42u29d7xlVXn//55rl7NP"
    };

    private Bitmap getLogoBitmap() {
        StringBuilder sb = new StringBuilder();
        for (String s : LOGO_B64) sb.append(s);
        byte[] bytes = Base64.decode(sb.toString(), Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    // ── Medidas A4 ────────────────────────────────────────────
    private static final float PW = 595f, PH = 842f;
    private static final float ML = 36f, MR = 36f;
    private static final float TW = PW - ML - MR;
    private static final float MB = 40f;
    private static final float LOGO_W = 77f, LOGO_H = 58f;
    private static final float TS_CAB = 12f, TS_WEB = 9f, TS_BODY = 11.5f, TS_PIE = 8f;
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

    // ── Tipo de documento ────────────────────────────────────
    private int tipoDoc = 0; // 0 = Recibo  1 = Devolución

    // ── Vistas ────────────────────────────────────────────────
    RadioGroup  rgTipo;
    RadioButton rbRecibo, rbDevolucion;

    // Comunes
    EditText etNombreDNI, etDomicilio, etDNI;

    // Recibo
    LinearLayout layoutRecibo;
    EditText etImporteEfectivo, etImporteTransferencia, etPlazoHoras;
    EditText etDireccionInmueble, etVendedorNombre, etCuentaBancaria, etObservaciones;

    // Devolución
    LinearLayout layoutDevolucion;
    EditText etFechaDevolucion, etImporteEfectivoDiv, etPlazoTransDev;
    EditText etImporteTransDev, etCuentaDevolucion, etFechaSenal, etDireccionDev;

    // Firma lugar/fecha
    EditText etLugar, etFirmaDia, etFirmaMes, etFirmaAnio;

    // Firma
    SignatureView signatureView;
    Button btnBorrarFirma, btnVerFirma, btnGuardar;

    // ── Lista ──────────────────────────────────────────────────
    Button btnFiltroHoy, btnFiltroAyer, btnFiltroMes, btnFiltroTodas;
    Spinner spFiltroAsesor;
    RecyclerView rvSenales;
    GenericListAdapter listAdapter;
    List<GenericItem> listaSenales = new ArrayList<>();

    // ── Firebase ────────────────────────────────────────────────
    FirebaseFirestore db;
    String userId;
    boolean esAdmin = false;

    // ── Edición ─────────────────────────────────────────────────
    String docIdEditando = null;
    Map<String, String> mapaAsesores = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_senal);
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
        rgTipo       = findViewById(R.id.rgTipo);
        rbRecibo     = findViewById(R.id.rbRecibo);
        rbDevolucion = findViewById(R.id.rbDevolucion);

        etNombreDNI = findViewById(R.id.etNombreDNI);
        etDomicilio = findViewById(R.id.etDomicilio);
        etDNI       = findViewById(R.id.etDNI);

        layoutRecibo          = findViewById(R.id.layoutRecibo);
        etImporteEfectivo     = findViewById(R.id.etImporteEfectivo);
        etImporteTransferencia= findViewById(R.id.etImporteTransferencia);
        etPlazoHoras          = findViewById(R.id.etPlazoHoras);
        etDireccionInmueble   = findViewById(R.id.etDireccionInmueble);
        etVendedorNombre      = findViewById(R.id.etVendedorNombre);
        etCuentaBancaria      = findViewById(R.id.etCuentaBancaria);
        etObservaciones       = findViewById(R.id.etObservaciones);

        layoutDevolucion    = findViewById(R.id.layoutDevolucion);
        etFechaDevolucion   = findViewById(R.id.etFechaDevolucion);
        etImporteEfectivoDiv= findViewById(R.id.etImporteEfectivoDiv);
        etPlazoTransDev     = findViewById(R.id.etPlazoTransDev);
        etImporteTransDev   = findViewById(R.id.etImporteTransDev);
        etCuentaDevolucion  = findViewById(R.id.etCuentaDevolucion);
        etFechaSenal        = findViewById(R.id.etFechaSenal);
        etDireccionDev      = findViewById(R.id.etDireccionDev);

        etLugar    = findViewById(R.id.etLugar);
        etFirmaDia = findViewById(R.id.etFirmaDia);
        etFirmaMes = findViewById(R.id.etFirmaMes);
        etFirmaAnio= findViewById(R.id.etFirmaAnio);

        signatureView  = findViewById(R.id.signatureView);
        btnBorrarFirma = findViewById(R.id.btnBorrarFirma);
        btnVerFirma    = findViewById(R.id.btnVerFirma);
        btnGuardar     = findViewById(R.id.btnGuardar);

        btnBorrarFirma.setOnClickListener(v -> signatureView.clear());
        btnVerFirma.setOnClickListener(v ->
                Toast.makeText(this,
                        signatureView.hasFirma() ? "Firma capturada ✓" : "Sin firma todavía",
                        Toast.LENGTH_SHORT).show());

        rgTipo.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbRecibo) {
                tipoDoc = 0;
                layoutRecibo.setVisibility(View.VISIBLE);
                layoutDevolucion.setVisibility(View.GONE);
            } else {
                tipoDoc = 1;
                layoutRecibo.setVisibility(View.GONE);
                layoutDevolucion.setVisibility(View.VISIBLE);
            }
        });

        btnGuardar.setOnClickListener(v -> guardar());
    }

    private void bindLista() {
        btnFiltroHoy   = findViewById(R.id.btnFiltroHoy);
        btnFiltroAyer  = findViewById(R.id.btnFiltroAyer);
        btnFiltroMes   = findViewById(R.id.btnFiltroMes);
        btnFiltroTodas = findViewById(R.id.btnFiltroTodas);
        spFiltroAsesor = findViewById(R.id.spFiltroAsesor);
        rvSenales      = findViewById(R.id.rvSenales);

        listAdapter = new GenericListAdapter(listaSenales, new GenericListAdapter.OnItemListener() {
            @Override public void onEditar(GenericItem item, int position)   { cargarEnFormulario(item); }
            @Override public void onEliminar(GenericItem item, int position) { confirmarEliminacion(item, position); }
        });
        rvSenales.setLayoutManager(new LinearLayoutManager(this));
        rvSenales.setAdapter(listAdapter);
        rvSenales.setNestedScrollingEnabled(false);

        btnFiltroHoy.setOnClickListener(v   -> cargarSenales("hoy"));
        btnFiltroAyer.setOnClickListener(v  -> cargarSenales("ayer"));
        btnFiltroMes.setOnClickListener(v   -> cargarSenales("mes"));
        btnFiltroTodas.setOnClickListener(v -> cargarSenales("todas"));

        spFiltroAsesor.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                cargarSenales("todas");
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    // ═════════════════════════════════════════════════════════
    // ROL Y ASESORES
    // ═════════════════════════════════════════════════════════

    private void comprobarRol() {
        db.collection("usuarios").whereEqualTo("uid", userId).get()
                .addOnSuccessListener(query -> {
                    if (!query.isEmpty()) {
                        String rol = query.getDocuments().get(0).getString("rol");
                        esAdmin = "admin".equals(rol);
                    }
                    spFiltroAsesor.setVisibility(esAdmin ? View.VISIBLE : View.GONE);
                    cargarSenales("hoy");
                });
    }

    private void cargarAsesores() {
        db.collection("usuarios").get().addOnSuccessListener(query -> {
            mapaAsesores.clear();
            List<String> listaAsesorNombres = new ArrayList<>();
            listaAsesorNombres.add("Todos");

            for (QueryDocumentSnapshot doc : query) {
                String uid      = doc.getString("uid");
                String telefono = doc.getString("telefono");
                if (uid != null && telefono != null) {
                    mapaAsesores.put(uid, telefono);
                    listaAsesorNombres.add(telefono);
                }
            }

            if (esAdmin) {
                ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                        android.R.layout.simple_spinner_item, listaAsesorNombres);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spFiltroAsesor.setAdapter(adapter);
            }
        });
    }

    // ═════════════════════════════════════════════════════════
    // GUARDAR / ACTUALIZAR
    // ═════════════════════════════════════════════════════════

    private void guardar() {
        if (val(etNombreDNI).isEmpty()) {
            etNombreDNI.setError("Obligatorio");
            etNombreDNI.requestFocus();
            return;
        }

        if (docIdEditando == null && !signatureView.hasFirma()) {
            Toast.makeText(this, "Añade la firma del cliente antes de guardar", Toast.LENGTH_LONG).show();
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("userId",    userId);
        data.put("timestamp", Timestamp.now());
        data.put("tipo",      tipoDoc == 0 ? "recibo" : "devolucion");
        data.put("nombre_dni", val(etNombreDNI));
        data.put("domicilio",  val(etDomicilio));
        data.put("dni",        val(etDNI));

        if (tipoDoc == 0) {
            data.put("importe_efectivo",     val(etImporteEfectivo));
            data.put("importe_transferencia",val(etImporteTransferencia));
            data.put("plazo_horas",          val(etPlazoHoras));
            data.put("direccion_inmueble",   val(etDireccionInmueble));
            data.put("vendedor_nombre",      val(etVendedorNombre));
            data.put("cuenta_bancaria",      val(etCuentaBancaria));
            data.put("observaciones",        val(etObservaciones));
        } else {
            data.put("fecha_devolucion",    val(etFechaDevolucion));
            data.put("importe_efectivo_dev",val(etImporteEfectivoDiv));
            data.put("plazo_trans_dev",     val(etPlazoTransDev));
            data.put("importe_trans_dev",   val(etImporteTransDev));
            data.put("cuenta_devolucion",   val(etCuentaDevolucion));
            data.put("fecha_senal",         val(etFechaSenal));
            data.put("direccion_dev",       val(etDireccionDev));
        }

        data.put("lugar",      val(etLugar));
        data.put("firma_dia",  val(etFirmaDia));
        data.put("firma_mes",  val(etFirmaMes));
        data.put("firma_anio", val(etFirmaAnio));

        String coleccion = "senales";

        if (docIdEditando != null) {
            db.collection(coleccion).document(docIdEditando)
                    .update(data)
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(this, "✔ Documento actualizado", Toast.LENGTH_SHORT).show();
                        limpiarFormulario();
                        cargarSenales("todas");
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "❌ " + e.getMessage(), Toast.LENGTH_LONG).show());
        } else {
            db.collection(coleccion).add(data)
                    .addOnSuccessListener(ref -> {
                        File pdf = buildPDF(data);
                        compartirPDF(pdf);
                        Toast.makeText(this,
                                (tipoDoc == 0 ? "Recibo" : "Devolución") + " guardada ✅",
                                Toast.LENGTH_SHORT).show();
                        limpiarFormulario();
                        cargarSenales("hoy");
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "❌ " + e.getMessage(), Toast.LENGTH_LONG).show());
        }
    }

    // ═════════════════════════════════════════════════════════
    // CARGA LISTA CON FILTROS
    // ═════════════════════════════════════════════════════════

    private void cargarSenales(String filtro) {
        Query query = esAdmin
                ? db.collection("senales")
                : db.collection("senales").whereEqualTo("userId", userId);

        query.get().addOnSuccessListener(snapshots -> {
            listaSenales.clear();

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

                boolean pasaFecha = "todas".equals(filtro) || (ts.after(dInicio) && ts.before(dFin));
                if (!pasaFecha) continue;

                String uidCreador    = doc.getString("userId");
                String asesorReal    = mapaAsesores.get(uidCreador);
                String asesorVisible = asesorReal != null ? asesorReal : "Sin asesor";

                String nombre  = doc.getString("nombre_dni");
                String tipo    = doc.getString("tipo");
                String tipoLabel = "devolucion".equals(tipo) ? "DEVOLUCIÓN" : "RECIBO";

                String titulo    = nombre != null ? nombre : "—";
                String subtitulo = tipoLabel + " · " + asesorVisible;

                String importe = "devolucion".equals(tipo)
                        ? doc.getString("importe_trans_dev")
                        : doc.getString("importe_efectivo");

                GenericItem item = new GenericItem(
                        doc.getId(), titulo, subtitulo,
                        importe, uidCreador, doc.getData(), ts, asesorVisible);
                listaSenales.add(item);
            }

            if (esAdmin && spFiltroAsesor.getSelectedItem() != null) {
                String seleccionado = spFiltroAsesor.getSelectedItem().toString();
                if (!seleccionado.equals("Todos")) {
                    List<GenericItem> filtradas = new ArrayList<>();
                    for (GenericItem item : listaSenales) {
                        if (seleccionado.equalsIgnoreCase(item.asesor)) {
                            filtradas.add(item);
                        }
                    }
                    listaSenales.clear();
                    listaSenales.addAll(filtradas);
                }
            }

            Collections.sort(listaSenales, (a, b) -> {
                if (a.timestamp == null || b.timestamp == null) return 0;
                return b.timestamp.compareTo(a.timestamp);
            });

            listAdapter.notifyDataSetChanged();
            if (listaSenales.isEmpty())
                Toast.makeText(this, "Sin documentos para este filtro", Toast.LENGTH_SHORT).show();

        }).addOnFailureListener(e ->
                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // ═════════════════════════════════════════════════════════
    // CARGAR EN FORMULARIO (EDICIÓN)
    // ═════════════════════════════════════════════════════════

    private void cargarEnFormulario(GenericItem item) {
        docIdEditando = item.docId;
        db.collection("senales").document(item.docId).get()
                .addOnSuccessListener(doc -> {
                    String tipo = doc.getString("tipo");

                    if ("devolucion".equals(tipo)) {
                        tipoDoc = 1;
                        rbDevolucion.setChecked(true);
                        layoutRecibo.setVisibility(View.GONE);
                        layoutDevolucion.setVisibility(View.VISIBLE);
                    } else {
                        tipoDoc = 0;
                        rbRecibo.setChecked(true);
                        layoutRecibo.setVisibility(View.VISIBLE);
                        layoutDevolucion.setVisibility(View.GONE);
                    }

                    etNombreDNI.setText(s(doc, "nombre_dni"));
                    etDomicilio.setText(s(doc, "domicilio"));
                    etDNI.setText(s(doc, "dni"));

                    if (tipoDoc == 0) {
                        etImporteEfectivo.setText(s(doc, "importe_efectivo"));
                        etImporteTransferencia.setText(s(doc, "importe_transferencia"));
                        etPlazoHoras.setText(s(doc, "plazo_horas"));
                        etDireccionInmueble.setText(s(doc, "direccion_inmueble"));
                        etVendedorNombre.setText(s(doc, "vendedor_nombre"));
                        etCuentaBancaria.setText(s(doc, "cuenta_bancaria"));
                        etObservaciones.setText(s(doc, "observaciones"));
                    } else {
                        etFechaDevolucion.setText(s(doc, "fecha_devolucion"));
                        etImporteEfectivoDiv.setText(s(doc, "importe_efectivo_dev"));
                        etPlazoTransDev.setText(s(doc, "plazo_trans_dev"));
                        etImporteTransDev.setText(s(doc, "importe_trans_dev"));
                        etCuentaDevolucion.setText(s(doc, "cuenta_devolucion"));
                        etFechaSenal.setText(s(doc, "fecha_senal"));
                        etDireccionDev.setText(s(doc, "direccion_dev"));
                    }

                    etLugar.setText(s(doc, "lugar"));
                    etFirmaDia.setText(s(doc, "firma_dia"));
                    etFirmaMes.setText(s(doc, "firma_mes"));
                    etFirmaAnio.setText(s(doc, "firma_anio"));

                    btnGuardar.setText("💾 Actualizar documento");

                    try {
                        findViewById(R.id.scrollSenal).scrollTo(0, 0);
                    } catch (Exception ignored) {}

                    Toast.makeText(this, "Editando: " + item.titulo, Toast.LENGTH_SHORT).show();
                });
    }

    // ═════════════════════════════════════════════════════════
    // ELIMINAR
    // ═════════════════════════════════════════════════════════

    private void confirmarEliminacion(GenericItem item, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar documento")
                .setMessage("¿Eliminar el documento de " + item.titulo + "?\nEsta acción no se puede deshacer.")
                .setPositiveButton("🗑 Eliminar", (d, w) -> {
                    db.collection("senales").document(item.docId)
                            .delete()
                            .addOnSuccessListener(unused -> {
                                listAdapter.remove(position);
                                Toast.makeText(this, "Documento eliminado ✓", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Error al eliminar", Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // ═════════════════════════════════════════════════════════
    // HELPERS FORMULARIO
    // ═════════════════════════════════════════════════════════

    private void limpiarFormulario() {
        docIdEditando = null;
        btnGuardar.setText("GUARDAR Y GENERAR PDF");
        rbRecibo.setChecked(true);
        tipoDoc = 0;
        layoutRecibo.setVisibility(View.VISIBLE);
        layoutDevolucion.setVisibility(View.GONE);

        etNombreDNI.setText(""); etDomicilio.setText(""); etDNI.setText("");
        etImporteEfectivo.setText(""); etImporteTransferencia.setText("");
        etPlazoHoras.setText(""); etDireccionInmueble.setText("");
        etVendedorNombre.setText(""); etCuentaBancaria.setText(""); etObservaciones.setText("");
        etFechaDevolucion.setText(""); etImporteEfectivoDiv.setText(""); etPlazoTransDev.setText("");
        etImporteTransDev.setText(""); etCuentaDevolucion.setText(""); etFechaSenal.setText(""); etDireccionDev.setText("");
        etLugar.setText(""); etFirmaDia.setText(""); etFirmaMes.setText(""); etFirmaAnio.setText("");
        signatureView.clear();
    }

    private String val(EditText et) { return et != null ? et.getText().toString().trim() : ""; }

    private String s(com.google.firebase.firestore.DocumentSnapshot doc, String key) {
        Object v = doc.get(key); return v != null ? String.valueOf(v) : "";
    }

    // ═════════════════════════════════════════════════════════
    // PDF — USANDO MISMO PATRÓN QUE ENCARGO
    // ═════════════════════════════════════════════════════════

    private File buildPDF(Map<String, Object> data) {
        try { mLogo  = BitmapFactory.decodeResource(getResources(), R.drawable.logo_casablanca); } catch (Exception e) { mLogo = null; }
        try { mSello = BitmapFactory.decodeResource(getResources(), R.drawable.sello_empresa);  } catch (Exception e) { mSello = null; }

        mDoc = new PdfDocument();
        mPageNum = 1;
        abrirPagina();

        if (tipoDoc == 0) {
            generarRecibo(data);
        } else {
            generarDevolucion(data);
        }

        mDoc.finishPage(mPage);
        File file = new File(getExternalFilesDir(null), "senal_" + System.currentTimeMillis() + ".pdf");
        try (FileOutputStream fos = new FileOutputStream(file)) { mDoc.writeTo(fos); mDoc.close(); } catch (Exception e) { e.printStackTrace(); }
        return file;
    }

    private void generarRecibo(Map<String, Object> data) {
        cabecera(); gap(20f);

        Paint pTit = paint(14f, C_BLACK, true, Paint.Align.CENTER);
        textLine("RECIBO DE SEÑAL", pTit, PW / 2f);
        gap(20f);

        Paint pB = paint(TS_BODY, C_BLACK, false, Paint.Align.LEFT);

        textLine("HEMOS RECIBIDO DE DON/DÑA.: " + fill(s2(data,"nombre_dni"),"_____________________"), pB, ML);
        gap(6f);
        textLine("CON D.N.I.: " + fill(s2(data,"dni"),"_______________________"), pB, ML);
        gap(6f);
        textLine("UN IMPORTE EN EFECTIVO POR VALOR DE: ( " + fill(s2(data,"importe_efectivo"),"__________") + " ) Euros", pB, ML);
        gap(6f);
        textLine("Y PENDIENTE DE TRANSFERENCIA EN PLAZO DE " + fill(s2(data,"plazo_horas"),"_____") + " HORAS HÁBILES", pB, ML);
        gap(6f);
        textLine("A LA CUENTA BANCARIA: " + fill(s2(data,"cuenta_bancaria"),"______________________"), pB, ML);
        gap(6f);
        textLine("LA CANTIDAD DE: ( " + fill(s2(data,"importe_transferencia"),"__________") + " ) Euros", pB, ML);
        gap(14f);

        parrafo("DICHA CANTIDAD ES ENTREGADA A SOLUCIONES VILLASAN S.L., EN CONCEPTO DE SEÑAL"
                + " POR LA COMPRAVENTA DE LA VIVIENDA SITA EN LA CALLE "
                + fill(s2(data,"direccion_inmueble"),"—"), pB);
        gap(10f);

        if (!s2(data,"vendedor_nombre").isEmpty()) {
            parrafo("VENDEDOR: " + s2(data,"vendedor_nombre"), pB);
            gap(8f);
        }

        if (!s2(data,"observaciones").isEmpty()) {
            parrafo("OBSERVACIONES: " + s2(data,"observaciones"), pB);
            gap(8f);
        }

        gap(20f);
        textLine("En " + fill(s2(data,"lugar"),"_________________________________")
                + ", a " + fill(s2(data,"firma_dia"),"______") + " de "
                + fill(s2(data,"firma_mes"),"_____________________") + " de "
                + fill(s2(data,"firma_anio"),"______") + ".", pB, ML);
        gap(20f);

        bloquesFirma(data, pB);
    }

    private void generarDevolucion(Map<String, Object> data) {
        cabecera(); gap(20f);

        Paint pTit = paint(14f, C_BLACK, true, Paint.Align.CENTER);
        textLine("DEVOLUCIÓN DE SEÑAL", pTit, PW / 2f);
        gap(20f);

        Paint pB = paint(TS_BODY, C_BLACK, false, Paint.Align.LEFT);

        parrafo("Dº " + s2(data,"nombre_dni") + " mayor de edad, con domicilio en "
                + s2(data,"domicilio") + " con D.N.I. " + s2(data,"dni"), pB);
        gap(20f);

        Paint pDec = paint(TS_BODY, C_BLACK, true, Paint.Align.CENTER);
        textLine("DECLARAN", pDec, PW / 2f);
        gap(20f);

        parrafo("Que a día " + s2(data,"fecha_devolucion")
                + " y de conformidad con el artículo 1454 del Código Civil, le ha sido entregado,"
                + " por parte de SOLUCIONES VILLASAN S.L., el importe en efectivo de "
                + s2(data,"importe_efectivo_dev") + " Euros"
                + " y se compromete que en plazo de " + s2(data,"plazo_trans_dev")
                + " horas hábiles a realizar una transferencia de "
                + s2(data,"importe_trans_dev") + " Euros"
                + " al número de cuenta de " + s2(data,"nombre_dni")
                + ", correspondiente a la señal entregada el día "
                + s2(data,"fecha_senal")
                + ", para la compra del inmueble sito en la calle "
                + s2(data,"direccion_dev") + ".", pB);
        gap(14f);

        parrafo("Las partes implicadas no podrán reclamarse recíprocamente nada en concepto alguno.", pB);
        gap(14f);

        parrafo("Y para que así conste, y a los efectos que proceda firmo por duplicado ejemplar, a continuación, en "
                + s2(data,"lugar") + ", a "
                + s2(data,"firma_dia") + " de "
                + s2(data,"firma_mes") + " de "
                + s2(data,"firma_anio") + ".", pB);
        gap(20f);

        bloquesFirma(data, pB);
    }

    private void bloquesFirma(Map<String, Object> data, Paint pB) {
        gap(14f);
        checkPage(140f);

        Paint pFL = paint(TS_BODY, C_BLACK, false, Paint.Align.LEFT);
        float colW = TW / 2f - 6f;
        float col2X = ML + TW / 2f + 5f;

        mCv.drawText("Por el Director", ML, mY + TS_BODY, pFL);
        mCv.drawText("El Cliente / El Representante", col2X, mY + TS_BODY, pFL);
        mY += TS_BODY * 1.8f;

        float selloMaxW = colW, selloMaxH = 90f;
        Paint boxP = new Paint(); boxP.setColor(Color.parseColor("#AAAAAA")); boxP.setStyle(Paint.Style.STROKE); boxP.setStrokeWidth(0.7f);

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
            if (signatureView != null && signatureView.hasFirma()) {
                Bitmap sig = signatureView.toBitmap();
                if (sig != null && sig.getWidth() > 0) {
                    mCv.drawBitmap(sig, null, new RectF(col2X, mY, col2X + colW, mY + boxH), new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG));
                }
            }
            mCv.drawRect(col2X, mY, col2X + colW, mY + boxH, boxP);
            mCv.drawText("Nombre: " + s2(data,"nombre_dni"), col2X, selloBottom + TS_BODY, pFL);
            mY = selloBottom + TS_BODY * 1.8f;
        } else {
            float boxH = 70f;
            mCv.drawRect(ML, mY, ML + colW, mY + boxH, boxP);
            if (signatureView != null && signatureView.hasFirma()) {
                Bitmap sig = signatureView.toBitmap();
                if (sig != null && sig.getWidth() > 0)
                    mCv.drawBitmap(sig, null, new RectF(col2X, mY, col2X + colW, mY + boxH), new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG));
            }
            mCv.drawRect(col2X, mY, col2X + colW, mY + boxH, boxP);
            mY += boxH + 8f;
            mCv.drawText("Soluciones Villasan S.L.", ML, mY + TS_BODY, pFL);
            mCv.drawText("Nombre: " + s2(data,"nombre_dni"), col2X, mY + TS_BODY, pFL);
            mY += TS_BODY * 1.8f;
        }
    }

    // ═════════════════════════════════════════════════════════
    // HELPERS PDF (MISMO PATRÓN QUE ENCARGO)
    // ═════════════════════════════════════════════════════════

    private void cabecera() {
        if (mLogo != null) mCv.drawBitmap(mLogo, null, new RectF(ML, mY, ML + LOGO_W, mY + LOGO_H), null);
        float cx = ML + LOGO_W + 8f + (TW - LOGO_W - 8f) / 2f;
        mCv.drawText("SOLUCIONES VILLASAN S.L.         N.I.F.  B75484741", cx, mY + TS_CAB + 2f, paint(TS_CAB, C_BLACK, false, Paint.Align.CENTER));
        mCv.drawText("Avenida de Beleña Nº16 local 1 - Guadalajara – Telf.: 949410344", cx, mY + TS_CAB + 14f, paint(10f, C_BLACK, false, Paint.Align.CENTER));
        mCv.drawText("casablancaguasvivas@gmail.com   www.casablancainmobiliarias.com", cx, mY + TS_CAB + 25f, paint(TS_WEB, C_BLUE, false, Paint.Align.CENTER));
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
        i.putExtra(Intent.EXTRA_SUBJECT, tipoDoc == 0 ? "Recibo de Señal" : "Devolución de Señal");
        i.putExtra(Intent.EXTRA_TEXT, "Adjunto documento de señal.");
        i.putExtra(Intent.EXTRA_STREAM, uri); i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(i, "Compartir PDF"));
    }
}