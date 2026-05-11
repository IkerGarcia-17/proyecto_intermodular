package dam.iker.padeldart;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Map;

// Pantalla de edición de perfil. Solo permite cambiar datos no sensibles:
// nombre, apellidos, edad, posición, provincia, pista favorita y foto.
// Email, DNI y método de pago quedan bloqueados por seguridad.
public class EditProfileActivity extends AppCompatActivity {

    private DatabaseHelper db;
    private SessionManager session;
    private long userId;

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
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        // Copiamos la imagen a almacenamiento interno para que la URI
                        // no expire entre sesiones (las URIs picker_get_content se revocan).
                        String rutaLocal = copiarFotoAAlmacenamientoInterno(uri);
                        if (rutaLocal != null) {
                            nuevaFotoUri = rutaLocal;
                            actualizarFotoUI(Uri.parse(rutaLocal));
                        } else {
                            // Fallback: usamos la URI original si la copia falla
                            nuevaFotoUri = uri.toString();
                            actualizarFotoUI(uri);
                        }
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        db      = DatabaseHelper.getInstance(this);
        session = SessionManager.getInstance(this);
        userId  = session.getUsuarioActualId();

        // Protección: si no hay sesión, cerramos la pantalla
        if (userId == -1) { finish(); return; }

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

    // Rellena los campos con los datos actuales del usuario en la BD
    private void cargarDatosActuales() {
        Map<String, Object> usuario = db.obtenerUsuario(userId);
        if (usuario == null) return;

        setTexto(etNombre,    usuario.get(DatabaseHelper.COL_NOMBRE));
        setTexto(etApellidos, usuario.get(DatabaseHelper.COL_APELLIDOS));
        setTexto(etPosicion,  usuario.get(DatabaseHelper.COL_POSICION));
        setTexto(etProvincia, usuario.get(DatabaseHelper.COL_PROVINCIA));
        setTexto(etPista,     usuario.get(DatabaseHelper.COL_PISTA_FAVORITA));

        // Edad como número (columna INTEGER)
        Object edadObj = usuario.get(DatabaseHelper.COL_EDAD);
        int edad = edadObj instanceof Number ? ((Number) edadObj).intValue() : 0;
        etEdad.setText(edad > 0 ? String.valueOf(edad) : "");

        // Mostramos la foto actual si existe, o la inicial del nombre
        String fotoUri = strOrEmpty(usuario.get(DatabaseHelper.COL_FOTO_PERFIL));
        if (!fotoUri.isEmpty()) {
            actualizarFotoUI(Uri.parse(fotoUri));
        } else {
            // Sin foto: mostramos la primera letra del nombre como avatar
            String nombre = strOrEmpty(usuario.get(DatabaseHelper.COL_NOMBRE));
            tvInicial.setText(!nombre.isEmpty()
                    ? String.valueOf(nombre.charAt(0)).toUpperCase() : "?");
        }
    }

    // Muestra la imagen en el círculo usando BitmapFactory (nunca setImageURI, que puede
    // lanzar SecurityException en onMeasure si la URI picker fue revocada entre sesiones).
    private void actualizarFotoUI(Uri uri) {
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            Bitmap bmp = BitmapFactory.decodeStream(is);
            if (bmp != null) {
                imgFoto.setImageBitmap(bmp);
                imgFoto.setVisibility(View.VISIBLE);
                tvInicial.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            // URI inválida o revocada: dejamos el estado anterior
        }
    }

    // Copia la imagen seleccionada desde la URI del picker al almacenamiento interno
    // de la app (filesDir/profile_photos/user_{id}.jpg). Devuelve la URI del fichero
    // copiado como "file:///..." o null si falla. El fichero interno es permanente y
    // accesible en cualquier sesión sin necesidad de permisos adicionales.
    private String copiarFotoAAlmacenamientoInterno(Uri origenUri) {
        try {
            File dir = new File(getFilesDir(), "profile_photos");
            if (!dir.exists()) dir.mkdirs();
            File destino = new File(dir, "user_" + userId + ".jpg");

            // Decodificamos el bitmap desde la URI del picker y lo guardamos como JPEG
            InputStream is = getContentResolver().openInputStream(origenUri);
            Bitmap bmp = BitmapFactory.decodeStream(is);
            if (bmp == null) return null;

            FileOutputStream fos = new FileOutputStream(destino);
            bmp.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.flush();
            fos.close();

            // Devolvemos la URI del fichero local para guardarla en la BD
            return destino.toURI().toString();
        } catch (Exception e) {
            return null;
        }
    }

    // Lanza el intent del sistema para seleccionar una imagen de la galería
    private void abrirSelectorFoto() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        pickerLauncher.launch(Intent.createChooser(intent,
                getString(R.string.reg_foto_seleccionar)));
    }

    // Lee los campos, valida y guarda en la BD
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

        // Delegamos la actualización al DatabaseHelper: solo toca los campos permitidos
        boolean ok = db.actualizarPerfil(userId, nombre, apellidos, posicion,
                provincia, pista, edad, nuevaFotoUri);

        if (ok) {
            Toast.makeText(this, getString(R.string.edit_ok), Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, getString(R.string.edit_error), Toast.LENGTH_SHORT).show();
        }
    }

    // Helpers
    private void setTexto(TextInputEditText et, Object valor) {
        if (et != null && valor instanceof String) et.setText((String) valor);
    }

    private String strOrEmpty(Object obj) {
        return (obj instanceof String) ? (String) obj : "";
    }
}
