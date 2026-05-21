package com.example.https;

import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class CrearCuentaActivity extends AppCompatActivity {

    EditText etTelefono, etPassword, etConfirmar;
    CheckBox checkPolitica;
    Button btnRegistrar;
    TextView txtLogin;

    boolean passwordVisiblePass = false;
    boolean passwordVisibleConfirm = false;

    FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_crear_cuenta);

        mAuth = FirebaseAuth.getInstance();

        etTelefono   = findViewById(R.id.etTelefono);
        etPassword   = findViewById(R.id.etPassword);
        etConfirmar  = findViewById(R.id.etConfirmar);
        checkPolitica = findViewById(R.id.checkPolitica);
        btnRegistrar  = findViewById(R.id.btnRegistrar);
        txtLogin      = findViewById(R.id.txtLogin);

        View card = findViewById(R.id.crearCard);

        if (card != null) {
            card.setAlpha(0f);
            card.setTranslationY(200);
            card.animate().alpha(1f).translationY(0).setDuration(800).start();
        }

        etTelefono.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_phone, 0, 0, 0);
        etPassword.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_eye, 0);
        etConfirmar.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_eye, 0);

        etPassword.setOnTouchListener((v, event) -> {
            final int DRAWABLE_RIGHT = 2;

            if (event.getAction() == MotionEvent.ACTION_UP &&
                    etPassword.getCompoundDrawables()[DRAWABLE_RIGHT] != null) {

                int width = etPassword.getCompoundDrawables()[DRAWABLE_RIGHT]
                        .getBounds().width();

                if (event.getRawX() >= (etPassword.getRight() - width)) {
                    togglePasswordPass(etPassword);
                    return true;
                }
            }
            return false;
        });

        etConfirmar.setOnTouchListener((v, event) -> {
            final int DRAWABLE_RIGHT = 2;

            if (event.getAction() == MotionEvent.ACTION_UP &&
                    etConfirmar.getCompoundDrawables()[DRAWABLE_RIGHT] != null) {

                int width = etConfirmar.getCompoundDrawables()[DRAWABLE_RIGHT]
                        .getBounds().width();

                if (event.getRawX() >= (etConfirmar.getRight() - width)) {
                    togglePasswordConfirm(etConfirmar);
                    return true;
                }
            }
            return false;
        });

        btnRegistrar.setOnClickListener(v -> registrarCuenta());
        txtLogin.setOnClickListener(v -> finish());
    }

    private void registrarCuenta() {

        String telefono = etTelefono.getText().toString().trim();
        String pass     = etPassword.getText().toString().trim();
        String confirm  = etConfirmar.getText().toString().trim();

        if (telefono.isEmpty()) {
            etTelefono.setError("Introduce teléfono");
            return;
        }

        if (pass.length() < 6) {
            etPassword.setError("Mínimo 6 caracteres");
            return;
        }

        if (!pass.equals(confirm)) {
            etConfirmar.setError("No coinciden");
            return;
        }

        if (!checkPolitica.isChecked()) {
            Toast.makeText(this, "Acepta la política", Toast.LENGTH_SHORT).show();
            return;
        }

        btnRegistrar.setEnabled(false);
        btnRegistrar.setText("Creando...");

        String emailFicticio = telefono + "@app.com";

        mAuth.createUserWithEmailAndPassword(emailFicticio, pass)
                .addOnSuccessListener(authResult -> {

                    String uid = authResult.getUser().getUid();

                    Map<String, Object> usuario = new HashMap<>();
                    usuario.put("telefono", telefono);
                    usuario.put("uid", uid);

                    FirebaseFirestore.getInstance()
                            .collection("usuarios")
                            .document(telefono)
                            .set(usuario)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Cuenta creada", Toast.LENGTH_LONG).show();
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                btnRegistrar.setEnabled(true);
                                btnRegistrar.setText("Registrarse");
                                Toast.makeText(this, "Firestore error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });

                })
                .addOnFailureListener(e -> {
                    btnRegistrar.setEnabled(true);
                    btnRegistrar.setText("Registrarse");
                    Toast.makeText(this, "Auth error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void togglePasswordPass(EditText editText) {
        if (passwordVisiblePass) {
            editText.setInputType(android.text.InputType.TYPE_CLASS_TEXT |
                    android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
            passwordVisiblePass = false;
        } else {
            editText.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            passwordVisiblePass = true;
        }
        editText.setSelection(editText.length());
    }

    private void togglePasswordConfirm(EditText editText) {
        if (passwordVisibleConfirm) {
            editText.setInputType(android.text.InputType.TYPE_CLASS_TEXT |
                    android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
            passwordVisibleConfirm = false;
        } else {
            editText.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            passwordVisibleConfirm = true;
        }
        editText.setSelection(editText.length());
    }
}