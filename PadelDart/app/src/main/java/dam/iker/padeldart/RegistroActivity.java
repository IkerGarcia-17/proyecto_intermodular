package dam.iker.padeldart;

import android.content.Intent;
import android.net.Uri;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RegistroActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // Desplegables y botón del mapa
    private AutoCompleteTextView dropdownProvincia;
    private AutoCompleteTextView dropdownClub;
    private Button btnVerMapa;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registro);

        // Inicializamos Firebase
        mAuth = FirebaseAuth.getInstance();
        FirebaseApp app = FirebaseApp.getInstance();
        db = FirebaseFirestore.getInstance(app, "default");

        // ==========================================
        // 1. ENLACES PERSONALES
        // ==========================================
        TextInputEditText etNombre = findViewById(R.id.etNombre);
        TextInputEditText etApellidos = findViewById(R.id.etApellidos);
        TextInputEditText etDni = findViewById(R.id.etDni);
        TextInputEditText etCorreo = findViewById(R.id.etCorreoReg);
        TextInputEditText etPass = findViewById(R.id.etPassReg);
        TextInputEditText etPass2 = findViewById(R.id.etPassReg2);
        TextInputEditText etDireccion = findViewById(R.id.etDireccion);
        TextInputEditText etCP = findViewById(R.id.etCP);

        AutoCompleteTextView dropdownPago = findViewById(R.id.dropdownPago);
        String[] opcionesPago = {"Tarjeta Bancaria", "PayPal", "Bizum", "Efectivo"};
        ArrayAdapter<String> adapterPago = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, opcionesPago);
        dropdownPago.setAdapter(adapterPago);

        // ==========================================
        // 2. ENLACES PÁDEL
        // ==========================================
        Slider sliderNivel = findViewById(R.id.sliderNivel);
        TextView tvValorNivel = findViewById(R.id.tvValorNivel);
        Button btnRestar = findViewById(R.id.btnRestarNivel);
        Button btnSumar = findViewById(R.id.btnSumarNivel);
        ImageView ivInfoNivel = findViewById(R.id.ivInfoNivel);
        RadioGroup rgPosicion = findViewById(R.id.rgPosicionPadel);
        RadioButton rbDrive = findViewById(R.id.rbDrive);
        RadioButton rbReves = findViewById(R.id.rbReves);

        dropdownProvincia = findViewById(R.id.dropdownProvinciaPadel);
        dropdownClub = findViewById(R.id.dropdownClubPadel);
        btnVerMapa = findViewById(R.id.btnVerMapa); // El nuevo botón del mapa

        // ==========================================
        // 3. ENLACES DARDOS
        // ==========================================
        CheckBox cbTieneDiana = findViewById(R.id.cbTieneDiana);
        RadioGroup rgTipoDiana = findViewById(R.id.rgTipoDiana);
        RadioButton rbPelo = findViewById(R.id.rbDianaPelo);
        RadioButton rbElectronica = findViewById(R.id.rbDianaElectronica);
        RadioButton rbAmbas = findViewById(R.id.rbDianaAmbas);

        Button btnCrear = findViewById(R.id.btnCrearCuentaDef);

        // ==========================================
        // INICIALIZAR LÓGICA DE DESPLEGABLES Y MAPA
        // ==========================================
        configurarDesplegablesPadel();

        // ==========================================
        // LÓGICA VISUAL Y EVENTOS
        // ==========================================
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

        // ==========================================
        // BOTÓN CREAR CUENTA
        // ==========================================
        btnCrear.setOnClickListener(v -> {
            String correo = etCorreo.getText().toString().trim();
            String pass = etPass.getText().toString().trim();

            if (correo.isEmpty() || pass.isEmpty() || !pass.equals(etPass2.getText().toString().trim())) {
                Toast.makeText(this, "Revisa correo y contraseñas", Toast.LENGTH_SHORT).show();
                return;
            }

            double nivelPuntaje = Math.round(sliderNivel.getValue() * 100.0) / 100.0;
            String categoriaCalculada;
            if (nivelPuntaje >= 6.0) categoriaCalculada = "1ª Categoría";
            else if (nivelPuntaje >= 5.0) categoriaCalculada = "2ª Categoría";
            else if (nivelPuntaje >= 4.0) categoriaCalculada = "3ª Categoría";
            else if (nivelPuntaje >= 3.0) categoriaCalculada = "4ª Categoría";
            else if (nivelPuntaje >= 2.0) categoriaCalculada = "5ª Categoría";
            else if (nivelPuntaje >= 1.0) categoriaCalculada = "5ªB Categoría";
            else categoriaCalculada = "Iniciación";

            String posicion = "";
            if (rbDrive.isChecked()) posicion = "Drive";
            if (rbReves.isChecked()) posicion = "Revés";

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

            String finalTipoDiana = tipoDiana;
            String finalPosicion = posicion;
            String finalCategoria = categoriaCalculada;

            mAuth.createUserWithEmailAndPassword(correo, pass)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            String uid = mAuth.getCurrentUser().getUid();
                            Map<String, Object> usuario = new HashMap<>();

                            usuario.put("email", correo);
                            usuario.put("nombre", etNombre.getText().toString().trim());
                            usuario.put("apellidos", etApellidos.getText().toString().trim());
                            usuario.put("dni", etDni.getText().toString().trim());
                            usuario.put("direccion", etDireccion.getText().toString().trim());
                            usuario.put("cp", etCP.getText().toString().trim());
                            usuario.put("metodo_pago", dropdownPago.getText().toString().trim());

                            Map<String, Object> statsPadel = new HashMap<>();
                            statsPadel.put("categoria_actual", finalCategoria);
                            statsPadel.put("puntuacion_media", nivelPuntaje);
                            statsPadel.put("total_valoraciones", 1);
                            statsPadel.put("numero_clases_dadas", 0);
                            statsPadel.put("posicion", finalPosicion);
                            statsPadel.put("provincia", dropdownProvincia.getText().toString().trim());
                            statsPadel.put("pista_favorita", dropdownClub.getText().toString().trim());
                            usuario.put("perfil_padel", statsPadel);

                            Map<String, Object> statsDardos = new HashMap<>();
                            statsDardos.put("media_puntos_tiradas", 0.00);
                            statsDardos.put("numero_partidas_pvp_jugadas", 0);
                            statsDardos.put("numero_partidas_pve_jugadas", 0);
                            statsDardos.put("numero_partidas_pvp_ganadas", 0);
                            statsDardos.put("numero_partidas_pve_ganadas", 0);
                            statsDardos.put("tiene_diana_propia", cbTieneDiana.isChecked());
                            if (cbTieneDiana.isChecked()) {
                                statsDardos.put("tipo_diana", finalTipoDiana);
                            }
                            usuario.put("perfil_dardos", statsDardos);

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

    // ==========================================
    // MÉTODOS DE MAPA Y DICCIONARIO
    // ==========================================
    private void configurarDesplegablesPadel() {
        String[] listaProvincias = {"A Coruña", "Álava", "Albacete", "Alicante", "Almería", "Asturias", "Ávila", "Badajoz", "Baleares", "Barcelona", "Burgos", "Cáceres", "Cádiz", "Cantabria", "Castellón", "Ceuta", "Ciudad Real", "Córdoba", "Cuenca", "Girona", "Granada", "Guadalajara", "Gipuzkoa", "Huelva", "Huesca", "Jaén", "La Rioja", "Las Palmas", "León", "Lleida", "Lugo", "Madrid", "Málaga", "Melilla", "Murcia", "Navarra", "Ourense", "Palencia", "Pontevedra", "Salamanca", "Santa Cruz de Tenerife", "Segovia", "Sevilla", "Soria", "Tarragona", "Teruel", "Toledo", "Valencia", "Valladolid", "Vizcaya", "Zamora", "Zaragoza"};

        ArrayAdapter<String> adapterProvincias = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                listaProvincias
        );
        dropdownProvincia.setAdapter(adapterProvincias);

        // Al elegir provincia
        dropdownProvincia.setOnItemClickListener((parent, view, position, id) -> {
            String provinciaSeleccionada = (String) parent.getItemAtPosition(position);

            List<String> clubes = obtenerPistasPorProvincia(provinciaSeleccionada);

            dropdownClub.setEnabled(true);
            ArrayAdapter<String> adapterClubes = new ArrayAdapter<>(
                    RegistroActivity.this,
                    android.R.layout.simple_dropdown_item_1line,
                    clubes
            );
            dropdownClub.setAdapter(adapterClubes);
            dropdownClub.setText("", false);

            btnVerMapa.setVisibility(View.GONE);
            dropdownClub.showDropDown();
        });

        // Al elegir club
        dropdownClub.setOnItemClickListener((parent, view, position, id) -> {
            String clubSeleccionado = (String) parent.getItemAtPosition(position);

            if (!clubSeleccionado.equals("Otro club / Pista de mi urbanización")) {
                btnVerMapa.setVisibility(View.VISIBLE);
            } else {
                btnVerMapa.setVisibility(View.GONE);
            }
        });

// Al pulsar el botón del mapa
        btnVerMapa.setOnClickListener(v -> {
            String club = dropdownClub.getText().toString();
            String provincia = dropdownProvincia.getText().toString();

            Uri gmmIntentUri = Uri.parse("geo:0,0?q=" + club + ", " + provincia);
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            mapIntent.setPackage("com.google.android.apps.maps");

            // MÉTODO MODERNO (A prueba de Android 11+)
            try {
                // Intentamos abrir Google Maps directamente
                startActivity(mapIntent);
            } catch (android.content.ActivityNotFoundException e) {
                // Si falla (porque no lo tiene instalado de verdad), avisamos
                Toast.makeText(this, "No tienes Google Maps instalado", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private List<String> obtenerPistasPorProvincia(String provincia) {
        Map<String, List<String>> baseDatosPistas = new HashMap<>();

        baseDatosPistas.put("Almería", Arrays.asList("ProPadel Indoor Club", "Club Deportivo Los Molinos", "Club Natación Almería", "Ego Sport Center"));
        baseDatosPistas.put("Cádiz", Arrays.asList("Pádel KD (Jerez)", "Las Salinas (San Fernando)", "Padel La Central (El Puerto)", "Club de Pádel La Barrosa", "Centro Deportivo Liceo"));
        baseDatosPistas.put("Córdoba", Arrays.asList("Play Center Córdoba", "Padel Indoor Córdoba", "Club Sierra Morena", "Open Arena"));
        baseDatosPistas.put("Granada", Arrays.asList("Padel Sport Granada Indoor", "Club Campus Pádel", "We Fitness Club", "O2 Centro Wellness"));
        baseDatosPistas.put("Huelva", Arrays.asList("Padel 7 Huelva", "Club Padel Indoor Huelva", "Real Club Marítimo de Huelva"));
        baseDatosPistas.put("Jaén", Arrays.asList("Padel Premium Jaén", "Club Padel Indoor Jaén", "Padel Akelarre"));
        baseDatosPistas.put("Málaga", Arrays.asList("Reserva del Higuerón", "Los Caballeros", "Vals Sport", "Padel Picasso", "Inacua Málaga"));
        baseDatosPistas.put("Sevilla", Arrays.asList("Club Río Grande", "Sato Sport", "Padel Premium Sevilla", "Bernier (Coria)", "La Cartuja"));
        baseDatosPistas.put("Huesca", Arrays.asList("Padel Indoor Huesca", "Club Tenis Osca", "Padel 360", "Zone Padel"));
        baseDatosPistas.put("Teruel", Arrays.asList("Padel Teruel Indoor", "Club de Tenis Teruel", "Padel Calamocha"));
        baseDatosPistas.put("Zaragoza", Arrays.asList("Padel Plaza Indoor", "Pádel Zaragoza", "Montecanal Centro Deportivo", "Regal Padel Club"));
        baseDatosPistas.put("Asturias", Arrays.asList("Paidesport Center", "Padel Indoor Oviedo", "Club Padel Gijón", "Cover Padel"));
        baseDatosPistas.put("Cantabria", Arrays.asList("Padel Besaya", "Club Parayas", "Gofit Santander", "Padel Indoor Camargo"));
        baseDatosPistas.put("Baleares", Arrays.asList("Pins Padel Club", "Palma Padel", "Padel Go In", "Fit Point"));
        baseDatosPistas.put("Las Palmas", Arrays.asList("El Cortijo Club de Campo", "Padel Indoor Gran Canaria", "Club La Calzada"));
        baseDatosPistas.put("Santa Cruz de Tenerife", Arrays.asList("Padel Canary", "Tecina Padel", "Las Palmeras", "Pádel Indoor Tenerife"));
        baseDatosPistas.put("Albacete", Arrays.asList("Club Padel Albacete", "Padel Indoor Albacete", "Tiro Pichón Albacete"));
        baseDatosPistas.put("Ciudad Real", Arrays.asList("Padel Center Ciudad Real", "Padel Plus", "Club de Ocio Nudos"));
        baseDatosPistas.put("Cuenca", Arrays.asList("Padel Cuenca", "Nuevo Tenis Cuenca", "Pádel Indoor Cuenca"));
        baseDatosPistas.put("Guadalajara", Arrays.asList("Pádel Cabanillas Golf", "Pádel Indoor Guadalajara", "Ciudad de la Raqueta"));
        baseDatosPistas.put("Toledo", Arrays.asList("Padel Indoor Toledo", "Imperial Padel Indoor", "Club Monteverde"));
        baseDatosPistas.put("Ávila", Arrays.asList("Padel Indoor Ávila", "Espacio Padel Ávila", "Tres60 Padel"));
        baseDatosPistas.put("Burgos", Arrays.asList("Padel Arena Burgos", "Padel Indoor Burgos", "Club Tenis Burgos"));
        baseDatosPistas.put("León", Arrays.asList("Padel Park León", "Central Padel León", "Tenis5padel Indoor"));
        baseDatosPistas.put("Palencia", Arrays.asList("Padel San Antonio", "Padel Palencia", "Club Padel Charca"));
        baseDatosPistas.put("Salamanca", Arrays.asList("Padel Home Salamanca", "Pádel Time", "Club Pádel Helmántico"));
        baseDatosPistas.put("Segovia", Arrays.asList("Padel Zone Segovia", "Padel Segovia", "Padel Indoor Segovia"));
        baseDatosPistas.put("Soria", Arrays.asList("Padel Soria", "Pádel Indoor Soria"));
        baseDatosPistas.put("Valladolid", Arrays.asList("Padel Arena Valladolid", "Club Raqueta Valladolid", "Padel D10", "Zaratán Padel"));
        baseDatosPistas.put("Zamora", Arrays.asList("Padel Indoor Zamora", "Zamora Padel", "Ciudad Deportiva Zamora"));
        baseDatosPistas.put("Barcelona", Arrays.asList("Star's Padel", "Padel Indoor Hospitalet", "Artós Sports Club", "Augusta Padel", "Padelarium"));
        baseDatosPistas.put("Girona", Arrays.asList("Padel Girona", "Padel Indoor Figueres", "Club de Tenis Girona", "Padel Costa Brava"));
        baseDatosPistas.put("Lleida", Arrays.asList("Padel Indoor Lleida", "Waps Padel", "Club Tennis Urgell"));
        baseDatosPistas.put("Tarragona", Arrays.asList("Tarragona Padel Indoor", "Padel Cambrils", "Club Tenis Tarragona", "Padel Reus"));
        baseDatosPistas.put("Alicante", Arrays.asList("Padelpoint (La Nucía)", "Blupadel", "Pádel Play San Vicente" , "San Jerónimo" , "Pádel Lacy" , "PadelCoca" , "Pádel FondoNet" , "Oxygen Club de Campo" , "Alonka Indoor" , "Centro Excursionista Eldense" , "Ecomm Padel Club" , "Ipadel (Elche)", "Club de Campo", "Padel Club Alicante"));
        baseDatosPistas.put("Castellón", Arrays.asList("Padel Center Castellón", "Jubilama Padel", "Padel Indoor Castellón", "Impala Sport Club"));
        baseDatosPistas.put("Valencia", Arrays.asList("Family Sport Center", "7Padel", "Sportcity", "Suma Fitness Club", "Bergamonte"));
        baseDatosPistas.put("Badajoz", Arrays.asList("El Corzo", "Padel Indoor Badajoz", "Golf Guadiana", "Padel Center Extremadura"));
        baseDatosPistas.put("Cáceres", Arrays.asList("Padel Indoor Cáceres", "Club de Tenis Cabezarrubia", "Padel Center Cáceres"));
        baseDatosPistas.put("A Coruña", Arrays.asList("Coruña Sport Centre", "Padel Plus", "Let Padel", "Padel Prix"));
        baseDatosPistas.put("Lugo", Arrays.asList("Padel Nuestro Lugo", "Club Fluvial", "D10 Lugo"));
        baseDatosPistas.put("Ourense", Arrays.asList("Padel Ourense", "Pádel Prix Ourense", "Padel Indoor Ourense"));
        baseDatosPistas.put("Pontevedra", Arrays.asList("Padelstop (Vigo)", "Mercantil de Vigo", "Padel Indoor Poniente", "Padel Nuestro Pontevedra"));
        baseDatosPistas.put("Madrid", Arrays.asList("Ciudad de la Raqueta", "Mad4Padel", "Euroindoor", "La Masó Sports Club", "Padel 2.0", "Sanset Padel", "El Estudiante"));
        baseDatosPistas.put("Murcia", Arrays.asList("Padel Nuestro Club", "Padel Center Murcia", "Olimpic Club", "Verdolay Padel"));
        baseDatosPistas.put("Navarra", Arrays.asList("Navarra Padel Máster Club", "Arena Entrena Padel", "Ciudad Deportiva Amaya", "Pádel Reyno de Navarra"));
        baseDatosPistas.put("La Rioja", Arrays.asList("Padel Indoor La Rioja", "Las Norias", "La Grajera Padel", "Alos Padel"));
        baseDatosPistas.put("Álava", Arrays.asList("Padel Ebro", "Bakh Baskonia", "Padel Norte"));
        baseDatosPistas.put("Gipuzkoa", Arrays.asList("Padel Indoor Bidasoa", "Pádel San Sebastián", "Pádel Zubieta"));
        baseDatosPistas.put("Vizcaya", Arrays.asList("Padel & Gol", "Padel Derio", "Esmas Padel", "Pádel Indoor Center"));
        baseDatosPistas.put("Ceuta", Arrays.asList("Club Loma Margarita", "Parque Marítimo", "Padel Ceuta Center"));
        baseDatosPistas.put("Melilla", Arrays.asList("Centro Autonómico de Pádel", "La Hípica", "Padel Melilla Indoor"));

        if (baseDatosPistas.containsKey(provincia)) {
            List<String> pistas = new ArrayList<>(baseDatosPistas.get(provincia));
            Collections.sort(pistas);
            pistas.add("Otro club / Pista de mi urbanización");
            return pistas;
        }

        return Arrays.asList("Polideportivo Municipal", "Club de Pádel Local", "Otro club / Pista de mi urbanización");
    }
}