package dam.iker.padeldart;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;

import com.google.android.material.card.MaterialCardView;

import dam.iker.padeldart.dardos.DardosActivity;

// Pantalla principal tras el login. Extiende BaseDrawerActivity para el menú lateral.
public class MainActivity extends BaseDrawerActivity {

    private static final String TAG = "MainActivity";

    // Referencias a las vistas de foto almacenadas como campo para poder
    // actualizarlas en onResume sin repetir toda la lógica de onCreate.
    private ImageView imgFotoMain;
    private TextView  tvInicialMain;
    private long      userId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        DatabaseHelper db      = DatabaseHelper.getInstance(this);
        SessionManager session = SessionManager.getInstance(this);

        // Seguridad: si se llega aquí sin sesión, volvemos al Login
        if (!session.haySesionActiva()) {
            Log.w(TAG, "Sin sesión activa, volviendo al Login");
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // Inflamos el layout. Si falla aquí el error se verá como crash normal,
        // no como un retorno silencioso al login.
        setContentView(R.layout.activity_main_principal);

        // Cargamos datos del usuario para el saludo personalizado
        long userId = session.getUsuarioActualId();
        Log.d(TAG, "Usuario logueado ID: " + userId);

        java.util.Map<String, Object> usuario = db.obtenerUsuario(userId);

        TextView tvSaludo    = findViewById(R.id.tvSaludoUsuario);
        TextView tvCategoria = findViewById(R.id.tvCategoriaUsuario);

        // Vistas de la foto de perfil (círculo con foto o inicial del nombre)
        ImageView imgFoto    = findViewById(R.id.imgFotoMain);
        TextView  tvInicial  = findViewById(R.id.tvInicialMain);

        if (usuario != null) {
            String nombre    = (String) usuario.get(DatabaseHelper.COL_NOMBRE);
            String categoria = (String) usuario.get(DatabaseHelper.COL_CATEGORIA);

            // Saludo personalizado con i18n: "¡Hola, Juan!" o "Hello, Juan!"
            tvSaludo.setText(getString(R.string.main_hola,
                    nombre != null ? nombre : getString(R.string.main_jugador)));
            tvCategoria.setText(categoria != null ? categoria : "");

            // Inicial del nombre como fallback si no hay foto
            if (tvInicial != null && nombre != null && !nombre.isEmpty()) {
                tvInicial.setText(String.valueOf(nombre.charAt(0)).toUpperCase());
            }

            // Foto de perfil: si el usuario tiene URI guardada la mostramos
            String fotoUri = (String) usuario.get(DatabaseHelper.COL_FOTO_PERFIL);
            if (fotoUri != null && !fotoUri.isEmpty() && imgFoto != null) {
                try {
                    imgFoto.setImageURI(Uri.parse(fotoUri));
                    imgFoto.setVisibility(View.VISIBLE);
                    if (tvInicial != null) tvInicial.setVisibility(View.GONE);
                } catch (Exception e) {
                    Log.w(TAG, "No se pudo cargar la foto de perfil: " + e.getMessage());
                }
            }
            Log.d(TAG, "Usuario: " + nombre + " | Categoría: " + categoria);
        } else {
            // Si el usuario no existe en BD, mostramos texto genérico de bienvenida
            Log.e(TAG, "No se encontró el usuario con ID " + userId + " en la BD");
            tvSaludo.setText(getString(R.string.main_bienvenido));
            tvCategoria.setText("");
        }

        // Guardamos las vistas de foto como campos para poder recargarlas en onResume
        // (si el usuario edita el perfil y vuelve, la foto principal se actualiza)
        this.imgFotoMain   = imgFoto;
        this.tvInicialMain = tvInicial;
        this.userId        = userId;

        // Tarjeta de Pádel → abre el hub del módulo de pádel
        MaterialCardView cardPadel = findViewById(R.id.cardPadel);
        cardPadel.setOnClickListener(v ->
                startActivity(new Intent(this, PadelActivity.class)));

        // Tarjeta de Dardos → abre el hub del módulo de dardos (elección PvP/PvE)
        MaterialCardView cardDardos = findViewById(R.id.cardDardos);
        cardDardos.setOnClickListener(v ->
                startActivity(new Intent(this, DardosActivity.class)));

        // onResume recargará la foto; si el usuario acaba de actualizar el perfil
        // verá el cambio sin necesidad de cerrar sesión.

        // Desde el menú principal el Atrás minimiza la app (no vuelve al login)
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                moveTaskToBack(true);
            }
        });
    }

    // onResume se dispara al volver de EditProfileActivity (u otras).
    // Recarga la foto de perfil de la pantalla principal y la del drawer
    // por si el usuario acaba de cambiarla desde el editor de perfil.
    @Override
    protected void onResume() {
        super.onResume(); // BaseDrawerActivity.onResume() refresca ya la cabecera del drawer

        // Recargamos además la foto grande de la pantalla de bienvenida
        if (userId == -1 || imgFotoMain == null || tvInicialMain == null) return;

        DatabaseHelper db = DatabaseHelper.getInstance(this);
        java.util.Map<String, Object> usuario = db.obtenerUsuario(userId);
        if (usuario == null) return;

        // Leemos la URI de foto guardada en la base de datos local
        String fotoUri = (String) usuario.get(DatabaseHelper.COL_FOTO_PERFIL);
        if (fotoUri != null && !fotoUri.isEmpty()) {
            try {
                // Mostramos la imagen y ocultamos el texto con la inicial
                imgFotoMain.setImageURI(android.net.Uri.parse(fotoUri));
                imgFotoMain.setVisibility(View.VISIBLE);
                tvInicialMain.setVisibility(View.GONE);
            } catch (Exception e) {
                // URI inválida o revocada: volvemos a mostrar la inicial
                Log.w(TAG, "onResume: foto inválida, mostrando inicial");
                imgFotoMain.setVisibility(View.GONE);
                tvInicialMain.setVisibility(View.VISIBLE);
            }
        } else {
            // Sin foto: aseguramos que se vea la inicial (podría haberla borrado)
            imgFotoMain.setVisibility(View.GONE);
            tvInicialMain.setVisibility(View.VISIBLE);
        }
    }
}
