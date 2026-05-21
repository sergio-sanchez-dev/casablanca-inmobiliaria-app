package com.example.https;
import java.util.Arrays;  // ← AGREGAR ESTE
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CaptacionActivity extends AppCompatActivity {

    // ── Campos formulario ────────────────────────────────────────────────────
    EditText etFechaDia, etFechaMes, etFechaAnio;
    EditText etP1Nombre, etP1Domicilio, etP1DNI, etP1Telefono, etP1Email;
    EditText etP2Nombre, etP2Domicilio, etP2DNI, etP2Telefono, etP2Email;
    EditText etDireccion, etPoblacion, etCP;
    EditText etZona, etTransportes, etComercios, etAparcamientos, etCentroSalud, etServicios;
    EditText etTipo, etOrientacion, etSupUtil, etSupConstruida;
    EditText etDormitorios, etBanos, etGaraje, etTrastero;
    EditText etCalefaccion, etAireAcondicionado;
    EditText etSuelos, etPuertas, etVentanas, etParedes;
    EditText etCEE, etLuzContrato, etAgua, etComunidad, etIBI, etHipoteca;
    EditText etAnioConstruccion, etAscensor, etZonasComunes;
    EditText etPlantaBajaM2, etPlantaBajaDesc;
    EditText etPrimeraPlantaM2, etPrimeraPlantaDesc;
    EditText etBuhardillaM2, etBuhardillaDesc;
    EditText etSotanoM2, etSotanoDesc;
    EditText etPatioM2, etPatioDesc;
    EditText etMuebles, etMotivoVenta, etMotivoPrecio;
    EditText etOtrasInmobiliarias, etVisitas, etOfertas, etObservaciones;
    Button btnGuardar;

    // ── Sección lista ────────────────────────────────────────────────────────
    Button btnFiltroHoy, btnFiltroAyer, btnFiltroMes, btnFiltroTodas;
    Spinner spFiltroAsesor;
    RecyclerView rvLista;
    GenericListAdapter listAdapter;
    List<GenericItem> listaItems = new ArrayList<>();

    // ── Firebase ─────────────────────────────────────────────────────────────
    FirebaseFirestore db;
    String userId;
    boolean esAdmin = false;
    String docIdEditando = null;

    // ── PDF ──────────────────────────────────────────────────────────────────
    private PdfDocument pdfDoc;
    private PdfDocument.Page pdfPage;
    private Canvas pdfCanvas;
    private int pdfPageNum = 1;
    private float pdfY = 0f;
    private static final int W = 595, H = 842, ML = 28, MR = 28, TW = W - ML - MR;
    private Paint pTitulo, pSeccion, pSeccionBg, pLabel, pValor, pBorder, pCeldaBg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_captacion);
        db     = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        bindFormulario();
        bindLista();
        comprobarRol();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // BIND
    // ═════════════════════════════════════════════════════════════════════════
    private void bindFormulario() {
        etFechaDia  = findViewById(R.id.etFechaDia);
        etFechaMes  = findViewById(R.id.etFechaMes);
        etFechaAnio = findViewById(R.id.etFechaAnio);
        etP1Nombre    = findViewById(R.id.etP1Nombre);
        etP1Domicilio = findViewById(R.id.etP1Domicilio);
        etP1DNI       = findViewById(R.id.etP1DNI);
        etP1Telefono  = findViewById(R.id.etP1Telefono);
        etP1Email     = findViewById(R.id.etP1Email);
        etP2Nombre    = findViewById(R.id.etP2Nombre);
        etP2Domicilio = findViewById(R.id.etP2Domicilio);
        etP2DNI       = findViewById(R.id.etP2DNI);
        etP2Telefono  = findViewById(R.id.etP2Telefono);
        etP2Email     = findViewById(R.id.etP2Email);
        etDireccion = findViewById(R.id.etDireccion);
        etPoblacion = findViewById(R.id.etPoblacion);
        etCP        = findViewById(R.id.etCP);
        etZona          = findViewById(R.id.etZona);
        etTransportes   = findViewById(R.id.etTransportes);
        etComercios     = findViewById(R.id.etComercios);
        etAparcamientos = findViewById(R.id.etAparcamientos);
        etCentroSalud   = findViewById(R.id.etCentroSalud);
        etServicios     = findViewById(R.id.etServicios);
        etTipo              = findViewById(R.id.etTipo);
        etOrientacion       = findViewById(R.id.etOrientacion);
        etSupUtil           = findViewById(R.id.etSupUtil);
        etSupConstruida     = findViewById(R.id.etSupConstruida);
        etDormitorios       = findViewById(R.id.etDormitorios);
        etBanos             = findViewById(R.id.etBanos);
        etGaraje            = findViewById(R.id.etGaraje);
        etTrastero          = findViewById(R.id.etTrastero);
        etCalefaccion       = findViewById(R.id.etCalefaccion);
        etAireAcondicionado = findViewById(R.id.etAireAcondicionado);
        etSuelos  = findViewById(R.id.etSuelos);
        etPuertas = findViewById(R.id.etPuertas);
        etVentanas= findViewById(R.id.etVentanas);
        etParedes = findViewById(R.id.etParedes);
        etCEE          = findViewById(R.id.etCEE);
        etLuzContrato  = findViewById(R.id.etLuzContrato);
        etAgua         = findViewById(R.id.etAgua);
        etComunidad    = findViewById(R.id.etComunidad);
        etIBI          = findViewById(R.id.etIBI);
        etHipoteca     = findViewById(R.id.etHipoteca);
        etAnioConstruccion = findViewById(R.id.etAnioConstruccion);
        etAscensor         = findViewById(R.id.etAscensor);
        etZonasComunes     = findViewById(R.id.etZonasComunes);
        etPlantaBajaM2      = findViewById(R.id.etPlantaBajaM2);
        etPlantaBajaDesc    = findViewById(R.id.etPlantaBajaDesc);
        etPrimeraPlantaM2   = findViewById(R.id.etPrimeraPlantaM2);
        etPrimeraPlantaDesc = findViewById(R.id.etPrimeraPlantaDesc);
        etBuhardillaM2   = findViewById(R.id.etBuhardillaM2);
        etBuhardillaDesc = findViewById(R.id.etBuhardillaDesc);
        etSotanoM2   = findViewById(R.id.etSotanoM2);
        etSotanoDesc = findViewById(R.id.etSotanoDesc);
        etPatioM2    = findViewById(R.id.etPatioM2);
        etPatioDesc  = findViewById(R.id.etPatioDesc);
        etMuebles            = findViewById(R.id.etMuebles);
        etMotivoVenta        = findViewById(R.id.etMotivoVenta);
        etMotivoPrecio       = findViewById(R.id.etMotivoPrecio);
        etOtrasInmobiliarias = findViewById(R.id.etOtrasInmobiliarias);
        etVisitas            = findViewById(R.id.etVisitas);
        etOfertas            = findViewById(R.id.etOfertas);
        etObservaciones      = findViewById(R.id.etObservaciones);
        btnGuardar = findViewById(R.id.btnGuardar);
        btnGuardar.setOnClickListener(v -> guardarOActualizar());
    }

    private void bindLista() {
        btnFiltroHoy = findViewById(R.id.btnFiltroHoy);
        btnFiltroAyer = findViewById(R.id.btnFiltroAyer);
        btnFiltroMes = findViewById(R.id.btnFiltroMes);
        btnFiltroTodas = findViewById(R.id.btnFiltroTodas);
        spFiltroAsesor = findViewById(R.id.spFiltroAsesor);
        rvLista = findViewById(R.id.rvVisitas);

        // ✅ PROTECCIÓN: Verifica que RecyclerView existe antes de configurarlo
        if (rvLista != null) {
            listAdapter = new GenericListAdapter(listaItems, new GenericListAdapter.OnItemListener() {
                @Override public void onEditar(GenericItem item, int pos)  { cargarEnFormulario(item); }
                @Override public void onEliminar(GenericItem item, int pos) { confirmarEliminacion(item, pos); }
            });
            rvLista.setLayoutManager(new LinearLayoutManager(this));
            rvLista.setAdapter(listAdapter);
            rvLista.setNestedScrollingEnabled(false);
        }

        // ✅ PROTECCIÓN: Verifica que los botones existen antes de setOnClickListener
        if (btnFiltroHoy != null) btnFiltroHoy.setOnClickListener(v -> cargarLista("hoy"));
        if (btnFiltroAyer != null) btnFiltroAyer.setOnClickListener(v -> cargarLista("ayer"));
        if (btnFiltroMes != null) btnFiltroMes.setOnClickListener(v -> cargarLista("mes"));
        if (btnFiltroTodas != null) btnFiltroTodas.setOnClickListener(v -> cargarLista("todas"));
    }

    // ═════════════════════════════════════════════════════════════════════════
    // ROL — igual que CalendarioActivity
    // ═════════════════════════════════════════════════════════════════════════
    private void comprobarRol() {
        db.collection("usuarios").whereEqualTo("uid", userId).get()
                .addOnSuccessListener(q -> {
                    if (!q.isEmpty()) {
                        String rol = q.getDocuments().get(0).getString("rol");
                        esAdmin = "admin".equals(rol);
                    }

                    // ✅ PROTECCIÓN contra null
                    if (spFiltroAsesor != null) {
                        spFiltroAsesor.setVisibility(esAdmin ? View.VISIBLE : View.GONE);

                        // ✅ Cargar asesores si es admin
                        if (esAdmin) {
                            cargarAsesoresSpinner();
                        }
                    }

                    cargarLista("hoy");
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error cargando rol", Toast.LENGTH_SHORT).show();
                    cargarLista("hoy");
                });
    }
    // ✅ NUEVO MÉTODO - CARGAR ASESores EN SPINNER
    private void cargarAsesoresSpinner() {
        if (spFiltroAsesor == null) return;

        List<String> asesores = new ArrayList<>();
        asesores.add("Todos");

        db.collection("usuarios")
                .whereIn("rol", Arrays.asList("admin", "asesor"))
                .get()
                .addOnSuccessListener(query -> {
                    for (QueryDocumentSnapshot doc : query) {
                        String nombre = doc.getString("nombre");
                        String uid = doc.getString("uid");
                        if (nombre != null && uid != null) {
                            asesores.add(nombre + " (" + uid.substring(0, 8) + "...)");
                        }
                    }
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            this, android.R.layout.simple_spinner_item, asesores);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spFiltroAsesor.setAdapter(adapter);
                })
                .addOnFailureListener(e -> {
                    // Fallback: solo "Todos"
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            this, android.R.layout.simple_spinner_item, asesores);
                    spFiltroAsesor.setAdapter(adapter);
                });
    }

    // ═════════════════════════════════════════════════════════════════════════
    // GUARDAR / ACTUALIZAR
    // ═════════════════════════════════════════════════════════════════════════
    private void guardarOActualizar() {
        if (val(etP1Nombre).isEmpty()) { etP1Nombre.setError("Obligatorio"); return; }

        Map<String, Object> data = construirData();

        if (docIdEditando != null) {
            db.collection("captaciones").document(docIdEditando).update(data)
                    .addOnSuccessListener(u -> {
                        Toast.makeText(this, "✔ Captación actualizada", Toast.LENGTH_SHORT).show();
                        limpiarFormulario();
                        cargarLista("todas");
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "❌ " + e.getMessage(), Toast.LENGTH_LONG).show());
        } else {
            db.collection("captaciones").add(data)
                    .addOnSuccessListener(ref -> {
                        File pdf = generarPDF(data);
                        enviarEmailConPDF(pdf);
                        Toast.makeText(this, "✅ Captación guardada", Toast.LENGTH_SHORT).show();
                        limpiarFormulario();
                        cargarLista("hoy");
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "❌ " + e.getMessage(), Toast.LENGTH_LONG).show());
        }
    }

    private Map<String, Object> construirData() {
        Map<String, Object> data = new HashMap<>();
        data.put("userId",    userId);
        data.put("timestamp", Timestamp.now());
        data.put("fecha_dia",  val(etFechaDia));
        data.put("fecha_mes",  val(etFechaMes));
        data.put("fecha_anio", val(etFechaAnio));
        put(data, "p1_nombre",    etP1Nombre);
        put(data, "p1_domicilio", etP1Domicilio);
        put(data, "p1_dni",       etP1DNI);
        put(data, "p1_telefono",  etP1Telefono);
        put(data, "p1_email",     etP1Email);
        put(data, "p2_nombre",    etP2Nombre);
        put(data, "p2_domicilio", etP2Domicilio);
        put(data, "p2_dni",       etP2DNI);
        put(data, "p2_telefono",  etP2Telefono);
        put(data, "p2_email",     etP2Email);
        put(data, "direccion",    etDireccion);
        put(data, "poblacion",    etPoblacion);
        put(data, "cp",           etCP);
        put(data, "zona",         etZona);
        put(data, "transportes",  etTransportes);
        put(data, "comercios",    etComercios);
        put(data, "aparcamientos",etAparcamientos);
        put(data, "centro_salud", etCentroSalud);
        put(data, "servicios",    etServicios);
        put(data, "tipo",               etTipo);
        put(data, "orientacion",        etOrientacion);
        put(data, "sup_util",           etSupUtil);
        put(data, "sup_construida",     etSupConstruida);
        put(data, "dormitorios",        etDormitorios);
        put(data, "banos",              etBanos);
        put(data, "garaje",             etGaraje);
        put(data, "trastero",           etTrastero);
        put(data, "calefaccion",        etCalefaccion);
        put(data, "aire_acondicionado", etAireAcondicionado);
        put(data, "suelos",    etSuelos);
        put(data, "puertas",   etPuertas);
        put(data, "ventanas",  etVentanas);
        put(data, "paredes",   etParedes);
        put(data, "cee",          etCEE);
        put(data, "luz_contrato", etLuzContrato);
        put(data, "agua",         etAgua);
        put(data, "comunidad",    etComunidad);
        put(data, "ibi",          etIBI);
        put(data, "hipoteca",     etHipoteca);
        put(data, "anio_construccion", etAnioConstruccion);
        put(data, "ascensor",         etAscensor);
        put(data, "zonas_comunes",    etZonasComunes);
        put(data, "planta_baja_m2",      etPlantaBajaM2);
        put(data, "planta_baja_desc",    etPlantaBajaDesc);
        put(data, "primera_planta_m2",   etPrimeraPlantaM2);
        put(data, "primera_planta_desc", etPrimeraPlantaDesc);
        put(data, "buhardilla_m2",   etBuhardillaM2);
        put(data, "buhardilla_desc", etBuhardillaDesc);
        put(data, "sotano_m2",   etSotanoM2);
        put(data, "sotano_desc", etSotanoDesc);
        put(data, "patio_m2",    etPatioM2);
        put(data, "patio_desc",  etPatioDesc);
        put(data, "muebles",             etMuebles);
        put(data, "motivo_venta",        etMotivoVenta);
        put(data, "motivo_precio",       etMotivoPrecio);
        put(data, "otras_inmobiliarias", etOtrasInmobiliarias);
        put(data, "visitas",             etVisitas);
        put(data, "ofertas",             etOfertas);
        put(data, "observaciones",       etObservaciones);
        return data;
    }

    // ═════════════════════════════════════════════════════════════════════════
    // CARGA CON FILTROS — mismo patrón que VisitasActivity
    // ═════════════════════════════════════════════════════════════════════════
    private void cargarLista(String filtro) {
        Query q;

        if (esAdmin && spFiltroAsesor != null && spFiltroAsesor.getSelectedItem() != null) {
            String sel = spFiltroAsesor.getSelectedItem().toString();
            if (!sel.equals("Todos")) {
                // Admin filtrando por asesor específico
                q = db.collection("captaciones").whereEqualTo("userId", sel);
            } else {
                // Admin ve TODO
                q = db.collection("captaciones");
            }
        } else {
            // Usuario normal SOLO ve sus captaciones
            q = db.collection("captaciones").whereEqualTo("userId", userId);
        }

        q.get()
                .addOnSuccessListener(snapshots -> procesarResultados(snapshots, filtro))
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }



    private void procesarResultados(com.google.firebase.firestore.QuerySnapshot snapshots, String filtro) {
        listaItems.clear();

        long inicio = 0, fin = Long.MAX_VALUE;
        if (!"todas".equals(filtro)) {
            Calendar cI = Calendar.getInstance(), cF = Calendar.getInstance();
            switch (filtro) {
                case "hoy":
                    cI.set(Calendar.HOUR_OF_DAY, 0); cI.set(Calendar.MINUTE, 0); cI.set(Calendar.SECOND, 0);
                    cF.set(Calendar.HOUR_OF_DAY,23); cF.set(Calendar.MINUTE,59); cF.set(Calendar.SECOND,59);
                    break;
                case "ayer":
                    cI.add(Calendar.DAY_OF_YEAR,-1);
                    cI.set(Calendar.HOUR_OF_DAY, 0); cI.set(Calendar.MINUTE, 0); cI.set(Calendar.SECOND, 0);
                    cF.add(Calendar.DAY_OF_YEAR,-1);
                    cF.set(Calendar.HOUR_OF_DAY,23); cF.set(Calendar.MINUTE,59); cF.set(Calendar.SECOND,59);
                    break;
                case "mes":
                    cI.set(Calendar.DAY_OF_MONTH,1);
                    cI.set(Calendar.HOUR_OF_DAY, 0); cI.set(Calendar.MINUTE, 0); cI.set(Calendar.SECOND, 0);
                    cF.set(Calendar.DAY_OF_MONTH, cF.getActualMaximum(Calendar.DAY_OF_MONTH));
                    cF.set(Calendar.HOUR_OF_DAY,23); cF.set(Calendar.MINUTE,59); cF.set(Calendar.SECOND,59);
                    break;
            }
            inicio = cI.getTimeInMillis();
            fin    = cF.getTimeInMillis();
        }

        for (QueryDocumentSnapshot doc : snapshots) {
            Timestamp ts = doc.getTimestamp("timestamp");
            if (ts == null) continue;
            long t = ts.toDate().getTime();
            if (!"todas".equals(filtro) && (t < inicio || t > fin)) continue;

            Map<String, Object> campos = doc.getData();
            String titulo    = str(doc, "p1_nombre");
            String subtitulo = str(doc, "direccion");
            String fecha     = str(doc, "fecha_dia") + "/" + str(doc, "fecha_mes") + "/" + str(doc, "fecha_anio");

            listaItems.add(new GenericItem(
                    doc.getId(), titulo,
                    subtitulo + " · " + fecha,
                    "",
                    str(doc, "userId"),
                    campos,
                    ts.toDate(),
                    ""
            ));
        }
        listAdapter.notifyDataSetChanged();
        if (listaItems.isEmpty())
            Toast.makeText(this, "Sin captaciones para este filtro", Toast.LENGTH_SHORT).show();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // CARGAR EN FORMULARIO
    // ═════════════════════════════════════════════════════════════════════════
    private void cargarEnFormulario(GenericItem item) {
        docIdEditando = item.docId;
        Map<String, Object> d = item.camposCompletos;
        if (d == null) return;

        set(etFechaDia,  d, "fecha_dia");  set(etFechaMes, d, "fecha_mes"); set(etFechaAnio, d, "fecha_anio");
        set(etP1Nombre, d,"p1_nombre"); set(etP1Domicilio,d,"p1_domicilio"); set(etP1DNI,d,"p1_dni");
        set(etP1Telefono,d,"p1_telefono"); set(etP1Email,d,"p1_email");
        set(etP2Nombre,d,"p2_nombre"); set(etP2Domicilio,d,"p2_domicilio"); set(etP2DNI,d,"p2_dni");
        set(etP2Telefono,d,"p2_telefono"); set(etP2Email,d,"p2_email");
        set(etDireccion,d,"direccion"); set(etPoblacion,d,"poblacion"); set(etCP,d,"cp");
        set(etZona,d,"zona"); set(etTransportes,d,"transportes"); set(etComercios,d,"comercios");
        set(etAparcamientos,d,"aparcamientos"); set(etCentroSalud,d,"centro_salud"); set(etServicios,d,"servicios");
        set(etTipo,d,"tipo"); set(etOrientacion,d,"orientacion"); set(etSupUtil,d,"sup_util");
        set(etSupConstruida,d,"sup_construida"); set(etDormitorios,d,"dormitorios"); set(etBanos,d,"banos");
        set(etGaraje,d,"garaje"); set(etTrastero,d,"trastero"); set(etCalefaccion,d,"calefaccion");
        set(etAireAcondicionado,d,"aire_acondicionado"); set(etSuelos,d,"suelos"); set(etPuertas,d,"puertas");
        set(etVentanas,d,"ventanas"); set(etParedes,d,"paredes"); set(etCEE,d,"cee");
        set(etLuzContrato,d,"luz_contrato"); set(etAgua,d,"agua"); set(etComunidad,d,"comunidad");
        set(etIBI,d,"ibi"); set(etHipoteca,d,"hipoteca"); set(etAnioConstruccion,d,"anio_construccion");
        set(etAscensor,d,"ascensor"); set(etZonasComunes,d,"zonas_comunes");
        set(etPlantaBajaM2,d,"planta_baja_m2"); set(etPlantaBajaDesc,d,"planta_baja_desc");
        set(etPrimeraPlantaM2,d,"primera_planta_m2"); set(etPrimeraPlantaDesc,d,"primera_planta_desc");
        set(etBuhardillaM2,d,"buhardilla_m2"); set(etBuhardillaDesc,d,"buhardilla_desc");
        set(etSotanoM2,d,"sotano_m2"); set(etSotanoDesc,d,"sotano_desc");
        set(etPatioM2,d,"patio_m2"); set(etPatioDesc,d,"patio_desc");
        set(etMuebles,d,"muebles"); set(etMotivoVenta,d,"motivo_venta"); set(etMotivoPrecio,d,"motivo_precio");
        set(etOtrasInmobiliarias,d,"otras_inmobiliarias"); set(etVisitas,d,"visitas");
        set(etOfertas,d,"ofertas"); set(etObservaciones,d,"observaciones");

        btnGuardar.setText("💾 Actualizar captación");
        findViewById(R.id.scrollCaptacion).scrollTo(0, 0);
        Toast.makeText(this, "Editando: " + item.titulo, Toast.LENGTH_SHORT).show();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // ELIMINAR
    // ═════════════════════════════════════════════════════════════════════════
    private void confirmarEliminacion(GenericItem item, int pos) {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar captación")
                .setMessage("¿Eliminar la captación de " + item.titulo + "?\nNo se puede deshacer.")
                .setPositiveButton("🗑 Eliminar", (d, w) ->
                        db.collection("captaciones").document(item.docId).delete()
                                .addOnSuccessListener(u -> {
                                    listAdapter.remove(pos);
                                    Toast.makeText(this, "Eliminada", Toast.LENGTH_SHORT).show();
                                }))
                .setNegativeButton("Cancelar", null).show();
    }

    // ═════════════════════════════════════════════════════════════════════════
    // HELPERS
    // ═════════════════════════════════════════════════════════════════════════
    private void limpiarFormulario() {
        docIdEditando = null;
        btnGuardar.setText("✅ Guardar captación");
        EditText[] todos = {etFechaDia,etFechaMes,etFechaAnio,etP1Nombre,etP1Domicilio,etP1DNI,
                etP1Telefono,etP1Email,etP2Nombre,etP2Domicilio,etP2DNI,etP2Telefono,etP2Email,
                etDireccion,etPoblacion,etCP,etZona,etTransportes,etComercios,etAparcamientos,
                etCentroSalud,etServicios,etTipo,etOrientacion,etSupUtil,etSupConstruida,
                etDormitorios,etBanos,etGaraje,etTrastero,etCalefaccion,etAireAcondicionado,
                etSuelos,etPuertas,etVentanas,etParedes,etCEE,etLuzContrato,etAgua,etComunidad,
                etIBI,etHipoteca,etAnioConstruccion,etAscensor,etZonasComunes,etPlantaBajaM2,
                etPlantaBajaDesc,etPrimeraPlantaM2,etPrimeraPlantaDesc,etBuhardillaM2,
                etBuhardillaDesc,etSotanoM2,etSotanoDesc,etPatioM2,etPatioDesc,etMuebles,
                etMotivoVenta,etMotivoPrecio,etOtrasInmobiliarias,etVisitas,etOfertas,etObservaciones};
        for (EditText et : todos) if (et != null) et.setText("");
    }

    private String val(EditText et) { return et != null ? et.getText().toString().trim() : ""; }
    private void put(Map<String, Object> m, String k, EditText et) { String v = val(et); if (!v.isEmpty()) m.put(k, v); }
    private void set(EditText et, Map<String, Object> d, String k) { if (et == null) return; Object v = d.get(k); et.setText(v != null ? String.valueOf(v) : ""); }
    private String str(QueryDocumentSnapshot doc, String k) { Object v = doc.get(k); return v != null ? String.valueOf(v) : ""; }
    private String s(Map<String, Object> m, String k) { Object v = m.get(k); return v != null ? String.valueOf(v) : ""; }

    // ═════════════════════════════════════════════════════════════════════════
    // PDF (tu código original sin cambios)
    // ═════════════════════════════════════════════════════════════════════════
    private File generarPDF(Map<String, Object> data) {
        iniciarPaints();
        pdfDoc = new PdfDocument();
        pdfPageNum = 1;
        nuevaPagina();
        final float ROW = 18f;
        pdfCanvas.drawText("FICHA DE CAPTACIÓN", ML, pdfY, pTitulo);
        String fecha = s(data,"fecha_dia")+"/"+s(data,"fecha_mes")+"/"+s(data,"fecha_anio");
        pdfCanvas.drawText("FECHA: "+fecha, W-MR-130, pdfY, pLabel); pdfY += 18f;
        seccion("CONTACTO / PROPIETARIO 1");
        fila1("Nombre y Apellidos",s(data,"p1_nombre"),ROW); fila1("Domicilio",s(data,"p1_domicilio"),ROW);
        fila2("DNI/NIE",s(data,"p1_dni"),"Teléfono",s(data,"p1_telefono"),ROW); fila1("e-mail",s(data,"p1_email"),ROW);
        seccion("PROPIETARIO 2");
        fila1("Nombre y Apellidos",s(data,"p2_nombre"),ROW); fila1("Domicilio",s(data,"p2_domicilio"),ROW);
        fila2("DNI/NIE",s(data,"p2_dni"),"Teléfono",s(data,"p2_telefono"),ROW); fila1("e-mail",s(data,"p2_email"),ROW);
        seccion("UBICACIÓN DEL INMUEBLE");
        fila1("Dirección",s(data,"direccion"),ROW); fila2("Población",s(data,"poblacion"),"CP",s(data,"cp"),ROW);
        seccion("CARACTERÍSTICAS ZONA");
        fila1("ZONA",s(data,"zona"),ROW);
        fila2("Transportes",s(data,"transportes"),"Comercios",s(data,"comercios"),ROW);
        fila2("Aparcamientos",s(data,"aparcamientos"),"Centro de Salud",s(data,"centro_salud"),ROW);
        fila1("Servicios",s(data,"servicios"),ROW);
        seccion("CARACTERÍSTICAS INMUEBLE");
        fila2("TIPO",s(data,"tipo"),"Orientación",s(data,"orientacion"),ROW);
        fila2("Superf. Útil",s(data,"sup_util"),"Superf. Construida",s(data,"sup_construida"),ROW);
        fila2("Dormitorios",s(data,"dormitorios"),"Baños",s(data,"banos"),ROW);
        fila2("Garaje",s(data,"garaje"),"Trastero",s(data,"trastero"),ROW);
        fila2("Calefacción",s(data,"calefaccion"),"Aire Acondicionado",s(data,"aire_acondicionado"),ROW);
        fila2("Suelos",s(data,"suelos"),"Puertas",s(data,"puertas"),ROW);
        fila2("Ventanas",s(data,"ventanas"),"Paredes",s(data,"paredes"),ROW);
        fila2("C.E.E.",s(data,"cee"),"Luz Contrato",s(data,"luz_contrato"),ROW);
        fila2("Agua",s(data,"agua"),"Comunidad",s(data,"comunidad"),ROW);
        fila2("I.B.I.",s(data,"ibi"),"Hipoteca",s(data,"hipoteca"),ROW);
        fila2("Año construcción",s(data,"anio_construccion"),"Ascensor",s(data,"ascensor"),ROW);
        fila1("Zonas comunes",s(data,"zonas_comunes"),ROW);
        seccion("DISTRIBUCIÓN");
        planta("PLANTA BAJA",s(data,"planta_baja_m2"),s(data,"planta_baja_desc"),ROW);
        planta("PRIMERA PLANTA",s(data,"primera_planta_m2"),s(data,"primera_planta_desc"),ROW);
        planta("BUHARDILLA",s(data,"buhardilla_m2"),s(data,"buhardilla_desc"),ROW);
        planta("PLANTA SÓTANO",s(data,"sotano_m2"),s(data,"sotano_desc"),ROW);
        planta("PATIO",s(data,"patio_m2"),s(data,"patio_desc"),ROW);
        seccion("OTROS");
        fila1("MUEBLES",s(data,"muebles"),ROW); fila1("MOTIVO DE VENTA",s(data,"motivo_venta"),ROW);
        fila1("MOTIVO PRECIO",s(data,"motivo_precio"),ROW);
        fila1("OTRAS INMOBILIARIAS",s(data,"otras_inmobiliarias"),ROW);
        fila1("VISITAS",s(data,"visitas"),ROW); fila1("OFERTAS",s(data,"ofertas"),ROW);
        seccion("OBSERVACIONES"); observaciones(s(data,"observaciones"));
        pdfDoc.finishPage(pdfPage);
        File file = new File(getExternalFilesDir(null),"captacion_"+System.currentTimeMillis()+".pdf");
        try { pdfDoc.writeTo(new FileOutputStream(file)); pdfDoc.close(); } catch (Exception e) { e.printStackTrace(); }
        return file;
    }

    private void iniciarPaints() {
        pTitulo=new Paint(); pTitulo.setTextSize(14f); pTitulo.setFakeBoldText(true); pTitulo.setColor(Color.parseColor("#1a237e"));
        pSeccion=new Paint(); pSeccion.setTextSize(9f); pSeccion.setFakeBoldText(true); pSeccion.setColor(Color.WHITE);
        pSeccionBg=new Paint(); pSeccionBg.setColor(Color.parseColor("#1a237e")); pSeccionBg.setStyle(Paint.Style.FILL);
        pLabel=new Paint(); pLabel.setTextSize(8f); pLabel.setFakeBoldText(true); pLabel.setColor(Color.parseColor("#333333"));
        pValor=new Paint(); pValor.setTextSize(8f); pValor.setColor(Color.parseColor("#111111"));
        pBorder=new Paint(); pBorder.setColor(Color.parseColor("#AAAAAA")); pBorder.setStyle(Paint.Style.STROKE); pBorder.setStrokeWidth(0.5f);
        pCeldaBg=new Paint(); pCeldaBg.setColor(Color.parseColor("#E8EAF6")); pCeldaBg.setStyle(Paint.Style.FILL);
    }
    private void nuevaPagina() {
        if (pdfPage!=null) pdfDoc.finishPage(pdfPage);
        PdfDocument.PageInfo info=new PdfDocument.PageInfo.Builder(W,H,pdfPageNum++).create();
        pdfPage=pdfDoc.startPage(info); pdfCanvas=pdfPage.getCanvas(); pdfY=36f;
    }
    private void checkPag(float n) { if (pdfY+n>H-30) nuevaPagina(); }
    private void seccion(String t) { checkPag(14f); pdfCanvas.drawRect(ML,pdfY,ML+TW,pdfY+14f,pSeccionBg); pdfCanvas.drawText(t,ML+4,pdfY+10f,pSeccion); pdfY+=14f; }
    private void fila1(String l,String v,float rH) { checkPag(rH); float lw=TW*0.30f; celda(ML,pdfY,lw,rH,l,null,true); celda(ML+lw,pdfY,TW-lw,rH,null,v,false); pdfY+=rH; }
    private void fila2(String l1,String v1,String l2,String v2,float rH) { checkPag(rH); float hw=TW/2f,lw=hw*0.45f,vw=hw-lw; celda(ML,pdfY,lw,rH,l1,null,true); celda(ML+lw,pdfY,vw,rH,null,v1,false); celda(ML+hw,pdfY,lw,rH,l2,null,true); celda(ML+hw+lw,pdfY,vw,rH,null,v2,false); pdfY+=rH; }
    private void planta(String n,String m2,String d,float rH) { checkPag(14f+rH); pdfCanvas.drawRect(ML,pdfY,ML+TW,pdfY+14f,pCeldaBg); pdfCanvas.drawRect(ML,pdfY,ML+TW,pdfY+14f,pBorder); pdfCanvas.drawText(n+"  ……… "+(m2.isEmpty()?"___":m2)+" m²",ML+3,pdfY+10f,pLabel); pdfY+=14f; pdfCanvas.drawRect(ML,pdfY,ML+TW,pdfY+rH,pBorder); if(!d.isEmpty()) pdfCanvas.drawText(d,ML+3,pdfY+rH-5,pValor); pdfY+=rH; }
    private void observaciones(String t) { float oH=80f; checkPag(oH); pdfCanvas.drawRect(ML,pdfY,ML+TW,pdfY+oH,pBorder); int cpl=(int)(TW/4.6f); float ty=pdfY+10f; while(!t.isEmpty()&&ty<pdfY+oH-4){String l=t.length()>cpl?t.substring(0,cpl):t; pdfCanvas.drawText(l,ML+3,ty,pValor); t=t.length()>cpl?t.substring(cpl):""; ty+=10f;} pdfY+=oH; }
    private void celda(float x,float y,float w,float h,String l,String v,boolean bg) { if(bg) pdfCanvas.drawRect(x,y,x+w,y+h,pCeldaBg); pdfCanvas.drawRect(x,y,x+w,y+h,pBorder); if(l!=null&&!l.isEmpty()) pdfCanvas.drawText(l,x+3,y+h-5,pLabel); if(v!=null&&!v.isEmpty()) pdfCanvas.drawText(v,x+3,y+h-5,pValor); }

    private void enviarEmailConPDF(File file) {
        Uri uri = FileProvider.getUriForFile(this, getPackageName()+".provider", file);
        Intent i = new Intent(Intent.ACTION_SEND); i.setType("application/pdf");
        i.putExtra(Intent.EXTRA_SUBJECT,"Nueva captación"); i.putExtra(Intent.EXTRA_TEXT,"Adjunto ficha de captación");
        i.putExtra(Intent.EXTRA_STREAM,uri); i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(i,"Enviar PDF"));
    }
}