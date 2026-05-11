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
    private static final int    DATABASE_VERSION = 5;  // v5: caché de pistas de pádel de España

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

    // --- Tabla de amistades / solicitudes de amistad ---
    // Cada fila representa una relación entre dos usuarios.
    // estado puede ser "PENDIENTE" (solicitud enviada, no aceptada aún) o "ACEPTADO".
    // La búsqueda de amigos se hace siempre en ambas direcciones: solicitante↔receptor.
    public static final String TABLE_AMIGOS = "amigos";
    private static final String CREATE_TABLE_AMIGOS =
            "CREATE TABLE " + TABLE_AMIGOS + " (" +
                    "id             INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "solicitante_id INTEGER NOT NULL, " +  // quien envió la solicitud
                    "receptor_id    INTEGER NOT NULL, " +  // quien la recibió
                    "estado         TEXT    NOT NULL DEFAULT 'PENDIENTE', " +
                    "timestamp      INTEGER NOT NULL" +
                    ");";

    // --- Tabla de caché de pistas de pádel de España (OSM/Overpass) ---
    // Se descarga en background la primera vez; las consultas posteriores son instantáneas.
    // municipio normalizado (minúsculas sin tildes) para búsqueda insensible a acentos.
    public static final String TABLE_PISTAS = "pistas_padel";
    private static final String CREATE_TABLE_PISTAS =
            "CREATE TABLE " + TABLE_PISTAS + " (" +
                    "osm_id     TEXT PRIMARY KEY, " +
                    "nombre     TEXT NOT NULL, " +
                    "municipio  TEXT, " +         // nombre tal como viene de OSM
                    "municipio_norm TEXT, " +      // lowercase sin tildes para búsquedas rápidas
                    "provincia  TEXT, " +
                    "lat        REAL, " +
                    "lon        REAL" +
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
        db.execSQL(CREATE_TABLE_AMIGOS);
        db.execSQL(CREATE_TABLE_PISTAS);
    }

    // Se ejecuta cuando DATABASE_VERSION sube. Añadimos solo las tablas/columnas nuevas
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
        if (oldVersion < 4) {
            // v4: tabla de amistades para la nueva sección "Amigos" del menú lateral
            db.execSQL(CREATE_TABLE_AMIGOS);
        }
        if (oldVersion < 5) {
            // v5: caché local de pistas de pádel de España (descargadas de OSM/Overpass)
            db.execSQL(CREATE_TABLE_PISTAS);
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
        // Incluimos foto_perfil del autor para mostrar su avatar en la tarjeta
        String sql =
                "SELECT a.id, a.autor_id, a.tipo, a.descripcion, a.timestamp, " +
                "u.nombre, u.apellidos, u.categoria_actual, u.foto_perfil " +
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
            a.put("foto_perfil",     cursor.getString(8));
            anuncios.add(a);
        }
        cursor.close();
        return anuncios;
    }


    // Actualiza el contenido de un anuncio propio (descripción) marcando nuevo timestamp.
    public boolean actualizarAnuncio(long id, String descripcion) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("descripcion", descripcion);
        cv.put("timestamp",   System.currentTimeMillis());
        return db.update(TABLE_ANUNCIOS, cv, "id = ?", new String[]{String.valueOf(id)}) > 0;
    }

    // Elimina un anuncio por su id.
    public boolean eliminarAnuncio(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(TABLE_ANUNCIOS, "id = ?", new String[]{String.valueOf(id)}) > 0;
    }

    // -----------------------------------------------------------------------
    //  CRUD - AMIGOS
    // -----------------------------------------------------------------------

    // Busca un usuario por su correo electrónico y devuelve sus datos básicos.
    // Devuelve null si no existe ninguna cuenta con ese correo.
    // Usado en FriendsActivity para buscar a quien agregar como amigo.
    public Map<String, Object> buscarUsuarioPorEmail(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(
                TABLE_USUARIOS,
                new String[]{COL_ID, COL_NOMBRE, COL_APELLIDOS, COL_CATEGORIA,
                             COL_PROVINCIA, COL_FOTO_PERFIL, COL_EMAIL},
                COL_EMAIL + " = ?",
                new String[]{email.trim().toLowerCase()},
                null, null, null
        );
        if (!cursor.moveToFirst()) { cursor.close(); return null; }

        // Construimos el Map con los campos necesarios para mostrar la tarjeta de usuario
        Map<String, Object> u = new HashMap<>();
        u.put(COL_ID,          cursor.getLong(0));
        u.put(COL_NOMBRE,      cursor.getString(1));
        u.put(COL_APELLIDOS,   cursor.getString(2));
        u.put(COL_CATEGORIA,   cursor.getString(3));
        u.put(COL_PROVINCIA,   cursor.getString(4));
        u.put(COL_FOTO_PERFIL, cursor.getString(5));
        u.put(COL_EMAIL,       cursor.getString(6));
        cursor.close();
        return u;
    }

    // Devuelve el estado de la relación entre dos usuarios.
    // Retorna "ACEPTADO", "PENDIENTE" o null si no hay ninguna relación.
    // Busca en ambas direcciones: A→B y B→A.
    public String estadoAmistad(long userId, long otroId) {
        SQLiteDatabase db = this.getReadableDatabase();
        String sql = "SELECT estado FROM " + TABLE_AMIGOS +
                " WHERE (solicitante_id = ? AND receptor_id = ?)" +
                "    OR (solicitante_id = ? AND receptor_id = ?)" +
                " LIMIT 1";
        Cursor c = db.rawQuery(sql, new String[]{
                String.valueOf(userId), String.valueOf(otroId),
                String.valueOf(otroId), String.valueOf(userId)
        });
        String estado = null;
        if (c.moveToFirst()) estado = c.getString(0);
        c.close();
        return estado;
    }

    // Envía una solicitud de amistad de solicitante → receptor.
    // Devuelve false si ya existe alguna relación entre ellos para evitar duplicados.
    public boolean enviarSolicitudAmistad(long solicitanteId, long receptorId) {
        // Primero comprobamos que no exista ya una solicitud o amistad previa
        if (estadoAmistad(solicitanteId, receptorId) != null) return false;

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("solicitante_id", solicitanteId);
        cv.put("receptor_id",    receptorId);
        cv.put("estado",         "PENDIENTE");
        cv.put("timestamp",      System.currentTimeMillis());
        return db.insert(TABLE_AMIGOS, null, cv) != -1;
    }

    // Acepta una solicitud pendiente identificada por los IDs de los dos usuarios.
    // El solicitante original y el receptor pueden estar en cualquier orden.
    public boolean aceptarSolicitud(long miId, long otroId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("estado", "ACEPTADO");
        // Actualizamos la fila donde otroId es el solicitante y yo soy el receptor
        int filas = db.update(TABLE_AMIGOS, cv,
                "solicitante_id = ? AND receptor_id = ? AND estado = 'PENDIENTE'",
                new String[]{String.valueOf(otroId), String.valueOf(miId)});
        return filas > 0;
    }

    // Elimina la relación de amistad o solicitud entre dos usuarios (en ambas direcciones).
    public boolean eliminarAmistad(long userId, long otroId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int filas = db.delete(TABLE_AMIGOS,
                "(solicitante_id = ? AND receptor_id = ?) OR (solicitante_id = ? AND receptor_id = ?)",
                new String[]{String.valueOf(userId), String.valueOf(otroId),
                             String.valueOf(otroId), String.valueOf(userId)});
        return filas > 0;
    }

    // Devuelve la lista de amigos aceptados del usuario indicado.
    // Hace JOIN con usuarios para obtener nombre, categoría, provincia y foto en una query.
    public List<Map<String, Object>> obtenerAmigos(long userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        // La amistad puede estar en cualquier dirección, usamos UNION para cubrir ambas
        String sql =
            "SELECT u.id, u.nombre, u.apellidos, u.categoria_actual, u.provincia, u.foto_perfil " +
            "FROM " + TABLE_AMIGOS + " a " +
            "JOIN " + TABLE_USUARIOS + " u ON u.id = a.receptor_id " +
            "WHERE a.solicitante_id = ? AND a.estado = 'ACEPTADO' " +
            "UNION " +
            "SELECT u.id, u.nombre, u.apellidos, u.categoria_actual, u.provincia, u.foto_perfil " +
            "FROM " + TABLE_AMIGOS + " a " +
            "JOIN " + TABLE_USUARIOS + " u ON u.id = a.solicitante_id " +
            "WHERE a.receptor_id = ? AND a.estado = 'ACEPTADO' " +
            "ORDER BY 2 ASC"; // ordenado por nombre

        Cursor c = db.rawQuery(sql, new String[]{
                String.valueOf(userId), String.valueOf(userId)});

        List<Map<String, Object>> lista = new ArrayList<>();
        while (c.moveToNext()) {
            Map<String, Object> u = new HashMap<>();
            u.put(COL_ID,          c.getLong(0));
            u.put(COL_NOMBRE,      c.getString(1));
            u.put(COL_APELLIDOS,   c.getString(2));
            u.put(COL_CATEGORIA,   c.getString(3));
            u.put(COL_PROVINCIA,   c.getString(4));
            u.put(COL_FOTO_PERFIL, c.getString(5));
            lista.add(u);
        }
        c.close();
        return lista;
    }

    // Devuelve las solicitudes de amistad PENDIENTES recibidas por el usuario.
    // Solo muestra las que aún no han sido aceptadas o rechazadas.
    public List<Map<String, Object>> obtenerSolicitudesPendientes(long receptorId) {
        SQLiteDatabase db = this.getReadableDatabase();
        String sql =
            "SELECT a.id, u.id, u.nombre, u.apellidos, u.categoria_actual, u.provincia, u.foto_perfil " +
            "FROM " + TABLE_AMIGOS + " a " +
            "JOIN " + TABLE_USUARIOS + " u ON u.id = a.solicitante_id " +
            "WHERE a.receptor_id = ? AND a.estado = 'PENDIENTE' " +
            "ORDER BY a.timestamp DESC";

        Cursor c = db.rawQuery(sql, new String[]{String.valueOf(receptorId)});

        List<Map<String, Object>> lista = new ArrayList<>();
        while (c.moveToNext()) {
            Map<String, Object> u = new HashMap<>();
            u.put("solicitud_id",  c.getLong(0));   // ID de la fila en TABLE_AMIGOS
            u.put(COL_ID,          c.getLong(1));
            u.put(COL_NOMBRE,      c.getString(2));
            u.put(COL_APELLIDOS,   c.getString(3));
            u.put(COL_CATEGORIA,   c.getString(4));
            u.put(COL_PROVINCIA,   c.getString(5));
            u.put(COL_FOTO_PERFIL, c.getString(6));
            lista.add(u);
        }
        c.close();
        return lista;
    }

    // -----------------------------------------------------------------------
    //  CRUD - PISTAS DE PÁDEL (caché OSM)
    // -----------------------------------------------------------------------

    // Inserta un lote de pistas en la tabla de caché, ignorando duplicados por osm_id.
    // Se llama desde el hilo de descarga con las pistas de toda España.
    public void insertarPistasBatch(List<Map<String, Object>> pistas) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.beginTransaction();
        try {
            for (Map<String, Object> p : pistas) {
                ContentValues cv = new ContentValues();
                cv.put("osm_id",        (String) p.get("osm_id"));
                cv.put("nombre",        (String) p.get("nombre"));
                cv.put("municipio",     (String) p.get("municipio"));
                cv.put("municipio_norm",(String) p.get("municipio_norm"));
                cv.put("provincia",     (String) p.get("provincia"));
                cv.put("lat",           p.get("lat") instanceof Double ? (Double) p.get("lat") : 0.0);
                cv.put("lon",           p.get("lon") instanceof Double ? (Double) p.get("lon") : 0.0);
                // OR IGNORE: si ya existe la pista por osm_id no falla ni sobreescribe
                db.insertWithOnConflict(TABLE_PISTAS, null, cv, SQLiteDatabase.CONFLICT_IGNORE);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    // Busca pistas en la caché local filtradas por municipio (búsqueda flexible sin tildes).
    // Devuelve lista de nombres ordenados alfabéticamente.
    public List<String> buscarPistasPorMunicipio(String municipio) {
        if (municipio == null || municipio.trim().isEmpty()) return new ArrayList<>();
        String norm = normalizarTexto(municipio);
        SQLiteDatabase db = this.getReadableDatabase();
        // LIKE con wildcard para capturar variantes: "San Sebastián de los Reyes" vs "San Sebastian"
        Cursor c = db.query(TABLE_PISTAS,
                new String[]{"nombre"},
                "municipio_norm LIKE ?",
                new String[]{"%" + norm + "%"},
                null, null, "nombre ASC");
        List<String> nombres = new ArrayList<>();
        while (c.moveToNext()) {
            String n = c.getString(0);
            if (!n.isEmpty() && !nombres.contains(n)) nombres.add(n);
        }
        c.close();
        return nombres;
    }

    // Total de pistas en la caché; 0 significa que la descarga aún no se ha hecho.
    public int contarPistas() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_PISTAS, null);
        int count = 0;
        if (c.moveToFirst()) count = c.getInt(0);
        c.close();
        return count;
    }

    // Normaliza texto para búsquedas insensibles a tildes y mayúsculas.
    public static String normalizarTexto(String texto) {
        if (texto == null) return "";
        // Quita tildes usando la tabla de reemplazos más comunes en español
        String s = texto.toLowerCase(java.util.Locale.forLanguageTag("es"));
        s = s.replace("á","a").replace("é","e").replace("í","i")
             .replace("ó","o").replace("ú","u").replace("ü","u")
             .replace("à","a").replace("è","e").replace("ï","i")
             .replace("ò","o").replace("ñ","n").replace("ç","c");
        return s.trim();
    }
}
