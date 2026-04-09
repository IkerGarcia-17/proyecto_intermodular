package dam.iker.padeldart;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

// Pantalla de recuperación de contraseña. De momento es una pantalla de reserva:
// el botón de envío muestra un Toast pero no hace nada real todavía.
// Cuando se implemente habrá que decidir si se hace por email (requiere servidor SMTP)
// o simplemente mostrando la contraseña directamente desde la BD local.
public class RecuperarPasswordActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recuperar_password);

        Button btnEnviarEmail = findViewById(R.id.btnEnviarEmail);
        Button btnVolverLogin = findViewById(R.id.btnVolverLogin);

        // finish() destruye esta Activity y vuelve automáticamente al Login,
        // que es la pantalla anterior en la pila de Activities.
        btnVolverLogin.setOnClickListener(v -> finish());

        // TODO: implementar recuperación real. Por ahora solo simula el envío.
        btnEnviarEmail.setOnClickListener(v ->
                Toast.makeText(this, "Simulando envío de correo...", Toast.LENGTH_SHORT).show());
    }
}
