package dam.iker.padeldart;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Gestiona toda la base de datos SQLite de la app. Extiende SQLiteOpenHelper,
// que es la clase de Android que se encarga de crear el archivo .db en el dispositivo
// y actualizarlo si en el futuro cambiamos la estructura de las tablas.
public class DatabaseHelper extends SQLiteOpenHelper {

    // Nombre del archivo que se guardará en el almacenamiento interno del móvil.
    // Cada vez que subamos DATABASE_VERSION, Android llama a onUpgrade automáticamente.
    private static final String DATABASE_NAME    = "padeldart.db";
    private static final int    DATABASE_VERSION = 3;  // v3: añadidos edad y foto_perfil al usuario

    // Nombre de la tabla principal. Solo tenemos una por ahora, que agrupa
    // datos personales, perfil de pádel y perfil de dardos del usuario.
    public static final String TABLE_USUARIOS = "usuarios";

    // --- Columnas de datos personales ---
    public static final String COL_ID         = "id";        // Clave primaria autoincremental
    public static final String COL_EMAIL      = "email";     // Único en la tabla, identificador de login
    public static final String COL_PASSWORD   = "password";
    public static final String COL_NOMBRE     = "nombre";
    public static final String COL_APELLIDOS  = "apellidos";
    public static final String COL_DNI        = "dni";
    public static final String COL_DIRECCION  = "direccion";
    public static final String COL_CP         = "cp";
    public static final String COL_METODO_PAGO = "metodo_pago";

    // --- Columnas del perfil de pádel ---
    // Equivalen al subdocumento "perfil_padel" que teníamos en Firestore,
    // aplanados en la misma tabla para simplificar las queries.
    public static final String COL_CATEGORIA          = "categoria_actual";
    public static final String COL_PUNTUACION         = "puntuacion_media";
    public static final String COL_TOTAL_VALORACIONES = "total_valoraciones";
    public static final String COL_CLASES_DADAS       = "numero_clases_dadas";
    public static final String COL_POSICION           = "posicion";      // "Drive" o "Revés"
    public static final String COL_PROVINCIA          = "provincia";
    public static final String COL_PISTA_FAVORITA     = "pista_favorita";

    // --- Columnas del perfil de dardos ---
    public static final String COL_MEDIA_PUNTOS  = "media_puntos_tiradas";
    public static final String COL_PARTIDAS_PVP  = "numero_partidas_pvp_jugadas";
    public static final String COL_PARTIDAS_PVE  = "numero_partidas_pve_jugadas";
    public static final String COL_GANADAS_PVP   = "numero_partidas_pvp_ganadas";
    public static final String COL_GANADAS_PVE   = "numero_partidas_pve_ganadas";
    public static final String COL_TIENE_DIANA   = "tiene_diana_propia";  // SQLite no tiene boolean: 0/1
    public static final String COL_TIPO_DIANA    = "tipo_diana";

    // --- Columnas añadidas en v3 ---
    public static final String COL_EDAD         = "edad";         // Edad en años
    public static final String COL_FOTO_PERFIL  = "foto_perfil";  // URI de la foto local

    // Sentencia SQL para crear la tabla principal de usuarios.
    // Construida concatenando constantes para evitar typos al renombrar columnas.
    private static final String CREATE_TABLE_USUARIOS =
            "CREATE TABLE " + TABLE_USUARIOS + " (" +
                    COL_ID               + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COL_EMAIL            + " TEXT UNIQUE NOT NULL, " +
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
                    COL_TIPO_DIANA       + " TEXT, " +
                    COL_EDAD             + " INTEGER DEFAULT 0, " +
                    COL_FOTO_PERFIL      + " TEXT DEFAULT ''" +
                    ");";

    // --- Tabla de mensajes directos entre usuarios ---
    // Almacena cada mensaje enviado entre dos usuarios, con su timestamp Unix (ms).
    public static final String TABLE_MENSAJES = "mensajes";
    private static final String CREATE_TABLE_MENSAJES =
            "CREATE TABLE " + TABLE_MENSAJES + " (" +
                    "id          INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "emisor_id   INTEGER NOT NULL, " +
                    "receptor_id INTEGER NOT NULL, " +
                    "contenido   TEXT NOT NULL, " +
                    "timestamp   INTEGER NOT NULL" +
                    ");";

    // --- Tabla del tablón de anuncios de la zona provincial ---
    // Tipos posibles: PARTIDA, PALA, CLASE_OFRECER, CLASE_SOLICITAR
    public static final String TABLE_ANUNCIOS = "anuncios_zona";
    private static final String CREATE_TABLE_ANUNCIOS =
            "CREATE TABLE " + TABLE_ANUNCIOS + " (" +
                    "id          INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "autor_id    INTEGER NOT NULL, " +
                    "tipo        TEXT NOT NULL, " +
                    "descripcion TEXT NOT NULL, " +
                    "provincia   TEXT NOT NULL, " +
                    "timestamp   INTEGER NOT NULL" +
                    ");";

    // Instancia única (Singleton): toda la app comparte la misma conexión a la BD
    // para evitar conflictos si varias Activities intentan escribir a la vez.
    private static DatabaseHelper instancia;

    // synchronized evita que dos hilos creen dos instancias distintas en el arranque.
    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instancia == null) {
            // getApplicationContext: no quedamos ligados al ciclo de vida de una Activity concreta.
            instancia = new DatabaseHelper(context.getApplicationContext());
        }
        return instancia;
    }

    // Constructor privado: solo puede llamarse desde getInstance().
    private DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // Android llama a este método la primera vez que la app abre la BD
    // (cuando el archivo .db todavía no existe en el dispositivo).
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_USUARIOS);
        db.execSQL(CREATE_TABLE_MENSAJES);
        db.execSQL(CREATE_TABLE_ANUNCIOS);
    }

    // Se ejecuta cuando DATABASE_VERSION sube. Añadimos solo las tablas nuevas
    // para no perder los datos de los usuarios ya registrados.
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            // v2: añadidos mensajes privados y tablón de anuncios provinciales
            db.execSQL(CREATE_TABLE_MENSAJES);
            db.execSQL(CREATE_TABLE_ANUNCIOS);
        }
        if (oldVersion < 3) {
            // v3: añadimos edad y foto de perfil; ALTER TABLE solo admite ADD COLUMN
            db.execSQL("ALTER TABLE " + TABLE_USUARIOS + " ADD COLUMN " + COL_EDAD        + " INTEGER DEFAULT 0");
            db.execSQL("ALTER TABLE " + TABLE_USUARIOS + " ADD COLUMN " + COL_FOTO_PERFIL + " TEXT DEFAULT ''");
        }
    }

    // Actualiza los campos editables del perfil del usuario (no toca email ni DNI).
    public boolean actualizarPerfil(long id, String nombre, String apellidos, String posicion,
                                    String provincia, String pistaFavorita, int edad, String fotoPerfil) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_NOMBRE,         nombre);
        cv.put(COL_APELLIDOS,      apellidos);
        cv.put(COL_POSICION,       posicion);
        cv.put(COL_PROVINCIA,      provincia);
        cv.put(COL_PISTA_FAVORITA, pistaFavorita);
        cv.put(COL_EDAD,           edad);
        // Solo actualizamos la foto si se proporcionó una nueva
        if (fotoPerfil != null && !fotoPerfil.isEmpty()) cv.put(COL_FOTO_PERFIL, fotoPerfil);
        int filas = db.update(TABLE_USUARIOS, cv, COL_ID + " = ?", new String[]{String.valueOf(id)});
        return filas > 0;
    }


    // -----------------------------------------------------------------------
    //  CRUD - USUARIOS
    // -----------------------------------------------------------------------

    // Inserta un usuario nuevo con todos sus datos de perfil.
    // Recibe un Map para que el Activity no necesite conocer los nombres de columna SQLite.
    // Devuelve el ID generado o -1 si el correo ya existe.
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

        // Perfil de pádel: la categoría y puntuación las calcula RegistroActivity
        values.put(COL_CATEGORIA,          (String) datos.get("categoria_actual"));
        values.put(COL_PUNTUACION,         (Double) datos.get("puntuacion_media"));
        values.put(COL_TOTAL_VALORACIONES, 1);
        values.put(COL_CLASES_DADAS,       0);
        values.put(COL_POSICION,           (String) datos.get("posicion"));
        values.put(COL_PROVINCIA,          (String) datos.get("provincia"));
        values.put(COL_PISTA_FAVORITA,     (String) datos.get("pista_favorita"));

        // Contadores de dardos: arrancan a cero en el momento del registro
        values.put(COL_MEDIA_PUNTOS, 0.0);
        values.put(COL_PARTIDAS_PVP, 0);
        values.put(COL_PARTIDAS_PVE, 0);
        values.put(COL_GANADAS_PVP,  0);
        values.put(COL_GANADAS_PVE,  0);

        // SQLite no tiene boolean: guardamos 1 si tiene diana propia, 0 si no.
        boolean tieneDiana = Boolean.TRUE.equals(datos.get("tiene_diana_propia"));
        values.put(COL_TIENE_DIANA, tieneDiana ? 1 : 0);
        if (tieneDiana && datos.containsKey("tipo_diana")) {
            values.put(COL_TIPO_DIANA, (String) datos.get("tipo_diana"));
        }

        // Campos añadidos en v3: edad y foto de perfil (opcionales, tienen default)
        Object edad = datos.get("edad");
        values.put(COL_EDAD, edad instanceof Integer ? (Integer) edad : 0);
        Object foto = datos.get("foto_perfil");
        values.put(COL_FOTO_PERFIL, foto instanceof String ? (String) foto : "");

        try {
            // insertOrThrow lanza SQLiteConstraintException si el email ya existe (columna UNIQUE)
            return db.insertOrThrow(TABLE_USUARIOS, null, values);
        } catch (android.database.sqlite.SQLiteConstraintException e) {
            return -1;
        }
    }

    // Comprueba email + contraseña y devuelve el ID del usuario si coinciden, o -1.
    // Es el equivalente local al signInWithEmailAndPassword() de Firebase Auth.
    public long iniciarSesion(String email, String password) {
        SQLiteDatabase db = this.getReadableDatabase();

        // Query parametrizada con "?" para prevenir inyección SQL
        Cursor cursor = db.query(
                TABLE_USUARIOS,
                new String[]{COL_ID},
                COL_EMAIL + " = ? AND " + COL_PASSWORD + " = ?",
                new String[]{email, password},
                null, null, null
        );

        long id = -1;
        if (cursor.moveToFirst()) id = cursor.getLong(0);
        // Siempre cerramos el Cursor para liberar el recurso de base de datos
        cursor.close();
        return id;
    }

    // Devuelve cuántos usuarios hay en la BD. Útil para diagnóstico en desarrollo.
    public int contarUsuarios() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_USUARIOS, null);
        int count = 0;
        if (cursor.moveToFirst()) count = cursor.getInt(0);
        cursor.close();
        return count;
    }

    // Consulta rápida para saber si un correo ya está en la BD antes de registrar.
    public boolean existeEmail(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USUARIOS, new String[]{COL_ID},
                COL_EMAIL + " = ?", new String[]{email}, null, null, null);
        boolean existe = cursor.moveToFirst();
        cursor.close();
        return existe;
    }

    // Devuelve todos los datos de un usuario como Map<String, Object>.
    // Los tipos se infieren del tipo de columna que reporta el propio Cursor.
    public Map<String, Object> obtenerUsuario(long id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USUARIOS, null,
                COL_ID + " = ?", new String[]{String.valueOf(id)},
                null, null, null);

        if (!cursor.moveToFirst()) { cursor.close(); return null; }

        Map<String, Object> usuario = new HashMap<>();
        // Recorremos todas las columnas dinámicamente para no tener que
        // escribir un get() por cada campo al añadir columnas nuevas.
        for (int i = 0; i < cursor.getColumnCount(); i++) {
            String col = cursor.getColumnName(i);
            switch (cursor.getType(i)) {
                case Cursor.FIELD_TYPE_INTEGER: usuario.put(col, cursor.getLong(i));   break;
                case Cursor.FIELD_TYPE_FLOAT:   usuario.put(col, cursor.getDouble(i)); break;
                default:                        usuario.put(col, cursor.getString(i));
            }
        }
        cursor.close();
        return usuario;
    }

    // Devuelve la lista de todos los usuarios excepto el indicado, con los campos
    // mínimos para mostrarlos en la lista de chat (nombre, apellidos, categoría).
    public List<Map<String, Object>> obtenerTodosUsuariosMenos(long miId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(
                TABLE_USUARIOS,
                new String[]{COL_ID, COL_NOMBRE, COL_APELLIDOS, COL_CATEGORIA, COL_PROVINCIA},
                COL_ID + " != ?",
                new String[]{String.valueOf(miId)},
                null, null, COL_NOMBRE + " ASC"
        );

        List<Map<String, Object>> lista = new ArrayList<>();
        while (cursor.moveToNext()) {
            Map<String, Object> u = new HashMap<>();
            u.put(COL_ID,        cursor.getLong(0));
            u.put(COL_NOMBRE,    cursor.getString(1));
            u.put(COL_APELLIDOS, cursor.getString(2));
            u.put(COL_CATEGORIA, cursor.getString(3));
            u.put(COL_PROVINCIA, cursor.getString(4));
            lista.add(u);
        }
        cursor.close();
        return lista;
    }


    // -----------------------------------------------------------------------
    //  CRUD - MENSAJES
    // -----------------------------------------------------------------------

    // Guarda un mensaje enviado entre dos usuarios en la tabla mensajes.
    // El timestamp es Unix milisegundos, lo que permite ordenar por tiempo.
    public long enviarMensaje(long emisorId, long receptorId, String contenido) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("emisor_id",   emisorId);
        values.put("receptor_id", receptorId);
        values.put("contenido",   contenido);
        values.put("timestamp",   System.currentTimeMillis());
        return db.insert(TABLE_MENSAJES, null, values);
    }

    // Devuelve todos los mensajes entre dos usuarios, ordenados de más antiguo a más nuevo.
    // La query OR cubre ambos sentidos de la conversación (yo -> tú Y tú -> yo).
    public List<Map<String, Object>> obtenerConversacion(long user1Id, long user2Id) {
        SQLiteDatabase db = this.getReadableDatabase();
        String sql =
                "SELECT emisor_id, receptor_id, contenido, timestamp FROM " + TABLE_MENSAJES +
                " WHERE (emisor_id = ? AND receptor_id = ?) OR (emisor_id = ? AND receptor_id = ?)" +
                " ORDER BY timestamp ASC";

        Cursor cursor = db.rawQuery(sql, new String[]{
                String.valueOf(user1Id), String.valueOf(user2Id),
                String.valueOf(user2Id), String.valueOf(user1Id)
        });

        List<Map<String, Object>> mensajes = new ArrayList<>();
        while (cursor.moveToNext()) {
            Map<String, Object> m = new HashMap<>();
            m.put("emisor_id",   cursor.getLong(0));
            m.put("receptor_id", cursor.getLong(1));
            m.put("contenido",   cursor.getString(2));
            m.put("timestamp",   cursor.getLong(3));
            mensajes.add(m);
        }
        cursor.close();
        return mensajes;
    }


    // -----------------------------------------------------------------------
    //  CRUD - ANUNCIOS DE ZONA
    // -----------------------------------------------------------------------

    // Publica un anuncio en el tablón de la zona provincial.
    // tipo: "PARTIDA" | "PALA" | "CLASE_OFRECER" | "CLASE_SOLICITAR"
    public long publicarAnuncio(long autorId, String tipo, String descripcion, String provincia) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("autor_id",    autorId);
        values.put("tipo",        tipo);
        values.put("descripcion", descripcion);
        values.put("provincia",   provincia);
        values.put("timestamp",   System.currentTimeMillis());
        return db.insert(TABLE_ANUNCIOS, null, values);
    }

    // Devuelve todos los anuncios de una provincia, del más reciente al más antiguo.
    // Hace un JOIN con usuarios para obtener el nombre del autor en la misma query.
    public List<Map<String, Object>> obtenerAnunciosProvincia(String provincia) {
        SQLiteDatabase db = this.getReadableDatabase();
        String sql =
                "SELECT a.id, a.autor_id, a.tipo, a.descripcion, a.timestamp, " +
                "u.nombre, u.apellidos, u.categoria_actual " +
                "FROM " + TABLE_ANUNCIOS + " a " +
                "JOIN " + TABLE_USUARIOS + " u ON a.autor_id = u.id " +
                "WHERE a.provincia = ? " +
                "ORDER BY a.timestamp DESC";

        Cursor cursor = db.rawQuery(sql, new String[]{provincia});

        List<Map<String, Object>> anuncios = new ArrayList<>();
        while (cursor.moveToNext()) {
            Map<String, Object> a = new HashMap<>();
            a.put("id",              cursor.getLong(0));
            a.put("autor_id",        cursor.getLong(1));
            a.put("tipo",            cursor.getString(2));
            a.put("descripcion",     cursor.getString(3));
            a.put("timestamp",       cursor.getLong(4));
            a.put("nombre",          cursor.getString(5));
            a.put("apellidos",       cursor.getString(6));
            a.put("categoria_actual", cursor.getString(7));
            anuncios.add(a);
        }
        cursor.close();
        return anuncios;
    }
}
