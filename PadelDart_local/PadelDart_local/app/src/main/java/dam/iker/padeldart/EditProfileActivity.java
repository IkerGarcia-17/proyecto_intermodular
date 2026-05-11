package dam.iker.padeldart;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;

// Pantalla de edición de perfil. Solo permite cambiar datos no sensibles:
// nombre, apellidos, edad, posición, provincia, pista favorita y foto.
// Email, DNI y método de pago quedan bloqueados por seguridad.
// Ahora usa FirebaseHelper para leer y escribir en Firestore.
public class EditProfileActivity extends AppCompatActivity {

    private FirebaseHelper fb;
    private SessionManager session;
    private String userId;          // UID de Firebase (antes era long de SQLite)

    // URI de la nueva foto elegida (null si no se cambió)
    private String nuevaFotoUri = null;

    // Vistas de la pantalla
    private TextInputEditText etNombre, etApellidos, etEdad;
    private TextInputEditText etPosicion, etProvincia, etPista;
    private ImageView imgFoto;
    private TextView tvInicial;

    // Launcher del selector de imágenes del sistema
    private final ActivityResultLauncher<Intent> pickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                // El usuario seleccionó una imagen de la galería
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        nuevaFotoUri = uri.toString();
                        actualizarFotoUI(uri);
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        fb      = FirebaseHelper.getInstance();
        session = SessionManager.getInstance(this);
        userId  = session.getUsuarioActualId();

        // Protección: si no hay sesión, cerramos la pantalla
        if (userId.isEmpty()) { finish(); return; }

        initVistas();
        cargarDatosActuales();
    }

    // Enlaza todas las vistas del layout y configura sus listeners
    private void initVistas() {
        etNombre    = findViewById(R.id.etNombreEdit);
        etApellidos = findViewById(R.id.etApellidosEdit);
        etEdad      = findViewById(R.id.etEdadEdit);
        etPosicion  = findViewById(R.id.etPosicionEdit);
        etProvincia = findViewById(R.id.etProvinciaEdit);
        etPista     = findViewById(R.id.etPistaEdit);
        imgFoto     = findViewById(R.id.imgFotoEdit);
        tvInicial   = findViewById(R.id.tvInicialEdit);

        // Botón atrás de la cabecera
        findViewById(R.id.btnAtrasEdit).setOnClickListener(v -> finish());

        // Botón de cambiar foto: abre el selector de imágenes del sistema
        findViewById(R.id.btnCambiarFoto).setOnClickListener(v -> abrirSelectorFoto());

        // Tocar la foto también abre el selector
        findViewById(R.id.cardFotoEdit).setOnClickListener(v -> abrirSelectorFoto());

        // Guardar cambios
        findViewById(R.id.btnGuardarEdit).setOnClickListener(v -> guardarCambios());
    }

    // Rellena los campos con los datos actuales del usuario desde Firestore
    private void cargarDatosActuales() {
        fb.obtenerUsuario(userId, usuario -> {
            if (usuario == null) return;

            setTexto(etNombre,    usuario.get(DatabaseHelper.COL_NOMBRE));
            setTexto(etApellidos, usuario.get(DatabaseHelper.COL_APELLIDOS));
            setTexto(etPosicion,  usuario.get(DatabaseHelper.COL_POSICION));
            setTexto(etProvincia, usuario.get(DatabaseHelper.COL_PROVINCIA));
            setTexto(etPista,     usuario.get(DatabaseHelper.COL_PISTA_FAVORITA));

            // Edad como número (columna INTEGER en Firestore devuelve Long)
            Object edadObj = usuario.get(DatabaseHelper.COL_EDAD);
            int edad = edadObj instanceof Number ? ((Number) edadObj).intValue() : 0;
            etEdad.setText(edad > 0 ? String.valueOf(edad) : "");

            // Mostramos la foto actual si existe, o la inicial del nombre
            String fotoUri = strOrEmpty(usuario.get(DatabaseHelper.COL_FOTO_PERFIL));
            if (!fotoUri.isEmpty()) {
                actualizarFotoUI(Uri.parse(fotoUri));
            } else {
                String nombre = strOrEmpty(usuario.get(DatabaseHelper.COL_NOMBRE));
                tvInicial.setText(!nombre.isEmpty()
                        ? String.valueOf(nombre.charAt(0)).toUpperCase() : "?");
            }
        });
    }

    // Muestra la imagen seleccionada en el círculo de foto y oculta la inicial
    private void actualizarFotoUI(Uri uri) {
        try {
            imgFoto.setImageURI(uri);
            imgFoto.setVisibility(View.VISIBLE);
            tvInicial.setVisibility(View.GONE);
        } catch (Exception e) {
            // URI inválida: dejamos el estado anterior
        }
    }

    // Lanza el intent del sistema para seleccionar una imagen de la galería
    private void abrirSelectorFoto() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        pickerLauncher.launch(Intent.createChooser(intent,
                getString(R.string.reg_foto_seleccionar)));
    }

    // Lee los campos, valida y actualiza en Firestore de forma asíncrona
    private void guardarCambios() {
        String nombre    = etNombre.getText().toString().trim();
        String apellidos = etApellidos.getText().toString().trim();
        String posicion  = etPosicion.getText().toString().trim();
        String provincia = etProvincia.getText().toString().trim();
        String pista     = etPista.getText().toString().trim();

        // La edad debe ser un número válido si se rellenó
        int edad = 0;
        String edadStr = etEdad.getText().toString().trim();
        if (!edadStr.isEmpty()) {
            try { edad = Integer.parseInt(edadStr); }
            catch (NumberFormatException ignored) {}
        }

        // Delegamos la actualización a FirebaseHelper: solo toca los campos permitidos
        fb.actualizarPerfil(userId, nombre, apellidos, posicion,
                provincia, pista, edad, nuevaFotoUri, ok -> {
                    if (ok) {
                        Toast.makeText(this, getString(R.string.edit_ok), Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(this, getString(R.string.edit_error), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Helpers
    private void setTexto(TextInputEditText et, Object valor) {
        if (et != null && valor instanceof String) et.setText((String) valor);
    }

    private String strOrEmpty(Object obj) {
        return (obj instanceof String) ? (String) obj : "";
    }
}
