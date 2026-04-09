package dam.iker.padeldart;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.HashMap;
import java.util.Map;

// Gestiona toda la base de datos SQLite de la app. Extiende SQLiteOpenHelper,
// que es la clase de Android que se encarga de crear el archivo .db en el dispositivo
// y actualizarlo si en el futuro cambiamos la estructura de las tablas.
public class DatabaseHelper extends SQLiteOpenHelper {

    // Nombre del archivo que se guardará en el almacenamiento interno del móvil
    // y versión del esquema: si subimos este número, Android llama a onUpgrade automáticamente.
    private static final String DATABASE_NAME = "padeldart.db";
    private static final int DATABASE_VERSION = 1;

    // Nombre de la tabla. Solo tenemos una por ahora, que agrupa
    // datos personales, perfil de pádel y perfil de dardos del usuario.
    public static final String TABLE_USUARIOS = "usuarios";

    // --- Columnas de datos personales ---
    public static final String COL_ID         = "id";           // Clave primaria autoincremental
    public static final String COL_EMAIL      = "email";        // Único en la tabla, funciona como identificador de login
    public static final String COL_PASSWORD   = "password";
    public static final String COL_NOMBRE     = "nombre";
    public static final String COL_APELLIDOS  = "apellidos";
    public static final String COL_DNI        = "dni";
    public static final String COL_DIRECCION  = "direccion";
    public static final String COL_CP         = "cp";
    public static final String COL_METODO_PAGO = "metodo_pago";

    // --- Columnas del perfil de pádel ---
    // Estos campos equivalen al subdocumento "perfil_padel" que teníamos en Firestore,
    // aquí los tenemos aplanados en la misma tabla para simplificar las queries.
    public static final String COL_CATEGORIA         = "categoria_actual";
    public static final String COL_PUNTUACION        = "puntuacion_media";
    public static final String COL_TOTAL_VALORACIONES = "total_valoraciones";
    public static final String COL_CLASES_DADAS      = "numero_clases_dadas";
    public static final String COL_POSICION          = "posicion";        // "Drive" o "Revés"
    public static final String COL_PROVINCIA         = "provincia";
    public static final String COL_PISTA_FAVORITA    = "pista_favorita";

    // --- Columnas del perfil de dardos ---
    public static final String COL_MEDIA_PUNTOS  = "media_puntos_tiradas";
    public static final String COL_PARTIDAS_PVP  = "numero_partidas_pvp_jugadas";
    public static final String COL_PARTIDAS_PVE  = "numero_partidas_pve_jugadas";
    public static final String COL_GANADAS_PVP   = "numero_partidas_pvp_ganadas";
    public static final String COL_GANADAS_PVE   = "numero_partidas_pve_ganadas";
    public static final String COL_TIENE_DIANA   = "tiene_diana_propia";  // SQLite no tiene boolean, usamos 0/1
    public static final String COL_TIPO_DIANA    = "tipo_diana";          // Solo se rellena si tiene_diana = 1

    // Sentencia SQL para crear la tabla. La construimos concatenando las constantes
    // para evitar errores de typo si en el futuro renombramos alguna columna.
    private static final String CREATE_TABLE_USUARIOS =
            "CREATE TABLE " + TABLE_USUARIOS + " (" +
                    COL_ID               + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COL_EMAIL            + " TEXT UNIQUE NOT NULL, " +   // UNIQUE evita registros duplicados por correo
                    COL_PASSWORD         + " TEXT NOT NULL, " +
                    COL_NOMBRE           + " TEXT, " +
                    COL_APELLIDOS        + " TEXT, " +
                    COL_DNI              + " TEXT, " +
                    COL_DIRECCION        + " TEXT, " +
                    COL_CP               + " TEXT, " +
                    COL_METODO_PAGO      + " TEXT, " +
                    COL_CATEGORIA        + " TEXT, " +
                    COL_PUNTUACION       + " REAL DEFAULT 0.0, " +
                    COL_TOTAL_VALORACIONES + " INTEGER DEFAULT 1, " +
                    COL_CLASES_DADAS     + " INTEGER DEFAULT 0, " +
                    COL_POSICION         + " TEXT, " +
                    COL_PROVINCIA        + " TEXT, " +
                    COL_PISTA_FAVORITA   + " TEXT, " +
                    COL_MEDIA_PUNTOS     + " REAL DEFAULT 0.0, " +
                    COL_PARTIDAS_PVP     + " INTEGER DEFAULT 0, " +
                    COL_PARTIDAS_PVE     + " INTEGER DEFAULT 0, " +
                    COL_GANADAS_PVP      + " INTEGER DEFAULT 0, " +
                    COL_GANADAS_PVE      + " INTEGER DEFAULT 0, " +
                    COL_TIENE_DIANA      + " INTEGER DEFAULT 0, " +
                    COL_TIPO_DIANA       + " TEXT" +
                    ");";

    // Instancia única de esta clase. Al ser Singleton nos aseguramos de que
    // toda la app comparte la misma conexión a la BD, lo que evita conflictos
    // si varias Activities intentan escribir a la vez.
    private static DatabaseHelper instancia;

    // Punto de acceso global al Singleton. El synchronized evita que dos hilos
    // creen dos instancias distintas en el arranque de la app.
    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instancia == null) {
            // Usamos getApplicationContext para que no quede ligado al ciclo de vida
            // de una Activity concreta y podamos reutilizarlo desde cualquier pantalla.
            instancia = new DatabaseHelper(context.getApplicationContext());
        }
        return instancia;
    }

    // Constructor privado: solo puede llamarse desde getInstance().
    private DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // Android llama a este método la primera vez que la app abre la BD,
    // es decir, cuando el archivo .db todavía no existe en el dispositivo.
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_USUARIOS);
    }

    // Se ejecuta cuando DATABASE_VERSION es mayor que la versión guardada en el dispositivo.
    // De momento borramos y recreamos; cuando la app esté en producción habrá que hacer
    // una migración real para no perder los datos de los usuarios.
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USUARIOS);
        onCreate(db);
    }


    // -----------------------------------------------------------------------
    //  OPERACIONES CRUD
    // -----------------------------------------------------------------------

    // Inserta un usuario nuevo con todos sus datos de perfil.
    // Recibe un Map porque así el código del Activity no necesita conocer
    // los nombres de columna internos de SQLite; eso es responsabilidad de esta clase.
    // Devuelve el ID generado si todo va bien, o -1 si el correo ya existe en la BD.
    public long registrarUsuario(Map<String, Object> datos) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        // Datos personales básicos
        values.put(COL_EMAIL,       (String) datos.get("email"));
        values.put(COL_PASSWORD,    (String) datos.get("password"));
        values.put(COL_NOMBRE,      (String) datos.get("nombre"));
        values.put(COL_APELLIDOS,   (String) datos.get("apellidos"));
        values.put(COL_DNI,         (String) datos.get("dni"));
        values.put(COL_DIRECCION,   (String) datos.get("direccion"));
        values.put(COL_CP,          (String) datos.get("cp"));
        values.put(COL_METODO_PAGO, (String) datos.get("metodo_pago"));

        // Perfil de pádel: la categoría y la puntuación las calcula el RegistroActivity
        // antes de llamar aquí, así que solo las guardamos tal cual llegan.
        values.put(COL_CATEGORIA,          (String) datos.get("categoria_actual"));
        values.put(COL_PUNTUACION,         (Double) datos.get("puntuacion_media"));
        values.put(COL_TOTAL_VALORACIONES, 1);   // Todo usuario empieza con 1 valoración (la suya propia)
        values.put(COL_CLASES_DADAS,       0);
        values.put(COL_POSICION,           (String) datos.get("posicion"));
        values.put(COL_PROVINCIA,          (String) datos.get("provincia"));
        values.put(COL_PISTA_FAVORITA,     (String) datos.get("pista_favorita"));

        // Perfil de dardos: todos los contadores arrancan a cero en el registro.
        values.put(COL_MEDIA_PUNTOS, 0.0);
        values.put(COL_PARTIDAS_PVP, 0);
        values.put(COL_PARTIDAS_PVE, 0);
        values.put(COL_GANADAS_PVP,  0);
        values.put(COL_GANADAS_PVE,  0);

        // SQLite no tiene tipo boolean: guardamos 1 si tiene diana, 0 si no.
        boolean tieneDiana = Boolean.TRUE.equals(datos.get("tiene_diana_propia"));
        values.put(COL_TIENE_DIANA, tieneDiana ? 1 : 0);

        // El tipo de diana solo tiene sentido si el usuario ha dicho que tiene una,
        // así que solo lo insertamos en ese caso.
        if (tieneDiana && datos.containsKey("tipo_diana")) {
            values.put(COL_TIPO_DIANA, (String) datos.get("tipo_diana"));
        }

        try {
            // insertOrThrow lanza SQLiteConstraintException si el email ya existe (columna UNIQUE),
            // lo que nos permite dar un mensaje de error específico en el Activity.
            return db.insertOrThrow(TABLE_USUARIOS, null, values);
        } catch (android.database.sqlite.SQLiteConstraintException e) {
            return -1;
        }
    }

    // Comprueba email + contraseña y devuelve el ID del usuario si coinciden,
    // o -1 si las credenciales son incorrectas. Es el equivalente local al
    // signInWithEmailAndPassword() que usábamos con Firebase Auth.
    public long iniciarSesion(String email, String password) {
        SQLiteDatabase db = this.getReadableDatabase();

        // Usamos query parametrizada (los "?") para evitar inyección SQL.
        // Solo pedimos la columna ID porque es lo único que necesitamos aquí.
        Cursor cursor = db.query(
                TABLE_USUARIOS,
                new String[]{COL_ID},
                COL_EMAIL + " = ? AND " + COL_PASSWORD + " = ?",
                new String[]{email, password},
                null, null, null
        );

        long id = -1;
        if (cursor.moveToFirst()) {
            id = cursor.getLong(0);
        }
        // Siempre hay que cerrar el Cursor para liberar el recurso.
        cursor.close();
        return id;
    }

    // Consulta rápida para saber si un correo ya está en la BD antes de intentar
    // registrarlo. Así podemos dar un mensaje más claro que dejar que falle el insert.
    public boolean existeEmail(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(
                TABLE_USUARIOS,
                new String[]{COL_ID},
                COL_EMAIL + " = ?",
                new String[]{email},
                null, null, null
        );
        boolean existe = cursor.moveToFirst();
        cursor.close();
        return existe;
    }

    // Devuelve todos los datos de un usuario como Map<String, Object>,
    // manteniendo la misma estructura que teníamos al leer de Firestore.
    // Los tipos los inferimos del tipo de columna que reporta el propio Cursor.
    public Map<String, Object> obtenerUsuario(long id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(
                TABLE_USUARIOS,
                null,                         // null = todas las columnas
                COL_ID + " = ?",
                new String[]{String.valueOf(id)},
                null, null, null
        );

        if (!cursor.moveToFirst()) {
            cursor.close();
            return null;
        }

        Map<String, Object> usuario = new HashMap<>();

        // Recorremos todas las columnas dinámicamente para no tener que
        // escribir un get() por cada campo cada vez que añadamos una columna nueva.
        for (int i = 0; i < cursor.getColumnCount(); i++) {
            String col = cursor.getColumnName(i);
            switch (cursor.getType(i)) {
                case Cursor.FIELD_TYPE_INTEGER:
                    usuario.put(col, cursor.getLong(i));
                    break;
                case Cursor.FIELD_TYPE_FLOAT:
                    usuario.put(col, cursor.getDouble(i));
                    break;
                default:
                    usuario.put(col, cursor.getString(i));
            }
        }
        cursor.close();
        return usuario;
    }
}
