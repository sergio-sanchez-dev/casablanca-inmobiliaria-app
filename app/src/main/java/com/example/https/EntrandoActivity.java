package com.example.https;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;

public class EntrandoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entrando);

        String usuario = getIntent().getStringExtra("usuario");

        new Handler().postDelayed(() -> {

            Intent intent = new Intent(EntrandoActivity.this, DashboardActivity.class);
            intent.putExtra("usuario", usuario);
            startActivity(intent);

            finish();

        }, 2000);
    }
}


