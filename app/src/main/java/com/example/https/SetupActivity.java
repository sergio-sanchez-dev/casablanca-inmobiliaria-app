package com.example.https;

import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.*;

import java.util.*;

public class SetupActivity extends AppCompatActivity {

    FirebaseFirestore db;
    TextView tvLog;

    String fechaHoy;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activitysetup);

        db = FirebaseFirestore.getInstance();

        tvLog = findViewById(R.id.tvLog);
        Button btnCrear = findViewById(R.id.btnCrearUsuarios);

        btnCrear.setText("📊 Ver estadísticas de hoy");

        Calendar c = Calendar.getInstance();
        fechaHoy = String.format("%02d/%02d/%04d",
                c.get(Calendar.DAY_OF_MONTH),
                c.get(Calendar.MONTH) + 1,
                c.get(Calendar.YEAR));

        btnCrear.setOnClickListener(v -> cargarEstadisticas());
    }

    private void cargarEstadisticas() {

        tvLog.setText("⏳ Cargando estadísticas...\n");

        db.collection("citas")
                .get()
                .addOnSuccessListener(snapshots -> {

                    int total = 0;
                    int pendientes = 0;
                    int confirmadas = 0;
                    int canceladas = 0;
                    int realizadas = 0;

                    for (DocumentSnapshot doc : snapshots) {

                        String fecha = doc.getString("fecha");
                        String estado = doc.getString("estado");

                        if (fecha == null || !fecha.equals(fechaHoy)) continue;

                        total++;

                        if ("pendiente".equals(estado)) pendientes++;
                        else if ("confirmada".equals(estado)) confirmadas++;
                        else if ("cancelada".equals(estado)) canceladas++;
                        else if ("realizada".equals(estado)) realizadas++;
                    }

                    mostrarDashboard(total, pendientes, confirmadas, canceladas, realizadas);
                })
                .addOnFailureListener(e ->
                        tvLog.setText("❌ Error: " + e.getMessage()));
    }

    private void mostrarDashboard(int total, int pen, int con, int can, int rea) {

        String texto =
                "📊 ESTADÍSTICAS HOY\n\n" +
                        "📅 Fecha: " + fechaHoy + "\n\n" +
                        "📌 Total citas: " + total + "\n" +
                        "🟡 Pendientes: " + pen + "\n" +
                        "🟢 Confirmadas: " + con + "\n" +
                        "🔴 Canceladas: " + can + "\n" +
                        "✅ Realizadas: " + rea + "\n";

        tvLog.setText(texto);
    }
}