package dam.iker.padeldart;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.Color;
import android.text.Editable;
import android.text.TextWatcher;
// Importamos Handler y Looper para controlar los tiempos de espera
import android.os.Handler;
import android.os.Looper;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.slider.Slider;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

// Clase principal que maneja todo el registro de golpe ocultando y mostrando paneles
public class RegistroActivity extends AppCompatActivity {

    private DatabaseHelper db;
    // Objeto central donde vamos metiendo todo lo que el usuario rellena
    private UsuarioRegistro usuario = new UsuarioRegistro();

    // Contenedores principales de cada paso del registro en el XML
    private View layoutPaso1, layoutPaso2, layoutPaso3, layoutPaso4;
    private AutoCompleteTextView dropdownProvincia, dropdownClub;
    private Button btnVerMapa;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registro);

        // Pillamos la instancia de la base de datos nada más empezar
        db = DatabaseHelper.getInstance(this);

        // Enlazamos las vistas principales que actúan de "pantallas"
        layoutPaso1 = findViewById(R.id.layoutPaso1);
        layoutPaso2 = findViewById(R.id.layoutPaso2);
        layoutPaso3 = findViewById(R.id.layoutPaso3);
        layoutPaso4 = findViewById(R.id.layoutPaso4);

        // Este es el botón global de arriba a la derecha para salir corriendo
        findViewById(R.id.btnVolverLoginGlobal).setOnClickListener(v -> finish());

        // Inicializamos los componentes de cada paso
        configurarPaso1();
        configurarPaso2();
        configurarPaso3();
        configurarPaso4();

        // Controlamos el botón físico "Atrás" del móvil para no salir de golpe
        configurarBotonFisicoAtras();
    }

    private void configurarBotonFisicoAtras() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Si estamos en un paso avanzado, retrocedemos al panel anterior
                if (layoutPaso4.getVisibility() == View.VISIBLE) {
                    layoutPaso4.setVisibility(View.GONE);
                    layoutPaso3.setVisibility(View.VISIBLE);
                } else if (layoutPaso3.getVisibility() == View.VISIBLE) {
                    layoutPaso3.setVisibility(View.GONE);
                    layoutPaso2.setVisibility(View.VISIBLE);
                } else if (layoutPaso2.getVisibility() == View.VISIBLE) {
                    layoutPaso2.setVisibility(View.GONE);
                    layoutPaso1.setVisibility(View.VISIBLE);
                } else {
                    // Si ya estamos en el paso 1 y le da a volver, chapamos la ventana
                    finish();
                }
            }
        });
    }

    private void configurarPaso1() {
        TextInputEditText etNombre    = findViewById(R.id.etNombre);
        TextInputEditText etApellidos = findViewById(R.id.etApellidos);
        TextInputEditText etDni       = findViewById(R.id.etDni);
        TextInputEditText etCorreo    = findViewById(R.id.etCorreo);
        TextInputEditText etPass      = findViewById(R.id.etPass);
        TextInputEditText etPass2     = findViewById(R.id.etPass2);
        AutoCompleteTextView etDireccion = findViewById(R.id.etDireccion);
        TextInputEditText etCP        = findViewById(R.id.etCP);
        Button btnSiguiente1          = findViewById(R.id.btnSiguiente1);
        Button btnAtras1              = findViewById(R.id.btnAtras1);

        // Referencias a los textos que chivan si la contraseña es segura
        TextView tvRuleLength  = findViewById(R.id.tvRuleLength);
        TextView tvRuleUpper   = findViewById(R.id.tvRuleUpper);
        TextView tvRuleNumber  = findViewById(R.id.tvRuleNumber);
        TextView tvRuleSpecial = findViewById(R.id.tvRuleSpecial);
        TextView tvPassMatch   = findViewById(R.id.tvPassMatch);

        // Listener en vivo para la primera contraseña (evalúa las reglas letra a letra)
        etPass.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                String pass = s.toString();

                // Evaluamos cada regla con expresiones regulares (magia negra de texto)
                boolean isLength = pass.length() >= 8 && pass.length() <= 16;
                boolean isUpper  = pass.matches(".*[A-Z].*");
                boolean isNumber = pass.matches(".*[0-9].*");
                boolean isSpecial = pass.matches(".*[^a-zA-Z0-9].*");

                // Pintamos los textos de verde o gris según si cumplen o no
                actualizarReglaTexto(tvRuleLength, isLength, "Entre 8 y 16 caracteres");
                actualizarReglaTexto(tvRuleUpper, isUpper, "Al menos una mayúscula");
                actualizarReglaTexto(tvRuleNumber, isNumber, "Al menos un número");
                actualizarReglaTexto(tvRuleSpecial, isSpecial, "Al menos un carácter especial");

                // También comprobamos si coinciden por si el usuario cambia la primera luego de rellenar la segunda
                comprobarCoincidencia(etPass.getText().toString(), etPass2.getText().toString(), tvPassMatch);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // Listener en vivo para la caja de repetir contraseña
        etPass2.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                comprobarCoincidencia(etPass.getText().toString(), s.toString(), tvPassMatch);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        btnAtras1.setOnClickListener(v -> finish());

        // Botón para avanzar al Paso 2, valida todo antes de dejarte pasar
        btnSiguiente1.setOnClickListener(v -> {
            String correo = etCorreo.getText().toString().trim();
            String pass   = etPass.getText().toString().trim();
            String pass2  = etPass2.getText().toString().trim();

            if (correo.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "El correo y la contraseña son obligatorios", Toast.LENGTH_SHORT).show();
                return;
            }

            // Exigimos seguridad máxima, si no, te quedas en esta pantalla
            if (pass.length() < 8 || pass.length() > 16 || !pass.matches(".*[A-Z].*") ||
                    !pass.matches(".*[0-9].*") || !pass.matches(".*[^a-zA-Z0-9].*")) {
                Toast.makeText(this, "La contraseña no cumple los requisitos de seguridad", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!pass.equals(pass2)) {
                Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show();
                return;
            }

            // Chequeo de base de datos para no tener emails duplicados
            if (db.existeEmail(correo)) {
                Toast.makeText(this, "Este correo ya está registrado", Toast.LENGTH_LONG).show();
                return;
            }

            // Volcamos todo al objeto que viaja entre los pasos
            usuario.nombre    = etNombre.getText().toString().trim();
            usuario.apellidos = etApellidos.getText().toString().trim();
            usuario.dni       = etDni.getText().toString().trim();
            usuario.email     = correo;
            usuario.password  = pass;
            usuario.direccion = etDireccion.getText().toString().trim();
            usuario.cp        = etCP.getText().toString().trim();

            layoutPaso1.setVisibility(View.GONE);
            layoutPaso2.setVisibility(View.VISIBLE);
        });


        // Usamos un Handler para no bombardear a la API con cada letra que tecleas
        final Handler handler = new Handler(Looper.getMainLooper());
        final Runnable[] runnable = new Runnable[1];

        etDireccion.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String busqueda = s.toString();

                // Si el usuario sigue tecleando, cancelamos la búsqueda anterior
                if (runnable[0] != null) {
                    handler.removeCallbacks(runnable[0]);
                }

                // Solo buscamos si tiene más de 4 letras para ahorrar recursos
                if (busqueda.length() > 4) {
                    runnable[0] = () -> buscarDireccionEnAPI(busqueda, etDireccion);
                    // Esperamos 600 milisegundos desde que deja de teclear para lanzar la petición
                    handler.postDelayed(runnable[0], 600);
                }
            }
        });

        // Qué pasa cuando el usuario pincha en una calle del desplegable
        etDireccion.setOnItemClickListener((parent, view, position, id) -> {
            DireccionSugerida seleccion = (DireccionSugerida) parent.getItemAtPosition(position);

            // Rescatamos el código postal del objeto y lo metemos en su cajita
            if (seleccion.codigoPostal != null && !seleccion.codigoPostal.isEmpty()) {
                etCP.setText(seleccion.codigoPostal);
                Toast.makeText(RegistroActivity.this, "Código Postal rellenado automáticamente", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void buscarDireccionEnAPI(String query, AutoCompleteTextView etDireccion) {
        // Hilo secundario para que la app no se quede pillada esperando a internet
        new Thread(() -> {
            try {
                // Mantenemos el limit=15 para tener variedad en los resultados
                String urlStr = "https://nominatim.openstreetmap.org/search?format=json&addressdetails=1&countrycodes=es&limit=15&q="
                        + query.replace(" ", "+");
                java.net.URL url = new java.net.URL(urlStr);
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();

                // DNI obligatorio para el servidor de OpenStreetMap
                conn.setRequestProperty("User-Agent", "PadelDartApp/1.0");

                // Leemos la respuesta de la API
                java.io.BufferedReader in = new java.io.BufferedReader(new java.io.InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                // Convertimos la respuesta a un array JSON manejable
                org.json.JSONArray jsonArray = new org.json.JSONArray(response.toString());
                List<DireccionSugerida> listaSugerencias = new ArrayList<>();

                for (int i = 0; i < jsonArray.length(); i++) {
                    org.json.JSONObject obj = jsonArray.getJSONObject(i);
                    String cp = "";
                    String calleStr = "";
                    String localidadStr = "";
                    String provinciaStr = "";

                    // Nos metemos en el bloque "address" donde está todo desglosado
                    if (obj.has("address")) {
                        org.json.JSONObject addressObj = obj.getJSONObject("address");

                        // 1. Guardamos el CP (sigue siendo invisible en el texto, pero se guarda en el objeto)
                        if (addressObj.has("postcode")) {
                            cp = addressObj.getString("postcode");
                        }

                        // 2. Buscamos el nombre de la vía (puede llamarse road, pedestrian, path...)
                        if (addressObj.has("road")) calleStr = addressObj.getString("road");
                        else if (addressObj.has("pedestrian")) calleStr = addressObj.getString("pedestrian");
                        else if (obj.has("name")) calleStr = obj.getString("name");

                        // 3. Buscamos la localidad (la API a veces lo llama city, town o village)
                        if (addressObj.has("city")) localidadStr = addressObj.getString("city");
                        else if (addressObj.has("town")) localidadStr = addressObj.getString("town");
                        else if (addressObj.has("village")) localidadStr = addressObj.getString("village");
                        else if (addressObj.has("municipality")) localidadStr = addressObj.getString("municipality");

                        // 4. Buscamos la provincia
                        if (addressObj.has("province")) provinciaStr = addressObj.getString("province");
                        else if (addressObj.has("state")) provinciaStr = addressObj.getString("state"); // A veces España usa state
                    }

                    // Ahora construimos nuestro texto limpio a medida: "Calle, Localidad, Provincia"
                    StringBuilder textoLimpio = new StringBuilder();

                    if (!calleStr.isEmpty()) textoLimpio.append(calleStr);

                    if (!localidadStr.isEmpty()) {
                        if (textoLimpio.length() > 0) textoLimpio.append(", ");
                        textoLimpio.append(localidadStr);
                    }

                    if (!provinciaStr.isEmpty()) {
                        // Si la provincia es exactamente igual a la localidad (ej: Madrid, Madrid), nos la ahorramos por estética
                        if (!provinciaStr.equals(localidadStr)) {
                            if (textoLimpio.length() > 0) textoLimpio.append(", ");
                            textoLimpio.append(provinciaStr);
                        }
                    }

                    // Por si acaso la API nos devuelve algo rarísimo y se queda en blanco,
                    // usamos el display_name original como red de seguridad.
                    String resultadoFinal = textoLimpio.toString();
                    if (resultadoFinal.isEmpty()) {
                        resultadoFinal = obj.getString("display_name");
                    }

                    // Añadimos el resultado a nuestra lista
                    listaSugerencias.add(new DireccionSugerida(resultadoFinal, cp));
                }

                // Volvemos al hilo principal para actualizar la interfaz
                runOnUiThread(() -> {
                    ArrayAdapter<DireccionSugerida> adapter = new ArrayAdapter<>(
                            RegistroActivity.this,
                            android.R.layout.simple_dropdown_item_1line,
                            listaSugerencias
                    );
                    etDireccion.setAdapter(adapter);
                    adapter.notifyDataSetChanged();
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    // Métodos de ayuda visual para pintar los textos

    private void actualizarReglaTexto(TextView tv, boolean cumple, String texto) {
        if (cumple) {
            tv.setText("✓ " + texto);
            // El color verde estándar de tu app que mola mucho
            tv.setTextColor(Color.parseColor("#94D500"));
        } else {
            tv.setText("✗ " + texto);
            // Grisáceo triste cuando aún les falta
            tv.setTextColor(Color.parseColor("#A0A0A0"));
        }
    }

    private void comprobarCoincidencia(String pass1, String pass2, TextView tvMatch) {
        // Escondemos el mensaje si la segunda caja está vacía, no hace falta agobiarles aún
        if (pass2.isEmpty()) {
            tvMatch.setVisibility(View.GONE);
            return;
        }

        tvMatch.setVisibility(View.VISIBLE);
        if (pass1.equals(pass2)) {
            tvMatch.setText("✓ Las contraseñas coinciden");
            tvMatch.setTextColor(Color.parseColor("#94D500"));
        } else {
            tvMatch.setText("✗ Las contraseñas no coinciden");
            // Usamos un rojito de alerta pero sin ser agresivo
            tvMatch.setTextColor(Color.parseColor("#FF848E"));
        }
    }

    private void configurarPaso2() {
        Slider sliderNivel        = findViewById(R.id.sliderNivel);
        TextView tvValorNivel     = findViewById(R.id.tvValorNivel);
        Button btnRestar          = findViewById(R.id.btnRestar);
        Button btnSumar           = findViewById(R.id.btnSumar);
        Button btnAtras2          = findViewById(R.id.btnAtras2);

        // Elementos de la parte de pádel
        Button btnSiguiente2      = findViewById(R.id.btnSiguiente2);
        RadioButton rbDrive       = findViewById(R.id.rbDrive);
        RadioButton rbReves       = findViewById(R.id.rbReves);
        dropdownProvincia         = findViewById(R.id.dropdownProvincia);
        dropdownClub              = findViewById(R.id.dropdownClub);

        // Inicializamos el botón del mapa y los desplegables
        btnVerMapa = findViewById(R.id.btnVerMapa);
        configurarDesplegables();

        // Lanzamos el pop-up que le explica al pobre usuario novato qué es cada nivel
        findViewById(R.id.ivInfoNivel).setOnClickListener(v ->
                new AlertDialog.Builder(this)
                        .setTitle("Niveles de Pádel")
                        .setMessage("Nivel 1.0: Acabas de empezar y no tienes experiencia previa. \n" +
                                "Nivel 1.5: Has tenido tus primeras clases, pero te cuesta mantener la pelota en juego. \n" +
                                "Nivel 2.0: Empiezas a entender conceptos básicos, pero tus golpes, sobre todo el revés, son poco controlados. \n" +
                                "Nivel 2.5: Mejoras la derecha, empiezas a usar el revés aunque aún con errores. Tus voleas de derecha son más fiables. \n" +
                                "Nivel 3.0: Derecha sólida, revés aceptable. Buenas voleas de derecha. Empiezas a dominar el globo y los rebotes lentos. \n" +
                                "Nivel 3.5: Golpes más potentes y variados. Mejor volea en red y posicionamiento. Bandejas decentes. \n" +
                                "Nivel 4.0: Excelente derecha y revés. Saque agresivo, voleas profundas y empiezas a probarte en el remate. \n" +
                                "Nivel 4.5: Buen control y colocación en la mayoría de golpes. Saques seguros y bajadas de pared eficaces. \n" +
                                "Nivel 5.0: Dominio completo de derecha y de revés. Remates y voleas agresivas. \n" +
                                "Nivel 5.5: Mismo nivel técnico pero con mayor capacidad de anticipación, consistencia y lectura táctica del partido. \n" +
                                "Nivel 6.0 y 7.0: Jugadores federados o de circuito. Entrenan a diario, compiten en torneos regionales (6.0) o internacionales (7.0).")
                        .setPositiveButton("Entendido", (d, w) -> d.dismiss())
                        .show()
        );

        // Al mover la barrita se cambia el texto arriba
        sliderNivel.addOnChangeListener((slider, value, fromUser) ->
                tvValorNivel.setText(String.format(Locale.getDefault(), "Nivel: %.2f", value)));

        // Botones gordos por si prefieren no usar el dedazo en el slider
        btnRestar.setOnClickListener(v -> {
            if (sliderNivel.getValue() > 0.0f)
                sliderNivel.setValue(Math.round((sliderNivel.getValue() - 0.01f) * 100f) / 100f);
        });

        btnSumar.setOnClickListener(v -> {
            if (sliderNivel.getValue() < 7.0f)
                sliderNivel.setValue(Math.round((sliderNivel.getValue() + 0.01f) * 100f) / 100f);
        });

        btnAtras2.setOnClickListener(v -> {
            layoutPaso2.setVisibility(View.GONE);
            layoutPaso1.setVisibility(View.VISIBLE);
        });

        btnSiguiente2.setOnClickListener(v -> {
            double nivel = Math.round(sliderNivel.getValue() * 100.0) / 100.0;
            String categoria;

            // Traductor automático de nivel numérico a categoría en texto
            if      (nivel >= 6.0) categoria = "1ª Categoría";
            else if (nivel >= 5.0) categoria = "2ª Categoría";
            else if (nivel >= 4.0) categoria = "3ª Categoría";
            else if (nivel >= 3.0) categoria = "4ª Categoría";
            else if (nivel >= 2.0) categoria = "5ª Categoría";
            else if (nivel >= 1.0) categoria = "5ªB Categoría";
            else                   categoria = "Iniciación";

            String posicion = rbDrive.isChecked() ? "Drive" : rbReves.isChecked() ? "Revés" : "";

            // Todo pa' la saca de nuestro objeto central
            usuario.nivelPadel     = nivel;
            usuario.categoriaPadel = categoria;
            usuario.posicion       = posicion;
            usuario.provincia      = dropdownProvincia.getText().toString().trim();
            usuario.pistaFavorita  = dropdownClub.getText().toString().trim();

            layoutPaso2.setVisibility(View.GONE);
            layoutPaso3.setVisibility(View.VISIBLE);
        });
    }

    private void configurarPaso3() {
        CheckBox cbTieneDiana     = findViewById(R.id.cbTieneDiana);
        LinearLayout llTipoDiana  = findViewById(R.id.llTipoDiana);
        RadioGroup rgTipoDiana    = findViewById(R.id.rgTipoDiana);
        RadioButton rbPelo        = findViewById(R.id.rbPelo);
        RadioButton rbElectronica = findViewById(R.id.rbElectronica);
        RadioButton rbAmbas       = findViewById(R.id.rbAmbas);
        Button btnAtras3          = findViewById(R.id.btnAtras3);
        Button btnSiguiente3      = findViewById(R.id.btnSiguiente3);

        // Enseñamos u ocultamos opciones de diana según le den al checkbox
        cbTieneDiana.setOnCheckedChangeListener((btn, checked) -> {
            llTipoDiana.setVisibility(checked ? View.VISIBLE : View.GONE);
            if (!checked) rgTipoDiana.clearCheck();
        });

        btnAtras3.setOnClickListener(v -> {
            layoutPaso3.setVisibility(View.GONE);
            layoutPaso2.setVisibility(View.VISIBLE);
        });

        btnSiguiente3.setOnClickListener(v -> {
            // Un poco de mano dura, si tienes diana dime de qué tipo
            if (cbTieneDiana.isChecked() && rgTipoDiana.getCheckedRadioButtonId() == -1) {
                Toast.makeText(this, "Selecciona el tipo de diana", Toast.LENGTH_SHORT).show();
                return;
            }

            usuario.tieneDiana = cbTieneDiana.isChecked();
            if (usuario.tieneDiana) {
                if      (rbPelo.isChecked())        usuario.tipoDiana = "Tradicional";
                else if (rbElectronica.isChecked()) usuario.tipoDiana = "Electrónica";
                else if (rbAmbas.isChecked())       usuario.tipoDiana = "Ambas";
            }

            layoutPaso3.setVisibility(View.GONE);
            layoutPaso4.setVisibility(View.VISIBLE);
        });
    }

    private void configurarPaso4() {
        RadioButton rbTarjeta  = findViewById(R.id.rbTarjeta);
        RadioButton rbPaypal   = findViewById(R.id.rbPaypal);
        RadioButton rbBizum    = findViewById(R.id.rbBizum);
        RadioButton rbEfectivo = findViewById(R.id.rbEfectivo);
        Button btnAtras4       = findViewById(R.id.btnAtras4);
        Button btnCrearCuenta  = findViewById(R.id.btnCrearCuenta);

        // Esto es oro UX, pinchas en la tarjeta entera y se marca el circulito
        findViewById(R.id.cardTarjeta).setOnClickListener(v  -> rbTarjeta.setChecked(true));
        findViewById(R.id.cardPaypal).setOnClickListener(v   -> rbPaypal.setChecked(true));
        findViewById(R.id.cardBizum).setOnClickListener(v    -> rbBizum.setChecked(true));
        findViewById(R.id.cardEfectivo).setOnClickListener(v -> rbEfectivo.setChecked(true));

        btnAtras4.setOnClickListener(v -> {
            layoutPaso4.setVisibility(View.GONE);
            layoutPaso3.setVisibility(View.VISIBLE);
        });

        btnCrearCuenta.setOnClickListener(v -> {
            String metodoPago = "Tarjeta Bancaria"; // Red de seguridad

            if      (rbTarjeta.isChecked())  metodoPago = "Tarjeta Bancaria";
            else if (rbPaypal.isChecked())   metodoPago = "PayPal";
            else if (rbBizum.isChecked())    metodoPago = "Bizum";
            else if (rbEfectivo.isChecked()) metodoPago = "Efectivo";

            usuario.metodoPago = metodoPago;

            // Llegó la hora de la verdad, empaquetamos todo para la Base de Datos
            Map<String, Object> datos = new HashMap<>();
            datos.put("email",             usuario.email);
            datos.put("password",          usuario.password);
            datos.put("nombre",            usuario.nombre);
            datos.put("apellidos",         usuario.apellidos);
            datos.put("dni",               usuario.dni);
            datos.put("direccion",         usuario.direccion);
            datos.put("cp",                usuario.cp);
            datos.put("metodo_pago",       usuario.metodoPago);
            datos.put("categoria_actual",  usuario.categoriaPadel);
            datos.put("puntuacion_media",  usuario.nivelPadel);
            datos.put("posicion",          usuario.posicion);
            datos.put("provincia",         usuario.provincia);
            datos.put("pista_favorita",    usuario.pistaFavorita);
            datos.put("tiene_diana_propia", usuario.tieneDiana);

            if (usuario.tieneDiana) {
                datos.put("tipo_diana", usuario.tipoDiana);
            }

            // Metemos la ficha en la ranura
            long id = db.registrarUsuario(datos);

            // Si devuelve -1 es que algo ha petado en el SQLite
            if (id != -1) {
                Toast.makeText(this, "¡Cuenta creada correctamente!", Toast.LENGTH_LONG).show();
                finish();
            } else {
                Toast.makeText(this, "Error al guardar la cuenta. Inténtalo de nuevo.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void configurarDesplegables() {
        String[] provincias = {
                "A Coruña","Álava","Albacete","Alicante","Almería","Asturias","Ávila","Badajoz",
                "Baleares","Barcelona","Burgos","Cáceres","Cádiz","Cantabria","Castellón","Ceuta",
                "Ciudad Real","Córdoba","Cuenca","Girona","Granada","Guadalajara","Gipuzkoa","Huelva",
                "Huesca","Jaén","La Rioja","Las Palmas","León","Lleida","Lugo","Madrid","Málaga",
                "Melilla","Murcia","Navarra","Ourense","Palencia","Pontevedra","Salamanca",
                "Santa Cruz de Tenerife","Segovia","Sevilla","Soria","Tarragona","Teruel","Toledo",
                "Valencia","Valladolid","Vizcaya","Zamora","Zaragoza"
        };

        dropdownProvincia.setAdapter(new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, provincias));

        // En cuanto elige provincia, abrimos la veda para que elija club
        dropdownProvincia.setOnItemClickListener((parent, view, position, id) -> {
            String prov = (String) parent.getItemAtPosition(position);
            List<String> clubes = obtenerPistasPorProvincia(prov);

            dropdownClub.setEnabled(true);
            dropdownClub.setAdapter(new ArrayAdapter<>(
                    this, android.R.layout.simple_dropdown_item_1line, clubes));
            dropdownClub.setText("", false);

            btnVerMapa.setVisibility(View.GONE);
            dropdownClub.showDropDown();
        });

        // Si elige una pista que no es de su urbanización, enseñamos el chivato del mapa
        dropdownClub.setOnItemClickListener((parent, view, position, id) -> {
            String club = (String) parent.getItemAtPosition(position);
            btnVerMapa.setVisibility(
                    club.equals("Otro club / Pista de mi urbanización") ? View.GONE : View.VISIBLE);
        });

        btnVerMapa.setOnClickListener(v -> {
            Uri uri = Uri.parse("geo:0,0?q=" + dropdownClub.getText() + ", " + dropdownProvincia.getText());
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, uri);
            mapIntent.setPackage("com.google.android.apps.maps");

            try {
                startActivity(mapIntent);
            } catch (android.content.ActivityNotFoundException e) {
                Toast.makeText(this, "No tienes Google Maps instalado", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private List<String> obtenerPistasPorProvincia(String provincia) {
        Map<String, List<String>> pistas = new HashMap<>();
        pistas.put("Almería",   Arrays.asList("ProPadel Indoor Club","Club Deportivo Los Molinos","Club Natación Almería","Ego Sport Center"));
        pistas.put("Cádiz",     Arrays.asList("Pádel KD (Jerez)","Las Salinas (San Fernando)","Padel La Central (El Puerto)","Club de Pádel La Barrosa","Centro Deportivo Liceo"));
        pistas.put("Córdoba",   Arrays.asList("Play Center Córdoba","Padel Indoor Córdoba","Club Sierra Morena","Open Arena"));
        pistas.put("Granada",   Arrays.asList("Padel Sport Granada Indoor","Club Campus Pádel","We Fitness Club","O2 Centro Wellness"));
        pistas.put("Huelva",    Arrays.asList("Padel 7 Huelva","Club Padel Indoor Huelva","Real Club Marítimo de Huelva"));
        pistas.put("Jaén",      Arrays.asList("Padel Premium Jaén","Club Padel Indoor Jaén","Padel Akelarre"));
        pistas.put("Málaga",    Arrays.asList("Reserva del Higuerón","Los Caballeros","Vals Sport","Padel Picasso","Inacua Málaga"));
        pistas.put("Sevilla",   Arrays.asList("Club Río Grande","Sato Sport","Padel Premium Sevilla","Bernier (Coria)","La Cartuja"));
        pistas.put("Huesca",    Arrays.asList("Padel Indoor Huesca","Club Tenis Osca","Padel 360","Zone Padel"));
        pistas.put("Teruel",    Arrays.asList("Padel Teruel Indoor","Club de Tenis Teruel","Padel Calamocha"));
        pistas.put("Zaragoza",  Arrays.asList("Padel Plaza Indoor","Pádel Zaragoza","Montecanal Centro Deportivo","Regal Padel Club"));
        pistas.put("Asturias",  Arrays.asList("Paidesport Center","Padel Indoor Oviedo","Club Padel Gijón","Cover Padel"));
        pistas.put("Cantabria", Arrays.asList("Padel Besaya","Club Parayas","Gofit Santander","Padel Indoor Camargo"));
        pistas.put("Baleares",  Arrays.asList("Pins Padel Club","Palma Padel","Padel Go In","Fit Point"));
        pistas.put("Las Palmas",Arrays.asList("El Cortijo Club de Campo","Padel Indoor Gran Canaria","Club La Calzada"));
        pistas.put("Santa Cruz de Tenerife", Arrays.asList("Padel Canary","Tecina Padel","Las Palmeras","Pádel Indoor Tenerife"));
        pistas.put("Albacete",  Arrays.asList("Club Padel Albacete","Padel Indoor Albacete","Tiro Pichón Albacete"));
        pistas.put("Ciudad Real", Arrays.asList("Padel Center Ciudad Real","Padel Plus","Club de Ocio Nudos"));
        pistas.put("Cuenca",    Arrays.asList("Padel Cuenca","Nuevo Tenis Cuenca","Pádel Indoor Cuenca"));
        pistas.put("Guadalajara", Arrays.asList("Pádel Cabanillas Golf","Pádel Indoor Guadalajara","Ciudad de la Raqueta"));
        pistas.put("Toledo",    Arrays.asList("Padel Indoor Toledo","Imperial Padel Indoor","Club Monteverde"));
        pistas.put("Ávila",     Arrays.asList("Padel Indoor Ávila","Espacio Padel Ávila","Tres60 Padel"));
        pistas.put("Burgos",    Arrays.asList("Padel Arena Burgos","Padel Indoor Burgos","Club Tenis Burgos"));
        pistas.put("León",      Arrays.asList("Padel Park León","Central Padel León","Tenis5padel Indoor"));
        pistas.put("Palencia",  Arrays.asList("Padel San Antonio","Padel Palencia","Club Padel Charca"));
        pistas.put("Salamanca", Arrays.asList("Padel Home Salamanca","Pádel Time","Club Pádel Helmántico"));
        pistas.put("Segovia",   Arrays.asList("Padel Zone Segovia","Padel Segovia","Padel Indoor Segovia"));
        pistas.put("Soria",     Arrays.asList("Padel Soria","Pádel Indoor Soria"));
        pistas.put("Valladolid",Arrays.asList("Padel Arena Valladolid","Club Raqueta Valladolid","Padel D10","Zaratán Padel"));
        pistas.put("Zamora",    Arrays.asList("Padel Indoor Zamora","Zamora Padel","Ciudad Deportiva Zamora"));
        pistas.put("Barcelona", Arrays.asList("Star's Padel","Padel Indoor Hospitalet","Artós Sports Club","Augusta Padel","Padelarium"));
        pistas.put("Girona",    Arrays.asList("Padel Girona","Padel Indoor Figueres","Club de Tenis Girona","Padel Costa Brava"));
        pistas.put("Lleida",    Arrays.asList("Padel Indoor Lleida","Waps Padel","Club Tennis Urgell"));
        pistas.put("Tarragona", Arrays.asList("Tarragona Padel Indoor","Padel Cambrils","Club Tenis Tarragona","Padel Reus"));
        pistas.put("Alicante",  Arrays.asList("Padelpoint (La Nucía)","Blupadel","Pádel Play San Vicente","San Jerónimo","Pádel Lacy","PadelCoca","Pádel FondoNet","Oxygen Club de Campo","Alonka Indoor","Centro Excursionista Eldense","Ecomm Padel Club","Ipadel (Elche)","Club de Campo","Padel Club Alicante"));
        pistas.put("Castellón", Arrays.asList("Padel Center Castellón","Jubilama Padel","Padel Indoor Castellón","Impala Sport Club"));
        pistas.put("Valencia",  Arrays.asList("Family Sport Center","7Padel","Sportcity","Suma Fitness Club","Bergamonte"));
        pistas.put("Badajoz",   Arrays.asList("El Corzo","Padel Indoor Badajoz","Golf Guadiana","Padel Center Extremadura"));
        pistas.put("Cáceres",   Arrays.asList("Padel Indoor Cáceres","Club de Tenis Cabezarrubia","Padel Center Cáceres"));
        pistas.put("A Coruña",  Arrays.asList("Coruña Sport Centre","Padel Plus","Let Padel","Padel Prix"));
        pistas.put("Lugo",      Arrays.asList("Padel Nuestro Lugo","Club Fluvial","D10 Lugo"));
        pistas.put("Ourense",   Arrays.asList("Padel Ourense","Pádel Prix Ourense","Padel Indoor Ourense"));
        pistas.put("Pontevedra",Arrays.asList("Padelstop (Vigo)","Mercantil de Vigo","Padel Indoor Poniente","Padel Nuestro Pontevedra"));
        pistas.put("Madrid",    Arrays.asList("Ciudad de la Raqueta","Mad4Padel","Euroindoor","La Masó Sports Club","Padel 2.0","Sanset Padel","El Estudiante"));
        pistas.put("Murcia",    Arrays.asList("Padel Nuestro Club","Padel Center Murcia","Olimpic Club","Verdolay Padel"));
        pistas.put("Navarra",   Arrays.asList("Navarra Padel Máster Club","Arena Entrena Padel","Ciudad Deportiva Amaya","Pádel Reyno de Navarra"));
        pistas.put("La Rioja",  Arrays.asList("Padel Indoor La Rioja","Las Norias","La Grajera Padel","Alos Padel"));
        pistas.put("Álava",     Arrays.asList("Padel Ebro","Bakh Baskonia","Padel Norte"));
        pistas.put("Gipuzkoa",  Arrays.asList("Padel Indoor Bidasoa","Pádel San Sebastián","Pádel Zubieta"));
        pistas.put("Vizcaya",   Arrays.asList("Padel & Gol","Padel Derio","Esmas Padel","Pádel Indoor Center"));
        pistas.put("Ceuta",     Arrays.asList("Club Loma Margarita","Parque Marítimo","Padel Ceuta Center"));
        pistas.put("Melilla",   Arrays.asList("Centro Autonómico de Pádel","La Hípica","Padel Melilla Indoor"));

        if (pistas.containsKey(provincia)) {
            List<String> lista = new ArrayList<>(pistas.get(provincia));
            Collections.sort(lista);
            lista.add("Otro club / Pista de mi urbanización");
            return lista;
        }

        return Arrays.asList("Polideportivo Municipal","Club de Pádel Local","Otro club / Pista de mi urbanización");
    }
}

// Esta miniclase hace de contenedor para no perder el código postal al pulsar en el texto
class DireccionSugerida {
    String nombreCompleto;
    String codigoPostal;

    public DireccionSugerida(String nombreCompleto, String codigoPostal) {
        this.nombreCompleto = nombreCompleto;
        this.codigoPostal = codigoPostal;
    }

    // Android usa automáticamente esto para saber qué dibujar en el menú desplegable
    @Override
    public String toString() {
        return nombreCompleto;
    }
}