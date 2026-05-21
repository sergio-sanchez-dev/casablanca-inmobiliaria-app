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

public class VisitasActivity extends AppCompatActivity {

    // ── Medidas A4 ────────────────────────────────────────────
    private static final float PW = 595f, PH = 842f;
    private static final float ML = 36f, MR = 36f;
    private static final float TW = PW - ML - MR;
    private static final float MB = 40f;
    private static final float LOGO_W = 77f, LOGO_H = 58f;
    private static final float TS_CAB = 12f, TS_WEB = 9f, TS_BODY = 11.5f;
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

    // ── Formulario (parte de arriba) ─────────────────────────────────────────
    EditText etClienteNombre, etClienteDNI, etClienteTelefono, etClienteEmail;
    EditText etFecha, etHora, etInmueble, etAsesor;
    EditText etEmpresaCIF, etEmpresaNombre, etEmpresaTelefono;
    EditText etHonorarios, etComentarios, etResultado;
    EditText etLugar, etFirmaDia, etFirmaMes, etFirmaAnio;
    CheckBox checkTerminos;
    SignatureView signatureView;
    Button btnBorrarFirma, btnVerFirma, btnFirmar, btnGuardar;

    // ── Sección lista (parte de abajo) ────────────────────────────────────────
    Button btnFiltroHoy, btnFiltroAyer, btnFiltroMes, btnFiltroTodas;
    Spinner spFiltroAsesor;
    RecyclerView rvVisitas;
    GenericListAdapter listAdapter;
    List<GenericItem> listaVisitas = new ArrayList<>();

    // ── Firebase ──────────────────────────────────────────────────────────────
    FirebaseFirestore db;
    String userId;
    boolean esAdmin = false;

    // ── Edición en curso (null = nueva) ──────────────────────────────────────
    String docIdEditando = null;
    Map<String, String> mapaAsesores = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_visitas);

        db     = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        cargarAsesores();

        bindFormulario();
        bindLista();
        comprobarRol();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // BIND
    // ═════════════════════════════════════════════════════════════════════════

    private void bindFormulario() {
        etClienteNombre   = findViewById(R.id.etClienteNombre);
        etClienteDNI      = findViewById(R.id.etClienteDNI);
        etClienteTelefono = findViewById(R.id.etClienteTelefono);
        etClienteEmail    = findViewById(R.id.etClienteEmail);
        etFecha           = findViewById(R.id.etFecha);
        etHora            = findViewById(R.id.etHora);
        etInmueble        = findViewById(R.id.etInmueble);
        etAsesor          = findViewById(R.id.etAsesor);
        etEmpresaCIF      = findViewById(R.id.etEmpresaCIF);
        etEmpresaNombre   = findViewById(R.id.etEmpresaNombre);
        etEmpresaTelefono = findViewById(R.id.etEmpresaTelefono);
        etHonorarios      = findViewById(R.id.etHonorarios);
        etComentarios     = findViewById(R.id.etComentarios);
        etResultado       = findViewById(R.id.etResultado);
        etLugar           = findViewById(R.id.etLugar);
        etFirmaDia        = findViewById(R.id.etFirmaDia);
        etFirmaMes        = findViewById(R.id.etFirmaMes);
        etFirmaAnio       = findViewById(R.id.etFirmaAnio);
        checkTerminos     = findViewById(R.id.checkTerminos);
        signatureView     = findViewById(R.id.signatureView);
        btnBorrarFirma    = findViewById(R.id.btnBorrarFirma);
        btnVerFirma       = findViewById(R.id.btnVerFirma);
        btnFirmar         = findViewById(R.id.btnFirmar);
        btnGuardar        = findViewById(R.id.btnGuardar);

        btnBorrarFirma.setOnClickListener(v -> signatureView.clear());
        btnVerFirma.setOnClickListener(v ->
                Toast.makeText(this,
                        signatureView.hasFirma() ? "Firma capturada ✓" : "Sin firma todavía",
                        Toast.LENGTH_SHORT).show());
        btnFirmar.setOnClickListener(v -> {
            if (!checkTerminos.isChecked()) {
                Toast.makeText(this, "El cliente debe aceptar los términos", Toast.LENGTH_SHORT).show();
            }
        });
        btnGuardar.setOnClickListener(v -> guardarOActualizar());
    }

    private void bindLista() {
        btnFiltroHoy   = findViewById(R.id.btnFiltroHoy);
        btnFiltroAyer  = findViewById(R.id.btnFiltroAyer);
        btnFiltroMes   = findViewById(R.id.btnFiltroMes);
        btnFiltroTodas = findViewById(R.id.btnFiltroTodas);
        spFiltroAsesor = findViewById(R.id.spFiltroAsesor);
        rvVisitas      = findViewById(R.id.rvVisitas);

        listAdapter = new GenericListAdapter(listaVisitas, new GenericListAdapter.OnItemListener() {
            @Override
            public void onEditar(GenericItem item, int position) {
                cargarEnFormulario(item);
            }

            @Override
            public void onEliminar(GenericItem item, int position) {
                confirmarEliminacion(item, position);
            }
        });

        rvVisitas.setLayoutManager(new LinearLayoutManager(this));
        rvVisitas.setAdapter(listAdapter);
        rvVisitas.setNestedScrollingEnabled(false);

        btnFiltroHoy.setOnClickListener(v   -> cargarVisitas("hoy"));
        btnFiltroAyer.setOnClickListener(v  -> cargarVisitas("ayer"));
        btnFiltroMes.setOnClickListener(v   -> cargarVisitas("mes"));
        btnFiltroTodas.setOnClickListener(v -> cargarVisitas("todas"));
    }

    // ═════════════════════════════════════════════════════════════════════════
    // ROL
    // ═════════════════════════════════════════════════════════════════════════

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
                    cargarVisitas("hoy");
                });
    }

    // ═════════════════════════════════════════════════════════════════════════
    // GUARDAR / ACTUALIZAR
    // ═════════════════════════════════════════════════════════════════════════

    private void guardarOActualizar() {
        if (v(etClienteNombre).isEmpty()) {
            etClienteNombre.setError("Obligatorio"); return;
        }
        if (!checkTerminos.isChecked()) {
            Toast.makeText(this, "El cliente debe aceptar los términos", Toast.LENGTH_SHORT).show();
            return;
        }
        if (docIdEditando == null && !signatureView.hasFirma()) {
            Toast.makeText(this, "Añade la firma del cliente", Toast.LENGTH_LONG).show();
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("userId",           userId);
        data.put("timestamp",        Timestamp.now());
        data.put("cliente_nombre",   v(etClienteNombre));
        data.put("cliente_dni",      v(etClienteDNI));
        data.put("cliente_telefono", v(etClienteTelefono));
        data.put("cliente_email",    v(etClienteEmail));
        data.put("fecha",            v(etFecha));
        data.put("hora",             v(etHora));
        data.put("inmueble",         v(etInmueble));
        data.put("asesor",           v(etAsesor));
        data.put("empresa_cif",      v(etEmpresaCIF));
        data.put("empresa_nombre",   v(etEmpresaNombre));
        data.put("empresa_telefono", v(etEmpresaTelefono));
        data.put("honorarios",       v(etHonorarios));
        data.put("comentarios",      v(etComentarios));
        data.put("resultado",        v(etResultado));
        data.put("lugar",            v(etLugar));
        data.put("firma_dia",        v(etFirmaDia));
        data.put("firma_mes",        v(etFirmaMes));
        data.put("firma_anio",       v(etFirmaAnio));
        data.put("terminos_aceptados", checkTerminos.isChecked());

        if (docIdEditando != null) {
            db.collection("visitas").document(docIdEditando)
                    .update(data)
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(this, "✔ Visita actualizada", Toast.LENGTH_SHORT).show();
                        limpiarFormulario();
                        cargarVisitas("todas");
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "❌ " + e.getMessage(), Toast.LENGTH_LONG).show());
        } else {
            db.collection("visitas").add(data)
                    .addOnSuccessListener(ref -> {
                        File pdf = generarPDF(data);
                        compartirPDF(pdf);
                        Toast.makeText(this, "✅ Visita guardada", Toast.LENGTH_SHORT).show();
                        limpiarFormulario();
                        cargarVisitas("hoy");
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "❌ " + e.getMessage(), Toast.LENGTH_LONG).show());
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // CARGA DE LISTA CON FILTROS
    // ═════════════════════════════════════════════════════════════════════════
    private void cargarVisitas(String filtro) {

        Query query;

        if (esAdmin) {
            query = db.collection("visitas");
        } else {
            query = db.collection("visitas")
                    .whereEqualTo("userId", userId);
        }

        query.get().addOnSuccessListener(snapshots -> {

            listaVisitas.clear();

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

                case "todas":
                    break;
            }

            Date dInicio = calInicio.getTime();
            Date dFin    = calFin.getTime();

            for (QueryDocumentSnapshot doc : snapshots) {

                Date ts = doc.getDate("timestamp");
                if (ts == null) continue;

                boolean pasa = true;

                if (!"todas".equals(filtro)) {
                    pasa = ts.after(dInicio) && ts.before(dFin);
                }

                if (!pasa) continue;

                String uidCreador = doc.getString("userId");
                String asesorReal = mapaAsesores.get(uidCreador);
                String asesorVisible = asesorReal != null ? asesorReal : "Sin asesor";

                String cliente  = doc.getString("cliente_nombre");
                String fecha    = doc.getString("fecha");
                String resultado = doc.getString("resultado");

                String titulo    = cliente != null ? cliente : "—";
                String subtitulo = (fecha != null ? fecha : "") + " · " + asesorVisible;

                GenericItem item = new GenericItem(
                        doc.getId(),
                        titulo,
                        subtitulo,
                        resultado,
                        uidCreador,
                        doc.getData(),
                        ts,
                        asesorVisible
                );

                listaVisitas.add(item);
            }

            if (esAdmin && spFiltroAsesor.getSelectedItem() != null) {
                String asesorSel = spFiltroAsesor.getSelectedItem().toString();

                if (!asesorSel.equals("Todos")) {

                    List<GenericItem> filtradas = new ArrayList<>();

                    for (GenericItem vi : listaVisitas) {
                        if (asesorSel.equalsIgnoreCase(vi.asesor)) {
                            filtradas.add(vi);
                        }
                    }

                    listaVisitas.clear();
                    listaVisitas.addAll(filtradas);
                }
            }

            Collections.sort(listaVisitas, (a, b) -> {
                if (a.timestamp == null || b.timestamp == null) return 0;
                return b.timestamp.compareTo(a.timestamp);
            });

            listAdapter.notifyDataSetChanged();

            if (listaVisitas.isEmpty()) {
                Toast.makeText(this, "Sin visitas para este filtro", Toast.LENGTH_SHORT).show();
            }

        }).addOnFailureListener(e ->
                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show()
        );
    }

    // ═════════════════════════════════════════════════════════════════════════
    // CARGAR EN FORMULARIO PARA EDITAR
    // ═════════════════════════════════════════════════════════════════════════

    private void cargarEnFormulario(GenericItem item) {
        docIdEditando = item.docId;

        db.collection("visitas").document(item.docId)
                .get()
                .addOnSuccessListener(doc -> {
                    etClienteNombre.setText(s(doc, "cliente_nombre"));
                    etClienteDNI.setText(s(doc, "cliente_dni"));
                    etClienteTelefono.setText(s(doc, "cliente_telefono"));
                    etClienteEmail.setText(s(doc, "cliente_email"));
                    etFecha.setText(s(doc, "fecha"));
                    etHora.setText(s(doc, "hora"));
                    etInmueble.setText(s(doc, "inmueble"));
                    etAsesor.setText(s(doc, "asesor"));
                    etEmpresaCIF.setText(s(doc, "empresa_cif"));
                    etEmpresaNombre.setText(s(doc, "empresa_nombre"));
                    etEmpresaTelefono.setText(s(doc, "empresa_telefono"));
                    etHonorarios.setText(s(doc, "honorarios"));
                    etComentarios.setText(s(doc, "comentarios"));
                    etResultado.setText(s(doc, "resultado"));
                    etLugar.setText(s(doc, "lugar"));
                    etFirmaDia.setText(s(doc, "firma_dia"));
                    etFirmaMes.setText(s(doc, "firma_mes"));
                    etFirmaAnio.setText(s(doc, "firma_anio"));

                    Boolean terminos = doc.getBoolean("terminos_aceptados");
                    checkTerminos.setChecked(terminos != null && terminos);

                    btnGuardar.setText("💾 Actualizar visita");

                    findViewById(R.id.scrollVisitas).scrollTo(0, 0);

                    Toast.makeText(this, "Editando visita de " + item.titulo,
                            Toast.LENGTH_SHORT).show();
                });
    }

    // ═════════════════════════════════════════════════════════════════════════
    // ELIMINAR
    // ═════════════════════════════════════════════════════════════════════════

    private void confirmarEliminacion(GenericItem item, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar visita")
                .setMessage("¿Eliminar la visita de " + item.titulo + "?\nEsta acción no se puede deshacer.")
                .setPositiveButton("🗑 Eliminar", (d, w) -> {
                    db.collection("visitas").document(item.docId)
                            .delete()
                            .addOnSuccessListener(unused -> {
                                listAdapter.remove(position);
                                Toast.makeText(this, "Visita eliminada", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Error al eliminar", Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // HELPERS
    // ═════════════════════════════════════════════════════════════════════════

    private void limpiarFormulario() {
        docIdEditando = null;
        btnGuardar.setText("✅ Guardar visita");
        etClienteNombre.setText(""); etClienteDNI.setText("");
        etClienteTelefono.setText(""); etClienteEmail.setText("");
        etFecha.setText(""); etHora.setText("");
        etInmueble.setText(""); etAsesor.setText("");
        etEmpresaCIF.setText(""); etEmpresaNombre.setText("");
        etEmpresaTelefono.setText(""); etHonorarios.setText("");
        etComentarios.setText(""); etResultado.setText("");
        etLugar.setText(""); etFirmaDia.setText("");
        etFirmaMes.setText(""); etFirmaAnio.setText("");
        checkTerminos.setChecked(false);
        signatureView.clear();
    }

    private String v(EditText et) {
        return et != null ? et.getText().toString().trim() : "";
    }

    private String s(com.google.firebase.firestore.DocumentSnapshot doc, String key) {
        Object val = doc.get(key);
        return val != null ? String.valueOf(val) : "";
    }

    // ═════════════════════════════════════════════════════════════════════════
    // PDF — CABECERA PERFECTA + CONTENIDO (PUEDE SER MULTIPÁGINA)
    // ═════════════════════════════════════════════════════════════════════════

    private File generarPDF(Map<String, Object> raw) {
        try { mLogo  = BitmapFactory.decodeResource(getResources(), R.drawable.logo_casablanca); } catch (Exception e) { mLogo = null; }
        try { mSello = BitmapFactory.decodeResource(getResources(), R.drawable.sello_empresa);  } catch (Exception e) { mSello = null; }

        mDoc = new PdfDocument();
        mPageNum = 1;
        abrirPagina();
        cabecera();
        gap(8f);

        Paint pTit = paint(14f, C_BLACK, true, Paint.Align.CENTER);
        mCv.drawText("PARTE DE VISITA", PW / 2f, mY, pTit);
        float tw = pTit.measureText("PARTE DE VISITA");
        Paint ul = new Paint(); ul.setColor(C_BLACK); ul.setStrokeWidth(0.8f);
        mCv.drawLine((PW - tw) / 2f, mY + 2f, (PW + tw) / 2f, mY + 2f, ul);
        gap(12f);

        Paint pB = paint(TS_BODY, C_BLACK, false, Paint.Align.LEFT);

        parrafo("Esta visita ha sido realizada por: " + fill(s2(raw, "asesor"), "........................................"), pB);
        gap(8f);

        parrafo("Cliente: " + fill(s2(raw, "cliente_nombre"), "........................................"), pB);
        parrafo("D./Dña " + fill(s2(raw, "cliente_nombre"), "........................................"), pB);
        parrafo("Telf. " + fill(s2(raw, "cliente_telefono"), "........................................"), pB);
        parrafo("Con D.N.I. " + fill(s2(raw, "cliente_dni"), "........................................"), pB);
        gap(8f);

        parrafo("Inmueble visitado:", pB);
        parrafo(fill(s2(raw, "inmueble"), ".............................................................................."), pB);
        gap(8f);

        parrafo("Si el cliente visitante representa alguna empresa especificar:", pB);
        parrafo("CIF: " + fill(s2(raw, "empresa_cif"), "........................................"), pB);
        parrafo("Nombre de la Empresa: " + fill(s2(raw, "empresa_nombre"), "........................................"), pB);
        parrafo("Teléfono: " + fill(s2(raw, "empresa_telefono"), "........................................"), pB);
        gap(10f);

        // ── PÁRRAFO LEGAL CON HONORARIOS DINÁMICOS ────────────────────────────
        parrafo(
                "El cliente declara que ha visitado el inmueble con esta agencia y que no lo había visitado antes. " +
                        "La visita del inmueble no tiene ningún costo ni honorario para el cliente, pero si el cliente desea " +
                        "comprarlo o alquilarlo (directamente o a través de familiares o empresas relacionadas) se compromete " +
                        "a realizarlo a través de esta agencia abonando el importe de " +
                        fill(s2(raw, "honorarios"), "4000€ más IVA") +
                        " en el momento que se considere aceptada una propuesta de compra.",
                pB
        );
        gap(12f);

        parrafo(
                "En " + fill(s2(raw, "lugar"), "..........................") +
                        " a las " + fill(s2(raw, "hora"), "......") +
                        " del " + fill(s2(raw, "firma_dia"), "..") +
                        " de " + fill(s2(raw, "firma_mes"), "....................") +
                        " de " + fill(s2(raw, "firma_anio"), "...."),
                pB
        );
        gap(25f);

        bloquesFirmaConSello(raw);

        return cerrarPDF("visita_");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BLOQUE FIRMAS CON SELLO
    // ─────────────────────────────────────────────────────────────────────────
    private void bloquesFirmaConSello(Map<String, Object> raw) {
        checkPage(95f);

        float selloMaxW = TW / 2f - 5f;
        float selloMaxH = 70f;

        Paint pLabel = paint(TS_BODY - 1f, C_BLACK, false, Paint.Align.LEFT);
        Paint pName  = paint(TS_BODY - 1.5f, C_BLACK, false, Paint.Align.LEFT);
        Paint boxP   = new Paint();
        boxP.setColor(Color.parseColor("#AAAAAA"));
        boxP.setStyle(Paint.Style.STROKE);
        boxP.setStrokeWidth(0.7f);

        // Etiquetas
        mCv.drawText("Por el Director", ML, mY + TS_BODY - 1.5f, pLabel);
        mCv.drawText("El Cliente", ML + selloMaxW + 12f, mY + TS_BODY - 1.5f, pLabel);
        mY += (TS_BODY - 1.5f) * 1.5f;

        if (mSello != null) {
            float ratio = (float) mSello.getWidth() / mSello.getHeight();
            float sW, sH;
            if (ratio >= 1f) {
                sW = selloMaxW;
                sH = sW / ratio;
                if (sH > selloMaxH) { sH = selloMaxH; sW = sH * ratio; }
            } else {
                sH = selloMaxH;
                sW = sH * ratio;
                if (sW > selloMaxW) { sW = selloMaxW; sH = sW / ratio; }
            }

            RectF destSello = new RectF(ML, mY, ML + sW, mY + sH);
            mCv.drawBitmap(mSello, null, destSello, new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG));

            float boxH = sH;

            if (signatureView != null && signatureView.hasFirma()) {
                Bitmap sig = signatureView.toBitmap();
                if (sig != null && sig.getWidth() > 0) {
                    mCv.drawBitmap(sig, null,
                            new RectF(ML + selloMaxW + 12f, mY, ML + selloMaxW + 12f + selloMaxW, mY + boxH),
                            new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG));
                }
            } else {
                mCv.drawRect(ML + selloMaxW + 12f, mY, ML + selloMaxW + 12f + selloMaxW, mY + boxH, boxP);
            }

            float bottom = mY + sH + 3f;
            mCv.drawText("Soluciones Villasan S.L.", ML, bottom + TS_BODY, pName);
            String clienteName = s2(raw, "cliente_nombre");
            mCv.drawText("Nombre: " + clienteName, ML + selloMaxW + 12f, bottom + TS_BODY, pName);
            mY = bottom + TS_BODY * 1.5f;
        } else {
            float boxH = 60f;
            mCv.drawRect(ML, mY, ML + selloMaxW, mY + boxH, boxP);

            if (signatureView != null && signatureView.hasFirma()) {
                Bitmap sig = signatureView.toBitmap();
                if (sig != null && sig.getWidth() > 0) {
                    mCv.drawBitmap(sig, null,
                            new RectF(ML + selloMaxW + 12f, mY, ML + selloMaxW + 12f + selloMaxW, mY + boxH),
                            new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG));
                }
            }
            mCv.drawRect(ML + selloMaxW + 12f, mY, ML + selloMaxW + 12f + selloMaxW, mY + boxH, boxP);
            mY += boxH + 5f;

            mCv.drawText("Soluciones Villasan S.L.", ML, mY + TS_BODY, pName);
            String clienteName = s2(raw, "cliente_nombre");
            mCv.drawText("Nombre: " + clienteName, ML + selloMaxW + 12f, mY + TS_BODY, pName);
            mY += TS_BODY * 1.5f;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS PDF
    // ─────────────────────────────────────────────────────────────────────────

    private void cabecera() {
        if (mLogo != null) mCv.drawBitmap(mLogo, null, new RectF(ML, mY, ML + LOGO_W, mY + LOGO_H), null);
        float cx = ML + LOGO_W + 8f + (TW - LOGO_W - 8f) / 2f;
        mCv.drawText("SOLUCIONES VILLASAN S.L.         N.I.F.  B75484741", cx, mY + TS_CAB + 2f, paint(TS_CAB, C_BLACK, false, Paint.Align.CENTER));
        mCv.drawText("Avenida de Beleña Nº16 local 1 - Guadalajara – Telf.: 949410344", cx, mY + TS_CAB + 14f, paint(10f, C_BLACK, false, Paint.Align.CENTER));
        mCv.drawText("casablancaguasvivas@gmail.com   www.casablancainmobiliarias.com", cx, mY + TS_CAB + 25f, paint(TS_WEB, C_BLUE, false, Paint.Align.CENTER));
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
        Object val = m.get(k);
        return val != null ? String.valueOf(val) : "";
    }

    private File cerrarPDF(String prefix) {
        mDoc.finishPage(mPage);
        File file = new File(getExternalFilesDir(null), prefix + System.currentTimeMillis() + ".pdf");
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
        i.putExtra(Intent.EXTRA_SUBJECT, "Parte de Visita");
        i.putExtra(Intent.EXTRA_STREAM, uri);
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(i, "Compartir PDF"));
    }

    private void cargarAsesores() {
        db.collection("usuarios")
                .get()
                .addOnSuccessListener(query -> {
                    mapaAsesores.clear();
                    for (QueryDocumentSnapshot doc : query) {
                        String uid      = doc.getString("uid");
                        String telefono = doc.getString("telefono");
                        if (uid != null && telefono != null) {
                            mapaAsesores.put(uid, telefono);
                        }
                    }
                    if (esAdmin) {
                        List<String> lista = new ArrayList<>();
                        lista.add("Todos");
                        lista.addAll(mapaAsesores.values());
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                                this, android.R.layout.simple_spinner_item, lista
                        );
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        spFiltroAsesor.setAdapter(adapter);
                    }
                });
    }
}