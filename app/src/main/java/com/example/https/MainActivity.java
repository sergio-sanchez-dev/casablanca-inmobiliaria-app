package com.example.https;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MotionEvent;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    private EditText etUsuario, etPassword;
    private Button btnLogin;
    private CheckBox checkRecordar;
    private TextView txtCrearCuenta;

    SharedPreferences prefs;
    boolean passwordVisible = false;

    FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        prefs = getSharedPreferences("loginPrefs", MODE_PRIVATE);

        // ✅ CONTROL REAL DE SESIÓN
        boolean recordar = prefs.getBoolean("recordar_sesion", false);
        FirebaseUser usuarioActual = mAuth.getCurrentUser();

        if (usuarioActual != null && recordar) {
            String telefono = usuarioActual.getEmail().replace("@app.com", "");
            irAlDashboard(telefono);
            return;
        } else {
            mAuth.signOut();
        }

        etUsuario   = findViewById(R.id.editTextUsuario);
        etPassword  = findViewById(R.id.editTextPassword);
        btnLogin    = findViewById(R.id.btnLogin);
        checkRecordar = findViewById(R.id.checkRecordar);
        txtCrearCuenta = findViewById(R.id.txtCrearCuenta);

        etPassword.setOnTouchListener((v, event) -> {
            final int DRAWABLE_RIGHT = 2;

            if (event.getAction() == MotionEvent.ACTION_UP) {

                if (etPassword.getCompoundDrawables().length > DRAWABLE_RIGHT
                        && etPassword.getCompoundDrawables()[DRAWABLE_RIGHT] != null) {

                    int width = etPassword.getCompoundDrawables()[DRAWABLE_RIGHT]
                            .getBounds().width();

                    if (event.getRawX() >= (etPassword.getRight() - width)) {
                        togglePassword(etPassword);
                        return true;
                    }
                }
            }
            return false;
        });


        // cargar usuario guardado
        String userGuardado = prefs.getString("usuario", "");
        etUsuario.setText(userGuardado);

        txtCrearCuenta.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, CrearCuentaActivity.class));
        });

        btnLogin.setOnClickListener(v -> {

            String usuario  = etUsuario.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if(usuario.isEmpty()){
                etUsuario.setError("Introduce usuario");
                return;
            }

            if(password.isEmpty()){
                etPassword.setError("Introduce contraseña");
                return;
            }

            login(usuario, password);
        });
    }

    private void togglePassword(EditText editText){
        if(passwordVisible){
            editText.setInputType(android.text.InputType.TYPE_CLASS_TEXT |
                    android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
            passwordVisible = false;
        } else {
            editText.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            passwordVisible = true;
        }
        editText.setSelection(editText.length());
    }

    private void login(String telefono, String password) {

        btnLogin.setEnabled(false);
        btnLogin.setText("Entrando...");

        String emailFicticio = telefono + "@app.com";

        mAuth.signInWithEmailAndPassword(emailFicticio, password)
                .addOnSuccessListener(authResult -> {

                    SharedPreferences.Editor editor = prefs.edit();

                    if(checkRecordar.isChecked()){
                        editor.putString("usuario", telefono);
                        editor.putBoolean("recordar_sesion", true);
                    } else {
                        editor.remove("usuario");
                        editor.putBoolean("recordar_sesion", false);
                    }

                    editor.apply();

                    irAlDashboard(telefono);
                })
                .addOnFailureListener(e -> {

                    btnLogin.setEnabled(true);
                    btnLogin.setText("Entrar");

                    Toast.makeText(this, "Credenciales incorrectas", Toast.LENGTH_SHORT).show();
                });
    }

    private void irAlDashboard(String telefono) {
        Intent intent = new Intent(MainActivity.this, EntrandoActivity.class);
        intent.putExtra("usuario", telefono);
        startActivity(intent);
        finish();
    }
}




