package dam.iker.padeldart;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

// Pantalla de Amigos: buscar por correo, aceptar solicitudes y ver la lista de amigos.
// Extiende BaseDrawerActivity para tener el menú lateral disponible aquí también.
public class FriendsActivity extends BaseDrawerActivity {

    private DatabaseHelper db;
    private SessionManager session;
    private long miId;

    // Vistas del buscador
    private TextInputEditText etBuscarEmail;
    private LinearLayout      llResultadoBusqueda;

    // Contenedores donde se inflan las tarjetas dinámicamente
    private LinearLayout llSolicitudes;
    private LinearLayout llAmigos;
    private LinearLayout llSeccionSolicitudes;

    // Datos del usuario encontrado en la búsqueda (guardados para el botón de acción)
    private Map<String, Object> usuarioEncontrado = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friends);

        db      = DatabaseHelper.getInstance(this);
        session = SessionManager.getInstance(this);
        miId    = session.getUsuarioActualId();

        // Referencias a las vistas del layout
        etBuscarEmail       = findViewById(R.id.etBuscarEmail);
        llResultadoBusqueda = findViewById(R.id.llResultadoBusqueda);
        llSolicitudes       = findViewById(R.id.llSolicitudes);
        llAmigos            = findViewById(R.id.llAmigos);
        llSeccionSolicitudes = findViewById(R.id.llSeccionSolicitudes);

        // Botón atrás de la cabecera: cierra esta Activity
        findViewById(R.id.btnAtrasAmigos).setOnClickListener(v -> finish());

        // Botón de búsqueda: ejecuta la búsqueda por correo al pulsarlo
        findViewById(R.id.btnBuscarAmigo).setOnClickListener(v -> buscarPorEmail());

        // Cargamos solicitudes pendientes y lista de amigos al abrir la pantalla
        cargarSolicitudesPendientes();
        cargarListaAmigos();
    }

    // onResume refresca la lista por si cambió desde otra pantalla
    @Override
    protected void onResume() {
        super.onResume();
        cargarSolicitudesPendientes();
        cargarListaAmigos();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Búsqueda de usuario por correo electrónico
    // ─────────────────────────────────────────────────────────────────────────

    // Busca en la BD un usuario cuyo correo coincida con el introducido.
    // Oculta el teclado, muestra el resultado y configura el botón de acción.
    private void buscarPorEmail() {
        String email = etBuscarEmail.getText() != null
                ? etBuscarEmail.getText().toString().trim() : "";

        if (email.isEmpty()) {
            Toast.makeText(this, getString(R.string.friends_email_vacio), Toast.LENGTH_SHORT).show();
            return;
        }

        // Ocultamos el teclado para que el resultado sea visible sin tener que hacer scroll
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(etBuscarEmail.getWindowToken(), 0);

        // No permitimos que el usuario se añada a sí mismo como amigo
        Map<String, Object> resultado = db.buscarUsuarioPorEmail(email);
        if (resultado == null) {
            Toast.makeText(this, getString(R.string.friends_no_encontrado), Toast.LENGTH_SHORT).show();
            llResultadoBusqueda.setVisibility(View.GONE);
            usuarioEncontrado = null;
            return;
        }

        long idEncontrado = (long) resultado.get(DatabaseHelper.COL_ID);
        if (idEncontrado == miId) {
            Toast.makeText(this, getString(R.string.friends_no_ti_mismo), Toast.LENGTH_SHORT).show();
            llResultadoBusqueda.setVisibility(View.GONE);
            return;
        }

        // Guardamos el resultado para que el botón de acción pueda usarlo
        usuarioEncontrado = resultado;
        mostrarResultadoBusqueda(resultado);
    }

    // Rellena la tarjeta de resultado con los datos del usuario encontrado.
    // Configura el botón de acción según el estado actual de la relación.
    private void mostrarResultadoBusqueda(Map<String, Object> usuario) {
        long idEncontrado = (long) usuario.get(DatabaseHelper.COL_ID);
        String nombre    = strSafe(usuario.get(DatabaseHelper.COL_NOMBRE));
        String apellidos = strSafe(usuario.get(DatabaseHelper.COL_APELLIDOS));
        String categoria = strSafe(usuario.get(DatabaseHelper.COL_CATEGORIA));
        String provincia = strSafe(usuario.get(DatabaseHelper.COL_PROVINCIA));
        String fotoUri   = strSafe(usuario.get(DatabaseHelper.COL_FOTO_PERFIL));

        // Rellenamos los TextViews del resultado
        ((TextView) findViewById(R.id.tvNombreResultado))
                .setText(nombre + " " + apellidos);
        ((TextView) findViewById(R.id.tvCategoriaResultado))
                .setText(categoria.isEmpty() ? "—" : categoria);
        ((TextView) findViewById(R.id.tvProvinciaResultado))
                .setText(provincia.isEmpty() ? "" : "📍 " + provincia);

        // Inicial del nombre como avatar por defecto
        TextView tvInicial = findViewById(R.id.tvInicialResultado);
        ImageView imgFoto  = findViewById(R.id.imgFotoResultado);
        tvInicial.setText(nombre.isEmpty() ? "?" : String.valueOf(nombre.charAt(0)).toUpperCase());

        // Foto del usuario encontrado: carga segura con InputStream para evitar crash
        if (!fotoUri.isEmpty()) {
            cargarFotoSegura(imgFoto, tvInicial, fotoUri);
        } else {
            imgFoto.setVisibility(View.GONE);
            tvInicial.setVisibility(View.VISIBLE);
        }

        // Configuramos el botón de acción según el estado de la relación entre ambos
        MaterialButton btnAccion = findViewById(R.id.btnAccionResultado);
        String estado = db.estadoAmistad(miId, idEncontrado);

        if ("ACEPTADO".equals(estado)) {
            // Ya son amigos: mostramos estado informativo en gris
            btnAccion.setText(getString(R.string.friends_ya_amigos));
            btnAccion.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    getResources().getColor(R.color.fondo_tarjeta_claro, getTheme())));
            btnAccion.setTextColor(getResources().getColor(R.color.texto_gris, getTheme()));
            btnAccion.setOnClickListener(null);
        } else if ("PENDIENTE".equals(estado)) {
            // Solicitud ya enviada: botón informativo sin acción
            btnAccion.setText(getString(R.string.friends_solicitud_pendiente));
            btnAccion.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFFF9800));
            btnAccion.setTextColor(getResources().getColor(R.color.fondo_oscuro, getTheme()));
            btnAccion.setOnClickListener(null);
        } else {
            // Sin relación previa: botón verde para enviar solicitud
            btnAccion.setText(getString(R.string.friends_agregar));
            btnAccion.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    getResources().getColor(R.color.verde_lima, getTheme())));
            btnAccion.setTextColor(getResources().getColor(R.color.fondo_oscuro, getTheme()));
            btnAccion.setOnClickListener(v -> enviarSolicitud(idEncontrado, btnAccion));
        }

        // Mostramos el bloque de resultado (estaba "gone" antes de la búsqueda)
        llResultadoBusqueda.setVisibility(View.VISIBLE);
    }

    // Envía la solicitud de amistad y actualiza el botón para reflejar el nuevo estado.
    private void enviarSolicitud(long idDestino, MaterialButton btnAccion) {
        boolean ok = db.enviarSolicitudAmistad(miId, idDestino);
        if (ok) {
            Toast.makeText(this, getString(R.string.friends_solicitud_enviada), Toast.LENGTH_SHORT).show();
            // Cambiamos el botón a estado "pendiente" sin necesidad de rebuscar
            btnAccion.setText(getString(R.string.friends_solicitud_pendiente));
            btnAccion.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(0xFFFF9800));
            btnAccion.setTextColor(getResources().getColor(R.color.fondo_oscuro, getTheme()));
            btnAccion.setOnClickListener(null);
        } else {
            Toast.makeText(this, getString(R.string.friends_error_solicitud), Toast.LENGTH_SHORT).show();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Solicitudes pendientes recibidas
    // ─────────────────────────────────────────────────────────────────────────

    // Consulta la BD y dibuja las tarjetas de solicitudes pendientes recibidas.
    // Muestra u oculta la sección completa según haya o no solicitudes.
    private void cargarSolicitudesPendientes() {
        llSolicitudes.removeAllViews();
        List<Map<String, Object>> solicitudes = db.obtenerSolicitudesPendientes(miId);

        // Si no hay solicitudes pendientes, ocultamos toda la sección para no mostrar espacio vacío
        if (solicitudes.isEmpty()) {
            llSeccionSolicitudes.setVisibility(View.GONE);
            return;
        }

        llSeccionSolicitudes.setVisibility(View.VISIBLE);
        for (Map<String, Object> sol : solicitudes) {
            agregarTarjetaSolicitud(sol);
        }
    }

    // Crea y añade la tarjeta de una solicitud pendiente con botones Aceptar / Rechazar.
    private void agregarTarjetaSolicitud(Map<String, Object> sol) {
        long   idSolicitante = (long) sol.get(DatabaseHelper.COL_ID);
        String nombre        = strSafe(sol.get(DatabaseHelper.COL_NOMBRE));
        String apellidos     = strSafe(sol.get(DatabaseHelper.COL_APELLIDOS));
        String categoria     = strSafe(sol.get(DatabaseHelper.COL_CATEGORIA));
        String provincia     = strSafe(sol.get(DatabaseHelper.COL_PROVINCIA));
        String fotoUri       = strSafe(sol.get(DatabaseHelper.COL_FOTO_PERFIL));

        // Tarjeta contenedora con fondo oscuro y borde naranja (color de solicitudes)
        MaterialCardView card = new MaterialCardView(this);
        LinearLayout.LayoutParams cardP = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cardP.setMargins(0, 0, 0, dpToPx(10));
        card.setLayoutParams(cardP);
        card.setRadius(dpToPx(12));
        card.setCardElevation(dpToPx(3));
        card.setCardBackgroundColor(getResources().getColor(R.color.fondo_tarjeta, getTheme()));
        card.setStrokeColor(0xFFFF9800);
        card.setStrokeWidth(dpToPx(1));

        LinearLayout inner = new LinearLayout(this);
        inner.setOrientation(LinearLayout.VERTICAL);
        inner.setPadding(dpToPx(14), dpToPx(12), dpToPx(14), dpToPx(12));

        // Fila superior: avatar + nombre + categoría
        LinearLayout fila = crearFilaUsuario(nombre, apellidos, categoria, provincia, fotoUri);
        inner.addView(fila);

        // Fila de botones: Aceptar (verde) y Rechazar (rojo)
        LinearLayout filaBotones = new LinearLayout(this);
        filaBotones.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams botsP = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        botsP.setMargins(0, dpToPx(10), 0, 0);
        filaBotones.setLayoutParams(botsP);

        MaterialButton btnAceptar = new MaterialButton(this);
        LinearLayout.LayoutParams aP = new LinearLayout.LayoutParams(0, dpToPx(40), 1f);
        aP.setMarginEnd(dpToPx(6));
        btnAceptar.setLayoutParams(aP);
        btnAceptar.setText(getString(R.string.friends_aceptar));
        btnAceptar.setTextColor(getResources().getColor(R.color.fondo_oscuro, getTheme()));
        btnAceptar.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                getResources().getColor(R.color.verde_lima, getTheme())));
        btnAceptar.setCornerRadius(dpToPx(10));
        btnAceptar.setTextSize(12f);
        // Al aceptar: actualizamos BD, eliminamos la tarjeta y recargamos la lista de amigos
        btnAceptar.setOnClickListener(v -> {
            db.aceptarSolicitud(miId, idSolicitante);
            Toast.makeText(this, getString(R.string.friends_aceptado), Toast.LENGTH_SHORT).show();
            cargarSolicitudesPendientes();
            cargarListaAmigos();
        });

        MaterialButton btnRechazar = new MaterialButton(this);
        LinearLayout.LayoutParams rP = new LinearLayout.LayoutParams(0, dpToPx(40), 1f);
        btnRechazar.setLayoutParams(rP);
        btnRechazar.setText(getString(R.string.friends_rechazar));
        btnRechazar.setTextColor(getResources().getColor(R.color.white, getTheme()));
        btnRechazar.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFE53935));
        btnRechazar.setCornerRadius(dpToPx(10));
        btnRechazar.setTextSize(12f);
        // Al rechazar: eliminamos la relación de la BD y recargamos solicitudes
        btnRechazar.setOnClickListener(v -> {
            db.eliminarAmistad(miId, idSolicitante);
            Toast.makeText(this, getString(R.string.friends_rechazado), Toast.LENGTH_SHORT).show();
            cargarSolicitudesPendientes();
        });

        filaBotones.addView(btnAceptar);
        filaBotones.addView(btnRechazar);
        inner.addView(filaBotones);
        card.addView(inner);
        llSolicitudes.addView(card);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Lista de amigos aceptados
    // ─────────────────────────────────────────────────────────────────────────

    // Consulta la BD y dibuja las tarjetas de los amigos aceptados.
    // Si no hay amigos, muestra un texto informativo invitando a agregar.
    private void cargarListaAmigos() {
        llAmigos.removeAllViews();
        List<Map<String, Object>> amigos = db.obtenerAmigos(miId);

        if (amigos.isEmpty()) {
            // Mensaje vacío: centrado, gris, con emoji para no parecer un error
            TextView tvVacio = new TextView(this);
            tvVacio.setText(getString(R.string.friends_sin_amigos));
            tvVacio.setTextColor(getResources().getColor(R.color.texto_gris, getTheme()));
            tvVacio.setTextSize(14f);
            tvVacio.setGravity(Gravity.CENTER);
            tvVacio.setPadding(dpToPx(16), dpToPx(24), dpToPx(16), dpToPx(16));
            llAmigos.addView(tvVacio);
            return;
        }

        for (Map<String, Object> amigo : amigos) {
            agregarTarjetaAmigo(amigo);
        }
    }

    // Crea y añade la tarjeta de un amigo aceptado con opción de eliminar la amistad.
    private void agregarTarjetaAmigo(Map<String, Object> amigo) {
        long   idAmigo   = (long) amigo.get(DatabaseHelper.COL_ID);
        String nombre    = strSafe(amigo.get(DatabaseHelper.COL_NOMBRE));
        String apellidos = strSafe(amigo.get(DatabaseHelper.COL_APELLIDOS));
        String categoria = strSafe(amigo.get(DatabaseHelper.COL_CATEGORIA));
        String provincia = strSafe(amigo.get(DatabaseHelper.COL_PROVINCIA));
        String fotoUri   = strSafe(amigo.get(DatabaseHelper.COL_FOTO_PERFIL));

        // Tarjeta con borde verde lima (color de amigos confirmados)
        MaterialCardView card = new MaterialCardView(this);
        LinearLayout.LayoutParams cardP = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cardP.setMargins(0, 0, 0, dpToPx(10));
        card.setLayoutParams(cardP);
        card.setRadius(dpToPx(12));
        card.setCardElevation(dpToPx(3));
        card.setCardBackgroundColor(getResources().getColor(R.color.fondo_tarjeta, getTheme()));
        card.setStrokeColor(getResources().getColor(R.color.verde_lima, getTheme()));
        card.setStrokeWidth(dpToPx(1));

        LinearLayout inner = new LinearLayout(this);
        inner.setOrientation(LinearLayout.HORIZONTAL);
        inner.setGravity(android.view.Gravity.CENTER_VERTICAL);
        inner.setPadding(dpToPx(14), dpToPx(12), dpToPx(14), dpToPx(12));

        // Lado izquierdo: avatar + datos del amigo
        LinearLayout fila = crearFilaUsuario(nombre, apellidos, categoria, provincia, fotoUri);
        LinearLayout.LayoutParams filaP = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        fila.setLayoutParams(filaP);
        inner.addView(fila);

        // Botón de eliminar amistad (icono ✕ en rojo discreto)
        MaterialButton btnEliminar = new MaterialButton(this,
                null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        LinearLayout.LayoutParams eP = new LinearLayout.LayoutParams(dpToPx(36), dpToPx(36));
        btnEliminar.setLayoutParams(eP);
        btnEliminar.setText("✕");
        btnEliminar.setTextColor(0xFFE53935);
        btnEliminar.setTextSize(12f);
        btnEliminar.setStrokeColor(android.content.res.ColorStateList.valueOf(0xFFE53935));
        btnEliminar.setStrokeWidth(dpToPx(1));
        btnEliminar.setCornerRadius(dpToPx(18));
        btnEliminar.setInsetTop(0);
        btnEliminar.setInsetBottom(0);
        btnEliminar.setPadding(0, 0, 0, 0);
        btnEliminar.setOnClickListener(v -> {
            db.eliminarAmistad(miId, idAmigo);
            Toast.makeText(this, getString(R.string.friends_eliminado), Toast.LENGTH_SHORT).show();
            cargarListaAmigos();
        });

        inner.addView(btnEliminar);
        card.addView(inner);
        llAmigos.addView(card);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helper: fila de usuario (avatar + nombre + categoría + provincia)
    // Reutilizada para solicitudes y lista de amigos
    // ─────────────────────────────────────────────────────────────────────────

    // Crea una LinearLayout horizontal con el avatar (foto o inicial) y datos del usuario.
    // Este componente es compartido por tarjetas de solicitudes y de amigos.
    private LinearLayout crearFilaUsuario(String nombre, String apellidos,
                                          String categoria, String provincia, String fotoUri) {
        LinearLayout fila = new LinearLayout(this);
        fila.setOrientation(LinearLayout.HORIZONTAL);
        fila.setGravity(android.view.Gravity.CENTER_VERTICAL);

        // Avatar circular: 48dp con borde verde lima
        MaterialCardView avatar = new MaterialCardView(this);
        LinearLayout.LayoutParams avP = new LinearLayout.LayoutParams(dpToPx(48), dpToPx(48));
        avatar.setLayoutParams(avP);
        avatar.setRadius(dpToPx(24));
        avatar.setCardElevation(0);
        avatar.setStrokeColor(getResources().getColor(R.color.verde_lima, getTheme()));
        avatar.setStrokeWidth(dpToPx(1));
        avatar.setCardBackgroundColor(
                getResources().getColor(R.color.fondo_tarjeta_claro, getTheme()));

        FrameLayout fl = new FrameLayout(this);
        fl.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        // Imagen de perfil (visible solo si se carga bien)
        ImageView imgFoto = new ImageView(this);
        imgFoto.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        imgFoto.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imgFoto.setVisibility(View.GONE);

        // Inicial del nombre como fallback
        TextView tvInicial = new TextView(this);
        tvInicial.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        tvInicial.setGravity(Gravity.CENTER);
        tvInicial.setText(nombre.isEmpty() ? "?" : String.valueOf(nombre.charAt(0)).toUpperCase());
        tvInicial.setTextColor(getResources().getColor(R.color.verde_lima, getTheme()));
        tvInicial.setTextSize(18f);
        tvInicial.setTypeface(null, android.graphics.Typeface.BOLD);

        fl.addView(imgFoto);
        fl.addView(tvInicial);
        avatar.addView(fl);
        fila.addView(avatar);

        // Carga segura de la foto (sin setImageURI que crashea en onMeasure)
        if (!fotoUri.isEmpty()) {
            cargarFotoSegura(imgFoto, tvInicial, fotoUri);
        }

        // Columna de texto: nombre + categoría + provincia
        LinearLayout textos = new LinearLayout(this);
        textos.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams tP = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        tP.setMargins(dpToPx(12), 0, 0, 0);
        textos.setLayoutParams(tP);

        TextView tvNombre = new TextView(this);
        tvNombre.setText(nombre + " " + apellidos);
        tvNombre.setTextColor(getResources().getColor(R.color.white, getTheme()));
        tvNombre.setTextSize(14f);
        tvNombre.setTypeface(null, android.graphics.Typeface.BOLD);
        textos.addView(tvNombre);

        // Categoría: solo si tiene valor, en verde suave
        if (!categoria.isEmpty()) {
            TextView tvCat = new TextView(this);
            tvCat.setText(categoria);
            tvCat.setTextColor(getResources().getColor(R.color.verde_lima_suave, getTheme()));
            tvCat.setTextSize(11f);
            textos.addView(tvCat);
        }

        // Provincia: solo si tiene valor, en gris
        if (!provincia.isEmpty()) {
            TextView tvProv = new TextView(this);
            tvProv.setText("📍 " + provincia);
            tvProv.setTextColor(getResources().getColor(R.color.texto_gris, getTheme()));
            tvProv.setTextSize(11f);
            textos.addView(tvProv);
        }

        fila.addView(textos);
        return fila;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Utilidades
    // ─────────────────────────────────────────────────────────────────────────

    // Convierte Object a String de forma segura; devuelve "" si es null o no es String
    private String strSafe(Object obj) {
        return (obj instanceof String) ? (String) obj : "";
    }

    // Convierte dp a píxeles usando la densidad real de la pantalla del dispositivo
    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}
