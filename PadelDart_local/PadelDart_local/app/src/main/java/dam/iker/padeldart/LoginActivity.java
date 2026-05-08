package dam.iker.padeldart;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;

// Pantalla de inicio de sesión. Es el launcher de la app (ver AndroidManifest).
public class LoginActivity extends AppCompatActivity {

    private static final String TAG           = "LoginActivity";
    // Preferencias para "Recordar usuario": guarda el email entre sesiones
    private static final String PREFS_LOGIN   = "padeldart_login";
    private static final String KEY_RECORDAR  = "recordar";
    private static final String KEY_EMAIL     = "email_recordado";

    private DatabaseHelper db;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Volvemos al tema principal antes de inflar: la splash ya se mostró como windowBackground
        setTheme(R.style.Theme_PadelDart);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db      = DatabaseHelper.getInstance(this);
        session = SessionManager.getInstance(this);

        // Si hay sesión activa, intentamos ir al menú principal.
        // Usamos un try-catch para que si MainActivity falla, no se quede
        // en un loop infinito (LoginActivity siempre volvería a intentarlo).
        if (session.haySesionActiva()) {
            Log.d(TAG, "Sesión activa encontrada, intentando ir a MainActivity");
            try {
                irAMainActivity();
                return;
            } catch (Exception e) {
                // Si algo falla al redirigir, borramos la sesión y mostramos el login normal
                Log.e(TAG, "Error al redirigir a MainActivity: " + e.getMessage());
                session.cerrarSesion();
            }
        }

        // --- Enlace con los elementos del layout ---
        TextInputEditText etCorreo   = findViewById(R.id.etCorreoLogin);
        TextInputEditText etPassword = findViewById(R.id.etPasswordLogin);
        Button    btnLogin    = findViewById(R.id.btnLogin);
        Button    btnGoogle   = findViewById(R.id.btnGoogle);
        ImageView btnCerrar   = findViewById(R.id.btnCerrarApp);
        TextView  tvCrearCuenta    = findViewById(R.id.tvCrearCuenta);
        TextView  tvOlvidePassword = findViewById(R.id.tvOlvidePassword);
        CheckBox  cbRecordar       = findViewById(R.id.cbRecordarUsuario);

        // Si el usuario pidió recordar su correo en el inicio anterior, lo precargamos
        SharedPreferences loginPrefs = getSharedPreferences(PREFS_LOGIN, MODE_PRIVATE);
        boolean recordado = loginPrefs.getBoolean(KEY_RECORDAR, false);
        if (recordado) {
            String emailGuardado = loginPrefs.getString(KEY_EMAIL, "");
            etCorreo.setText(emailGuardado);
            if (cbRecordar != null) cbRecordar.setChecked(true);
        }

        tvCrearCuenta.setOnClickListener(v ->
                startActivity(new Intent(this, RegistroActivity.class)));

        tvOlvidePassword.setOnClickListener(v ->
                startActivity(new Intent(this, RecuperarPasswordActivity.class)));

        // La X cierra la app por completo
        btnCerrar.setOnClickListener(v -> {
            finishAffinity();
            System.exit(0);
        });

        // --- Lógica de login ---
        btnLogin.setOnClickListener(v -> {
            String correo   = etCorreo.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (correo.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, getString(R.string.login_error_campos), Toast.LENGTH_SHORT).show();
                return;
            }

            int totalUsuarios = db.contarUsuarios();
            Log.d(TAG, "Intentando login con: " + correo + " (usuarios en BD: " + totalUsuarios + ")");

            long userId = db.iniciarSesion(correo, password);
            Log.d(TAG, "Resultado iniciarSesion: " + userId);

            if (userId != -1) {
                // Guardamos el email si "Recordar usuario" está marcado
                boolean marcar = cbRecordar != null && cbRecordar.isChecked();
                loginPrefs.edit()
                        .putBoolean(KEY_RECORDAR, marcar)
                        .putString(KEY_EMAIL, marcar ? correo : "")
                        .apply();

                session.iniciarSesion(userId);
                Toast.makeText(this, getString(R.string.login_bienvenido), Toast.LENGTH_SHORT).show();
                irAMainActivity();
            } else {
                String msg = totalUsuarios == 0
                        ? getString(R.string.login_error_no_cuentas)
                        : getString(R.string.login_error_credenciales);
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
            }
        });

        btnGoogle.setOnClickListener(v ->
                Toast.makeText(this, getString(R.string.login_proximamente_google), Toast.LENGTH_SHORT).show());
    }

    // Navega a la pantalla principal y cierra el Login para que el Atrás no vuelva aquí.
    // Sin FLAG_ACTIVITY_CLEAR_TASK: si MainActivity falla, se muestra el error normalmente
    // en vez de volver a LoginActivity de forma silenciosa.
    private void irAMainActivity() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
