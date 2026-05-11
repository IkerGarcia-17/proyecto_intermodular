package dam.iker.padeldart;

import android.content.Context;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

// Controla si hay un usuario logueado y quién es.
// Antes usaba SharedPreferences para guardar el ID de SQLite; ahora delega en
// FirebaseAuth, que persiste la sesión automáticamente entre reinicios de la app.
public class SessionManager {

    // Singleton: una sola instancia para toda la app (Context ya no es necesario)
    private static SessionManager instancia;

    public static synchronized SessionManager getInstance(Context context) {
        if (instancia == null) instancia = new SessionManager();
        return instancia;
    }

    private SessionManager() {}

    // Firebase ya persiste la sesión internamente; este método solo existe
    // para mantener la misma API que antes y no romper los Activity que lo llaman
    public void iniciarSesion(String userId) { /* Firebase gestiona la persistencia */ }

    // Cierra sesión limpiando el token de Firebase Auth en el dispositivo
    public void cerrarSesion() {
        FirebaseAuth.getInstance().signOut();
    }

    // Devuelve true si Firebase tiene un usuario autenticado actualmente
    public boolean haySesionActiva() {
        return FirebaseAuth.getInstance().getCurrentUser() != null;
    }

    // Devuelve el UID del usuario activo, o "" si no hay sesión.
    // Antes devolvía long (-1 si sin sesión); ahora es String ("" si sin sesión).
    public String getUsuarioActualId() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return user != null ? user.getUid() : "";
    }
}
