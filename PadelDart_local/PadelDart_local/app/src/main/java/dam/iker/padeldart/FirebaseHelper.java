package dam.iker.padeldart;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Singleton que centraliza todas las operaciones con Firebase Auth y Firestore.
// Reemplaza a DatabaseHelper como capa de acceso a datos, ahora en la nube.
// Auth gestiona credenciales; Firestore almacena perfiles, mensajes y anuncios.
public class FirebaseHelper {

    // Interfaces de callback: cada método asíncrono llama a uno de estos al terminar.
    // Los callbacks llegan en el hilo principal, por lo que se puede actualizar la UI directamente.
    public interface StringCb { void onResult(String  value); } // null → error / no encontrado
    public interface BoolCb   { void onResult(boolean ok);    }
    public interface MapCb    { void onResult(Map<String, Object> data); } // null → no existe
    public interface ListCb   { void onResult(List<Map<String, Object>> list); }

    private final FirebaseAuth      auth;
    private final FirebaseFirestore fs;

    // Singleton: una sola instancia de conexión para toda la app
    private static FirebaseHelper instancia;
    public static synchronized FirebaseHelper getInstance() {
        if (instancia == null) instancia = new FirebaseHelper();
        return instancia;
    }

    private FirebaseHelper() {
        auth = FirebaseAuth.getInstance();
        fs   = FirebaseFirestore.getInstance();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  AUTH — login y registro delegados en Firebase Authentication
    // ─────────────────────────────────────────────────────────────────────────

    // Autentica email + contraseña; devuelve el UID del usuario o null si falla
    public void iniciarSesion(String email, String password, StringCb cb) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(r -> cb.onResult(r.getUser().getUid()))
                .addOnFailureListener(e -> cb.onResult(null));
    }

    // Crea cuenta en Firebase Auth y guarda el perfil en Firestore bajo el UID generado.
    // La contraseña NO se almacena en Firestore; la gestiona Firebase internamente.
    public void registrarUsuario(Map<String, Object> datos, StringCb cb) {
        String email    = (String) datos.get("email");
        String password = (String) datos.get("password");

        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(r -> {
                    String uid = r.getUser().getUid();
                    // Copiamos todos los campos excepto la contraseña al documento de Firestore
                    Map<String, Object> perfil = new HashMap<>(datos);
                    perfil.remove("password");

                    // Contadores de partidas y estadísticas empiezan a cero en el registro
                    perfil.put("puntuacion_media",            0.0);
                    perfil.put("total_valoraciones",          1L);
                    perfil.put("numero_clases_dadas",         0L);
                    perfil.put("media_puntos_tiradas",        0.0);
                    perfil.put("numero_partidas_pvp_jugadas", 0L);
                    perfil.put("numero_partidas_pve_jugadas", 0L);
                    perfil.put("numero_partidas_pvp_ganadas", 0L);
                    perfil.put("numero_partidas_pve_ganadas", 0L);

                    // El UID de Firebase Auth es el ID del documento en la colección "usuarios"
                    fs.collection("usuarios").document(uid).set(perfil)
                            .addOnSuccessListener(v -> cb.onResult(uid))
                            .addOnFailureListener(e -> cb.onResult(null));
                })
                .addOnFailureListener(e -> cb.onResult(null));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  USUARIOS — lectura y actualización de perfiles en Firestore
    // ─────────────────────────────────────────────────────────────────────────

    // Recupera todos los campos del documento del usuario; añade "id" = UID para compatibilidad
    public void obtenerUsuario(String uid, MapCb cb) {
        fs.collection("usuarios").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Map<String, Object> data = new HashMap<>(doc.getData());
                        data.put("id", uid); // Clave "id" esperada por las Activities
                        cb.onResult(data);
                    } else {
                        cb.onResult(null);
                    }
                })
                .addOnFailureListener(e -> cb.onResult(null));
    }

    // Devuelve todos los usuarios menos el indicado, ordenados por nombre en local
    public void obtenerTodosUsuariosMenos(String miUid, ListCb cb) {
        fs.collection("usuarios").get()
                .addOnSuccessListener(snap -> {
                    List<Map<String, Object>> lista = new ArrayList<>();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        if (!doc.getId().equals(miUid)) {
                            Map<String, Object> u = new HashMap<>(doc.getData());
                            u.put("id", doc.getId()); // UID como "id" para las tarjetas del chat
                            lista.add(u);
                        }
                    }
                    // Ordenamos por nombre en memoria para no necesitar índice compuesto en Firestore
                    lista.sort((a, b) -> {
                        String na = a.get("nombre") instanceof String ? (String) a.get("nombre") : "";
                        String nb = b.get("nombre") instanceof String ? (String) b.get("nombre") : "";
                        return na.compareToIgnoreCase(nb);
                    });
                    cb.onResult(lista);
                })
                .addOnFailureListener(e -> cb.onResult(new ArrayList<>()));
    }

    // Actualiza los campos editables del perfil; no toca email, DNI ni método de pago
    public void actualizarPerfil(String uid, String nombre, String apellidos, String posicion,
                                  String provincia, String pistaFavorita, int edad,
                                  String fotoPerfil, BoolCb cb) {
        Map<String, Object> upd = new HashMap<>();
        upd.put("nombre",         nombre);
        upd.put("apellidos",      apellidos);
        upd.put("posicion",       posicion);
        upd.put("provincia",      provincia);
        upd.put("pista_favorita", pistaFavorita);
        upd.put("edad",           (long) edad);
        // Solo actualizamos foto_perfil si el usuario seleccionó una nueva
        if (fotoPerfil != null && !fotoPerfil.isEmpty()) upd.put("foto_perfil", fotoPerfil);

        fs.collection("usuarios").document(uid).update(upd)
                .addOnSuccessListener(v -> cb.onResult(true))
                .addOnFailureListener(e -> cb.onResult(false));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  MENSAJES — chat privado con actualizaciones en tiempo real
    // ─────────────────────────────────────────────────────────────────────────

    // Genera un ID de conversación consistente ordenando los dos UIDs alfabéticamente.
    // Así la misma conversación tiene el mismo ID independientemente de quién consulta.
    private String idConversacion(String uid1, String uid2) {
        return uid1.compareTo(uid2) < 0 ? uid1 + "_" + uid2 : uid2 + "_" + uid1;
    }

    // Guarda un mensaje en Firestore con el ID de conversación para filtrar después
    public void enviarMensaje(String emisorId, String receptorId, String contenido, BoolCb cb) {
        Map<String, Object> msg = new HashMap<>();
        msg.put("emisor_id",       emisorId);
        msg.put("receptor_id",     receptorId);
        msg.put("contenido",       contenido);
        msg.put("timestamp",       System.currentTimeMillis());
        msg.put("conversacion_id", idConversacion(emisorId, receptorId));

        fs.collection("mensajes").add(msg)
                .addOnSuccessListener(v -> cb.onResult(true))
                .addOnFailureListener(e -> cb.onResult(false));
    }

    // Registra un listener en tiempo real para la conversación; los mensajes llegan al instante.
    // Devuelve ListenerRegistration para que el Activity lo cancele en onDestroy y evitar fugas.
    public ListenerRegistration escucharConversacion(String uid1, String uid2, ListCb cb) {
        String convId = idConversacion(uid1, uid2);
        return fs.collection("mensajes")
                .whereEqualTo("conversacion_id", convId)
                .addSnapshotListener((snap, err) -> {
                    if (err != null || snap == null) { cb.onResult(new ArrayList<>()); return; }
                    List<Map<String, Object>> lista = new ArrayList<>();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        lista.add(new HashMap<>(doc.getData()));
                    }
                    // Ordenamos por timestamp en local para no necesitar un índice compuesto
                    lista.sort((a, b) -> {
                        long ta = a.get("timestamp") instanceof Number ? ((Number) a.get("timestamp")).longValue() : 0L;
                        long tb = b.get("timestamp") instanceof Number ? ((Number) b.get("timestamp")).longValue() : 0L;
                        return Long.compare(ta, tb);
                    });
                    cb.onResult(lista);
                });
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  ANUNCIOS DE ZONA — tablón provincial compartido
    // ─────────────────────────────────────────────────────────────────────────

    // Publica un anuncio incrustando nombre y categoría del autor para evitar JOINs al leer.
    // Primero carga el perfil del autor y después escribe el documento del anuncio.
    public void publicarAnuncio(String autorId, String tipo, String descripcion,
                                 String provincia, BoolCb cb) {
        fs.collection("usuarios").document(autorId).get()
                .addOnSuccessListener(doc -> {
                    Map<String, Object> anuncio = new HashMap<>();
                    anuncio.put("autor_id",    autorId);
                    anuncio.put("tipo",        tipo);
                    anuncio.put("descripcion", descripcion);
                    anuncio.put("provincia",   provincia);
                    anuncio.put("timestamp",   System.currentTimeMillis());
                    // Desnormalizamos el autor: guardamos sus datos aquí para no necesitar un JOIN
                    if (doc.exists()) {
                        anuncio.put("nombre",          doc.getString("nombre"));
                        anuncio.put("apellidos",        doc.getString("apellidos"));
                        anuncio.put("categoria_actual", doc.getString("categoria_actual"));
                    }
                    fs.collection("anuncios").add(anuncio)
                            .addOnSuccessListener(v -> cb.onResult(true))
                            .addOnFailureListener(e -> cb.onResult(false));
                })
                .addOnFailureListener(e -> cb.onResult(false));
    }

    // Devuelve los anuncios de una provincia; ordena por timestamp en local (más reciente primero)
    public void obtenerAnunciosProvincia(String provincia, ListCb cb) {
        fs.collection("anuncios")
                .whereEqualTo("provincia", provincia)
                .get()
                .addOnSuccessListener(snap -> {
                    List<Map<String, Object>> lista = new ArrayList<>();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        Map<String, Object> a = new HashMap<>(doc.getData());
                        a.put("id", doc.getId());
                        lista.add(a);
                    }
                    // Orden descendente por timestamp en memoria para evitar índice compuesto
                    lista.sort((a, b) -> {
                        long ta = a.get("timestamp") instanceof Number ? ((Number) a.get("timestamp")).longValue() : 0L;
                        long tb = b.get("timestamp") instanceof Number ? ((Number) b.get("timestamp")).longValue() : 0L;
                        return Long.compare(tb, ta); // Descendente
                    });
                    cb.onResult(lista);
                })
                .addOnFailureListener(e -> cb.onResult(new ArrayList<>()));
    }
}
