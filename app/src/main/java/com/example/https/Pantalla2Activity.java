package com.example.https;

import android.content.Intent;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.*;



public class Pantalla2Activity extends AppCompatActivity {

    private Button btnAgregar;
    private RecyclerView rvCitas;
    private Spinner spAsesor, spTipoCita, spHora, spTipoVia;
    private EditText etNombreCalle, etCiudad;

    private List<String> listaCitas;
    private CitasAdapter adapter;

    private String usuario;
    FirebaseFirestore db;

    String[] asesores = {"Adri","Alex","Javi","David"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pantalla2);

        db = FirebaseFirestore.getInstance();

        // 🔹 usuario
        usuario = getIntent().getStringExtra("usuario");

        if(usuario == null){
            usuario = "admin";
            Toast.makeText(this,"Usuario no detectado",Toast.LENGTH_SHORT).show();
        }

        // 🔹 UI
        spAsesor = findViewById(R.id.spAsesor);
        spTipoCita = findViewById(R.id.spTipoCita);
        spHora = findViewById(R.id.spHora);
        spTipoVia = findViewById(R.id.spTipoVia);
        etNombreCalle = findViewById(R.id.etNombreCalle);
        etCiudad = findViewById(R.id.etCiudad);
        btnAgregar = findViewById(R.id.btnAgregar);
        rvCitas = findViewById(R.id.rvCitas);

        spAsesor.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, asesores));

        spTipoCita.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Miguel","Aceptacion","Adquisicion","Asesoramiento financiero","Bancos","Certificado energetico","Coger encargo","Contrato de alquiler","Entrevista","Escritura publica","Gestion de encargo","Propuesta","Re-Adquisicion","Reportaje Fotografico","Tasacion","Tasks","Visita con cb hipotecas","Zona"}));

        spHora.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item,
                new String[]{"10:00","11:00","12:00","13:00","14:00","15:00","16:00","17:00","18:00","19:00","20:00","21:00"}));

        spTipoVia.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Calle","Avenida","Plaza","Paseo"}));

        listaCitas = new ArrayList<>();

        adapter = new CitasAdapter(listaCitas, position -> {

            String citaBorrar = listaCitas.get(position);

            listaCitas.remove(position);
            adapter.notifyItemRemoved(position);

            borrarCitaFirestore(citaBorrar);
        });

        rvCitas.setLayoutManager(new LinearLayoutManager(this));
        rvCitas.setAdapter(adapter);

        //  cargar desde nube
        cargarCitasFirestore();

        btnAgregar.setOnClickListener(v -> {

            String asesor = spAsesor.getSelectedItem().toString();
            String tipo = spTipoCita.getSelectedItem().toString();
            String hora = spHora.getSelectedItem().toString();

            String direccion = spTipoVia.getSelectedItem().toString() + " "
                    + etNombreCalle.getText().toString().trim()
                    + ", "
                    + etCiudad.getText().toString().trim();

            String citaMostrar = asesor + " - " + tipo + " - " + hora + " - " + direccion;

            listaCitas.add(citaMostrar);
            adapter.notifyItemInserted(listaCitas.size() - 1);

            // guardar en FIRESTORE
            Map<String, Object> cita = new HashMap<>();
            cita.put("usuario", usuario);
            cita.put("cita", citaMostrar);

            db.collection("citas").add(cita);

            abrirCalendarioSpinner(asesor, tipo, hora, direccion);

            etNombreCalle.setText("");
            etCiudad.setText("");
        });
    }

    // CARGAR CITAS DESDE LA NUBE
    private void cargarCitasFirestore() {

        listaCitas.clear();

        db.collection("citas")
                .whereEqualTo("usuario", usuario)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {

                        String cita = doc.getString("cita");
                        listaCitas.add(cita);
                    }

                    adapter.notifyDataSetChanged();
                });
    }

    //  BORRAR EN FIRESTORE
    private void borrarCitaFirestore(String citaCompleta) {

        db.collection("citas")
                .whereEqualTo("usuario", usuario)
                .whereEqualTo("cita", citaCompleta)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        db.collection("citas").document(doc.getId()).delete();
                    }
                });
    }

    private void abrirCalendarioSpinner(String asesor, String tipo, String hora, String direccion) {

        String[] partesHora = hora.split(":");

        int h = Integer.parseInt(partesHora[0]);
        int m = Integer.parseInt(partesHora[1]);

        Calendar inicio = Calendar.getInstance();
        inicio.set(Calendar.HOUR_OF_DAY, h);
        inicio.set(Calendar.MINUTE, m);

        Calendar fin = (Calendar) inicio.clone();
        fin.add(Calendar.HOUR_OF_DAY, 1);

        Intent intent = new Intent(Intent.ACTION_INSERT);
        intent.setData(CalendarContract.Events.CONTENT_URI);

        intent.putExtra(CalendarContract.Events.TITLE, tipo);
        intent.putExtra(CalendarContract.Events.DESCRIPTION, "Cita con asesor: " + asesor);
        intent.putExtra(CalendarContract.Events.EVENT_LOCATION, direccion);

        intent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, inicio.getTimeInMillis());
        intent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME, fin.getTimeInMillis());

        intent.setPackage("com.google.android.calendar");

        startActivity(intent);
    }
}









