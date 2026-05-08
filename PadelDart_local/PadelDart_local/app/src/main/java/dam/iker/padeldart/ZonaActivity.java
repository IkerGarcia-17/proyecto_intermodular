package dam.iker.padeldart;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

// Tablón de anuncios de la zona provincial. Extiende BaseDrawerActivity para el menú lateral.
// Los formularios de publicación son específicos por tipo: PALA, CLASE, PARTIDA.
public class ZonaActivity extends BaseDrawerActivity {

    private DatabaseHelper db;
    private SessionManager session;

    private long   miId;

    // provinciaActual puede cambiar cuando el usuario usa el selector;
    // al inicio se inicializa con la provincia guardada en el perfil del usuario.
    private String provinciaActual;

    // Provincia original del usuario (la que marcó en el registro/edición de perfil).
    // Se guarda por separado para poder siempre volver a ella como opción por defecto.
    private String miProvinciaOriginal;

    private LinearLayout llAnuncios;
    private TextView     tvTituloZona;

    // Filtro activo: null = todos, "PARTIDA", "PALA", "CLASE_OFRECER", "CLASE_SOLICITAR"
    private String filtroActivo = null;
    private List<Map<String, Object>> todosLosAnuncios;

    // Lista completa de provincias españolas disponibles en el selector.
    // Ordenadas alfabéticamente para facilitar la búsqueda manual.
    private static final String[] PROVINCIAS = {
        "Álava", "Albacete", "Alicante", "Almería", "Asturias", "Ávila",
        "Badajoz", "Baleares", "Barcelona", "Burgos", "Cáceres", "Cádiz",
        "Cantabria", "Castellón", "Ciudad Real", "Córdoba", "Cuenca",
        "Gerona", "Granada", "Guadalajara", "Guipúzcoa", "Huelva", "Huesca",
        "Jaén", "La Coruña", "La Rioja", "Las Palmas", "León", "Lérida",
        "Lugo", "Madrid", "Málaga", "Murcia", "Navarra", "Orense", "Palencia",
        "Pontevedra", "Salamanca", "Santa Cruz de Tenerife", "Segovia",
        "Sevilla", "Soria", "Tarragona", "Teruel", "Toledo", "Valencia",
        "Valladolid", "Vizcaya", "Zamora", "Zaragoza"
    };

    // Estado del selector de foto para el formulario de PALA
    private ActivityResultLauncher<Intent> fotoLauncher;
    private String    fotoPalaUri    = null;
    private ImageView imgPreviewPala = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_zona);

        db      = DatabaseHelper.getInstance(this);
        session = SessionManager.getInstance(this);
        miId    = session.getUsuarioActualId();

        // Obtenemos la provincia del usuario para filtrar y pre-rellenar campos.
        // Esta provincia es la que el usuario indicó en su perfil y será la primera
        // que aparezca al entrar; podrá cambiarla sin modificar su perfil.
        Map<String, Object> yo = db.obtenerUsuario(miId);
        miProvinciaOriginal = yo != null ? (String) yo.get(DatabaseHelper.COL_PROVINCIA) : null;
        provinciaActual     = miProvinciaOriginal; // comenzamos en la provincia del usuario

        // Buscamos las vistas principales del layout
        llAnuncios   = findViewById(R.id.llAnuncios);
        tvTituloZona = findViewById(R.id.tvTituloZona);

        // Botón atrás: cierra esta Activity y vuelve a PadelActivity
        findViewById(R.id.btnAtras).setOnClickListener(v -> finish());

        // Actualizamos el título con la provincia actualmente seleccionada
        actualizarTituloProvincia();

        // Botón "Cambiar provincia": muestra un diálogo con todas las provincias.
        // La provincia del usuario aparece marcada como seleccionada por defecto.
        findViewById(R.id.btnCambiarProvincia).setOnClickListener(v -> mostrarSelectorProvincia());

        // El lanzador de selección de imagen debe registrarse en onCreate,
        // antes de cualquier interacción del usuario (requisito del API de AndroidX).
        fotoLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            fotoPalaUri = uri.toString();
                            try {
                                // Permiso persistente para mantener acceso a la URI tras reiniciar
                                getContentResolver().takePersistableUriPermission(
                                        uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            } catch (Exception ignored) {}

                            // Cargamos la preview con InputStream para evitar SecurityException
                            // en onMeasure que ocurre si se usa setImageURI directamente.
                            if (imgPreviewPala != null) {
                                try {
                                    java.io.InputStream is = getContentResolver().openInputStream(uri);
                                    android.graphics.Bitmap bmp = android.graphics.BitmapFactory.decodeStream(is);
                                    if (bmp != null) {
                                        imgPreviewPala.setImageBitmap(bmp);
                                        imgPreviewPala.setVisibility(View.VISIBLE);
                                    }
                                } catch (Exception ignored2) {}
                            }
                        }
                    }
                });

        // FAB: botón flotante inferior derecha para crear nuevo anuncio
        ExtendedFloatingActionButton fab = findViewById(R.id.fabNuevoAnuncio);
        fab.setOnClickListener(v -> mostrarDialogoNuevoAnuncio());

        configurarFiltros();
        cargarAnuncios();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Provincia: título y selector
    // ─────────────────────────────────────────────────────────────────────────

    // Actualiza el título y el botón selector con la provincia actualmente activa.
    // Se llama al inicio y cada vez que el usuario elige una provincia diferente.
    private void actualizarTituloProvincia() {
        String nombre = provinciaActual != null ? provinciaActual : "Mi Zona";

        // Título grande de la cabecera: "📌  Madrid"
        if (tvTituloZona != null) {
            tvTituloZona.setText("📌  " + nombre);
        }

        // Botón selector: muestra la provincia activa para que el usuario sepa
        // qué provincia está viendo en el tablón sin leer el título.
        MaterialButton btnProv = findViewById(R.id.btnCambiarProvincia);
        if (btnProv != null) {
            btnProv.setText("📍 " + nombre);
        }
    }

    // Muestra un AlertDialog con la lista completa de provincias españolas.
    // La provincia actualmente seleccionada aparece marcada al abrir el diálogo.
    // Al confirmar se recarga el tablón con los anuncios de la nueva provincia.
    private void mostrarSelectorProvincia() {
        // Buscamos el índice de la provincia activa para pre-seleccionarla en el diálogo
        int idxActual = provinciaActual != null
                ? Arrays.asList(PROVINCIAS).indexOf(provinciaActual) : 0;
        if (idxActual < 0) idxActual = 0; // si no se encuentra, seleccionamos la primera

        // setSingleChoiceItems muestra radio buttons con una provincia seleccionable
        final int[] seleccionado = {idxActual};
        new AlertDialog.Builder(this)
                .setTitle("📌 Selecciona una provincia")
                .setSingleChoiceItems(PROVINCIAS, idxActual, (dialog, which) -> {
                    // Guardamos temporalmente la elección sin aplicarla aún
                    seleccionado[0] = which;
                })
                .setPositiveButton("Ver anuncios", (dialog, which) -> {
                    // Aplicamos la provincia elegida: actualizamos título y recargamos tablón
                    provinciaActual = PROVINCIAS[seleccionado[0]];
                    actualizarTituloProvincia();
                    cargarAnuncios();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Filtros y carga de anuncios
    // ─────────────────────────────────────────────────────────────────────────

    private void configurarFiltros() {
        MaterialButton btnTodos     = findViewById(R.id.btnFiltroTodos);
        MaterialButton btnPartida   = findViewById(R.id.btnFiltroPartida);
        MaterialButton btnPala      = findViewById(R.id.btnFiltroPala);
        MaterialButton btnOfrecer   = findViewById(R.id.btnFiltroOfrecer);
        MaterialButton btnSolicitar = findViewById(R.id.btnFiltroSolicitar);

        btnTodos.setOnClickListener(v     -> aplicarFiltro(null));
        btnPartida.setOnClickListener(v   -> aplicarFiltro("PARTIDA"));
        btnPala.setOnClickListener(v      -> aplicarFiltro("PALA"));
        btnOfrecer.setOnClickListener(v   -> aplicarFiltro("CLASE_OFRECER"));
        btnSolicitar.setOnClickListener(v -> aplicarFiltro("CLASE_SOLICITAR"));
    }

    private void aplicarFiltro(String tipo) {
        filtroActivo = tipo;
        mostrarAnunciosFiltrados();
    }

    // Consulta BD y almacena todos los anuncios de la provincia actualmente seleccionada.
    // Si no hay provincia (perfil incompleto y nunca se seleccionó), muestra aviso.
    private void cargarAnuncios() {
        if (provinciaActual == null || provinciaActual.isEmpty()) {
            mostrarMensaje("No tienes provincia asignada. Actualiza tu perfil o selecciona una provincia.");
            return;
        }
        todosLosAnuncios = db.obtenerAnunciosProvincia(provinciaActual);
        mostrarAnunciosFiltrados();
    }

    // Dibuja solo los anuncios que pasan el filtro activo
    private void mostrarAnunciosFiltrados() {
        llAnuncios.removeAllViews();
        if (todosLosAnuncios == null || todosLosAnuncios.isEmpty()) {
            mostrarMensaje("Aún no hay anuncios en tu zona. ¡Sé el primero en publicar!");
            return;
        }
        boolean hayAlguno = false;
        for (Map<String, Object> anuncio : todosLosAnuncios) {
            String tipo = (String) anuncio.get("tipo");
            if (filtroActivo != null && !filtroActivo.equals(tipo)) continue;
            agregarTarjetaAnuncio(anuncio);
            hayAlguno = true;
        }
        if (!hayAlguno) mostrarMensaje("No hay anuncios de este tipo en tu zona todavía.");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Tarjeta de anuncio (con soporte de miniatura para PALA)
    // ─────────────────────────────────────────────────────────────────────────

    private void agregarTarjetaAnuncio(Map<String, Object> anuncio) {
        String tipo        = (String) anuncio.get("tipo");
        String descripcion = (String) anuncio.get("descripcion");
        String nombre      = (String) anuncio.get("nombre");
        String apellidos   = (String) anuncio.get("apellidos");
        String categoria   = (String) anuncio.get("categoria_actual");
        long   timestamp   = (Long)   anuncio.get("timestamp");

        // Separamos la URI de la foto (solo PALA) del texto mostrable
        String fotoUri     = null;
        String descVisible = descripcion;
        if (descripcion != null && descripcion.contains("\n@@FOTO:")) {
            int idx    = descripcion.indexOf("\n@@FOTO:");
            fotoUri     = descripcion.substring(idx + "\n@@FOTO:".length());
            descVisible = descripcion.substring(0, idx);
        }

        // Tarjeta contenedora
        MaterialCardView card = new MaterialCardView(this);
        LinearLayout.LayoutParams cardP = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cardP.setMargins(0, 0, 0, dpToPx(12));
        card.setLayoutParams(cardP);
        card.setRadius(dpToPx(12));
        card.setCardElevation(dpToPx(4));
        card.setCardBackgroundColor(getResources().getColor(R.color.fondo_tarjeta, getTheme()));

        LinearLayout inner = new LinearLayout(this);
        inner.setOrientation(LinearLayout.VERTICAL);
        inner.setPadding(dpToPx(16), dpToPx(14), dpToPx(16), dpToPx(14));

        // Fila: autor + badge de tipo
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView tvAutor = new TextView(this);
        tvAutor.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        tvAutor.setText((nombre != null ? nombre : "") + " " + (apellidos != null ? apellidos : "")
                + (categoria != null ? "  ·  " + categoria : ""));
        tvAutor.setTextColor(getResources().getColor(R.color.verde_lima_suave, getTheme()));
        tvAutor.setTextSize(13f);
        tvAutor.setTypeface(null, android.graphics.Typeface.BOLD);

        TextView tvTipo = new TextView(this);
        tvTipo.setText(textoTipo(tipo));
        tvTipo.setTextColor(getResources().getColor(R.color.fondo_oscuro, getTheme()));
        tvTipo.setTextSize(10f);
        tvTipo.setTypeface(null, android.graphics.Typeface.BOLD);
        tvTipo.setPadding(dpToPx(10), dpToPx(4), dpToPx(10), dpToPx(4));
        GradientDrawable badge = new GradientDrawable();
        badge.setCornerRadius(dpToPx(12));
        badge.setColor(colorTipo(tipo));
        tvTipo.setBackground(badge);

        header.addView(tvAutor);
        header.addView(tvTipo);
        inner.addView(header);

        // Miniatura de la pala: la cargamos con InputStream para evitar SecurityException
        // en onMeasure. Si la URI está revocada simplemente no mostramos la imagen.
        if (fotoUri != null && !fotoUri.isEmpty()) {
            try {
                // Intentamos decodificar el bitmap antes de crear el ImageView,
                // así el error ocurre aquí donde el catch lo puede manejar.
                java.io.InputStream is = getContentResolver().openInputStream(Uri.parse(fotoUri));
                android.graphics.Bitmap bmp = android.graphics.BitmapFactory.decodeStream(is);
                if (bmp != null) {
                    ImageView imgPala = new ImageView(this);
                    LinearLayout.LayoutParams imgP = new LinearLayout.LayoutParams(dpToPx(100), dpToPx(100));
                    imgP.setMargins(0, dpToPx(10), 0, 0);
                    imgPala.setLayoutParams(imgP);
                    imgPala.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    GradientDrawable imgBg = new GradientDrawable();
                    imgBg.setCornerRadius(dpToPx(8));
                    imgBg.setColor(0xFF1A1A1A);
                    imgPala.setBackground(imgBg);
                    imgPala.setImageBitmap(bmp); // usamos setImageBitmap, nunca setImageURI
                    inner.addView(imgPala);
                }
            } catch (Exception ignored) {} // URI inválida, revocada o sin permiso: omitimos foto
        }

        // Texto del anuncio (ya sin la línea @@FOTO)
        TextView tvDesc = new TextView(this);
        LinearLayout.LayoutParams descP = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        descP.setMargins(0, dpToPx(10), 0, 0);
        tvDesc.setLayoutParams(descP);
        tvDesc.setText(descVisible);
        tvDesc.setTextColor(getResources().getColor(R.color.white, getTheme()));
        tvDesc.setTextSize(14f);
        tvDesc.setLineSpacing(dpToPx(2), 1f);
        inner.addView(tvDesc);

        // Timestamp inferior derecho
        TextView tvFecha = new TextView(this);
        LinearLayout.LayoutParams fechaP = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        fechaP.setMargins(0, dpToPx(8), 0, 0);
        tvFecha.setLayoutParams(fechaP);
        tvFecha.setText(new SimpleDateFormat("dd MMM yyyy · HH:mm", Locale.getDefault())
                .format(new Date(timestamp)));
        tvFecha.setTextColor(getResources().getColor(R.color.texto_gris_suave, getTheme()));
        tvFecha.setTextSize(11f);
        tvFecha.setGravity(Gravity.END);
        inner.addView(tvFecha);

        card.addView(inner);
        llAnuncios.addView(card);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PASO 1: selección de tipo de anuncio
    // ─────────────────────────────────────────────────────────────────────────

    private void mostrarDialogoNuevoAnuncio() {
        LinearLayout layout = crearDialogLayout();
        layout.addView(crearTituloDialog("✦  " + getString(R.string.zona_nuevo_titulo)));
        layout.addView(crearLabel(getString(R.string.zona_tipo_anuncio)));

        // RadioGroup con un color distinto por tipo para mayor claridad
        RadioGroup rg = new RadioGroup(this);
        rg.setOrientation(RadioGroup.VERTICAL);
        rg.setPadding(0, dpToPx(6), 0, 0);

        String[] tipos     = {"PARTIDA", "PALA", "CLASE_OFRECER", "CLASE_SOLICITAR"};
        String[] etiquetas = {
            getString(R.string.zona_rb_partida),
            getString(R.string.zona_rb_pala),
            getString(R.string.zona_rb_ofrecer),
            getString(R.string.zona_rb_solicitar)
        };
        // Colores de los radio buttons: PARTIDA en rojo para distinguirse visualmente
        int[] coloresTipo = {0xFFFF5252, 0xFF1E88E5, 0xFFFB8C00, 0xFF8E24AA};

        for (int i = 0; i < tipos.length; i++) {
            RadioButton rb = new RadioButton(this);
            rb.setText(etiquetas[i]);
            rb.setTag(tipos[i]);
            rb.setTextColor(coloresTipo[i]);
            rb.setButtonTintList(ColorStateList.valueOf(coloresTipo[i]));
            rb.setTextSize(15f);
            rb.setPadding(dpToPx(8), dpToPx(10), 0, dpToPx(10));
            rg.addView(rb);
        }
        layout.addView(rg);

        final AlertDialog[] dialogRef = {null};
        LinearLayout llBotones = crearFilaBotones();

        MaterialButton btnCancelar = crearBotonSecundario(getString(R.string.btn_cancelar));
        LinearLayout.LayoutParams cP = new LinearLayout.LayoutParams(0, dpToPx(48), 1f);
        cP.setMarginEnd(dpToPx(8));
        btnCancelar.setLayoutParams(cP);
        btnCancelar.setOnClickListener(v -> { if (dialogRef[0] != null) dialogRef[0].dismiss(); });

        MaterialButton btnSiguiente = crearBotonPrimario(getString(R.string.zona_siguiente));
        LinearLayout.LayoutParams sP = new LinearLayout.LayoutParams(0, dpToPx(48), 1f);
        sP.setMarginStart(dpToPx(8));
        btnSiguiente.setLayoutParams(sP);
        btnSiguiente.setOnClickListener(v -> {
            int checked = rg.getCheckedRadioButtonId();
            if (checked == -1) {
                Toast.makeText(this, getString(R.string.zona_selecciona_tipo), Toast.LENGTH_SHORT).show();
                return;
            }
            String tipoElegido = (String) rg.findViewById(checked).getTag();
            if (dialogRef[0] != null) dialogRef[0].dismiss();
            // Cada tipo abre su formulario especializado
            switch (tipoElegido) {
                case "PALA":            mostrarFormularioPala();        break;
                case "CLASE_OFRECER":   mostrarFormularioClase(false);  break;
                case "CLASE_SOLICITAR": mostrarFormularioClase(true);   break;
                case "PARTIDA":         mostrarFormularioPartida();      break;
            }
        });

        llBotones.addView(btnCancelar);
        llBotones.addView(btnSiguiente);
        layout.addView(llBotones);
        dialogRef[0] = mostrarDialog(layout);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Formulario PALA: nombre, modelo, estado, imagen, descripción libre
    // ─────────────────────────────────────────────────────────────────────────

    private void mostrarFormularioPala() {
        fotoPalaUri    = null;
        imgPreviewPala = null;

        LinearLayout outer = crearDialogLayout();
        outer.addView(crearTituloDialog("🏓  " + getString(R.string.zona_titulo_pala)));

        // ScrollView para que los campos no se corten en pantallas pequeñas
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        ScrollView sv = new ScrollView(this);
        sv.addView(form);
        sv.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(420)));

        // Nombre de la pala
        form.addView(crearLabel(getString(R.string.zona_pala_nombre)));
        EditText etNombre = crearEditText(getString(R.string.zona_pala_nombre), false);
        form.addView(etNombre);

        // Modelo
        addSpacer(form, 12);
        form.addView(crearLabel(getString(R.string.zona_pala_modelo)));
        EditText etModelo = crearEditText(getString(R.string.zona_pala_modelo), false);
        form.addView(etModelo);

        // Estado del producto (RadioGroup vertical con colores)
        addSpacer(form, 12);
        form.addView(crearLabel(getString(R.string.zona_pala_estado)));
        RadioGroup rgEstado = new RadioGroup(this);
        rgEstado.setOrientation(RadioGroup.VERTICAL);
        final String[] estados = {
            getString(R.string.zona_pala_nuevo),
            getString(R.string.zona_pala_muy_bueno),
            getString(R.string.zona_pala_bueno),
            getString(R.string.zona_pala_regular)
        };
        for (int i = 0; i < estados.length; i++) {
            RadioButton rb = new RadioButton(this);
            rb.setText(estados[i]);
            rb.setTextColor(getResources().getColor(R.color.white, getTheme()));
            rb.setButtonTintList(ColorStateList.valueOf(0xFF1E88E5));
            rb.setTextSize(13f);
            rb.setPadding(dpToPx(4), dpToPx(6), dpToPx(16), dpToPx(6));
            rgEstado.addView(rb);
            if (i == 2) rb.setChecked(true); // "Bueno" seleccionado por defecto
        }
        form.addView(rgEstado);

        // Imagen: preview + botón selector
        addSpacer(form, 12);
        form.addView(crearLabel(getString(R.string.zona_pala_imagen)));

        imgPreviewPala = new ImageView(this);
        LinearLayout.LayoutParams imgP = new LinearLayout.LayoutParams(dpToPx(110), dpToPx(110));
        imgP.setMargins(0, dpToPx(6), 0, dpToPx(6));
        imgPreviewPala.setLayoutParams(imgP);
        imgPreviewPala.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imgPreviewPala.setVisibility(View.GONE);
        GradientDrawable imgBg = new GradientDrawable();
        imgBg.setStroke(dpToPx(1), 0xFF333333);
        imgBg.setCornerRadius(dpToPx(8));
        imgBg.setColor(0xFF1A1A1A);
        imgPreviewPala.setBackground(imgBg);
        form.addView(imgPreviewPala);

        MaterialButton btnFoto = crearBotonSecundario(getString(R.string.zona_pala_imagen));
        btnFoto.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(44)));
        btnFoto.setOnClickListener(v -> {
            Intent pick = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            pick.setType("image/*");
            pick.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                    | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            fotoLauncher.launch(pick);
        });
        form.addView(btnFoto);

        // Descripción opcional
        addSpacer(form, 12);
        form.addView(crearLabel(getString(R.string.zona_desc_opcional)));
        EditText etDesc = crearEditTextMultiline(getString(R.string.zona_desc_hint));
        form.addView(etDesc);

        outer.addView(sv);

        final AlertDialog[] dialogRef = {null};
        LinearLayout llBotones = crearFilaBotones();

        MaterialButton btnCancelar = crearBotonSecundario(getString(R.string.btn_cancelar));
        LinearLayout.LayoutParams cP = new LinearLayout.LayoutParams(0, dpToPx(48), 1f);
        cP.setMarginEnd(dpToPx(8));
        btnCancelar.setLayoutParams(cP);
        btnCancelar.setOnClickListener(v -> { if (dialogRef[0] != null) dialogRef[0].dismiss(); });

        MaterialButton btnPublicar = crearBotonPrimario(getString(R.string.zona_publicar));
        LinearLayout.LayoutParams pP = new LinearLayout.LayoutParams(0, dpToPx(48), 1f);
        pP.setMarginStart(dpToPx(8));
        btnPublicar.setLayoutParams(pP);
        btnPublicar.setOnClickListener(v -> {
            String nombrePala = etNombre.getText().toString().trim();
            if (nombrePala.isEmpty()) {
                Toast.makeText(this, getString(R.string.zona_pala_nombre) + " requerido",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            String modeloPala = etModelo.getText().toString().trim();
            int idxE = rgEstado.indexOfChild(rgEstado.findViewById(rgEstado.getCheckedRadioButtonId()));
            String estadoText = idxE >= 0 ? estados[idxE] : estados[2];
            String desc = etDesc.getText().toString().trim();

            // Descripción estructurada con emojis; la foto se embebe al final
            StringBuilder sb = new StringBuilder();
            sb.append("🏓 ").append(nombrePala);
            if (!modeloPala.isEmpty()) sb.append(" · ").append(modeloPala);
            sb.append("\n📦 Estado: ").append(estadoText);
            if (!desc.isEmpty()) sb.append("\n💬 ").append(desc);
            if (fotoPalaUri != null) sb.append("\n@@FOTO:").append(fotoPalaUri);

            long id = db.publicarAnuncio(miId, "PALA", sb.toString(), provinciaActual);
            if (id != -1) {
                Toast.makeText(this, getString(R.string.zona_publicado), Toast.LENGTH_SHORT).show();
                if (dialogRef[0] != null) dialogRef[0].dismiss();
                cargarAnuncios();
            } else {
                Toast.makeText(this, getString(R.string.zona_error_publicar), Toast.LENGTH_SHORT).show();
            }
        });

        llBotones.addView(btnCancelar);
        llBotones.addView(btnPublicar);
        outer.addView(llBotones);
        dialogRef[0] = mostrarDialog(outer);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Formulario CLASE (reutilizado para ofrecer/solicitar)
    // Campos: localidad, provincia, precio/hora, incluye pista, pista/club, descripción
    // ─────────────────────────────────────────────────────────────────────────

    private void mostrarFormularioClase(boolean esSolicitar) {
        LinearLayout outer = crearDialogLayout();
        String titulo = esSolicitar
                ? "🔍  " + getString(R.string.zona_titulo_solicitar)
                : "📚  " + getString(R.string.zona_titulo_ofrecer);
        outer.addView(crearTituloDialog(titulo));

        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        ScrollView sv = new ScrollView(this);
        sv.addView(form);
        sv.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(400)));

        // Localidad y provincia (provincia pre-rellenada con la del usuario)
        form.addView(crearLabel(getString(R.string.zona_localidad)));
        EditText etLocalidad = crearEditText(getString(R.string.zona_localidad), false);
        form.addView(etLocalidad);

        addSpacer(form, 12);
        form.addView(crearLabel(getString(R.string.edit_provincia)));
        EditText etProvincia = crearEditText(getString(R.string.edit_provincia), false);
        if (provinciaActual != null) etProvincia.setText(provinciaActual);
        form.addView(etProvincia);

        // Precio por hora
        addSpacer(form, 12);
        form.addView(crearLabel(getString(R.string.zona_clase_precio)));
        EditText etPrecio = crearEditText("Ej: 30", true);
        form.addView(etPrecio);

        // Checkbox: incluye reserva de pista
        addSpacer(form, 12);
        CheckBox cbPista = new CheckBox(this);
        cbPista.setText(getString(R.string.zona_clase_incluye_pista));
        cbPista.setTextColor(getResources().getColor(R.color.white, getTheme()));
        cbPista.setButtonTintList(ColorStateList.valueOf(0xFFFB8C00));
        cbPista.setTextSize(14f);
        form.addView(cbPista);

        // Pista o club donde se impartirá
        addSpacer(form, 12);
        form.addView(crearLabel(getString(R.string.zona_pista_club)));
        EditText etPistaClub = crearEditText(getString(R.string.zona_pista_club), false);
        form.addView(etPistaClub);

        addSpacer(form, 12);
        form.addView(crearLabel(getString(R.string.zona_desc_opcional)));
        EditText etDesc = crearEditTextMultiline(getString(R.string.zona_desc_hint));
        form.addView(etDesc);

        outer.addView(sv);

        final AlertDialog[] dialogRef = {null};
        LinearLayout llBotones = crearFilaBotones();

        MaterialButton btnCancelar = crearBotonSecundario(getString(R.string.btn_cancelar));
        LinearLayout.LayoutParams cP = new LinearLayout.LayoutParams(0, dpToPx(48), 1f);
        cP.setMarginEnd(dpToPx(8));
        btnCancelar.setLayoutParams(cP);
        btnCancelar.setOnClickListener(v -> { if (dialogRef[0] != null) dialogRef[0].dismiss(); });

        MaterialButton btnPublicar = crearBotonPrimario(getString(R.string.zona_publicar));
        LinearLayout.LayoutParams pP = new LinearLayout.LayoutParams(0, dpToPx(48), 1f);
        pP.setMarginStart(dpToPx(8));
        btnPublicar.setLayoutParams(pP);
        btnPublicar.setOnClickListener(v -> {
            String localidad = etLocalidad.getText().toString().trim();
            String provincia = etProvincia.getText().toString().trim();
            String precio    = etPrecio.getText().toString().trim();
            String pistaClub = etPistaClub.getText().toString().trim();
            String desc      = etDesc.getText().toString().trim();

            if (localidad.isEmpty() || provincia.isEmpty()) {
                Toast.makeText(this, getString(R.string.zona_localidad) + " y provincia requeridos",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            StringBuilder sb = new StringBuilder();
            sb.append("📍 ").append(localidad).append(", ").append(provincia);
            if (!precio.isEmpty()) sb.append("\n💶 ").append(precio).append("€/h");
            if (!pistaClub.isEmpty()) sb.append("\n🏟️ ").append(pistaClub);
            sb.append(cbPista.isChecked()
                    ? "\n✅ Incluye reserva de pista"
                    : "\n❌ No incluye reserva de pista");
            if (!desc.isEmpty()) sb.append("\n💬 ").append(desc);

            String tipo = esSolicitar ? "CLASE_SOLICITAR" : "CLASE_OFRECER";
            long id = db.publicarAnuncio(miId, tipo, sb.toString(), provinciaActual);
            if (id != -1) {
                Toast.makeText(this, getString(R.string.zona_publicado), Toast.LENGTH_SHORT).show();
                if (dialogRef[0] != null) dialogRef[0].dismiss();
                cargarAnuncios();
            } else {
                Toast.makeText(this, getString(R.string.zona_error_publicar), Toast.LENGTH_SHORT).show();
            }
        });

        llBotones.addView(btnCancelar);
        llBotones.addView(btnPublicar);
        outer.addView(llBotones);
        dialogRef[0] = mostrarDialog(outer);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Formulario PARTIDA: categoría, jugadores, acompañante (opcional),
    //                     localidad, provincia, pista, descripción
    // ─────────────────────────────────────────────────────────────────────────

    private void mostrarFormularioPartida() {
        LinearLayout outer = crearDialogLayout();
        outer.addView(crearTituloDialog("🏆  " + getString(R.string.zona_rb_partida)));

        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        ScrollView sv = new ScrollView(this);
        sv.addView(form);
        sv.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(450)));

        // Categoría del partido
        form.addView(crearLabel(getString(R.string.zona_partida_categoria)));
        EditText etCategoria = crearEditText("Ej: 3ª Mixta, 2ª Masculina…", false);
        form.addView(etCategoria);

        // Número de jugadores buscados
        addSpacer(form, 12);
        form.addView(crearLabel(getString(R.string.zona_partida_jugadores)));
        EditText etJugadores = crearEditText("Ej: 2", true);
        form.addView(etJugadores);

        // Sección acompañante: nombre + categoría (ambos opcionales)
        addSpacer(form, 16);
        TextView tvAcomp = crearLabel(getString(R.string.zona_partida_acompanante));
        tvAcomp.setTextColor(0xFFFB8C00); // Naranja para distinguir la sección
        form.addView(tvAcomp);

        form.addView(crearLabel(getString(R.string.zona_partida_acomp_nombre)));
        EditText etAcompNombre = crearEditText(getString(R.string.zona_partida_acomp_nombre), false);
        form.addView(etAcompNombre);

        addSpacer(form, 8);
        form.addView(crearLabel(getString(R.string.zona_partida_acomp_categoria)));
        EditText etAcompCat = crearEditText(getString(R.string.zona_partida_acomp_categoria), false);
        form.addView(etAcompCat);

        // Localidad y provincia
        addSpacer(form, 12);
        form.addView(crearLabel(getString(R.string.zona_localidad)));
        EditText etLocalidad = crearEditText(getString(R.string.zona_localidad), false);
        form.addView(etLocalidad);

        addSpacer(form, 12);
        form.addView(crearLabel(getString(R.string.edit_provincia)));
        EditText etProvincia = crearEditText(getString(R.string.edit_provincia), false);
        if (provinciaActual != null) etProvincia.setText(provinciaActual);
        form.addView(etProvincia);

        // Pista donde se jugará
        addSpacer(form, 12);
        form.addView(crearLabel(getString(R.string.zona_pista_club)));
        EditText etPista = crearEditText(getString(R.string.zona_pista_club), false);
        form.addView(etPista);

        addSpacer(form, 12);
        form.addView(crearLabel(getString(R.string.zona_desc_opcional)));
        EditText etDesc = crearEditTextMultiline(getString(R.string.zona_desc_hint));
        form.addView(etDesc);

        outer.addView(sv);

        final AlertDialog[] dialogRef = {null};
        LinearLayout llBotones = crearFilaBotones();

        MaterialButton btnCancelar = crearBotonSecundario(getString(R.string.btn_cancelar));
        LinearLayout.LayoutParams cP = new LinearLayout.LayoutParams(0, dpToPx(48), 1f);
        cP.setMarginEnd(dpToPx(8));
        btnCancelar.setLayoutParams(cP);
        btnCancelar.setOnClickListener(v -> { if (dialogRef[0] != null) dialogRef[0].dismiss(); });

        MaterialButton btnPublicar = crearBotonPrimario(getString(R.string.zona_publicar));
        LinearLayout.LayoutParams pP = new LinearLayout.LayoutParams(0, dpToPx(48), 1f);
        pP.setMarginStart(dpToPx(8));
        btnPublicar.setLayoutParams(pP);
        btnPublicar.setOnClickListener(v -> {
            String categoria = etCategoria.getText().toString().trim();
            String jugadores = etJugadores.getText().toString().trim();
            String localidad = etLocalidad.getText().toString().trim();
            String provincia = etProvincia.getText().toString().trim();
            String pista     = etPista.getText().toString().trim();
            String acompNom  = etAcompNombre.getText().toString().trim();
            String acompCat  = etAcompCat.getText().toString().trim();
            String desc      = etDesc.getText().toString().trim();

            if (localidad.isEmpty() || provincia.isEmpty()) {
                Toast.makeText(this, getString(R.string.zona_localidad) + " y provincia requeridos",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            StringBuilder sb = new StringBuilder();
            if (!categoria.isEmpty()) sb.append("🏆 ").append(categoria).append("\n");
            sb.append("👥 ").append(jugadores.isEmpty() ? "1" : jugadores).append(" jugador(es) buscado(s)");
            sb.append("\n📍 ").append(localidad).append(", ").append(provincia);
            if (!pista.isEmpty()) sb.append("\n🏟️ ").append(pista);
            // Acompañante solo si tiene nombre
            if (!acompNom.isEmpty()) {
                sb.append("\n👤 ").append(acompNom);
                if (!acompCat.isEmpty()) sb.append(" (").append(acompCat).append(")");
            }
            if (!desc.isEmpty()) sb.append("\n💬 ").append(desc);

            long id = db.publicarAnuncio(miId, "PARTIDA", sb.toString(), provinciaActual);
            if (id != -1) {
                Toast.makeText(this, getString(R.string.zona_publicado), Toast.LENGTH_SHORT).show();
                if (dialogRef[0] != null) dialogRef[0].dismiss();
                cargarAnuncios();
            } else {
                Toast.makeText(this, getString(R.string.zona_error_publicar), Toast.LENGTH_SHORT).show();
            }
        });

        llBotones.addView(btnCancelar);
        llBotones.addView(btnPublicar);
        outer.addView(llBotones);
        dialogRef[0] = mostrarDialog(outer);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers de construcción de UI para los formularios
    // ─────────────────────────────────────────────────────────────────────────

    // Contenedor raíz del diálogo: fondo negro con esquinas redondeadas
    private LinearLayout crearDialogLayout() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dpToPx(24), dpToPx(24), dpToPx(24), dpToPx(20));
        GradientDrawable fondo = new GradientDrawable();
        fondo.setColor(0xFF0D0D0D);
        fondo.setCornerRadius(dpToPx(24));
        layout.setBackground(fondo);
        return layout;
    }

    // Título grande en verde lima brillante
    private TextView crearTituloDialog(String texto) {
        TextView tv = new TextView(this);
        tv.setText(texto);
        tv.setTextColor(getResources().getColor(R.color.verde_lima_brillante, getTheme()));
        tv.setTextSize(18f);
        tv.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        p.setMargins(0, 0, 0, dpToPx(16));
        tv.setLayoutParams(p);
        return tv;
    }

    // Label pequeño encima de cada campo del formulario
    private TextView crearLabel(String texto) {
        TextView tv = new TextView(this);
        tv.setText(texto);
        tv.setTextColor(getResources().getColor(R.color.verde_lima_suave, getTheme()));
        tv.setTextSize(12f);
        tv.setTypeface(null, android.graphics.Typeface.BOLD);
        return tv;
    }

    // EditText de una línea con borde sutil (numeric para precios/números)
    private EditText crearEditText(String hint, boolean numeric) {
        EditText et = new EditText(this);
        et.setHint(hint);
        et.setTextColor(getResources().getColor(R.color.white, getTheme()));
        et.setHintTextColor(getResources().getColor(R.color.texto_gris, getTheme()));
        et.setSingleLine(true);
        et.setInputType(numeric
                ? android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
                : android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        GradientDrawable border = new GradientDrawable();
        border.setStroke(dpToPx(1), getResources().getColor(R.color.fondo_tarjeta_claro, getTheme()));
        border.setCornerRadius(dpToPx(8));
        et.setBackground(border);
        et.setPadding(dpToPx(12), dpToPx(10), dpToPx(12), dpToPx(10));
        return et;
    }

    // EditText multilínea para descripciones opcionales
    private EditText crearEditTextMultiline(String hint) {
        EditText et = new EditText(this);
        et.setHint(hint);
        et.setTextColor(getResources().getColor(R.color.white, getTheme()));
        et.setHintTextColor(getResources().getColor(R.color.texto_gris, getTheme()));
        et.setMinLines(2);
        et.setMaxLines(4);
        et.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        GradientDrawable border = new GradientDrawable();
        border.setStroke(dpToPx(1), getResources().getColor(R.color.fondo_tarjeta_claro, getTheme()));
        border.setCornerRadius(dpToPx(8));
        et.setBackground(border);
        et.setPadding(dpToPx(12), dpToPx(10), dpToPx(12), dpToPx(10));
        return et;
    }

    // Botón principal con fondo verde lima
    private MaterialButton crearBotonPrimario(String texto) {
        MaterialButton btn = new MaterialButton(this);
        btn.setText(texto);
        btn.setTextColor(getResources().getColor(R.color.fondo_oscuro, getTheme()));
        btn.setBackgroundTintList(ColorStateList.valueOf(
                getResources().getColor(R.color.verde_lima, getTheme())));
        btn.setCornerRadius(dpToPx(12));
        btn.setTextSize(14f);
        btn.setTypeface(null, android.graphics.Typeface.BOLD);
        return btn;
    }

    // Botón secundario con fondo gris oscuro
    private MaterialButton crearBotonSecundario(String texto) {
        MaterialButton btn = new MaterialButton(this);
        btn.setText(texto);
        btn.setTextColor(getResources().getColor(R.color.texto_gris, getTheme()));
        btn.setBackgroundTintList(ColorStateList.valueOf(
                getResources().getColor(R.color.fondo_tarjeta_claro, getTheme())));
        btn.setCornerRadius(dpToPx(12));
        btn.setTextSize(14f);
        return btn;
    }

    // Fila horizontal de dos botones (Cancelar + Acción principal)
    private LinearLayout crearFilaBotones() {
        LinearLayout ll = new LinearLayout(this);
        ll.setOrientation(LinearLayout.HORIZONTAL);
        ll.setWeightSum(2f);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        p.setMargins(0, dpToPx(16), 0, 0);
        ll.setLayoutParams(p);
        return ll;
    }

    // Crea y muestra el AlertDialog con ventana transparente (solo se ve nuestro layout)
    private AlertDialog mostrarDialog(LinearLayout layout) {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(layout)
                .create();
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        return dialog;
    }

    // Añade un separador invisible entre campos del formulario
    private void addSpacer(LinearLayout parent, int dp) {
        View spacer = new View(this);
        spacer.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(dp)));
        parent.addView(spacer);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Utilidades de presentación de tarjetas
    // ─────────────────────────────────────────────────────────────────────────

    private String textoTipo(String tipo) {
        switch (tipo != null ? tipo : "") {
            case "PARTIDA":         return "PARTIDA";
            case "PALA":            return "PALA";
            case "CLASE_OFRECER":   return "DAR CLASES";
            case "CLASE_SOLICITAR": return "BUSCO CLASES";
            default:                return tipo != null ? tipo : "?";
        }
    }

    // Colores de los badges por tipo de anuncio.
    // PARTIDA usa rojo (#FF5252) para distinguirse claramente del verde corporativo
    // que antes dificultaba diferenciar partidas del resto de anuncios.
    private int colorTipo(String tipo) {
        switch (tipo != null ? tipo : "") {
            case "PARTIDA":         return 0xFFFF5252; // Rojo: distingue partidas del verde app
            case "PALA":            return 0xFF2196F3; // Azul
            case "CLASE_OFRECER":   return 0xFFFF9800; // Naranja
            case "CLASE_SOLICITAR": return 0xFF9C27B0; // Morado
            default:                return 0xFF666666;
        }
    }

    // Muestra un texto informativo cuando no hay anuncios que mostrar
    private void mostrarMensaje(String msg) {
        TextView tv = new TextView(this);
        tv.setText(msg);
        tv.setTextColor(getResources().getColor(R.color.texto_gris, getTheme()));
        tv.setTextSize(14f);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(dpToPx(16), dpToPx(48), dpToPx(16), dpToPx(16));
        llAnuncios.addView(tv);
    }

    // Convierte dp a píxeles usando la densidad real de la pantalla
    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}
