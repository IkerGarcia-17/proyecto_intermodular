package dam.iker.padeldart;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class RecuperarPasswordActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recuperar_password);

        Button btnEnviarEmail = findViewById(R.id.btnEnviarEmail);
        Button btnVolverLogin = findViewById(R.id.btnVolverLogin);

        // 1. Acción de Volver (Destruye esta pantalla y vuelve a la anterior)
        btnVolverLogin.setOnClickListener(v -> {
            finish();
        });

        // 2. Acción de enviar correo (Próximamente con Firebase)
        btnEnviarEmail.setOnClickListener(v -> {
            Toast.makeText(RecuperarPasswordActivity.this, "Simulando envío de correo...", Toast.LENGTH_SHORT).show();
        });
    }
}