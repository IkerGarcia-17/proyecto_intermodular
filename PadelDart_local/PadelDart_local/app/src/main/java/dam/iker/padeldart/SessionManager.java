package dam.iker.padeldart;

import android.content.Context;
import android.content.SharedPreferences;

// Controla si hay un usuario logueado y quién es.
// Antes este papel lo hacía FirebaseAuth.getCurrentUser(), que nos devolvía
// el usuario activo de forma global. Aquí hacemos lo mismo pero guardando
// el ID en SharedPreferences, que es el almacenamiento clave-valor de Android.
public class SessionManager {

    // Nombre del archivo de preferencias que Android crea en el almacenamiento interno.
    private static final String PREFS_NAME = "padeldart_session";

    // Clave bajo la que guardamos el ID del usuario logueado.
    private static final String KEY_USER_ID = "user_id";

    // Valor centinela: si SharedPreferences devuelve este número, es que no hay sesión.
    // Usamos -1 porque los IDs de SQLite empiezan en 1.
    private static final long NO_SESSION = -1L;

    private final SharedPreferences prefs;

    // Singleton, igual que en DatabaseHelper: una sola instancia para toda la app.
    private static SessionManager instancia;

    public static synchronized SessionManager getInstance(Context context) {
        if (instancia == null) {
            instancia = new SessionManager(context.getApplicationContext());
        }
        return instancia;
    }

    private SessionManager(Context context) {
        // MODE_PRIVATE: solo nuestra app puede leer este archivo de preferencias.
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    // Guarda el ID del usuario que acaba de autenticarse correctamente.
    // Se llama justo después de que DatabaseHelper confirme las credenciales.
    public void iniciarSesion(long userId) {
        prefs.edit().putLong(KEY_USER_ID, userId).apply();
    }

    // Borra el ID guardado. Equivale al mAuth.signOut() de Firebase.
    // Después de esto, haySesionActiva() devolverá false.
    public void cerrarSesion() {
        prefs.edit().putLong(KEY_USER_ID, NO_SESSION).apply();
    }

    // Útil para comprobar al arrancar la app si el usuario ya estaba logueado
    // y saltar directamente al menú principal sin pasar por el login.
    public boolean haySesionActiva() {
        return prefs.getLong(KEY_USER_ID, NO_SESSION) != NO_SESSION;
    }

    // Devuelve el ID del usuario activo para poder hacer consultas en DatabaseHelper.
    // Si no hay sesión devuelve -1, así que conviene comprobar haySesionActiva() antes.
    public long getUsuarioActualId() {
        return prefs.getLong(KEY_USER_ID, NO_SESSION);
    }
}
