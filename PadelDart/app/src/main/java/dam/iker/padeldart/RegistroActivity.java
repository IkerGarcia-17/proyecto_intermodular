package dam.iker.padeldart;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.slider.Slider;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class RegistroActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registro);

        mAuth = FirebaseAuth.getInstance();
        FirebaseApp app = FirebaseApp.getInstance();
        db = FirebaseFirestore.getInstance(app, "default");

        // 1. Enlaces Personales
        TextInputEditText etNombre = findViewById(R.id.etNombre);
        TextInputEditText etApellidos = findViewById(R.id.etApellidos);
        TextInputEditText etDni = findViewById(R.id.etDni);
        TextInputEditText etCorreo = findViewById(R.id.etCorreoReg);
        TextInputEditText etPass = findViewById(R.id.etPassReg);
        TextInputEditText etPass2 = findViewById(R.id.etPassReg2);
        TextInputEditText etDireccion = findViewById(R.id.etDireccion);
        TextInputEditText etCP = findViewById(R.id.etCP);

        // Dropdown de Pago
        AutoCompleteTextView dropdownPago = findViewById(R.id.dropdownPago);
        String[] opcionesPago = {"Tarjeta Bancaria", "PayPal", "Bizum", "Efectivo"};
        ArrayAdapter<String> adapterPago = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, opcionesPago);
        dropdownPago.setAdapter(adapterPago);

        // 2. Enlaces Pádel
        Slider sliderNivel = findViewById(R.id.sliderNivel);
        TextView tvValorNivel = findViewById(R.id.tvValorNivel);
        Button btnRestar = findViewById(R.id.btnRestarNivel);
        Button btnSumar = findViewById(R.id.btnSumarNivel);
        ImageView ivInfoNivel = findViewById(R.id.ivInfoNivel);
        RadioGroup rgPosicion = findViewById(R.id.rgPosicionPadel);
        RadioButton rbDrive = findViewById(R.id.rbDrive);
        RadioButton rbReves = findViewById(R.id.rbReves);
        TextInputEditText etPista = findViewById(R.id.etPista);

        // 3. Enlaces Dardos
        CheckBox cbTieneDiana = findViewById(R.id.cbTieneDiana);
        RadioGroup rgTipoDiana = findViewById(R.id.rgTipoDiana);
        RadioButton rbPelo = findViewById(R.id.rbDianaPelo);
        RadioButton rbElectronica = findViewById(R.id.rbDianaElectronica);
        RadioButton rbAmbas = findViewById(R.id.rbDianaAmbas);

        Button btnCrear = findViewById(R.id.btnCrearCuentaDef);

        // --- FUNCIONES VISUALES (Info, Slider, Diana) ---
        ivInfoNivel.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Niveles de Pádel")
                    .setMessage("Nivel 1.0: Acabas de empezar y no tienes experiencia previa. " +
                            "\nNivel 1.5: Has tenido tus primeras clases, pero te cuesta mantener la pelota en juego. " +
                            "\nNivel 2.0: Empiezas a entender conceptos básicos, pero tus golpes, sobre todo el revés, son poco controlados. " +
                            "\nNivel 2.5: Mejoras la derecha, empiezas a usar el revés aunque aún con errores. Tus voleas de derecha son más fiables. " +
                            "\nNivel 3.0: Derecha sólida, revés aceptable. Buenas voleas de derecha. Empiezas a dominar el globo y los rebotes lentos. " +
                            "\nNivel: 3.5: Golpes más potentes y variados. Mejor volea en red y posicionamiento. Bandejas decentes. " +
                            "\nNivel 4.0: Excelente derecha y revés. Saque agresivo, voleas profundas y empiezas a probarte en el remate. " +
                            "\nNivel 4.5: Buen control y colocación en la mayoría de golpes. Saques seguros y bajadas de pared eficaces. " +
                            "\nNivel 5.0: Dominio completo de derecha y de revés. Remates y voleas agresivas. " +
                            "\nNivel 5.5: Mismo nivel técnico pero con mayor capacidad de anticipación, consistencia y lectura táctica del partido. " +
                            "\nNivel 6.0 y 7.0: Jugadores federados o de circuito. Entrenan a diario, compiten en torneos regionales (nivel 6.0) o internacionales (nivel 7.0). Dominan todos los aspectos técnicos, físicos y tácticos del pádel.")
                    .setPositiveButton("Entendido", (dialog, which) -> dialog.dismiss()).show();
        });

        sliderNivel.addOnChangeListener((slider, value, fromUser) -> {
            tvValorNivel.setText(String.format(Locale.getDefault(), "Nivel: %.2f", value));
        });

        btnRestar.setOnClickListener(v -> {
            if (sliderNivel.getValue() > 0.0f) {
                sliderNivel.setValue(Math.round((sliderNivel.getValue() - 0.01f) * 100.0f) / 100.0f);
            }
        });

        btnSumar.setOnClickListener(v -> {
            if (sliderNivel.getValue() < 7.0f) {
                sliderNivel.setValue(Math.round((sliderNivel.getValue() + 0.01f) * 100.0f) / 100.0f);
            }
        });

        cbTieneDiana.setOnCheckedChangeListener((buttonView, isChecked) -> {
            rgTipoDiana.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            if (!isChecked) rgTipoDiana.clearCheck();
        });

        // --- BOTÓN CREAR CUENTA Y LÓGICA DE BASE DE DATOS ---
        btnCrear.setOnClickListener(v -> {
            // Capturar básicos
            String correo = etCorreo.getText().toString().trim();
            String pass = etPass.getText().toString().trim();

            if (correo.isEmpty() || pass.isEmpty() || !pass.equals(etPass2.getText().toString().trim())) {
                Toast.makeText(this, "Revisa correo y contraseñas", Toast.LENGTH_SHORT).show();
                return;
            }

            // REDONDEO PERFECTO DEL NIVEL (Solución al 3.529999)
            double nivelPuntaje = Math.round(sliderNivel.getValue() * 100.0) / 100.0;

            // CÁLCULO INTELIGENTE DE CATEGORÍA
            String categoriaCalculada;
            if (nivelPuntaje >= 6.0) categoriaCalculada = "1ª Categoría";
            else if (nivelPuntaje >= 5.0) categoriaCalculada = "2ª Categoría";
            else if (nivelPuntaje >= 4.0) categoriaCalculada = "3ª Categoría";
            else if (nivelPuntaje >= 3.0) categoriaCalculada = "4ª Categoría";
            else if (nivelPuntaje >= 2.0) categoriaCalculada = "5ª Categoría";
            else if (nivelPuntaje >= 1.0) categoriaCalculada = "5ªB Categoría";
            else categoriaCalculada = "Iniciación";

            // POSICIÓN PÁDEL
            String posicion = "";
            if (rbDrive.isChecked()) posicion = "Drive";
            if (rbReves.isChecked()) posicion = "Revés";

            // DARDOS DIANA
            String tipoDiana = "";
            if (cbTieneDiana.isChecked()) {
                if (rbPelo.isChecked()) tipoDiana = "Tradicional";
                else if (rbElectronica.isChecked()) tipoDiana = "Electrónica";
                else if (rbAmbas.isChecked()) tipoDiana = "Ambas";
                else {
                    Toast.makeText(this, "Selecciona el tipo de diana", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            // A Firebase
            String finalTipoDiana = tipoDiana;
            String finalPosicion = posicion;
            String finalCategoria = categoriaCalculada;

            mAuth.createUserWithEmailAndPassword(correo, pass)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            String uid = mAuth.getCurrentUser().getUid();

                            // ESTRUCTURA MAESTRA DE LA BASE DE DATOS
                            Map<String, Object> usuario = new HashMap<>();

                            // 1. Personales
                            usuario.put("email", correo);
                            usuario.put("nombre", etNombre.getText().toString().trim());
                            usuario.put("apellidos", etApellidos.getText().toString().trim());
                            usuario.put("dni", etDni.getText().toString().trim());
                            usuario.put("direccion", etDireccion.getText().toString().trim());
                            usuario.put("cp", etCP.getText().toString().trim());
                            usuario.put("metodo_pago", dropdownPago.getText().toString().trim());

                            // 2. Pádel
                            Map<String, Object> statsPadel = new HashMap<>();
                            statsPadel.put("categoria_actual", finalCategoria);
                            statsPadel.put("puntuacion_media", nivelPuntaje);
                            statsPadel.put("total_valoraciones", 1); // 1 porque la inicial ya cuenta
                            statsPadel.put("numero_clases_dadas", 0);
                            statsPadel.put("posicion", finalPosicion);
                            statsPadel.put("pista_favorita", etPista.getText().toString().trim());
                            usuario.put("perfil_padel", statsPadel);

                            // 3. Dardos
                            Map<String, Object> statsDardos = new HashMap<>();
                            statsDardos.put("media_puntos_tiradas", 0.00); // x.xx format
                            statsDardos.put("numero_partidas_pvp_jugadas", 0);
                            statsDardos.put("numero_partidas_pve_jugadas", 0);
                            statsDardos.put("numero_partidas_pvp_ganadas", 0);
                            statsDardos.put("numero_partidas_pve_ganadas", 0);
                            statsDardos.put("tiene_diana_propia", cbTieneDiana.isChecked());
                            if (cbTieneDiana.isChecked()) {
                                statsDardos.put("tipo_diana", finalTipoDiana);
                            }
                            usuario.put("perfil_dardos", statsDardos);

                            // Guardar
                            db.collection("Usuarios").document(uid).set(usuario)
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(this, "¡Cuenta Creada Perfectamente!", Toast.LENGTH_LONG).show();
                                        mAuth.signOut();
                                        finish();
                                    })
                                    .addOnFailureListener(e -> Toast.makeText(this, "Error Base Datos", Toast.LENGTH_SHORT).show());
                        } else {
                            Toast.makeText(this, "Error: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        });
    }
}