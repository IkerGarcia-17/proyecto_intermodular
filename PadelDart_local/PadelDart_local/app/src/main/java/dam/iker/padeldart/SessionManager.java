package dam.iker.padeldart;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.firebase.auth.FirebaseAuth;

// Controla si hay un usuario logueado y quién es.
// Firebase Auth persiste las credenciales; SharedPreferences almacena el ID SQLite local
// para que las Activities puedan consultar la BD sin hacer una query asíncrona.
public class SessionManager {

    private static final String PREFS  = "padeldart_session";
    private static final String KEY_ID = "user_id";

    private final SharedPreferences prefs;
    private static SessionManager instancia;

    public static synchronized SessionManager getInstance(Context context) {
        if (instancia == null) instancia = new SessionManager(context.getApplicationContext());
        return instancia;
    }

    private SessionManager(Context ctx) {
        prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    // Guarda el ID SQLite del usuario tras el login; Firebase Auth ya persistió sus credenciales
    public void iniciarSesion(long userId) {
        prefs.edit().putLong(KEY_ID, userId).apply();
    }

    // Cierra sesión: elimina el token de Firebase y el ID local de SharedPreferences
    public void cerrarSesion() {
        FirebaseAuth.getInstance().signOut();
        prefs.edit().remove(KEY_ID).apply();
    }

    // Sesión válida si Firebase tiene usuario autenticado Y tenemos el ID SQLite guardado
    public boolean haySesionActiva() {
        return FirebaseAuth.getInstance().getCurrentUser() != null
                && prefs.getLong(KEY_ID, -1) != -1;
    }

    // Devuelve el ID SQLite del usuario activo, o -1 si no hay sesión
    public long getUsuarioActualId() {
        return prefs.getLong(KEY_ID, -1);
    }
}
