package com.example.https;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class TareasActivity extends AppCompatActivity {

    RecyclerView rvTareas;
    TareasAdapter adapter;
    List<String> listaTareas;

    // 🔥 AÑADIDO
    boolean isAdmin = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tareas);

        rvTareas = findViewById(R.id.rvTareas);
        rvTareas.setLayoutManager(new LinearLayoutManager(this));

        listaTareas = new ArrayList<>();
        adapter = new TareasAdapter(listaTareas);
        rvTareas.setAdapter(adapter);

        // 🔥 detectar admin
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        FirebaseFirestore.getInstance()
                .collection("usuarios")
                .document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Boolean admin = doc.getBoolean("isAdmin");
                        isAdmin = admin != null && admin;
                    }
                });

        // Cargar tareas de ejemplo
        listaTareas.add("Tarea 1");
        listaTareas.add("Tarea 2");
        listaTareas.add("Tarea 3");
        adapter.notifyDataSetChanged();
    }
}