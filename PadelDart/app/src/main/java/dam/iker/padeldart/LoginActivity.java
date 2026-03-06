package dam.iker.padeldart;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicializamos Firebase
        mAuth = FirebaseAuth.getInstance();

        // Enlazamos la interfaz
        TextInputEditText etCorreo = findViewById(R.id.etCorreoLogin);
        TextInputEditText etPassword = findViewById(R.id.etPasswordLogin);
        Button btnLogin = findViewById(R.id.btnLogin);
        Button btnGoogle = findViewById(R.id.btnGoogle);
        ImageView btnCerrar = findViewById(R.id.btnCerrarApp);
        TextView tvCrearCuenta = findViewById(R.id.tvCrearCuenta);
        TextView tvOlvidePassword = findViewById(R.id.tvOlvidePassword);

        // 1. NAVEGACIÓN
        tvCrearCuenta.setOnClickListener(v -> startActivity(new Intent(this, RegistroActivity.class)));
        tvOlvidePassword.setOnClickListener(v -> startActivity(new Intent(this, RecuperarPasswordActivity.class)));

        // 2. CERRAR LA APLICACIÓN DE GOLPE
        btnCerrar.setOnClickListener(v -> {
            finishAffinity(); // Cierra todas las pantallas abiertas
            System.exit(0);   // Apaga el proceso
        });

        // 3. INICIO DE SESIÓN CON CORREO (El que ya funciona en BD)
        btnLogin.setOnClickListener(v -> {
            String correo = etCorreo.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (correo.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Rellena todos los campos", Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.signInWithEmailAndPassword(correo, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(this, "¡Bienvenido!", Toast.LENGTH_SHORT).show();
                            // Aquí saltamos a la App Principal (MainActivity)
                            startActivity(new Intent(LoginActivity.this, LoginActivity.class));
                            finish(); // Destruimos el Login para que no pueda volver atrás con el botón del móvil
                        } else {
                            Toast.makeText(this, "Error: Correo o contraseña incorrectos", Toast.LENGTH_LONG).show();
                        }
                    });
        });

        // 4. BOTÓN GOOGLE (Preparado para el próximo paso)
        btnGoogle.setOnClickListener(v -> {
            Toast.makeText(this, "Próximamente: Inicio con Google", Toast.LENGTH_SHORT).show();
        });
    }
}