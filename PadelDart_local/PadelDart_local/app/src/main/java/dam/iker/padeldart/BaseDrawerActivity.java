package dam.iker.padeldart;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.button.MaterialButton;

import java.util.Map;

// Clase base para todas las Activities autenticadas.
// Envuelve cualquier layout en un DrawerLayout con menú lateral.
// Extender esta clase en lugar de AppCompatActivity añade el drawer automáticamente.
public abstract class BaseDrawerActivity extends AppCompatActivity {

    // El DrawerLayout raíz que envuelve todo el contenido
    protected DrawerLayout drawerLayout;

    // ── Ciclo de vida ────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Las subclases llaman a setContentView() normalmente;
        // el override de abajo se encarga de insertar el drawer.
    }

    // onResume se ejecuta cada vez que esta Activity vuelve al primer plano,
    // lo que incluye el retorno desde EditProfileActivity.
    // Así la foto del drawer se refresca automáticamente si el usuario
    // acaba de actualizar su foto de perfil sin cerrar y reabrir sesión.
    @Override
    protected void onResume() {
        super.onResume();
        refrescarFotoCabecera();
    }

    // Sobrescribimos setContentView para envolver el layout de la subclase
    // dentro del DrawerLayout definido en activity_base_drawer.xml.
    @Override
    public void setContentView(int layoutResID) {
        // 1. Inflamos la envolvente del drawer
        View raiz = getLayoutInflater().inflate(R.layout.activity_base_drawer, null);

        // 2. Inflamos el layout propio de la Activity e insertamos en el contenedor
        FrameLayout contenedor = raiz.findViewById(R.id.contenidoActividad);
        getLayoutInflater().inflate(layoutResID, contenedor, true);

        // 3. Establecemos el DrawerLayout como vista raíz de la Activity
        super.setContentView(raiz);

        drawerLayout = raiz.findViewById(R.id.drawerLayout);

        // 4. Configuramos el hamburger y los datos del drawer
        configurarDrawer();
    }

    // ── Configuración del drawer ─────────────────────────────────────────────

    private void configurarDrawer() {
        // El botón hamburger abre el panel lateral al pulsarlo
        MaterialButton btnHamburguesa = findViewById(R.id.btnHamburguesa);
        if (btnHamburguesa != null) {
            btnHamburguesa.setOnClickListener(v ->
                    drawerLayout.openDrawer(GravityCompat.START));
        }

        // Cargamos los datos del usuario logueado para la cabecera del drawer
        refrescarFotoCabecera();

        // Cada opción del menú navega a su Activity correspondiente
        LinearLayout itemEditar  = findViewById(R.id.itemEditarPerfil);
        LinearLayout itemAjustes = findViewById(R.id.itemAjustes);

        if (itemEditar  != null) itemEditar.setOnClickListener(v -> abrirEditarPerfil());
        if (itemAjustes != null) itemAjustes.setOnClickListener(v -> abrirAjustes());

        // Botón de cerrar sesión en rojo: limpia la sesión y vuelve al Login
        MaterialButton btnLogout = findViewById(R.id.btnCerrarSesionDrawer);
        if (btnLogout != null) btnLogout.setOnClickListener(v -> cerrarSesion());
    }

    // Método público para que subclases (ej. EditProfileActivity) puedan forzar
    // un refresco de la cabecera del drawer tras guardar cambios en el perfil.
    // También se llama desde onResume para mantenerla siempre actualizada.
    protected void refrescarFotoCabecera() {
        SessionManager session = SessionManager.getInstance(this);
        long userId = session.getUsuarioActualId();
        if (userId == -1) return; // sin sesión activa no hay nada que cargar

        DatabaseHelper db = DatabaseHelper.getInstance(this);
        Map<String, Object> usuario = db.obtenerUsuario(userId);
        if (usuario != null) rellenarCabecera(usuario);
    }

    // Rellena el header del drawer con la foto, nombre, categoría y edad del usuario.
    private void rellenarCabecera(Map<String, Object> usuario) {
        TextView tvNombre    = findViewById(R.id.tvNombreDrawer);
        TextView tvCategoria = findViewById(R.id.tvCategoriaDrawer);
        TextView tvEdad      = findViewById(R.id.tvEdadDrawer);
        TextView tvInicial   = findViewById(R.id.tvInicialPerfil);
        ImageView imgFoto    = findViewById(R.id.imgFotoPerfilDrawer);

        // Nombre completo: "Nombre Apellidos"
        String nombre    = strOrDefault(usuario.get(DatabaseHelper.COL_NOMBRE),    "Usuario");
        String apellidos = strOrDefault(usuario.get(DatabaseHelper.COL_APELLIDOS), "");
        if (tvNombre != null) tvNombre.setText(nombre + " " + apellidos);

        // Categoría de pádel
        String categoria = strOrDefault(usuario.get(DatabaseHelper.COL_CATEGORIA), "—");
        if (tvCategoria != null) tvCategoria.setText(categoria);

        // Edad: si es 0 o no existe, no mostramos nada
        Object edadObj = usuario.get(DatabaseHelper.COL_EDAD);
        int edad = edadObj instanceof Number ? ((Number) edadObj).intValue() : 0;
        if (tvEdad != null) {
            tvEdad.setText(edad > 0 ? edad + " " + getString(R.string.drawer_anos) : "");
        }

        // Inicial del nombre como avatar por defecto
        if (tvInicial != null && !nombre.isEmpty()) {
            tvInicial.setText(String.valueOf(nombre.charAt(0)).toUpperCase());
        }

        // Foto de perfil: si el usuario tiene URI guardada, la mostramos; si no, la inicial
        String fotoUri = strOrDefault(usuario.get(DatabaseHelper.COL_FOTO_PERFIL), "");
        if (!fotoUri.isEmpty() && imgFoto != null && tvInicial != null) {
            try {
                imgFoto.setImageURI(Uri.parse(fotoUri));
                imgFoto.setVisibility(View.VISIBLE);
                tvInicial.setVisibility(View.GONE);
            } catch (Exception e) {
                // URI inválida: dejamos la inicial
            }
        }
    }

    // ── Navegación desde el drawer ───────────────────────────────────────────

    // Abre EditProfileActivity y cierra el drawer
    private void abrirEditarPerfil() {
        drawerLayout.closeDrawer(GravityCompat.START);
        startActivity(new Intent(this, EditProfileActivity.class));
    }

    // Abre SettingsActivity y cierra el drawer
    private void abrirAjustes() {
        drawerLayout.closeDrawer(GravityCompat.START);
        startActivity(new Intent(this, SettingsActivity.class));
    }

    // Cierra sesión: borra el ID de SharedPreferences y vuelve al Login
    private void cerrarSesion() {
        SessionManager.getInstance(this).cerrarSesion();
        Intent intent = new Intent(this, LoginActivity.class);
        // Limpiamos el stack de Activities para que el Atrás no vuelva al menú autenticado
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    // Método de conveniencia para abrir el drawer desde una subclase
    protected void abrirDrawer() {
        if (drawerLayout != null) drawerLayout.openDrawer(GravityCompat.START);
    }

    // Convierte Object a String de forma segura, devolviendo defaultVal si es null
    private String strOrDefault(Object obj, String defaultVal) {
        return (obj instanceof String && !((String) obj).isEmpty()) ? (String) obj : defaultVal;
    }
}
