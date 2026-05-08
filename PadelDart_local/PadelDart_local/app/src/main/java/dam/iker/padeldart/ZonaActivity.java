package dam.iker.padeldart;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
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

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

// Tablón de anuncios de la zona provincial.
// Gestiona la publicación y visualización de anuncios PALA, PARTIDA y CLASES.
// Conecta con la API del INE para municipios y con Overpass/OSM para pistas de pádel.
public class ZonaActivity extends BaseDrawerActivity {

    // Callback genérico para selección de un ítem de lista (evita java.util.function.Consumer API 24+)
    interface OnItemSelected { void onSelected(String item); }

    // ─────────────────────────────────────────────────────────────────────────
    // Códigos INE de provincia para la API de municipios
    // https://servicios.ine.es/wstempus/js/ES/MUNICIPIOS/{codigo}
    // ─────────────────────────────────────────────────────────────────────────
    private static final Map<String, String> CODIGO_INE = new LinkedHashMap<>();
    static {
        CODIGO_INE.put("Álava","01"); CODIGO_INE.put("Albacete","02");
        CODIGO_INE.put("Alicante","03"); CODIGO_INE.put("Almería","04");
        CODIGO_INE.put("Ávila","05"); CODIGO_INE.put("Badajoz","06");
        CODIGO_INE.put("Baleares","07"); CODIGO_INE.put("Barcelona","08");
        CODIGO_INE.put("Burgos","09"); CODIGO_INE.put("Cáceres","10");
        CODIGO_INE.put("Cádiz","11"); CODIGO_INE.put("Castellón","12");
        CODIGO_INE.put("Ciudad Real","13"); CODIGO_INE.put("Córdoba","14");
        CODIGO_INE.put("La Coruña","15"); CODIGO_INE.put("Cuenca","16");
        CODIGO_INE.put("Gerona","17"); CODIGO_INE.put("Granada","18");
        CODIGO_INE.put("Guadalajara","19"); CODIGO_INE.put("Guipúzcoa","20");
        CODIGO_INE.put("Huelva","21"); CODIGO_INE.put("Huesca","22");
        CODIGO_INE.put("Jaén","23"); CODIGO_INE.put("León","24");
        CODIGO_INE.put("Lérida","25"); CODIGO_INE.put("La Rioja","26");
        CODIGO_INE.put("Lugo","27"); CODIGO_INE.put("Madrid","28");
        CODIGO_INE.put("Málaga","29"); CODIGO_INE.put("Murcia","30");
        CODIGO_INE.put("Navarra","31"); CODIGO_INE.put("Orense","32");
        CODIGO_INE.put("Asturias","33"); CODIGO_INE.put("Palencia","34");
        CODIGO_INE.put("Las Palmas","35"); CODIGO_INE.put("Pontevedra","36");
        CODIGO_INE.put("Salamanca","37"); CODIGO_INE.put("Santa Cruz de Tenerife","38");
        CODIGO_INE.put("Cantabria","39"); CODIGO_INE.put("Segovia","40");
        CODIGO_INE.put("Sevilla","41"); CODIGO_INE.put("Soria","42");
        CODIGO_INE.put("Tarragona","43"); CODIGO_INE.put("Teruel","44");
        CODIGO_INE.put("Toledo","45"); CODIGO_INE.put("Valencia","46");
        CODIGO_INE.put("Valladolid","47"); CODIGO_INE.put("Vizcaya","48");
        CODIGO_INE.put("Zamora","49"); CODIGO_INE.put("Zaragoza","50");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Marcas y modelos de palas del mercado 2024-2025
    // ─────────────────────────────────────────────────────────────────────────
    private static final String[] MARCAS_PALA = {
        "Bullpadel", "Head", "Adidas", "NOX", "Babolat", "Wilson",
        "Siux", "Varlion", "Dunlop", "Starvie", "Black Crown",
        "Royal Padel", "Wingpadel", "Artengo", "Kuikma", "Power Padel",
        "Oxdog", "RS Padel", "Enebe", "Tipsapadel"
    };

    // Modelos más representativos por marca (actualizado a temporada 2024/2025)
    private static final Map<String, String[]> MODELOS_POR_MARCA = new LinkedHashMap<>();
    static {
        MODELOS_POR_MARCA.put("Bullpadel", new String[]{
            "Hack 03","Hack 02","Hack Hybrid 03","Vertex 04 Comfort",
            "Vertex 03","Magnum 03","Neuron 03","Kiowa 03","Indiga 03","Gbomi 03"
        });
        MODELOS_POR_MARCA.put("Head", new String[]{
            "Delta Pro 2024","Delta Elite 2024","Flash Pro","Alpha Pro 2024",
            "Speed Pro","Graphene 360 Alpha Elite","Delta Motion","Flash Hybrid"
        });
        MODELOS_POR_MARCA.put("Adidas", new String[]{
            "Metalbone 3.3 HRD","Metalbone 3.2","Metalbone Light 3.2",
            "Adipower Multiweight CTRL 3.3","Adipower Team CTRL 3.3",
            "Adipower Light 3.3","RX Light 3.3","Match Light 3.3"
        });
        MODELOS_POR_MARCA.put("NOX", new String[]{
            "AT10 Luxury Carbon 2024","AT10 Genius 18K","ML10 Pro Cup 3K",
            "Stinger Luxury Carbon","Equation WPT","Nerbo WPT","Casual Genius"
        });
        MODELOS_POR_MARCA.put("Babolat", new String[]{
            "Technical Veron 2024","Air Veron 2024","Technical Viper 2024",
            "Defiance Carbon 2024","Falcon Carbon","Counter Veron 2024"
        });
        MODELOS_POR_MARCA.put("Wilson", new String[]{
            "Bela Elite V2","Bela Pro V2","Ultra Spin 2024",
            "Bela Carbon 2024","Force Pro","Blade Team V2"
        });
        MODELOS_POR_MARCA.put("Siux", new String[]{
            "Electra Carbon 24K","Carbone Elite 14K","Diablo Gloss",
            "Hurricane","Fenix Luxury","Diablo 3K"
        });
        MODELOS_POR_MARCA.put("Varlion", new String[]{
            "Lethal Weapon Carbon LW","Avant Carbon LW","Summum Carbon 8","LW Comfort"
        });
        MODELOS_POR_MARCA.put("Dunlop", new String[]{
            "Speed Max Carbon 2024","Speed Ultra Carbon 2024","Speed Pro","Speed Fast"
        });
        MODELOS_POR_MARCA.put("Starvie", new String[]{
            "Metheora 3K 2024","Basalto Astrum","Astrum 3K","Raptor Endurance Pro"
        });
        MODELOS_POR_MARCA.put("Black Crown", new String[]{
            "Piton 7.0 Soft","Piton 5.0 Soft","Piton Attack","Piton 7.0"
        });
        MODELOS_POR_MARCA.put("Royal Padel", new String[]{
            "Whip 03K Pro","M27 Polyethylene","Ranger 03K","190 Whip 2024"
        });
        MODELOS_POR_MARCA.put("Wingpadel", new String[]{
            "W-Vulcan 3K","W-Storm 3K","W-Fire","W-Cobra"
        });
        MODELOS_POR_MARCA.put("Artengo", new String[]{
            "PR990 Power","PR860 Soft","PR190 Hybrid","PR190 Lite"
        });
        MODELOS_POR_MARCA.put("Kuikma", new String[]{
            "PL 900 Carbon","PL 800 Hybrid","PL 700","PL 590","PL 500"
        });
        MODELOS_POR_MARCA.put("Power Padel", new String[]{
            "Master Carbon 3K","Master Pro 2024","Classic 2024"
        });
        MODELOS_POR_MARCA.put("Oxdog", new String[]{
            "Ultrapower HES 6.1","Ultra HES 5.1","Vieille 4.1"
        });
        MODELOS_POR_MARCA.put("RS Padel", new String[]{
            "Gaucho","X Carbon Series","Attacker"
        });
        MODELOS_POR_MARCA.put("Enebe", new String[]{
            "Tornado 7.1","Overline 9.1","Equinox 8.1"
        });
        MODELOS_POR_MARCA.put("Tipsapadel", new String[]{
            "Quantum","Atom Pro","Ion"
        });
    }

    // Provincias españolas ordenadas para el selector
    private static final String[] PROVINCIAS = {
        "Álava","Albacete","Alicante","Almería","Asturias","Ávila",
        "Badajoz","Baleares","Barcelona","Burgos","Cáceres","Cádiz",
        "Cantabria","Castellón","Ciudad Real","Córdoba","Cuenca",
        "Gerona","Granada","Guadalajara","Guipúzcoa","Huelva","Huesca",
        "Jaén","La Coruña","La Rioja","Las Palmas","León","Lérida",
        "Lugo","Madrid","Málaga","Murcia","Navarra","Orense","Palencia",
        "Pontevedra","Salamanca","Santa Cruz de Tenerife","Segovia",
        "Sevilla","Soria","Tarragona","Teruel","Toledo","Valencia",
        "Valladolid","Vizcaya","Zamora","Zaragoza"
    };

    // ─────────────────────────────────────────────────────────────────────────
    // Campos de instancia
    // ─────────────────────────────────────────────────────────────────────────
    private DatabaseHelper db;
    private SessionManager session;
    private long   miId;
    private String provinciaActual;
    private String miProvinciaOriginal;

    private LinearLayout llAnuncios;
    private TextView     tvTituloZona;
    private String filtroActivo = null;
    private List<Map<String, Object>> todosLosAnuncios;

    // Soporte de hasta 3 fotos para el formulario PALA.
    // El launcher se registra una sola vez en onCreate (requisito AndroidX).
    // slotFotoActual indica a cuál de los 3 slots pertenece la foto elegida.
    private ActivityResultLauncher<Intent> fotoLauncher;
    private final String[]    fotosUrisPala   = {null, null, null};
    private final ImageView[] imgPreviewsPala = new ImageView[3];
    private int               slotFotoActual  = 0;

    // ─────────────────────────────────────────────────────────────────────────
    // Ciclo de vida
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_zona);

        db      = DatabaseHelper.getInstance(this);
        session = SessionManager.getInstance(this);
        miId    = session.getUsuarioActualId();

        Map<String, Object> yo = db.obtenerUsuario(miId);
        miProvinciaOriginal = yo != null ? (String) yo.get(DatabaseHelper.COL_PROVINCIA) : null;
        provinciaActual     = miProvinciaOriginal;

        llAnuncios   = findViewById(R.id.llAnuncios);
        tvTituloZona = findViewById(R.id.tvTituloZona);

        findViewById(R.id.btnAtras).setOnClickListener(v -> finish());
        actualizarTituloProvincia();
        findViewById(R.id.btnCambiarProvincia).setOnClickListener(v -> mostrarSelectorProvincia());

        // El launcher de foto debe registrarse antes de cualquier interacción del usuario.
        // Cuando el usuario elige una imagen, la copiamos al almacenamiento interno para
        // que la URI no expire entre sesiones (las picker URIs son efímeras).
        fotoLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            String rutaLocal = copiarFotoAnuncio(uri, slotFotoActual);
                            String uriGuardar = rutaLocal != null ? rutaLocal : uri.toString();
                            fotosUrisPala[slotFotoActual] = uriGuardar;
                            ImageView preview = imgPreviewsPala[slotFotoActual];
                            if (preview != null) {
                                Bitmap bmp = loadBitmap(uriGuardar);
                                if (bmp != null) {
                                    preview.setImageBitmap(bmp);
                                    preview.setVisibility(View.VISIBLE);
                                }
                            }
                        }
                    }
                });

        ExtendedFloatingActionButton fab = findViewById(R.id.fabNuevoAnuncio);
        fab.setOnClickListener(v -> mostrarDialogoNuevoAnuncio());

        configurarFiltros();
        cargarAnuncios();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Provincia: título y selector
    // ─────────────────────────────────────────────────────────────────────────

    private void actualizarTituloProvincia() {
        String nombre = provinciaActual != null ? provinciaActual : "Mi Zona";
        if (tvTituloZona != null) tvTituloZona.setText("📌  " + nombre);
        MaterialButton btnProv = findViewById(R.id.btnCambiarProvincia);
        if (btnProv != null) btnProv.setText("📍 " + nombre);
    }

    private void mostrarSelectorProvincia() {
        int idxActual = provinciaActual != null
                ? Arrays.asList(PROVINCIAS).indexOf(provinciaActual) : 0;
        if (idxActual < 0) idxActual = 0;
        final int[] sel = {idxActual};
        new AlertDialog.Builder(this)
                .setTitle("📌 Selecciona una provincia")
                .setSingleChoiceItems(PROVINCIAS, idxActual, (d, w) -> sel[0] = w)
                .setPositiveButton("Ver anuncios", (d, w) -> {
                    provinciaActual = PROVINCIAS[sel[0]];
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

    private void cargarAnuncios() {
        if (provinciaActual == null || provinciaActual.isEmpty()) {
            mostrarMensaje("No tienes provincia asignada. Actualiza tu perfil o selecciona una.");
            return;
        }
        todosLosAnuncios = db.obtenerAnunciosProvincia(provinciaActual);
        mostrarAnunciosFiltrados();
    }

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
    // Tarjetas de anuncio
    // ─────────────────────────────────────────────────────────────────────────

    // Dispatcher: las PALA en nuevo formato reciben su tarjeta especial "magic card".
    // El resto (incluyendo PALA en formato antiguo) usan la tarjeta estándar.
    private void agregarTarjetaAnuncio(Map<String, Object> anuncio) {
        String tipo        = (String) anuncio.get("tipo");
        String descripcion = (String) anuncio.get("descripcion");
        if ("PALA".equals(tipo) && descripcion != null && descripcion.contains("@@NOMBRE:")) {
            agregarTarjetaPala(anuncio);
            return;
        }

        // ── Tarjeta estándar (PARTIDA, CLASE, PALA legado) ──────────────────
        String nombre    = strSafe(anuncio.get("nombre"));
        String apellidos = strSafe(anuncio.get("apellidos"));
        String categoria = strSafe(anuncio.get("categoria_actual"));
        String fotoAutor = strSafe(anuncio.get("foto_perfil"));
        long   timestamp = anuncio.get("timestamp") instanceof Long ? (Long) anuncio.get("timestamp") : 0L;

        // Separamos @@FOTO: legacy de la descripción visible
        String fotoUri = null; String descVisible = descripcion;
        if (descripcion != null && descripcion.contains("\n@@FOTO:")) {
            int idx = descripcion.indexOf("\n@@FOTO:");
            fotoUri = descripcion.substring(idx + "\n@@FOTO:".length());
            descVisible = descripcion.substring(0, idx);
        }

        MaterialCardView card = crearCard(dpToPx(12));
        LinearLayout inner = crearInnerLayout(dpToPx(14));

        // Fila de autor: avatar + nombre/categoría + badge de tipo
        inner.addView(crearFilaAutor(fotoAutor, nombre, apellidos, categoria, tipo));

        // Miniatura legacy de PALA
        if (fotoUri != null) {
            Bitmap bmp = loadBitmap(fotoUri);
            if (bmp != null) {
                ImageView imgPala = new ImageView(this);
                LinearLayout.LayoutParams imgP = new LinearLayout.LayoutParams(dpToPx(100), dpToPx(100));
                imgP.setMargins(0, dpToPx(10), 0, 0);
                imgPala.setLayoutParams(imgP);
                imgPala.setScaleType(ImageView.ScaleType.CENTER_CROP);
                GradientDrawable imgBg = new GradientDrawable();
                imgBg.setCornerRadius(dpToPx(8)); imgBg.setColor(0xFF1A1A1A);
                imgPala.setBackground(imgBg);
                imgPala.setImageBitmap(bmp);
                inner.addView(imgPala);
            }
        }

        // Texto del anuncio
        TextView tvDesc = new TextView(this);
        LinearLayout.LayoutParams dP = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        dP.setMargins(0, dpToPx(10), 0, 0);
        tvDesc.setLayoutParams(dP);
        tvDesc.setText(descVisible);
        tvDesc.setTextColor(getResources().getColor(R.color.white, getTheme()));
        tvDesc.setTextSize(14f);
        tvDesc.setLineSpacing(dpToPx(2), 1f);
        inner.addView(tvDesc);

        inner.addView(crearFecha(timestamp));
        card.addView(inner);
        llAnuncios.addView(card);
    }

    // Tarjeta estilo "Magic card" para anuncios PALA con foto.
    // Sección superior: autor. Foto grande central. Nombre grande.
    // Chips de marca y modelo. Descripción y fecha al pie.
    private void agregarTarjetaPala(Map<String, Object> anuncio) {
        String desc      = strSafe(anuncio.get("descripcion"));
        String nombre    = strSafe(anuncio.get("nombre"));
        String apellidos = strSafe(anuncio.get("apellidos"));
        String categoria = strSafe(anuncio.get("categoria_actual"));
        String fotoAutor = strSafe(anuncio.get("foto_perfil"));
        long   timestamp = anuncio.get("timestamp") instanceof Long ? (Long) anuncio.get("timestamp") : 0L;

        String nombrePala = extraerTag(desc, "NOMBRE");
        String marca      = extraerTag(desc, "MARCA");
        String modelo     = extraerTag(desc, "MODELO");
        String estado     = extraerTag(desc, "ESTADO");
        String descTexto  = extraerTag(desc, "DESC");
        String fotosRaw   = extraerTag(desc, "FOTOS");
        String[] fotosArr = fotosRaw.isEmpty() ? new String[0] : fotosRaw.split("\\|");

        // Tarjeta con borde azul para palas (distinguida del verde de la app)
        MaterialCardView card = new MaterialCardView(this);
        LinearLayout.LayoutParams cardP = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cardP.setMargins(0, 0, 0, dpToPx(14));
        card.setLayoutParams(cardP);
        card.setRadius(dpToPx(14));
        card.setCardElevation(dpToPx(6));
        card.setCardBackgroundColor(getResources().getColor(R.color.fondo_tarjeta, getTheme()));
        card.setStrokeColor(0xFF1E88E5);
        card.setStrokeWidth(dpToPx(1));

        LinearLayout inner = new LinearLayout(this);
        inner.setOrientation(LinearLayout.VERTICAL);
        inner.setPadding(0, 0, 0, dpToPx(14));

        // ── Fila de autor en la cabecera ────────────────────────────────────
        LinearLayout header = crearFilaAutor(fotoAutor, nombre, apellidos, categoria, "PALA");
        LinearLayout.LayoutParams hP = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        hP.setMargins(dpToPx(14), dpToPx(12), dpToPx(14), dpToPx(10));
        header.setLayoutParams(hP);
        inner.addView(header);

        // ── Foto principal ──────────────────────────────────────────────────
        if (fotosArr.length > 0) {
            final ImageView[] imgPrincipal = new ImageView[1];
            imgPrincipal[0] = new ImageView(this);
            imgPrincipal[0].setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(220)));
            imgPrincipal[0].setScaleType(ImageView.ScaleType.CENTER_CROP);
            Bitmap bmpMain = loadBitmap(fotosArr[0]);
            if (bmpMain != null) imgPrincipal[0].setImageBitmap(bmpMain);
            inner.addView(imgPrincipal[0]);

            // Miniaturas: si hay más de una foto, fila de thumbnails deslizable
            if (fotosArr.length > 1) {
                HorizontalScrollView hsvThumb = new HorizontalScrollView(this);
                hsvThumb.setScrollbars(0);
                LinearLayout.LayoutParams hsvP = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                hsvP.setMargins(dpToPx(14), dpToPx(8), dpToPx(14), 0);
                hsvThumb.setLayoutParams(hsvP);

                LinearLayout llThumbs = new LinearLayout(this);
                llThumbs.setOrientation(LinearLayout.HORIZONTAL);
                for (String fotoUri : fotosArr) {
                    Bitmap bmp = loadBitmap(fotoUri);
                    if (bmp == null) continue;

                    MaterialCardView thumbCard = new MaterialCardView(this);
                    LinearLayout.LayoutParams tP = new LinearLayout.LayoutParams(dpToPx(56), dpToPx(56));
                    tP.setMarginEnd(dpToPx(6));
                    thumbCard.setLayoutParams(tP);
                    thumbCard.setRadius(dpToPx(6));
                    thumbCard.setCardElevation(dpToPx(2));
                    thumbCard.setStrokeColor(0xFF1E88E5); thumbCard.setStrokeWidth(dpToPx(1));

                    ImageView thumb = new ImageView(this);
                    thumb.setLayoutParams(new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
                    thumb.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    thumb.setImageBitmap(bmp);

                    // Al pulsar el thumbnail, la foto principal se actualiza
                    final Bitmap bmpFinal = bmp;
                    thumbCard.setOnClickListener(v -> imgPrincipal[0].setImageBitmap(bmpFinal));
                    thumbCard.addView(thumb);
                    llThumbs.addView(thumbCard);
                }
                hsvThumb.addView(llThumbs);
                inner.addView(hsvThumb);
            }
        }

        // ── Nombre de la pala en grande ─────────────────────────────────────
        LinearLayout infoLayout = new LinearLayout(this);
        infoLayout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams ilP = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        ilP.setMargins(dpToPx(16), dpToPx(12), dpToPx(16), 0);
        infoLayout.setLayoutParams(ilP);

        if (!nombrePala.isEmpty()) {
            TextView tvNombrePala = new TextView(this);
            tvNombrePala.setText(nombrePala);
            tvNombrePala.setTextColor(getResources().getColor(R.color.white, getTheme()));
            tvNombrePala.setTextSize(20f);
            tvNombrePala.setTypeface(null, Typeface.BOLD);
            tvNombrePala.setLetterSpacing(0.02f);
            infoLayout.addView(tvNombrePala);
        }

        // Chips de marca y modelo en fila
        if (!marca.isEmpty() || !modelo.isEmpty()) {
            LinearLayout chipRow = new LinearLayout(this);
            chipRow.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams crP = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            crP.setMargins(0, dpToPx(6), 0, 0);
            chipRow.setLayoutParams(crP);
            if (!marca.isEmpty()) chipRow.addView(crearChip(marca, 0xFF1E88E5));
            if (!modelo.isEmpty()) {
                LinearLayout.LayoutParams mp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                mp.setMarginStart(dpToPx(6));
                View chip = crearChip(modelo, 0xFF546E7A);
                chip.setLayoutParams(mp);
                chipRow.addView(chip);
            }
            infoLayout.addView(chipRow);
        }

        // Estado y descripción
        if (!estado.isEmpty()) {
            TextView tvEstado = new TextView(this);
            LinearLayout.LayoutParams estP = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            estP.setMargins(0, dpToPx(8), 0, 0);
            tvEstado.setLayoutParams(estP);
            tvEstado.setText("📦 Estado: " + estado);
            tvEstado.setTextColor(getResources().getColor(R.color.verde_lima_suave, getTheme()));
            tvEstado.setTextSize(12f);
            infoLayout.addView(tvEstado);
        }
        if (!descTexto.isEmpty()) {
            TextView tvD = new TextView(this);
            LinearLayout.LayoutParams tdP = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            tdP.setMargins(0, dpToPx(6), 0, 0);
            tvD.setLayoutParams(tdP);
            tvD.setText(descTexto);
            tvD.setTextColor(getResources().getColor(R.color.white, getTheme()));
            tvD.setTextSize(13f);
            tvD.setLineSpacing(dpToPx(2), 1f);
            infoLayout.addView(tvD);
        }
        infoLayout.addView(crearFecha(timestamp));
        inner.addView(infoLayout);
        card.addView(inner);
        llAnuncios.addView(card);
    }

    // Crea la fila de cabecera de una tarjeta: avatar circular del autor + nombre + badge de tipo.
    private LinearLayout crearFilaAutor(String fotoAutor, String nombre, String apellidos,
                                        String categoria, String tipo) {
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        // Avatar circular 38dp con foto o inicial
        MaterialCardView avatarCard = new MaterialCardView(this);
        LinearLayout.LayoutParams avP = new LinearLayout.LayoutParams(dpToPx(38), dpToPx(38));
        avP.setMarginEnd(dpToPx(10));
        avatarCard.setLayoutParams(avP);
        avatarCard.setRadius(dpToPx(19));
        avatarCard.setCardElevation(0);
        avatarCard.setStrokeColor(getResources().getColor(R.color.verde_lima, getTheme()));
        avatarCard.setStrokeWidth(dpToPx(1));

        FrameLayout avatarFrame = new FrameLayout(this);
        avatarFrame.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        avatarFrame.setBackgroundColor(
                getResources().getColor(R.color.fondo_tarjeta_claro, getTheme()));

        TextView tvIni = new TextView(this);
        tvIni.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        tvIni.setGravity(Gravity.CENTER);
        tvIni.setText(!nombre.isEmpty() ? String.valueOf(nombre.charAt(0)).toUpperCase() : "?");
        tvIni.setTextColor(getResources().getColor(R.color.verde_lima, getTheme()));
        tvIni.setTextSize(14f); tvIni.setTypeface(null, Typeface.BOLD);

        ImageView imgFotoAvatar = new ImageView(this);
        imgFotoAvatar.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        imgFotoAvatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imgFotoAvatar.setVisibility(View.GONE);

        avatarFrame.addView(tvIni);
        avatarFrame.addView(imgFotoAvatar);
        avatarCard.addView(avatarFrame);
        header.addView(avatarCard);

        // Cargamos la foto del autor usando el helper heredado de BaseDrawerActivity
        if (!fotoAutor.isEmpty()) cargarFotoSegura(imgFotoAvatar, tvIni, fotoAutor);

        // Nombre + categoría en columna
        LinearLayout colNombre = new LinearLayout(this);
        colNombre.setOrientation(LinearLayout.VERTICAL);
        colNombre.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView tvAutor = new TextView(this);
        tvAutor.setText(nombre + " " + apellidos);
        tvAutor.setTextColor(getResources().getColor(R.color.verde_lima_suave, getTheme()));
        tvAutor.setTextSize(13f); tvAutor.setTypeface(null, Typeface.BOLD);

        TextView tvCat = new TextView(this);
        tvCat.setText(categoria);
        tvCat.setTextColor(getResources().getColor(R.color.texto_gris, getTheme()));
        tvCat.setTextSize(11f);

        colNombre.addView(tvAutor);
        if (!categoria.isEmpty()) colNombre.addView(tvCat);
        header.addView(colNombre);

        // Badge de tipo
        TextView tvTipoBadge = new TextView(this);
        tvTipoBadge.setText(textoTipo(tipo));
        tvTipoBadge.setTextColor(getResources().getColor(R.color.fondo_oscuro, getTheme()));
        tvTipoBadge.setTextSize(10f); tvTipoBadge.setTypeface(null, Typeface.BOLD);
        tvTipoBadge.setPadding(dpToPx(10), dpToPx(4), dpToPx(10), dpToPx(4));
        GradientDrawable badge = new GradientDrawable();
        badge.setCornerRadius(dpToPx(12)); badge.setColor(colorTipo(tipo));
        tvTipoBadge.setBackground(badge);
        header.addView(tvTipoBadge);
        return header;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PASO 1: selección de tipo de anuncio
    // ─────────────────────────────────────────────────────────────────────────

    private void mostrarDialogoNuevoAnuncio() {
        LinearLayout layout = crearDialogLayout();
        layout.addView(crearTituloDialog("✦  " + getString(R.string.zona_nuevo_titulo)));
        layout.addView(crearLabel(getString(R.string.zona_tipo_anuncio)));

        RadioGroup rg = new RadioGroup(this);
        rg.setOrientation(RadioGroup.VERTICAL);
        rg.setPadding(0, dpToPx(6), 0, 0);

        String[] tipos     = {"PARTIDA","PALA","CLASE_OFRECER","CLASE_SOLICITAR"};
        String[] etiquetas = {
            getString(R.string.zona_rb_partida), getString(R.string.zona_rb_pala),
            getString(R.string.zona_rb_ofrecer), getString(R.string.zona_rb_solicitar)
        };
        int[] coloresTipo = {0xFFFF5252, 0xFF1E88E5, 0xFFFB8C00, 0xFF8E24AA};

        for (int i = 0; i < tipos.length; i++) {
            RadioButton rb = new RadioButton(this);
            rb.setText(etiquetas[i]); rb.setTag(tipos[i]);
            rb.setTextColor(coloresTipo[i]);
            rb.setButtonTintList(ColorStateList.valueOf(coloresTipo[i]));
            rb.setTextSize(15f);
            rb.setPadding(dpToPx(8), dpToPx(10), 0, dpToPx(10));
            rg.addView(rb);
        }
        layout.addView(rg);

        final AlertDialog[] dialogRef = {null};
        LinearLayout llBotones = crearFilaBotones();
        MaterialButton btnC = crearBotonSecundario(getString(R.string.btn_cancelar));
        LinearLayout.LayoutParams cP = new LinearLayout.LayoutParams(0, dpToPx(48), 1f);
        cP.setMarginEnd(dpToPx(8)); btnC.setLayoutParams(cP);
        btnC.setOnClickListener(v -> { if (dialogRef[0] != null) dialogRef[0].dismiss(); });

        MaterialButton btnS = crearBotonPrimario(getString(R.string.zona_siguiente));
        LinearLayout.LayoutParams sP = new LinearLayout.LayoutParams(0, dpToPx(48), 1f);
        sP.setMarginStart(dpToPx(8)); btnS.setLayoutParams(sP);
        btnS.setOnClickListener(v -> {
            int checked = rg.getCheckedRadioButtonId();
            if (checked == -1) {
                Toast.makeText(this, getString(R.string.zona_selecciona_tipo), Toast.LENGTH_SHORT).show();
                return;
            }
            String tipoElegido = (String) rg.findViewById(checked).getTag();
            if (dialogRef[0] != null) dialogRef[0].dismiss();
            switch (tipoElegido) {
                case "PALA":            mostrarFormularioPala();        break;
                case "CLASE_OFRECER":   mostrarFormularioClase(false);  break;
                case "CLASE_SOLICITAR": mostrarFormularioClase(true);   break;
                case "PARTIDA":         mostrarFormularioPartida();      break;
            }
        });
        llBotones.addView(btnC); llBotones.addView(btnS);
        layout.addView(llBotones);
        dialogRef[0] = mostrarDialog(layout);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Formulario PALA: marca API, modelo API, nombre libre, estado, 3 fotos, desc
    // ─────────────────────────────────────────────────────────────────────────

    private void mostrarFormularioPala() {
        // Reseteamos los slots de foto del formulario anterior
        Arrays.fill(fotosUrisPala, null);
        Arrays.fill(imgPreviewsPala, null);

        LinearLayout outer = crearDialogLayout();
        outer.addView(crearTituloDialog("🏓  " + getString(R.string.zona_titulo_pala)));

        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        ScrollView sv = new ScrollView(this);
        sv.addView(form);
        sv.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(460)));

        // ── Selección de MARCA ──────────────────────────────────────────────
        form.addView(crearLabel("🏷️  Marca"));
        final String[] marcaSeleccionada = {""};
        MaterialButton btnMarca = crearBotonSecundario("Seleccionar marca  ▾");
        btnMarca.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(44)));
        btnMarca.setOnClickListener(v ->
            mostrarDialogoLista("🏷️ Selecciona la marca", Arrays.asList(MARCAS_PALA), item -> {
                marcaSeleccionada[0] = item;
                btnMarca.setText("🏷️ " + item);
            })
        );
        form.addView(btnMarca);

        // ── Selección de MODELO (filtrado por marca) ────────────────────────
        addSpacer(form, 10);
        form.addView(crearLabel("📋  Modelo"));
        final String[] modeloSeleccionado = {""};
        MaterialButton btnModelo = crearBotonSecundario("Seleccionar modelo  ▾");
        btnModelo.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(44)));
        btnModelo.setOnClickListener(v -> {
            String[] modelos = marcaSeleccionada[0].isEmpty()
                    ? new String[]{"Selecciona primero una marca"}
                    : MODELOS_POR_MARCA.getOrDefault(marcaSeleccionada[0], new String[]{"Modelo libre"});
            if (marcaSeleccionada[0].isEmpty()) {
                Toast.makeText(this, "Selecciona primero la marca", Toast.LENGTH_SHORT).show();
                return;
            }
            mostrarDialogoLista("📋 Modelo de " + marcaSeleccionada[0],
                    Arrays.asList(modelos), item -> {
                        modeloSeleccionado[0] = item;
                        btnModelo.setText("📋 " + item);
                    });
        });
        form.addView(btnModelo);

        // ── Nombre personalizado ────────────────────────────────────────────
        addSpacer(form, 10);
        form.addView(crearLabel(getString(R.string.zona_pala_nombre) + " (personalizado)"));
        EditText etNombre = crearEditText("Ej: Hack 03 – edición limitada", false);
        form.addView(etNombre);

        // ── Estado ──────────────────────────────────────────────────────────
        addSpacer(form, 10);
        form.addView(crearLabel(getString(R.string.zona_pala_estado)));
        RadioGroup rgEstado = new RadioGroup(this);
        rgEstado.setOrientation(RadioGroup.VERTICAL);
        final String[] estados = {
            getString(R.string.zona_pala_nuevo), getString(R.string.zona_pala_muy_bueno),
            getString(R.string.zona_pala_bueno), getString(R.string.zona_pala_regular)
        };
        for (int i = 0; i < estados.length; i++) {
            RadioButton rb = new RadioButton(this);
            rb.setText(estados[i]);
            rb.setTextColor(getResources().getColor(R.color.white, getTheme()));
            rb.setButtonTintList(ColorStateList.valueOf(0xFF1E88E5));
            rb.setTextSize(13f);
            rb.setPadding(dpToPx(4), dpToPx(6), dpToPx(16), dpToPx(6));
            rgEstado.addView(rb);
            if (i == 2) rb.setChecked(true);
        }
        form.addView(rgEstado);

        // ── Hasta 3 fotos en una fila horizontal ────────────────────────────
        addSpacer(form, 10);
        form.addView(crearLabel("📷  Fotos de la pala (hasta 3)"));
        LinearLayout fotoRow = new LinearLayout(this);
        fotoRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams frP = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        frP.setMargins(0, dpToPx(6), 0, dpToPx(6));
        fotoRow.setLayoutParams(frP);

        for (int slot = 0; slot < 3; slot++) {
            final int s = slot;
            LinearLayout slotLayout = new LinearLayout(this);
            slotLayout.setOrientation(LinearLayout.VERTICAL);
            slotLayout.setGravity(Gravity.CENTER_HORIZONTAL);
            LinearLayout.LayoutParams slP = new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            slP.setMarginEnd(slot < 2 ? dpToPx(8) : 0);
            slotLayout.setLayoutParams(slP);

            // Cuadro de preview con fondo gris
            MaterialCardView previewCard = new MaterialCardView(this);
            LinearLayout.LayoutParams pcP = new LinearLayout.LayoutParams(dpToPx(90), dpToPx(90));
            previewCard.setLayoutParams(pcP);
            previewCard.setRadius(dpToPx(10));
            previewCard.setCardElevation(dpToPx(2));
            previewCard.setStrokeColor(0xFF333333); previewCard.setStrokeWidth(dpToPx(1));

            FrameLayout previewFrame = new FrameLayout(this);
            previewFrame.setLayoutParams(new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
            previewFrame.setBackgroundColor(0xFF1A1A1A);

            TextView tvPlaceholder = new TextView(this);
            tvPlaceholder.setLayoutParams(new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
            tvPlaceholder.setGravity(Gravity.CENTER);
            tvPlaceholder.setText("➕");
            tvPlaceholder.setTextSize(24f);
            tvPlaceholder.setTextColor(0xFF555555);

            ImageView imgPrev = new ImageView(this);
            imgPrev.setLayoutParams(new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
            imgPrev.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imgPrev.setVisibility(View.GONE);
            imgPreviewsPala[s] = imgPrev;

            previewFrame.addView(tvPlaceholder);
            previewFrame.addView(imgPrev);
            previewCard.addView(previewFrame);

            // Al tocar el cuadro se abre el selector de imágenes para ese slot
            previewCard.setOnClickListener(v -> {
                slotFotoActual = s;
                Intent pick = new Intent(Intent.ACTION_GET_CONTENT);
                pick.setType("image/*");
                fotoLauncher.launch(Intent.createChooser(pick,
                        getString(R.string.reg_foto_seleccionar)));
            });
            slotLayout.addView(previewCard);
            fotoRow.addView(slotLayout);
        }
        form.addView(fotoRow);

        // ── Descripción ─────────────────────────────────────────────────────
        addSpacer(form, 10);
        form.addView(crearLabel(getString(R.string.zona_desc_opcional)));
        EditText etDesc = crearEditTextMultiline(
                "Ej: Pala poco usada, excelente estado. Golpeo potente y buena salida de bola. " +
                "Ideal para jugadores de nivel 3ª en adelante. Precio negociable.");
        form.addView(etDesc);

        outer.addView(sv);
        final AlertDialog[] dialogRef = {null};
        LinearLayout llBotones = crearFilaBotones();

        MaterialButton btnC = crearBotonSecundario(getString(R.string.btn_cancelar));
        LinearLayout.LayoutParams cP = new LinearLayout.LayoutParams(0, dpToPx(48), 1f);
        cP.setMarginEnd(dpToPx(8)); btnC.setLayoutParams(cP);
        btnC.setOnClickListener(v -> { if (dialogRef[0] != null) dialogRef[0].dismiss(); });

        MaterialButton btnPub = crearBotonPrimario(getString(R.string.zona_publicar));
        LinearLayout.LayoutParams pP = new LinearLayout.LayoutParams(0, dpToPx(48), 1f);
        pP.setMarginStart(dpToPx(8)); btnPub.setLayoutParams(pP);
        btnPub.setOnClickListener(v -> {
            String nomPala = etNombre.getText().toString().trim();
            String marca   = marcaSeleccionada[0];
            String modelo  = modeloSeleccionado[0];
            // El nombre puede ser el modelo si no se especificó uno personalizado
            String nombreFinal = nomPala.isEmpty()
                    ? (modelo.isEmpty() ? marca : modelo)
                    : nomPala;
            if (nombreFinal.isEmpty() && marca.isEmpty()) {
                Toast.makeText(this, "Indica al menos la marca o nombre de la pala",
                        Toast.LENGTH_SHORT).show();
                return;
            }
            int idxE = rgEstado.indexOfChild(
                    rgEstado.findViewById(rgEstado.getCheckedRadioButtonId()));
            String estadoText = idxE >= 0 ? estados[idxE] : estados[2];
            String descText = etDesc.getText().toString().trim();

            // Construimos la descripción con el nuevo formato de tags para la magic card
            StringBuilder sb = new StringBuilder();
            sb.append("@@NOMBRE:").append(nombreFinal).append("\n");
            if (!marca.isEmpty())  sb.append("@@MARCA:").append(marca).append("\n");
            if (!modelo.isEmpty()) sb.append("@@MODELO:").append(modelo).append("\n");
            sb.append("@@ESTADO:").append(estadoText).append("\n");
            if (!descText.isEmpty()) sb.append("@@DESC:").append(descText).append("\n");

            // Recopilamos las URIs de fotos no nulas, separadas por |
            StringBuilder fotos = new StringBuilder();
            for (String uri : fotosUrisPala) {
                if (uri != null) { if (fotos.length() > 0) fotos.append("|"); fotos.append(uri); }
            }
            if (fotos.length() > 0) sb.append("@@FOTOS:").append(fotos);

            long id = db.publicarAnuncio(miId, "PALA", sb.toString(), provinciaActual);
            if (id != -1) {
                Toast.makeText(this, getString(R.string.zona_publicado), Toast.LENGTH_SHORT).show();
                if (dialogRef[0] != null) dialogRef[0].dismiss();
                cargarAnuncios();
            } else {
                Toast.makeText(this, getString(R.string.zona_error_publicar), Toast.LENGTH_SHORT).show();
            }
        });
        llBotones.addView(btnC); llBotones.addView(btnPub);
        outer.addView(llBotones);
        dialogRef[0] = mostrarDialog(outer);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Formulario CLASE: localidad (API INE) → pista (API Overpass), precio, desc
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
                LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(430)));

        // ── Localidad y pista (con APIs) ────────────────────────────────────
        final String[] ciudadSel = {""};
        final String[] pistaSel  = {""};
        agregarSeccionLocalidadPista(form, ciudadSel, pistaSel);

        // ── Precio por hora (solo para OFRECER) ─────────────────────────────
        EditText etPrecio = null;
        CheckBox cbPista  = null;
        if (!esSolicitar) {
            addSpacer(form, 10);
            form.addView(crearLabel(getString(R.string.zona_clase_precio)));
            etPrecio = crearEditText("Ej: 30", true);
            form.addView(etPrecio);

            addSpacer(form, 8);
            cbPista = new CheckBox(this);
            cbPista.setText(getString(R.string.zona_clase_incluye_pista));
            cbPista.setTextColor(getResources().getColor(R.color.white, getTheme()));
            cbPista.setButtonTintList(ColorStateList.valueOf(0xFFFB8C00));
            cbPista.setTextSize(14f);
            form.addView(cbPista);
        }

        addSpacer(form, 10);
        form.addView(crearLabel(getString(R.string.zona_desc_opcional)));
        String hintDesc = esSolicitar
                ? "Ej: Llevo 2 años jugando, quiero mejorar el revés. Disponible tardes entre semana."
                : "Ej: Entrenador federado con 5 años de experiencia. Clases individuales y grupales. Metodología adaptada al nivel del alumno.";
        EditText etDesc = crearEditTextMultiline(hintDesc);
        form.addView(etDesc);

        outer.addView(sv);
        final AlertDialog[] dialogRef = {null};
        LinearLayout llBotones = crearFilaBotones();

        final EditText precioFinal = etPrecio;
        final CheckBox cbPistaFinal = cbPista;

        MaterialButton btnC = crearBotonSecundario(getString(R.string.btn_cancelar));
        LinearLayout.LayoutParams cP = new LinearLayout.LayoutParams(0, dpToPx(48), 1f);
        cP.setMarginEnd(dpToPx(8)); btnC.setLayoutParams(cP);
        btnC.setOnClickListener(v -> { if (dialogRef[0] != null) dialogRef[0].dismiss(); });

        MaterialButton btnPub = crearBotonPrimario(getString(R.string.zona_publicar));
        LinearLayout.LayoutParams pP = new LinearLayout.LayoutParams(0, dpToPx(48), 1f);
        pP.setMarginStart(dpToPx(8)); btnPub.setLayoutParams(pP);
        btnPub.setOnClickListener(v -> {
            if (ciudadSel[0].isEmpty()) {
                Toast.makeText(this, "Selecciona la localidad", Toast.LENGTH_SHORT).show();
                return;
            }
            StringBuilder sb = new StringBuilder();
            sb.append("@@LOCALIDAD:").append(ciudadSel[0]).append(", ").append(provinciaActual).append("\n");
            if (!pistaSel[0].isEmpty()) sb.append("@@PISTA:").append(pistaSel[0]).append("\n");
            if (!esSolicitar && precioFinal != null) {
                String precio = precioFinal.getText().toString().trim();
                if (!precio.isEmpty()) sb.append("@@PRECIO:").append(precio).append("€/h\n");
                sb.append("@@INCLUYE_PISTA:").append(cbPistaFinal != null && cbPistaFinal.isChecked() ? "Sí" : "No").append("\n");
            }
            String desc = etDesc.getText().toString().trim();
            if (!desc.isEmpty()) sb.append("@@DESC:").append(desc);

            // Convertir a formato legible para tarjeta estándar (no es magic card)
            String tipo = esSolicitar ? "CLASE_SOLICITAR" : "CLASE_OFRECER";
            long id = db.publicarAnuncio(miId, tipo, convertirClaseADesc(sb.toString()), provinciaActual);
            if (id != -1) {
                Toast.makeText(this, getString(R.string.zona_publicado), Toast.LENGTH_SHORT).show();
                if (dialogRef[0] != null) dialogRef[0].dismiss();
                cargarAnuncios();
            } else {
                Toast.makeText(this, getString(R.string.zona_error_publicar), Toast.LENGTH_SHORT).show();
            }
        });
        llBotones.addView(btnC); llBotones.addView(btnPub);
        outer.addView(llBotones);
        dialogRef[0] = mostrarDialog(outer);
    }

    // Convierte los tags @@XXX: al formato emoji legible que se muestra en la tarjeta estándar
    private String convertirClaseADesc(String tagged) {
        String localidad   = extraerTag(tagged, "LOCALIDAD");
        String pista       = extraerTag(tagged, "PISTA");
        String precio      = extraerTag(tagged, "PRECIO");
        String incluyePist = extraerTag(tagged, "INCLUYE_PISTA");
        String desc        = extraerTag(tagged, "DESC");

        StringBuilder sb = new StringBuilder();
        if (!localidad.isEmpty()) sb.append("📍 ").append(localidad).append("\n");
        if (!precio.isEmpty())    sb.append("💶 ").append(precio).append("\n");
        if (!pista.isEmpty())     sb.append("🏟️ ").append(pista).append("\n");
        if (!incluyePist.isEmpty()) {
            sb.append("Sí".equals(incluyePist) ? "✅" : "❌")
              .append(" Incluye pista: ").append(incluyePist).append("\n");
        }
        if (!desc.isEmpty()) sb.append("💬 ").append(desc);
        return sb.toString().trim();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Formulario PARTIDA: categoría, jugadores, miembros dinámicos, localidad API
    // ─────────────────────────────────────────────────────────────────────────

    private void mostrarFormularioPartida() {
        LinearLayout outer = crearDialogLayout();
        outer.addView(crearTituloDialog("🏆  " + getString(R.string.zona_rb_partida)));

        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        ScrollView sv = new ScrollView(this);
        sv.addView(form);
        sv.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(480)));

        // Categoría
        form.addView(crearLabel(getString(R.string.zona_partida_categoria)));
        EditText etCategoria = crearEditText("Ej: 3ª Mixta, 2ª Masculina…", false);
        form.addView(etCategoria);

        // Número de jugadores buscados
        addSpacer(form, 10);
        form.addView(crearLabel(getString(R.string.zona_partida_jugadores)));
        EditText etJugadores = crearEditText("Ej: 2", true);
        form.addView(etJugadores);

        // ── Sección de miembros dinámicos ───────────────────────────────────
        addSpacer(form, 12);
        TextView tvMiembrosLabel = crearLabel("👤 Miembros que traes contigo (opcional)");
        tvMiembrosLabel.setTextColor(0xFFFB8C00);
        form.addView(tvMiembrosLabel);

        LinearLayout llMiembros = new LinearLayout(this);
        llMiembros.setOrientation(LinearLayout.VERTICAL);
        form.addView(llMiembros);

        addSpacer(form, 6);
        MaterialButton btnAnyadirMiembro = crearBotonSecundario("➕ Añadir miembro");
        btnAnyadirMiembro.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(40)));
        btnAnyadirMiembro.setOnClickListener(v -> {
            if (llMiembros.getChildCount() >= 3) {
                Toast.makeText(this, "Máximo 3 miembros", Toast.LENGTH_SHORT).show();
                return;
            }
            llMiembros.addView(crearFilaMiembro(llMiembros));
        });
        form.addView(btnAnyadirMiembro);

        // ── Localidad y pista con APIs ──────────────────────────────────────
        final String[] ciudadSel = {""};
        final String[] pistaSel  = {""};
        agregarSeccionLocalidadPista(form, ciudadSel, pistaSel);

        // Descripción
        addSpacer(form, 10);
        form.addView(crearLabel(getString(R.string.zona_desc_opcional)));
        EditText etDesc = crearEditTextMultiline(
                "Ej: Buscamos pareja para amistoso el sábado a las 18h. " +
                "Nivel 3ª. Somos serios y puntuales. Traemos pelotas.");
        form.addView(etDesc);

        outer.addView(sv);
        final AlertDialog[] dialogRef = {null};
        LinearLayout llBotones = crearFilaBotones();

        MaterialButton btnC = crearBotonSecundario(getString(R.string.btn_cancelar));
        LinearLayout.LayoutParams cP = new LinearLayout.LayoutParams(0, dpToPx(48), 1f);
        cP.setMarginEnd(dpToPx(8)); btnC.setLayoutParams(cP);
        btnC.setOnClickListener(v -> { if (dialogRef[0] != null) dialogRef[0].dismiss(); });

        MaterialButton btnPub = crearBotonPrimario(getString(R.string.zona_publicar));
        LinearLayout.LayoutParams pP = new LinearLayout.LayoutParams(0, dpToPx(48), 1f);
        pP.setMarginStart(dpToPx(8)); btnPub.setLayoutParams(pP);
        btnPub.setOnClickListener(v -> {
            if (ciudadSel[0].isEmpty()) {
                Toast.makeText(this, "Selecciona la localidad", Toast.LENGTH_SHORT).show();
                return;
            }
            String categoria = etCategoria.getText().toString().trim();
            String jugadores = etJugadores.getText().toString().trim();
            String desc      = etDesc.getText().toString().trim();

            StringBuilder sb = new StringBuilder();
            if (!categoria.isEmpty()) sb.append("🏆 ").append(categoria).append("\n");
            sb.append("👥 ").append(jugadores.isEmpty() ? "1" : jugadores)
              .append(" jugador(es) buscado(s)\n");
            sb.append("📍 ").append(ciudadSel[0]).append(", ").append(provinciaActual).append("\n");
            if (!pistaSel[0].isEmpty()) sb.append("🏟️ ").append(pistaSel[0]).append("\n");

            // Recopilamos los miembros añadidos dinámicamente
            for (int i = 0; i < llMiembros.getChildCount(); i++) {
                View fila = llMiembros.getChildAt(i);
                if (fila.getTag() instanceof EditText[]) {
                    EditText[] fields = (EditText[]) fila.getTag();
                    String nom = fields[0].getText().toString().trim();
                    String cat = fields[1].getText().toString().trim();
                    if (!nom.isEmpty()) {
                        sb.append("👤 ").append(nom);
                        if (!cat.isEmpty()) sb.append(" (").append(cat).append(")");
                        sb.append("\n");
                    }
                }
            }
            if (!desc.isEmpty()) sb.append("💬 ").append(desc);

            long id = db.publicarAnuncio(miId, "PARTIDA", sb.toString().trim(), provinciaActual);
            if (id != -1) {
                Toast.makeText(this, getString(R.string.zona_publicado), Toast.LENGTH_SHORT).show();
                if (dialogRef[0] != null) dialogRef[0].dismiss();
                cargarAnuncios();
            } else {
                Toast.makeText(this, getString(R.string.zona_error_publicar), Toast.LENGTH_SHORT).show();
            }
        });
        llBotones.addView(btnC); llBotones.addView(btnPub);
        outer.addView(llBotones);
        dialogRef[0] = mostrarDialog(outer);
    }

    // Crea una fila dinámica de miembro con nombre + categoría + botón eliminar.
    // El tag de la vista raíz es EditText[]{etNombre, etCategoria} para leerlos al publicar.
    private View crearFilaMiembro(LinearLayout parent) {
        LinearLayout fila = new LinearLayout(this);
        fila.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable border = new GradientDrawable();
        border.setStroke(dpToPx(1), 0xFF333333);
        border.setCornerRadius(dpToPx(8));
        fila.setBackground(border);
        fila.setPadding(dpToPx(10), dpToPx(8), dpToPx(10), dpToPx(8));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dpToPx(8), 0, 0);
        fila.setLayoutParams(lp);

        // Cabecera de fila: etiqueta + botón ✕
        LinearLayout filaHeader = new LinearLayout(this);
        filaHeader.setOrientation(LinearLayout.HORIZONTAL);
        filaHeader.setGravity(Gravity.CENTER_VERTICAL);

        TextView tvLabel = new TextView(this);
        tvLabel.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        tvLabel.setText("👤 Miembro " + (parent.getChildCount() + 1));
        tvLabel.setTextColor(0xFFFB8C00);
        tvLabel.setTextSize(12f); tvLabel.setTypeface(null, Typeface.BOLD);

        MaterialButton btnElim = crearBotonSecundario("✕");
        LinearLayout.LayoutParams bP = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, dpToPx(30));
        btnElim.setLayoutParams(bP);
        btnElim.setTextSize(12f);
        btnElim.setOnClickListener(v -> parent.removeView(fila));

        filaHeader.addView(tvLabel); filaHeader.addView(btnElim);
        fila.addView(filaHeader);

        // Nombre
        EditText etNombre = crearEditText("Nombre del miembro", false);
        LinearLayout.LayoutParams nP = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        nP.setMargins(0, dpToPx(6), 0, 0); etNombre.setLayoutParams(nP);
        fila.addView(etNombre);

        // Categoría
        EditText etCat = crearEditText("Categoría (ej: 3ª Mixta)", false);
        LinearLayout.LayoutParams cLP = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cLP.setMargins(0, dpToPx(5), 0, 0); etCat.setLayoutParams(cLP);
        fila.addView(etCat);

        // Guardamos las referencias a los EditTexts en el tag de la fila para leerlos al publicar
        fila.setTag(new EditText[]{etNombre, etCat});
        return fila;
    }

    // Bloque reutilizable: label provincia (read-only) + selector ciudad API INE + selector pista API Overpass
    private void agregarSeccionLocalidadPista(LinearLayout form,
                                               String[] ciudadSel, String[] pistaSel) {
        // Provincia: texto informativo, no editable (ya está fijada por el contexto)
        addSpacer(form, 10);
        form.addView(crearLabel(getString(R.string.edit_provincia)));
        TextView tvProv = new TextView(this);
        tvProv.setText("📌 " + (provinciaActual != null ? provinciaActual : "—"));
        tvProv.setTextColor(getResources().getColor(R.color.verde_lima_suave, getTheme()));
        tvProv.setTextSize(14f); tvProv.setTypeface(null, Typeface.BOLD);
        tvProv.setPadding(dpToPx(4), dpToPx(6), dpToPx(4), dpToPx(6));
        form.addView(tvProv);

        // Selector de localidad via API INE
        addSpacer(form, 10);
        form.addView(crearLabel("🏙️ Ciudad / Pueblo  ·  (requiere conexión)"));
        MaterialButton btnCiudad = crearBotonSecundario("📍 Seleccionar ciudad / pueblo  ▾");
        btnCiudad.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(44)));
        btnCiudad.setOnClickListener(v ->
            mostrarSelectorLocalidad(provinciaActual, btnCiudad, item -> {
                ciudadSel[0] = item;
                // Al cambiar la ciudad reseteamos la pista
                pistaSel[0] = "";
                // btnPista se referencia más abajo, se actualiza en el callback
            })
        );
        form.addView(btnCiudad);

        // Selector de pista via API Overpass/OSM
        addSpacer(form, 10);
        form.addView(crearLabel("🏟️ Pista de pádel  ·  (requiere ciudad seleccionada)"));
        MaterialButton btnPista = crearBotonSecundario("🏟️ Seleccionar pista  ▾");
        btnPista.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(44)));
        btnPista.setOnClickListener(v ->
            mostrarSelectorPista(ciudadSel[0], btnPista, item -> pistaSel[0] = item)
        );
        form.addView(btnPista);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // API: municipios del INE + pistas de pádel via Overpass/OSM
    // ─────────────────────────────────────────────────────────────────────────

    // Muestra un diálogo con todos los municipios de la provincia actual.
    // La carga es asíncrona (Thread): mientras descarga desactiva el botón.
    private void mostrarSelectorLocalidad(String provincia, MaterialButton btn,
                                           OnItemSelected callback) {
        if (provincia == null || provincia.isEmpty()) {
            Toast.makeText(this, "Selecciona primero una provincia", Toast.LENGTH_SHORT).show();
            return;
        }
        String textoOrig = btn.getText().toString();
        btn.setText("⏳ Cargando municipios…"); btn.setEnabled(false);

        new Thread(() -> {
            List<String> municipios = fetchMunicipiosINE(provincia);
            runOnUiThread(() -> {
                btn.setEnabled(true); btn.setText(textoOrig);
                if (municipios.isEmpty()) {
                    Toast.makeText(this,
                            "No se pudo cargar la lista. Comprueba tu conexión.",
                            Toast.LENGTH_LONG).show();
                } else {
                    mostrarDialogoLista("📍 Ciudad / Pueblo en " + provincia, municipios, item -> {
                        btn.setText("📍 " + item);
                        callback.onSelected(item);
                    });
                }
            });
        }).start();
    }

    // Busca pistas de pádel en la ciudad/pueblo seleccionado usando la API Overpass (OpenStreetMap).
    // Si no encuentra resultados ofrece entrada manual.
    private void mostrarSelectorPista(String ciudad, MaterialButton btn,
                                       OnItemSelected callback) {
        if (ciudad == null || ciudad.isEmpty()) {
            Toast.makeText(this, "Selecciona primero la ciudad/pueblo", Toast.LENGTH_SHORT).show();
            return;
        }
        String textoOrig = btn.getText().toString();
        btn.setText("⏳ Buscando pistas…"); btn.setEnabled(false);

        new Thread(() -> {
            List<String> pistas = fetchPistasOverpass(ciudad);
            runOnUiThread(() -> {
                btn.setEnabled(true); btn.setText(textoOrig);
                if (pistas.isEmpty()) {
                    // Sin resultados de OSM: ofrecemos entrada manual directamente
                    mostrarDialogoPistaManual(btn, callback);
                } else {
                    // Añadimos opción de escribir manualmente al final de la lista
                    pistas.add("✏️  Escribir manualmente…");
                    mostrarDialogoLista("🏟️ Pistas en " + ciudad, pistas, item -> {
                        if (item.startsWith("✏️")) {
                            mostrarDialogoPistaManual(btn, callback);
                        } else {
                            btn.setText("🏟️ " + item);
                            callback.onSelected(item);
                        }
                    });
                }
            });
        }).start();
    }

    // Diálogo de entrada manual de pista cuando OSM no devuelve resultados
    private void mostrarDialogoPistaManual(MaterialButton btn, OnItemSelected callback) {
        EditText et = new EditText(this);
        et.setHint("Nombre de la pista o club");
        et.setTextColor(Color.WHITE); et.setHintTextColor(0xFF888888);
        LinearLayout container = new LinearLayout(this);
        container.setPadding(dpToPx(20), dpToPx(16), dpToPx(20), 0);
        container.addView(et);
        new AlertDialog.Builder(this)
            .setTitle("🏟️ Introduce la pista")
            .setView(container)
            .setPositiveButton("OK", (d, w) -> {
                String text = et.getText().toString().trim();
                if (!text.isEmpty()) { btn.setText("🏟️ " + text); callback.onSelected(text); }
            })
            .setNegativeButton("Cancelar", null)
            .show();
    }

    // Llama a la API del INE y devuelve la lista de municipios ordenada alfabéticamente.
    // Se ejecuta en un hilo de fondo; devuelve lista vacía si hay error de red.
    private List<String> fetchMunicipiosINE(String provincia) {
        List<String> result = new ArrayList<>();
        try {
            String codigo = CODIGO_INE.get(provincia);
            if (codigo == null) return result;
            URL url = new URL("https://servicios.ine.es/wstempus/js/ES/MUNICIPIOS/" + codigo);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(10000); conn.setReadTimeout(15000);
            conn.setRequestProperty("Accept", "application/json");
            if (conn.getResponseCode() != 200) return result;

            StringBuilder sb = new StringBuilder();
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), "UTF-8"));
            String line; while ((line = reader.readLine()) != null) sb.append(line);
            reader.close();

            JSONArray arr = new JSONArray(sb.toString());
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                if (obj.has("NOMBRE")) {
                    String nombre = obj.getString("NOMBRE").trim();
                    if (!nombre.isEmpty()) result.add(nombre);
                }
            }
            Collections.sort(result, (a, b) -> a.compareToIgnoreCase(b));
        } catch (Exception ignored) {}
        return result;
    }

    // Busca instalaciones de pádel en la ciudad mediante la API Overpass (datos de OpenStreetMap).
    // Devuelve los nombres de los locales encontrados, vacía si error o sin resultados.
    private List<String> fetchPistasOverpass(String ciudad) {
        List<String> pistas = new ArrayList<>();
        try {
            // Query: todas las entidades etiquetadas sport=padel dentro del área con ese nombre
            String query = "[out:json][timeout:20];" +
                    "area[\"name\"=\"" + ciudad.replace("\"", "\\\"") + "\"]->.a;" +
                    "nwr[\"sport\"=\"padel\"](area.a);" +
                    "out center;";
            String urlStr = "https://overpass-api.de/api/interpreter?data=" +
                    URLEncoder.encode(query, "UTF-8");
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(15000); conn.setReadTimeout(25000);
            conn.setRequestProperty("User-Agent", "PadelDart-TFG/1.0");
            if (conn.getResponseCode() != 200) return pistas;

            StringBuilder sb = new StringBuilder();
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), "UTF-8"));
            String line; while ((line = reader.readLine()) != null) sb.append(line);
            reader.close();

            JSONObject response = new JSONObject(sb.toString());
            JSONArray elements  = response.getJSONArray("elements");
            for (int i = 0; i < elements.length(); i++) {
                JSONObject el = elements.getJSONObject(i);
                if (el.has("tags")) {
                    JSONObject tags = el.getJSONObject("tags");
                    if (tags.has("name")) {
                        String name = tags.getString("name").trim();
                        if (!name.isEmpty() && !pistas.contains(name)) pistas.add(name);
                    }
                }
            }
            Collections.sort(pistas, (a, b) -> a.compareToIgnoreCase(b));
        } catch (Exception ignored) {}
        return pistas;
    }

    // Muestra un AlertDialog con lista de ítems simples y llama a callback al seleccionar.
    private void mostrarDialogoLista(String titulo, List<String> items,
                                     OnItemSelected callback) {
        String[] arr = items.toArray(new String[0]);
        new AlertDialog.Builder(this)
                .setTitle(titulo)
                .setItems(arr, (d, which) -> callback.onSelected(arr[which]))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers de foto y parsing
    // ─────────────────────────────────────────────────────────────────────────

    // Copia una foto de anuncio al almacenamiento interno para que la URI nunca expire.
    // Devuelve la ruta file:// del archivo local, o null si falla.
    private String copiarFotoAnuncio(Uri origen, int slot) {
        try {
            File dir = new File(getFilesDir(), "anuncio_photos");
            if (!dir.exists()) dir.mkdirs();
            File dest = new File(dir, "anuncio_" + System.currentTimeMillis() + "_" + slot + ".jpg");
            InputStream is = getContentResolver().openInputStream(origen);
            Bitmap bmp = BitmapFactory.decodeStream(is);
            if (bmp == null) return null;
            FileOutputStream fos = new FileOutputStream(dest);
            bmp.compress(Bitmap.CompressFormat.JPEG, 88, fos);
            fos.flush(); fos.close();
            return dest.toURI().toString();
        } catch (Exception e) { return null; }
    }

    // Carga un Bitmap desde una URI string (content:// o file://).
    // Devuelve null si la URI es inválida o el archivo no existe.
    private Bitmap loadBitmap(String uriStr) {
        if (uriStr == null || uriStr.isEmpty()) return null;
        try {
            InputStream is;
            Uri uri = Uri.parse(uriStr);
            if ("file".equals(uri.getScheme())) {
                is = new FileInputStream(new File(uri.getPath()));
            } else {
                is = getContentResolver().openInputStream(uri);
            }
            if (is == null) return null;
            Bitmap bmp = BitmapFactory.decodeStream(is);
            is.close();
            return bmp;
        } catch (Exception e) { return null; }
    }

    // Extrae el valor de un tag @@TAG:valor\n de la descripción estructurada.
    // Devuelve "" si el tag no existe en el texto.
    private String extraerTag(String texto, String tag) {
        if (texto == null || texto.isEmpty()) return "";
        String prefix = "@@" + tag + ":";
        int idx = texto.indexOf(prefix);
        if (idx < 0) return "";
        int end = texto.indexOf("\n", idx);
        String val = end < 0 ? texto.substring(idx + prefix.length())
                             : texto.substring(idx + prefix.length(), end);
        return val.trim();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers de construcción de UI para los formularios
    // ─────────────────────────────────────────────────────────────────────────

    private LinearLayout crearDialogLayout() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dpToPx(24), dpToPx(24), dpToPx(24), dpToPx(20));
        GradientDrawable fondo = new GradientDrawable();
        fondo.setColor(0xFF0D0D0D); fondo.setCornerRadius(dpToPx(24));
        layout.setBackground(fondo);
        return layout;
    }

    private TextView crearTituloDialog(String texto) {
        TextView tv = new TextView(this);
        tv.setText(texto);
        tv.setTextColor(getResources().getColor(R.color.verde_lima_brillante, getTheme()));
        tv.setTextSize(18f); tv.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        p.setMargins(0, 0, 0, dpToPx(16)); tv.setLayoutParams(p);
        return tv;
    }

    private TextView crearLabel(String texto) {
        TextView tv = new TextView(this);
        tv.setText(texto);
        tv.setTextColor(getResources().getColor(R.color.verde_lima_suave, getTheme()));
        tv.setTextSize(12f); tv.setTypeface(null, Typeface.BOLD);
        return tv;
    }

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

    private EditText crearEditTextMultiline(String hint) {
        EditText et = new EditText(this);
        et.setHint(hint); et.setHintTextColor(0xFF666666);
        et.setTextColor(getResources().getColor(R.color.white, getTheme()));
        et.setMinLines(2); et.setMaxLines(4);
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

    private MaterialButton crearBotonPrimario(String texto) {
        MaterialButton btn = new MaterialButton(this);
        btn.setText(texto); btn.setTextColor(getResources().getColor(R.color.fondo_oscuro, getTheme()));
        btn.setBackgroundTintList(ColorStateList.valueOf(
                getResources().getColor(R.color.verde_lima, getTheme())));
        btn.setCornerRadius(dpToPx(12)); btn.setTextSize(14f);
        btn.setTypeface(null, Typeface.BOLD);
        return btn;
    }

    private MaterialButton crearBotonSecundario(String texto) {
        MaterialButton btn = new MaterialButton(this);
        btn.setText(texto); btn.setTextColor(getResources().getColor(R.color.texto_gris, getTheme()));
        btn.setBackgroundTintList(ColorStateList.valueOf(
                getResources().getColor(R.color.fondo_tarjeta_claro, getTheme())));
        btn.setCornerRadius(dpToPx(12)); btn.setTextSize(13f);
        return btn;
    }

    private LinearLayout crearFilaBotones() {
        LinearLayout ll = new LinearLayout(this);
        ll.setOrientation(LinearLayout.HORIZONTAL); ll.setWeightSum(2f);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        p.setMargins(0, dpToPx(16), 0, 0); ll.setLayoutParams(p);
        return ll;
    }

    private AlertDialog mostrarDialog(LinearLayout layout) {
        AlertDialog dialog = new AlertDialog.Builder(this).setView(layout).create();
        dialog.show();
        if (dialog.getWindow() != null)
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        return dialog;
    }

    private void addSpacer(LinearLayout parent, int dp) {
        View spacer = new View(this);
        spacer.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(dp)));
        parent.addView(spacer);
    }

    // Chip pequeño de color con texto (para marca/modelo en la magic card)
    private View crearChip(String texto, int color) {
        TextView tv = new TextView(this);
        tv.setText(texto);
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(11f); tv.setTypeface(null, Typeface.BOLD);
        tv.setPadding(dpToPx(10), dpToPx(4), dpToPx(10), dpToPx(4));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(color); bg.setCornerRadius(dpToPx(12));
        tv.setBackground(bg);
        return tv;
    }

    // Helpers para crear tarjetas y sus contenedores internos (evita repetición en agregarTarjetaAnuncio)
    private MaterialCardView crearCard(int marginBottom) {
        MaterialCardView card = new MaterialCardView(this);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        p.setMargins(0, 0, 0, marginBottom); card.setLayoutParams(p);
        card.setRadius(dpToPx(12)); card.setCardElevation(dpToPx(4));
        card.setCardBackgroundColor(getResources().getColor(R.color.fondo_tarjeta, getTheme()));
        return card;
    }

    private LinearLayout crearInnerLayout(int paddingV) {
        LinearLayout inner = new LinearLayout(this);
        inner.setOrientation(LinearLayout.VERTICAL);
        inner.setPadding(dpToPx(16), paddingV, dpToPx(16), paddingV);
        return inner;
    }

    private TextView crearFecha(long timestamp) {
        TextView tvFecha = new TextView(this);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        p.setMargins(0, dpToPx(8), 0, 0); tvFecha.setLayoutParams(p);
        tvFecha.setText(new SimpleDateFormat("dd MMM yyyy · HH:mm", Locale.getDefault())
                .format(new Date(timestamp)));
        tvFecha.setTextColor(getResources().getColor(R.color.texto_gris_suave, getTheme()));
        tvFecha.setTextSize(11f); tvFecha.setGravity(Gravity.END);
        return tvFecha;
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

    private int colorTipo(String tipo) {
        switch (tipo != null ? tipo : "") {
            case "PARTIDA":         return 0xFFFF5252;
            case "PALA":            return 0xFF2196F3;
            case "CLASE_OFRECER":   return 0xFFFF9800;
            case "CLASE_SOLICITAR": return 0xFF9C27B0;
            default:                return 0xFF666666;
        }
    }

    private void mostrarMensaje(String msg) {
        TextView tv = new TextView(this);
        tv.setText(msg);
        tv.setTextColor(getResources().getColor(R.color.texto_gris, getTheme()));
        tv.setTextSize(14f); tv.setGravity(Gravity.CENTER);
        tv.setPadding(dpToPx(16), dpToPx(48), dpToPx(16), dpToPx(16));
        llAnuncios.addView(tv);
    }

    private String strSafe(Object obj) {
        return (obj instanceof String) ? (String) obj : "";
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}
