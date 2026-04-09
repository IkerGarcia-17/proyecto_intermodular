package dam.iker.padeldart;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;

// Pantalla de inicio de sesión. Es la Activity de entrada a la app (ver AndroidManifest).
// Antes usaba FirebaseAuth para validar credenciales contra la nube;
// ahora consulta directamente la BD local a través de DatabaseHelper.
public class LoginActivity extends AppCompatActivity {

    private DatabaseHelper db;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Obtenemos las instancias Singleton. No creamos objetos nuevos aquí
        // porque tanto la BD como la sesión se comparten con el resto de Activities.
        db      = DatabaseHelper.getInstance(this);
        session = SessionManager.getInstance(this);

        // --- Enlace con los elementos del layout ---
        TextInputEditText etCorreo   = findViewById(R.id.etCorreoLogin);
        TextInputEditText etPassword = findViewById(R.id.etPasswordLogin);
        Button    btnLogin    = findViewById(R.id.btnLogin);
        Button    btnGoogle   = findViewById(R.id.btnGoogle);
        ImageView btnCerrar   = findViewById(R.id.btnCerrarApp);
        TextView  tvCrearCuenta    = findViewById(R.id.tvCrearCuenta);
        TextView  tvOlvidePassword = findViewById(R.id.tvOlvidePassword);

        // Texto "¿No tienes cuenta?" -> abre el formulario de registro
        tvCrearCuenta.setOnClickListener(v ->
                startActivity(new Intent(this, RegistroActivity.class)));

        // Texto "Olvidé mi contraseña" -> pantalla de recuperación
        tvOlvidePassword.setOnClickListener(v ->
                startActivity(new Intent(this, RecuperarPasswordActivity.class)));

        // La X de la esquina cierra la app por completo.
        // finishAffinity() limpia la pila de Activities y System.exit() mata el proceso.
        btnCerrar.setOnClickListener(v -> {
            finishAffinity();
            System.exit(0);
        });

        // --- Lógica principal de login ---
        btnLogin.setOnClickListener(v -> {
            String correo   = etCorreo.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            // Validación básica antes de tocar la BD
            if (correo.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Rellena todos los campos", Toast.LENGTH_SHORT).show();
                return;
            }

            // iniciarSesion() lanza una query SELECT contra la tabla usuarios.
            // Si encuentra una fila con ese email y contraseña devuelve su ID; si no, -1.
            long userId = db.iniciarSesion(correo, password);

            if (userId != -1) {
                // Guardamos el ID en SharedPreferences para que el resto de la app
                // sepa quién está logueado sin tener que volver a consultar la BD.
                session.iniciarSesion(userId);
                Toast.makeText(this, "¡Bienvenido!", Toast.LENGTH_SHORT).show();

                // TODO: cambiar LoginActivity.class por MainActivity.class cuando esté creada.
                // Por ahora se queda aquí para que el flujo no rompa en tiempo de desarrollo.
                startActivity(new Intent(LoginActivity.this, LoginActivity.class));
                finish(); // Destruimos el login para que no aparezca al pulsar "atrás"
            } else {
                Toast.makeText(this, "Error: Correo o contraseña incorrectos", Toast.LENGTH_LONG).show();
            }
        });

        // El botón de Google está preparado visualmente pero aún sin implementar.
        btnGoogle.setOnClickListener(v ->
                Toast.makeText(this, "Próximamente: Inicio con Google", Toast.LENGTH_SHORT).show());
    }
}
