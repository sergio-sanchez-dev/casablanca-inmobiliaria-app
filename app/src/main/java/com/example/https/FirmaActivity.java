package com.example.https;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class FirmaActivity extends AppCompatActivity {

    FirmaView firmaView;
    Button btnLimpiar, btnConfirmar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_firma);

        firmaView    = findViewById(R.id.firmaView);
        btnLimpiar   = findViewById(R.id.btnLimpiar);
        btnConfirmar = findViewById(R.id.btnConfirmar);

        btnLimpiar.setOnClickListener(v -> firmaView.limpiar());

        btnConfirmar.setOnClickListener(v -> {
            if (firmaView.estaVacia()) {
                Toast.makeText(this, "Por favor firme antes de confirmar", Toast.LENGTH_SHORT).show();
                return;
            }
            Toast.makeText(this, "Firma registrada ✓", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    // ✅ Vista personalizada para dibujar la firma
    public static class FirmaView extends View {

        private Paint paint;
        private Path path;
        private boolean firmaTrazada = false;

        public FirmaView(Context context) {
            super(context);
            init();
        }

        public FirmaView(Context context, AttributeSet attrs) {
            super(context, attrs);
            init();
        }

        private void init() {
            paint = new Paint();
            paint.setColor(Color.BLACK);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(5f);
            paint.setAntiAlias(true);
            path = new Path();
            setBackgroundColor(Color.WHITE);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            canvas.drawPath(path, paint);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            float x = event.getX();
            float y = event.getY();
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    path.moveTo(x, y);
                    firmaTrazada = true;
                    break;
                case MotionEvent.ACTION_MOVE:
                    path.lineTo(x, y);
                    break;
                case MotionEvent.ACTION_UP:
                    break;
            }
            invalidate();
            return true;
        }

        public void limpiar() {
            path.reset();
            firmaTrazada = false;
            invalidate();
        }

        public boolean estaVacia() {
            return !firmaTrazada;
        }

        public Bitmap getBitmap() {
            Bitmap bitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            draw(canvas);
            return bitmap;
        }
    }
}