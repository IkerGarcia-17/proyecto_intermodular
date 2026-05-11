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

    // Modelos por marca — cobertura 2020-2026 (≥70 % del catálogo real por marca)
    // Al mostrarse se añade dinámicamente "✏️ Otro (escribir a mano)" al final de cada lista.
    private static final Map<String, String[]> MODELOS_POR_MARCA = new LinkedHashMap<>();
    static {
        // ── BULLPADEL ─────────────────────────────────────────────────────────
        MODELOS_POR_MARCA.put("Bullpadel", new String[]{
            // Hack (serie top, 2020-2025)
            "Hack 04","Hack 04 Comfort","Hack 04 Junior",
            "Hack 03","Hack 03 Comfort","Hack 03 Junior",
            "Hack Control 03","Hack Hybrid 03","Hack LTD 2024",
            "Hack 02","Hack 02 Comfort",
            // Vertex (potencia, 2020-2025)
            "Vertex 05","Vertex 05 Comfort",
            "Vertex 04","Vertex 04 Comfort",
            "Vertex 03","Vertex 03 Comfort",
            // Magnum / Neuron / Kiowa / Indiga / Gbomi (2021-2025)
            "Magnum 04","Magnum 03","Magnum 02","Magnum Light 03",
            "Neuron 04","Neuron 03","Neuron 02",
            "Kiowa 04","Kiowa 03","Kiowa 02",
            "Indiga 04","Indiga 03","Indiga 02",
            "Gbomi 03","Gbomi 02",
            // Otras (2022-2025)
            "Flow Light 04","Flow Light 03",
            "Crossfire 04","Crossfire 03",
            "Trilogy Luxury 2025","Trilogy Luxury"
        });

        // ── HEAD ──────────────────────────────────────────────────────────────
        MODELOS_POR_MARCA.put("Head", new String[]{
            // Delta (serie control, 2020-2025)
            "Delta Pro 2025","Delta Pro 2024","Delta Pro 2023","Delta Pro 2022","Delta Pro 2021",
            "Delta Elite 2025","Delta Elite 2024","Delta Elite 2023","Delta Elite 2022",
            "Delta Hybrid 2024","Delta Motion 2024","Delta Motion 2023",
            "Delta Tour","Delta Team","Delta Junior",
            // Flash (2020-2025)
            "Flash Pro 2025","Flash Pro 2024","Flash Pro 2023","Flash Pro 2022",
            "Flash Elite 2024","Flash Hybrid 2024","Flash Hybrid 2023","Flash Team",
            // Alpha (2020-2025)
            "Alpha Pro 2025","Alpha Pro 2024","Alpha Pro 2023","Alpha Pro 2022",
            "Alpha Elite 2024","Alpha Elite 2023","Alpha Motion 2024",
            "Alpha Tour","Alpha Team","Alpha Junior",
            // Speed (2020-2024)
            "Speed Pro 2024","Speed Pro 2023","Speed Elite 2024",
            "Speed Team","Speed Light","Speed Junior",
            // Otras (2022-2025)
            "Zephyr Pro 2025","Zephyr Pro 2024","Gravity Pro 2024","Instinct Pro 2024",
            "Graphene 360+ Alpha Elite","Graphene 360+ Delta Pro"
        });

        // ── ADIDAS ────────────────────────────────────────────────────────────
        MODELOS_POR_MARCA.put("Adidas", new String[]{
            // Metalbone (2020-2025)
            "Metalbone 3.4 HRD","Metalbone 3.3 HRD","Metalbone 3.2 HRD","Metalbone 3.1 HRD",
            "Metalbone CTRL 3.4","Metalbone CTRL 3.3","Metalbone CTRL 3.2",
            "Metalbone Light 3.3","Metalbone Light 3.2",
            // Adipower Multiweight (2021-2025)
            "Adipower Multiweight 3.4","Adipower Multiweight 3.3",
            "Adipower Multiweight CTRL 3.4","Adipower Multiweight CTRL 3.3",
            "Adipower Multiweight Light 3.3",
            // Adipower Team / Junior (2021-2024)
            "Adipower Team 3.3","Adipower Team CTRL 3.3",
            "Adipower Light 3.3","Adipower Junior 3.3",
            // Essnova (2023-2025)
            "Essnova Carbon 3.4","Essnova Carbon CTRL 3.4",
            "Essnova Carbon 3.3","Essnova Carbon CTRL 3.3","Essnova Light 3.3",
            // Drive / RX / Match (2022-2025)
            "Drive 3.3","Drive Light 3.3",
            "RX 3.3","RX Light 3.3",
            "Match Light 3.3","Match 3.3","Match Junior"
        });

        // ── NOX ───────────────────────────────────────────────────────────────
        MODELOS_POR_MARCA.put("NOX", new String[]{
            // AT10 / AT11 (2020-2025)
            "AT11 Luxury Carbon 2025","AT11 Genius 18K 2025",
            "AT10 Luxury Carbon 2024","AT10 Genius 18K 2024","AT10 Genius 12K 2024",
            "AT10 Luxury Carbon 2023","AT10 Genius 18K 2023",
            "AT10 Luxury Carbon 2022","AT10 Genius 18K 2022",
            "AT10 Luxury Carbon 2021","AT10 Evolution 2020",
            // ML10 (2020-2025)
            "ML10 Pro Cup Luxury 2025","ML10 Pro Cup 3K 2025",
            "ML10 Pro Cup Luxury 2024","ML10 Pro Cup 3K 2024","ML10 Shotgun 3K 2024",
            "ML10 Pro Cup 3K 2023","ML10 Pro Cup 3K 2022","ML10 Pro Cup 2021",
            // VK10 (2024-2025)
            "VK10 Luxury Carbon 2025","VK10 Luxury Carbon 2024",
            // Stinger (2022-2025)
            "Stinger Luxury Carbon 2025","Stinger Luxury Carbon 2024",
            "Stinger 18K","Stinger WPT",
            // Equation / Nerbo / Casual (2022-2025)
            "Equation WPT 2025","Equation WPT 2024","Equation LTD",
            "Nerbo WPT 2025","Nerbo WPT 2024","Nerbo Luxury",
            "Casual Genius 2024","Casual WPT",
            // X-One (2023-2025)
            "X-One Luxury 2025","X-One Exever 2024","X-One Compact","X-One 5.1"
        });

        // ── BABOLAT ───────────────────────────────────────────────────────────
        MODELOS_POR_MARCA.put("Babolat", new String[]{
            // Veron (2020-2025)
            "Technical Veron 2025","Technical Veron 2024","Technical Veron 2023",
            "Air Veron 2025","Air Veron 2024","Air Veron 2023","Air Veron 2022",
            "Counter Veron 2024","Counter Veron 2023",
            "Juan Lebron Signature 2024","Juan Lebron Signature 2023",
            // Viper (2020-2025)
            "Technical Viper 2025","Technical Viper 2024","Technical Viper 2023",
            "Air Viper 2024","Air Viper 2023",
            "Counter Viper 2024","Counter Viper 2023",
            // Vertuo (2022-2025)
            "Technical Vertuo 2025","Technical Vertuo 2024","Technical Vertuo 2023",
            "Air Vertuo 2024","Air Vertuo 2023",
            // Defiance / Falcon (2022-2025)
            "Defiance Carbon 2025","Defiance Carbon 2024","Defiance Hybrid 2024",
            "Falcon Carbon 2025","Falcon Carbon 2024","Falcon Hybrid 2024",
            // Gama media (2020-2024)
            "Reveal 2024","Reveal 2023","Reflex 2024","Revo 2024","Fly Spirit","Fly Team"
        });

        // ── WILSON ────────────────────────────────────────────────────────────
        MODELOS_POR_MARCA.put("Wilson", new String[]{
            // Bela (2020-2025)
            "Bela Elite V3 2025","Bela Elite V2","Bela Elite V1",
            "Bela Pro V3 2025","Bela Pro V2","Bela Pro V1",
            "Bela Tour V2","Bela Team V2","Bela CTRL V2","Bela Junior",
            "Bela Carbon 2025","Bela Carbon 2024",
            // Ultra (2020-2025)
            "Ultra Spin 2025","Ultra Spin 2024","Ultra Spin 2023",
            "Ultra Control 2024","Ultra Control 2023","Ultra Indoor 2024",
            // Force / Blade / Envy (2021-2025)
            "Force Pro V2","Force Pro V1","Force Team","Force Light",
            "Blade V2","Blade Team V2",
            "Envy Carbon 2025","Envy Carbon 2024","Envy Team 2024"
        });

        // ── SIUX ──────────────────────────────────────────────────────────────
        MODELOS_POR_MARCA.put("Siux", new String[]{
            // Electra (2021-2025)
            "Electra Carbon 2025 24K","Electra Carbon 2025 16K",
            "Electra Carbon 24K 2024","Electra Carbon 16K 2024","Electra 3K 2024",
            "Electra Carbon 24K 2023","Electra Carbon 16K 2023",
            "Electra Carbon 2022","Electra Carbon 2021",
            // Carbone (2021-2025)
            "Carbone Elite 2025 14K","Carbone Elite 14K 2024","Carbone 3K 2024",
            "Carbone Elite 2023","Carbone Elite 2022",
            // Diablo (2020-2025)
            "Diablo Gloss Attack 2025","Diablo Gloss Attack 2024","Diablo Gloss 2024",
            "Diablo Attack 2024","Diablo 3K 2024","Diablo 3K 2023","Diablo 3K 2022",
            // Hurricane / Fenix / Trident / Pegasus (2020-2025)
            "Hurricane Carbon 2025","Hurricane Carbon 2024","Hurricane 2024","Hurricane 2022",
            "Fenix Luxury 2024","Fenix Carbon 2024","Fenix 3K 2024","Fenix 2023",
            "Trident Carbon 2025","Trident Carbon 2024","Trident 2024",
            "Pegasus 3K 2024","Pegasus 2023"
        });

        // ── VARLION ───────────────────────────────────────────────────────────
        MODELOS_POR_MARCA.put("Varlion", new String[]{
            // Lethal Weapon (2021-2025)
            "Lethal Weapon Carbon LW 2025","Lethal Weapon Carbon LW 2024",
            "Lethal Weapon 3K 2024","Lethal Weapon 3K 2023","Lethal Weapon 2022",
            // Avant (2020-2025)
            "Avant Carbon LW 2025","Avant Carbon LW 2024","Avant Carbon LW 2023",
            "Avant Hexagon LW 2024","Avant Hexagon LW 2023","Avant LW 2022","Avant LW 2021",
            // Summum / Bourne (2020-2024)
            "Summum Carbon 8 2024","Summum Carbon 8 2023","Summum Power 2022",
            "Bourne Hexagon Carbon 2024","Bourne LW 2023","Bourne 2022",
            // LW Comfort / Junior (2020-2024)
            "LW Comfort Carbon 2024","LW Comfort 2024","LW Comfort 2023","LW Junior"
        });

        // ── DUNLOP ────────────────────────────────────────────────────────────
        MODELOS_POR_MARCA.put("Dunlop", new String[]{
            // Speed Max (2020-2025)
            "Speed Max Carbon 2025","Speed Max Carbon 2024","Speed Max Carbon 2023",
            "Speed Max Pro 2024","Speed Max Pro 2023","Speed Max 2022","Speed Max 2021",
            // Speed Ultra (2021-2025)
            "Speed Ultra Carbon 2025","Speed Ultra Carbon 2024","Speed Ultra Carbon 2023",
            "Speed Ultra 2022","Speed Ultra 2021",
            // Speed Pro / Elite / Fast (2020-2025)
            "Speed Pro 2025","Speed Pro 2024","Speed Pro 2023","Speed Pro 2022",
            "Speed Elite 2024","Speed Elite 2023",
            "Speed Fast 2024","Speed Fast 2023","Speed Lite 2024","Speed Junior",
            // Otras (2020-2024)
            "Inferno Carbon 2024","Inferno Carbon 2023",
            "Aero Star 2024","Galaxy 2.0","Galaxy Pro"
        });

        // ── STARVIE ───────────────────────────────────────────────────────────
        MODELOS_POR_MARCA.put("Starvie", new String[]{
            // Metheora (2021-2025)
            "Metheora Pro 2025","Metheora 3K 2025",
            "Metheora Pro 2024","Metheora 3K 2024","Metheora LTD 2024",
            "Metheora Pro 2023","Metheora 3K 2023","Metheora 2022",
            // Basalto (2021-2025)
            "Basalto Astrum 2025","Basalto Astrum 2024","Basalto Pro 2024",
            "Basalto Draco 2024","Basalto 2023","Basalto Pro 2022",
            // Astrum (2020-2024)
            "Astrum 3K 2024","Astrum Pro 2024","Astrum 3K 2023","Astrum 2022",
            // Raptor / Black Hole / Ariadne (2020-2025)
            "Raptor Endurance Pro 2025","Raptor Endurance Pro 2024","Raptor Attack 2024","Raptor 2023",
            "Black Hole Pro 2025","Black Hole Pro 2024","Black Hole 2.0","Black Hole 2023",
            "Ariadne Pro 2024","Ariadne Genius 2024","Ariadne 2023"
        });

        // ── BLACK CROWN ───────────────────────────────────────────────────────
        MODELOS_POR_MARCA.put("Black Crown", new String[]{
            // Piton Soft (2020-2025)
            "Piton 7.0 Soft 2025","Piton 7.0 Soft 2024","Piton 7.0 Soft 2023",
            "Piton 6.0 Soft 2024","Piton 5.0 Soft 2025","Piton 5.0 Soft 2024",
            "Piton 3.0 Soft",
            // Piton HRD (2022-2025)
            "Piton 7.0 HRD 2025","Piton 7.0 HRD 2024","Piton 5.0 HRD 2024",
            // Piton Attack (2022-2025)
            "Piton Attack 7.0 2025","Piton Attack 7.0 2024","Piton Attack 5.0 2024",
            // Quantum / Hurricane / Cobra / Mustang (2020-2025)
            "Quantum Power 2025","Quantum Power 2024","Quantum 10.0",
            "Hurricane Pro 2025","Hurricane Pro 2024","Hurricane 10.0",
            "Cobra Carbon 2024","Cobra Carbon 2023","Mustang 2024"
        });

        // ── ROYAL PADEL ───────────────────────────────────────────────────────
        MODELOS_POR_MARCA.put("Royal Padel", new String[]{
            // Whip (2020-2025)
            "Whip 03K Pro 2025","Whip 03K Pro 2024","Whip 03K 2024","Whip 03K 2023",
            "190 Whip 2025","190 Whip 2024","190 Whip 2023",
            "787 Whip 2024","787 Whip 2023",
            // M27 (2020-2025)
            "M27 Carbon Pro 2025","M27 Carbon Pro 2024","M27 Carbon Pro 2023",
            "M27 Polyethylene 2024","M27 Polyethylene 2023","M27 2022",
            // Ranger / Triumph / Victory (2021-2025)
            "Ranger 03K 2025","Ranger 03K 2024","Ranger 03K 2023","Ranger Pro 2024",
            "Triumph Pro 2025","Triumph Pro 2024","Triumph 2023","Victory 2024"
        });

        // ── WINGPADEL ─────────────────────────────────────────────────────────
        MODELOS_POR_MARCA.put("Wingpadel", new String[]{
            "W-Vulcan 3K 2025","W-Vulcan 3K 2024","W-Vulcan HRD","W-Vulcan Carbon",
            "W-Storm 3K 2025","W-Storm 3K 2024","W-Storm Carbon","W-Storm 2023",
            "W-Fire 3K 2025","W-Fire 3K 2024","W-Fire Carbon","W-Fire 2023",
            "W-Cobra 3K 2024","W-Cobra Carbon","W-Cobra 2023",
            "W-Eagle 3K 2024","W-Eagle 2023",
            "W-Panther Carbon 2024","W-Panther 2023"
        });

        // ── ARTENGO (Decathlon) ───────────────────────────────────────────────
        MODELOS_POR_MARCA.put("Artengo", new String[]{
            "PR990 Power 2025","PR990 Power 2024","PR990 Hybrid 2024",
            "PR990 Comfort 2024","PR990 Power 2023","PR990 2022",
            "PR860 Power 2025","PR860 Power 2024","PR860 Soft 2024",
            "PR860 Hybrid 2024","PR860 2023","PR860 2022",
            "PR790 Hybrid 2024","PR760 Power 2024",
            "PR590 2024","PR560 2024","PR190 Hybrid 2024",
            "PR190 Lite","PR130 2024","PR130 2023"
        });

        // ── KUIKMA (Decathlon) ────────────────────────────────────────────────
        MODELOS_POR_MARCA.put("Kuikma", new String[]{
            "PL 900 Carbon Pro 2025","PL 900 Carbon Pro 2024",
            "PL 900 Carbon 2025","PL 900 Carbon 2024","PL 900 Hybrid 2024",
            "PL 800 Carbon 2025","PL 800 Carbon 2024","PL 800 Hybrid 2024","PL 800 2023",
            "PL 700 Carbon 2024","PL 700 2024","PL 700 2023",
            "PL 590 Power 2024","PL 590 2024","PL 590 2023",
            "PL 500 2024","PL 500 2023","PL 350 2024"
        });

        // ── POWER PADEL ───────────────────────────────────────────────────────
        MODELOS_POR_MARCA.put("Power Padel", new String[]{
            "Master Carbon 18K 2025","Master Carbon 18K 2024",
            "Master Carbon 3K 2024","Master Carbon 3K 2023",
            "Master Pro 2025","Master Pro 2024","Master Pro 2023",
            "Classic Carbon 2024","Classic Carbon 2023",
            "Classic Pro 2024","Classic 2024","Classic 2023",
            "Warrior Carbon 2024","Warrior Pro 2024","Warrior 2023","Team 3.0"
        });

        // ── OXDOG ─────────────────────────────────────────────────────────────
        MODELOS_POR_MARCA.put("Oxdog", new String[]{
            "Ultrapower HES 8.1 2025","Ultrapower HES 8.1 2024",
            "Ultrapower HES 6.1 2024","Ultrapower HES 5.1 2024",
            "Ultra HES 7.1 2024","Ultra HES 5.1 2024","Ultra HES 4.1 2024",
            "Ultra HES 7.1 2023","Ultra HES 5.1 2023",
            "Vieille HES 5.1 2024","Vieille HES 4.1 2024","Vieille 4.1 2023","Vieille 3.1",
            "Hype HES 6.1 2024","Hype HES 5.1 2024","Hype HES 4.1 2024",
            "Hype HES 6.1 2023","Hype HES 5.1 2023",
            "Gt1 HES 2024","Gt1 HES 2023","Xsmash HES 2024"
        });

        // ── RS PADEL ──────────────────────────────────────────────────────────
        MODELOS_POR_MARCA.put("RS Padel", new String[]{
            "Gaucho Carbon Pro 2025","Gaucho Carbon Pro 2024",
            "Gaucho Carbon 2024","Gaucho Carbon 2023","Gaucho LTD 2024",
            "X Carbon Elite 2025","X Carbon Elite 2024","X Carbon Series 2024",
            "X Carbon Team 2024","X Carbon 2023",
            "Attacker Pro 2024","Attacker Carbon 2024","Attacker 2023",
            "Quantum Carbon 2024","Quantum 2024","Slam Pro 2024"
        });

        // ── ENEBE ─────────────────────────────────────────────────────────────
        MODELOS_POR_MARCA.put("Enebe", new String[]{
            "Tornado 9.1 2025","Tornado 9.1 2024","Tornado 7.1 2024","Tornado 5.1 2024",
            "Tornado 9.1 2023","Tornado 7.1 2023","Tornado 5.1 2023",
            "Overline 9.1 2024","Overline 7.1 2024","Overline 5.1 2024",
            "Overline 9.1 2023","Overline 7.1 2023",
            "Equinox 9.1 2024","Equinox 8.1 2024","Equinox 7.1 2024","Equinox 5.1 2024",
            "Equinox 9.1 2023","Equinox 8.1 2023",
            "Hurricane 9.1 2024","Hurricane 7.1 2024",
            "Telstar Carbon 2024","Star Carbon 2024"
        });

        // ── TIPSAPADEL ────────────────────────────────────────────────────────
        MODELOS_POR_MARCA.put("Tipsapadel", new String[]{
            "Quantum Pro Carbon 2025","Quantum Pro Carbon 2024",
            "Quantum Pro 2024","Quantum Carbon 2024","Quantum 2024","Quantum 2023",
            "Atom Pro Carbon 2025","Atom Pro Carbon 2024",
            "Atom Pro 2024","Atom Elite 2024","Atom Carbon 2024","Atom 2023",
            "Ion Pro 2024","Ion Carbon 2024","Ion 2024","Ion 2023",
            "Wave Pro 2024","Wave Carbon 2024","Wave 2024",
            "Photon Carbon 2024","Photon 2024"
        });
    }

    // Mapeo de nombres de provincia del app → nombre que usa OpenStreetMap.
    // Algunas provincias tienen nombre oficial distinto al topónimo local que OSM prioriza.
    // Solo se incluyen las que difieren; las demás usan el mismo nombre.
    private static final Map<String, String> NOMBRE_OSM_PROVINCIA = new LinkedHashMap<>();
    static {
        NOMBRE_OSM_PROVINCIA.put("La Coruña",  "A Coruña");
        NOMBRE_OSM_PROVINCIA.put("Gerona",     "Girona");
        NOMBRE_OSM_PROVINCIA.put("Lérida",     "Lleida");
        NOMBRE_OSM_PROVINCIA.put("Orense",     "Ourense");
        NOMBRE_OSM_PROVINCIA.put("Vizcaya",    "Bizkaia");
        NOMBRE_OSM_PROVINCIA.put("Guipúzcoa", "Gipuzkoa");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Municipios de España con infraestructura de pádel (≥3 instalaciones).
    // Datos recopilados de FEP, OSM y directorios de clubes 2024-2025.
    // Fuente local: sin llamada de red → respuesta instantánea.
    // ─────────────────────────────────────────────────────────────────────────
    private static final Map<String, List<String>> MUNICIPIOS_CON_PADEL = new LinkedHashMap<>();
    static {
        MUNICIPIOS_CON_PADEL.put("Álava", Arrays.asList(
            "Vitoria-Gasteiz","Llodio","Amurrio","Laudio","Salvatierra","Ayala","Iruña de Oca","Ribera Alta"
        ));
        MUNICIPIOS_CON_PADEL.put("Albacete", Arrays.asList(
            "Albacete","Hellín","Villarrobledo","Almansa","La Roda","Caudete","Tobarra","Balsa Nova","Chinchilla de Monte-Aragón"
        ));
        MUNICIPIOS_CON_PADEL.put("Alicante", Arrays.asList(
            "Alicante","Elche","Torrevieja","Orihuela","Benidorm","Alcoy","Villena","Elda","Petrer",
            "Denia","Jávea","Calpe","San Vicente del Raspeig","Santa Pola","Crevillent",
            "Guardamar del Segura","Mutxamel","Novelda","Rojales","Pilar de la Horadada",
            "Aspe","Ibi","Sax","Catral","Cox","Altea","Finestrat","Alfàs del Pi",
            "Teulada","Monóvar","Pego","El Campello","Torrevieja","Agost"
        ));
        MUNICIPIOS_CON_PADEL.put("Almería", Arrays.asList(
            "Almería","El Ejido","Roquetas de Mar","Vícar","Adra","Huércal-Overa","Berja",
            "Vera","Níjar","Pulpí","Cuevas del Almanzora","Olula del Río","Huércal de Almería",
            "Carboneras","Garrucha","Mojácar"
        ));
        MUNICIPIOS_CON_PADEL.put("Asturias", Arrays.asList(
            "Oviedo","Gijón","Avilés","Siero","Langreo","Mieres","Castrillón",
            "Corvera de Asturias","Llanera","Ribadesella","Navia","Cangas del Narcea","Llanes"
        ));
        MUNICIPIOS_CON_PADEL.put("Ávila", Arrays.asList(
            "Ávila","Arévalo","El Barco de Ávila","Piedrahíta","Candeleda"
        ));
        MUNICIPIOS_CON_PADEL.put("Badajoz", Arrays.asList(
            "Badajoz","Mérida","Don Benito","Almendralejo","Villanueva de la Serena",
            "Zafra","Montijo","Olivenza","Azuaga","Jerez de los Caballeros","Plasencia"
        ));
        MUNICIPIOS_CON_PADEL.put("Baleares", Arrays.asList(
            "Palma","Calvià","Llucmajor","Manacor","Inca","Maó","Eivissa",
            "Sant Antoni de Portmany","Marratxí","Felanitx","Pollença","Sóller","Alcúdia",
            "Ciutadella de Menorca","Campos","Sa Pobla","Andratx","Santanyí"
        ));
        MUNICIPIOS_CON_PADEL.put("Barcelona", Arrays.asList(
            "Barcelona","L'Hospitalet de Llobregat","Badalona","Terrassa","Sabadell","Mataró",
            "Santa Coloma de Gramenet","Sant Cugat del Vallès","Cornellà de Llobregat",
            "El Prat de Llobregat","Manresa","Granollers","Gavà","Viladecans",
            "Mollet del Vallès","Esplugues de Llobregat","Cerdanyola del Vallès","Rubí",
            "Vilanova i la Geltrú","Castelldefels","Barberà del Vallès","Martorell",
            "Berga","Igualada","Vic","Sitges","Abrera","Calella","Premià de Mar",
            "Pineda de Mar","Malgrat de Mar","Cardedeu","Molins de Rei","Sant Feliu de Llobregat"
        ));
        MUNICIPIOS_CON_PADEL.put("Burgos", Arrays.asList(
            "Burgos","Miranda de Ebro","Aranda de Duero","Briviesca","Villarcayo"
        ));
        MUNICIPIOS_CON_PADEL.put("Cáceres", Arrays.asList(
            "Cáceres","Plasencia","Navalmoral de la Mata","Trujillo","Miajadas","Moraleja"
        ));
        MUNICIPIOS_CON_PADEL.put("Cádiz", Arrays.asList(
            "Jerez de la Frontera","Algeciras","San Fernando","El Puerto de Santa María",
            "Chiclana de la Frontera","Sanlúcar de Barrameda","La Línea de la Concepción",
            "Cádiz","Rota","Conil de la Frontera","Barbate","Puerto Real","Tarifa",
            "Los Barrios","San Roque","Arcos de la Frontera","Medina Sidonia","Ubrique"
        ));
        MUNICIPIOS_CON_PADEL.put("Cantabria", Arrays.asList(
            "Santander","Torrelavega","Castro-Urdiales","Laredo","Camargo",
            "Santa Cruz de Bezana","Piélagos","Polanco","El Astillero"
        ));
        MUNICIPIOS_CON_PADEL.put("Castellón", Arrays.asList(
            "Castellón de la Plana","Vila-real","Benicarló","Vinaròs","Nules",
            "Onda","Almazora","Burriana","Benicàssim","Oropesa del Mar","Peñíscola",
            "Alcalà de Xivert","La Vall d'Uixó","Segorbe"
        ));
        MUNICIPIOS_CON_PADEL.put("Ciudad Real", Arrays.asList(
            "Ciudad Real","Puertollano","Tomelloso","Valdepeñas","Alcázar de San Juan",
            "Manzanares","Daimiel","Bolaños de Calatrava","Miguelturra","Malagón"
        ));
        MUNICIPIOS_CON_PADEL.put("Córdoba", Arrays.asList(
            "Córdoba","Lucena","Puente Genil","Priego de Córdoba","Cabra","Montilla",
            "Baena","Pozoblanco","Peñarroya-Pueblonuevo","Palma del Río","La Carlota",
            "Aguilar de la Frontera","Rute","Hinojosa del Duque"
        ));
        MUNICIPIOS_CON_PADEL.put("Cuenca", Arrays.asList(
            "Cuenca","Tarancón","Motilla del Palancar","San Clemente","Quintanar del Rey"
        ));
        MUNICIPIOS_CON_PADEL.put("Gerona", Arrays.asList(
            "Girona","Lloret de Mar","Blanes","Figueres","Salt","Roses",
            "Platja d'Aro","Olot","Palafrugell","Palamós","Ripoll","Santa Coloma de Farners"
        ));
        MUNICIPIOS_CON_PADEL.put("Granada", Arrays.asList(
            "Granada","Motril","Almuñécar","Loja","Baza","Guadix","Armilla",
            "Las Gabias","Maracena","Peligros","La Zubia","Alhama de Granada",
            "Íllora","Churriana de la Vega","Atarfe","Jun","Cenes de la Vega",
            "Albolote","Cúllar Vega","Ogíjares","Huétor Vega","Baza"
        ));
        MUNICIPIOS_CON_PADEL.put("Guadalajara", Arrays.asList(
            "Guadalajara","Azuqueca de Henares","Cabanillas del Campo","Alovera","Marchamalo"
        ));
        MUNICIPIOS_CON_PADEL.put("Guipúzcoa", Arrays.asList(
            "Donostia-San Sebastián","Irun","Eibar","Errenteria","Zarautz",
            "Hernani","Tolosa","Mondragón","Bergara","Beasain","Oñati","Zumarraga"
        ));
        MUNICIPIOS_CON_PADEL.put("Huelva", Arrays.asList(
            "Huelva","Almonte","Lepe","Moguer","Ayamonte","Isla Cristina",
            "Cartaya","Palos de la Frontera","Nerva","Valverde del Camino","Aracena"
        ));
        MUNICIPIOS_CON_PADEL.put("Huesca", Arrays.asList(
            "Huesca","Barbastro","Monzón","Jaca","Fraga","Binéfar","Sabiñánigo","Graus"
        ));
        MUNICIPIOS_CON_PADEL.put("Jaén", Arrays.asList(
            "Jaén","Linares","Andújar","Úbeda","Baeza","Martos","Alcalá la Real",
            "Mancha Real","Torredelcampo","La Carolina","Bailén","Mengíbar","Villacarrillo"
        ));
        MUNICIPIOS_CON_PADEL.put("La Coruña", Arrays.asList(
            "A Coruña","Ferrol","Santiago de Compostela","Oleiros","Arteixo",
            "Culleredo","Narón","Cambre","Carballo","Betanzos","Ribeira","Boiro",
            "Santa Comba","Ordes","Ames","Teo"
        ));
        MUNICIPIOS_CON_PADEL.put("La Rioja", Arrays.asList(
            "Logroño","Calahorra","Arnedo","Haro","Lardero","Nájera","Alfaro","Autol","Pradejón"
        ));
        MUNICIPIOS_CON_PADEL.put("Las Palmas", Arrays.asList(
            "Las Palmas de Gran Canaria","Telde","Arucas","San Bartolomé de Tirajana",
            "Santa Lucía de Tirajana","Ingenio","Agüimes","Mogán","Gáldar","Arrecife",
            "Puerto del Rosario","Tías","Yaiza","Antigua"
        ));
        MUNICIPIOS_CON_PADEL.put("León", Arrays.asList(
            "León","Ponferrada","San Andrés del Rabanedo","Astorga","Villablino","La Bañeza"
        ));
        MUNICIPIOS_CON_PADEL.put("Lérida", Arrays.asList(
            "Lleida","Mollerussa","Balaguer","Tàrrega","Cervera","Tremp","La Seu d'Urgell"
        ));
        MUNICIPIOS_CON_PADEL.put("Lugo", Arrays.asList(
            "Lugo","Viveiro","Burela","Vilalba","Monforte de Lemos","Ribadeo","Chantada","Sarria"
        ));
        MUNICIPIOS_CON_PADEL.put("Madrid", Arrays.asList(
            "Madrid","Móstoles","Alcalá de Henares","Fuenlabrada","Leganés","Getafe",
            "Alcorcón","Torrejón de Ardoz","Parla","Alcobendas","Las Rozas de Madrid",
            "Pozuelo de Alarcón","Coslada","San Sebastián de los Reyes","Arganda del Rey",
            "Colmenar Viejo","Majadahonda","Valdemoro","Collado Villalba","Boadilla del Monte",
            "Rivas-Vaciamadrid","Tres Cantos","Galapagar","Algete","Pinto",
            "San Fernando de Henares","Navalcarnero","Torrelodones","Villalba",
            "Arroyomolinos","El Escorial","Brunete","Villaviciosa de Odón",
            "Mejorada del Campo","Paracuellos de Jarama","Aranjuez","Ciempozuelos",
            "Humanes de Madrid","Griñón","Moraleja de Enmedio","Getafe","Alcobendas"
        ));
        MUNICIPIOS_CON_PADEL.put("Málaga", Arrays.asList(
            "Málaga","Marbella","Vélez-Málaga","Torremolinos","Fuengirola","Mijas",
            "Benalmádena","Estepona","Nerja","Antequera","Ronda","Alhaurín de la Torre",
            "Coin","Torrox","Manilva","Marbella","Benahavís","Casares","Alhaurín el Grande",
            "Ojén","Istán","Cártama","Pizarra","Álora","Archidona","Antequera","Campillos",
            "Algarrobo","Vélez-Málaga","La Axarquía","Frigiliana"
        ));
        MUNICIPIOS_CON_PADEL.put("Murcia", Arrays.asList(
            "Murcia","Cartagena","Lorca","Molina de Segura","Alcantarilla","Yecla",
            "Mazarrón","Jumilla","Cieza","Bullas","Calasparra","San Javier","Torre-Pacheco",
            "Los Alcázares","Totana","Águilas","Puerto Lumbreras","Fuente Álamo",
            "San Pedro del Pinatar","Archena","Abarán","Cehegín","Moratalla",
            "Caravaca de la Cruz","Mula","La Unión"
        ));
        MUNICIPIOS_CON_PADEL.put("Navarra", Arrays.asList(
            "Pamplona","Tudela","Barañáin","Burlada","Huarte","Villava","Estella",
            "Tafalla","Sarriguren","Zizur Mayor","Noáin","Berriozar","Ansoáin","Cizur Menor"
        ));
        MUNICIPIOS_CON_PADEL.put("Orense", Arrays.asList(
            "Ourense","O Carballiño","Verín","O Barco de Valdeorras","Xinzo de Limia","Celanova"
        ));
        MUNICIPIOS_CON_PADEL.put("Palencia", Arrays.asList(
            "Palencia","Guardo","Venta de Baños","Aguilar de Campoo","Saldaña"
        ));
        MUNICIPIOS_CON_PADEL.put("Pontevedra", Arrays.asList(
            "Vigo","Pontevedra","Vilagarcía de Arousa","Redondela","Sanxenxo","O Porriño",
            "Cangas","Baiona","Gondomar","Moaña","Marín","Bueu","Caldas de Reis",
            "Cambados","Tui","A Guarda","Lalín"
        ));
        MUNICIPIOS_CON_PADEL.put("Salamanca", Arrays.asList(
            "Salamanca","Béjar","Santa Marta de Tormes","Carbajosa de la Sagrada","Ciudad Rodrigo"
        ));
        MUNICIPIOS_CON_PADEL.put("Santa Cruz de Tenerife", Arrays.asList(
            "Santa Cruz de Tenerife","San Cristóbal de La Laguna","Arona","Adeje",
            "Los Llanos de Aridane","Puerto de la Cruz","La Orotava","Santa Úrsula",
            "Güímar","Granadilla de Abona","San Miguel de Abona","Garachico","La Laguna"
        ));
        MUNICIPIOS_CON_PADEL.put("Segovia", Arrays.asList(
            "Segovia","Cuéllar","El Espinar","Palazuelos de Eresma","San Ildefonso"
        ));
        MUNICIPIOS_CON_PADEL.put("Sevilla", Arrays.asList(
            "Sevilla","Dos Hermanas","Alcalá de Guadaíra","Utrera","Écija","El Arahal",
            "Mairena del Aljarafe","Bormujos","Tomares","Carmona","Marchena",
            "Morón de la Frontera","La Rinconada","Coria del Río","Alcalá del Río",
            "Lebrija","Osuna","Estepa","Lora del Río","Las Cabezas de San Juan",
            "Gines","Albaida del Aljarafe","Castilleja de la Cuesta","Gelves",
            "Espartinas","Mairena del Alcor","El Viso del Alcor","Montequinto"
        ));
        MUNICIPIOS_CON_PADEL.put("Soria", Arrays.asList(
            "Soria","Almazán","El Burgo de Osma"
        ));
        MUNICIPIOS_CON_PADEL.put("Tarragona", Arrays.asList(
            "Tarragona","Reus","Tortosa","Valls","El Vendrell","Salou","Cambrils",
            "Calafell","Constantí","Vila-seca","Amposta","Deltebre","Vandellòs i l'Hospitalet de l'Infant"
        ));
        MUNICIPIOS_CON_PADEL.put("Teruel", Arrays.asList(
            "Teruel","Alcañiz","Andorra","Utrillas","Calamocha"
        ));
        MUNICIPIOS_CON_PADEL.put("Toledo", Arrays.asList(
            "Toledo","Talavera de la Reina","Illescas","Seseña","Sonseca",
            "Torrijos","Consuegra","Ocaña","Madridejos","Mora","Quintanar de la Orden",
            "Cabañas de la Sagra","Olías del Rey","Bargas"
        ));
        MUNICIPIOS_CON_PADEL.put("Valencia", Arrays.asList(
            "Valencia","Torrent","Gandía","Paterna","Burjassot","Manises","Alzira",
            "Ontinyent","Sagunto","Aldaia","Catarroja","Xàtiva","Quart de Poblet",
            "Mislata","Tavernes de la Valldigna","Sueca","Silla","L'Eliana","Bétera",
            "Paiporta","Oliva","Cullera","Puig","Foios","Alboraya","Picanya","Sedaví",
            "Massanassa","Lliria","Requena","Utiel","Carlet","Algemesí","Alcàsser",
            "Benifaió","Xirivella","Bonrepòs i Mirambell","Alfafar"
        ));
        MUNICIPIOS_CON_PADEL.put("Valladolid", Arrays.asList(
            "Valladolid","Laguna de Duero","Medina del Campo","Arroyo de la Encomienda",
            "Tordesillas","Peñafiel","Olmedo","Cisterniga"
        ));
        MUNICIPIOS_CON_PADEL.put("Vizcaya", Arrays.asList(
            "Bilbao","Barakaldo","Getxo","Basauri","Santurtzi","Portugalete","Leioa",
            "Ermua","Durango","Galdakao","Amorebieta-Etxano","Erandio","Sopela","Mungia",
            "Gernika-Lumo","Bermeo","Ondarroa","Markina-Xemein"
        ));
        MUNICIPIOS_CON_PADEL.put("Zamora", Arrays.asList(
            "Zamora","Benavente","Toro","Zamora"
        ));
        MUNICIPIOS_CON_PADEL.put("Zaragoza", Arrays.asList(
            "Zaragoza","Calatayud","Ejea de los Caballeros","Tarazona","Utebo",
            "Cuarte de Huerva","La Muela","Fuentes de Ebro","Pedrola","Alagón",
            "Alcañiz","Caspe","Daroca","Épila","Zuera"
        ));
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

        // Botones de editar/eliminar solo para los anuncios del propio usuario
        long autorId = anuncio.get("autor_id") instanceof Long ? (Long) anuncio.get("autor_id") : -1L;
        long anuncioId = anuncio.get("id") instanceof Long ? (Long) anuncio.get("id") : -1L;
        if (autorId == miId && anuncioId != -1) {
            inner.addView(crearFilaAccionesAnuncio(anuncioId, tipo, descVisible, anuncio));
        } else if (autorId != -1 && autorId != miId) {
            // Fila de acciones para anuncios ajenos
            final long fAutorId = autorId;
            LinearLayout filaCliente = new LinearLayout(this);
            filaCliente.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams filaP = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            filaP.setMargins(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(4));
            filaCliente.setLayoutParams(filaP);

            // Botón Hacer oferta
            MaterialButton btnOferta = crearBotonSecundario("💸  Hacer oferta");
            LinearLayout.LayoutParams ofP = new LinearLayout.LayoutParams(0, dpToPx(40), 1f);
            ofP.setMarginEnd(dpToPx(6)); btnOferta.setLayoutParams(ofP);
            btnOferta.setTextSize(12f);
            btnOferta.setOnClickListener(v -> mostrarDialogoHacerOferta(
                    fAutorId,
                    strSafe(anuncio.get("nombre")),
                    strSafe(anuncio.get("apellidos")),
                    strSafe(anuncio.get("categoria_actual")),
                    tipo, strSafe(anuncio.get("descripcion"))));

            // Botón Contactar (chat directo)
            MaterialButton btnChat = crearBotonPrimario("💬  Contactar");
            LinearLayout.LayoutParams chatP = new LinearLayout.LayoutParams(0, dpToPx(40), 1f);
            chatP.setMarginStart(dpToPx(6)); btnChat.setLayoutParams(chatP);
            btnChat.setTextSize(12f);
            btnChat.setOnClickListener(v -> {
                Intent intent = new Intent(ZonaActivity.this, ConversacionActivity.class);
                intent.putExtra("receptor_id", fAutorId);
                intent.putExtra("receptor_nombre",    strSafe(anuncio.get("nombre")));
                intent.putExtra("receptor_apellidos", strSafe(anuncio.get("apellidos")));
                intent.putExtra("receptor_categoria", strSafe(anuncio.get("categoria_actual")));
                startActivity(intent);
            });

            filaCliente.addView(btnOferta);
            filaCliente.addView(btnChat);
            inner.addView(filaCliente);
        }

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
        String precio     = extraerTag(desc, "PRECIO");
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
                hsvThumb.setScrollBarSize(0);
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

        // Precio: chip corporativo con fondo verde lima, texto oscuro, sin emojis
        if (!precio.isEmpty()) {
            LinearLayout.LayoutParams prP = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            prP.setMargins(0, dpToPx(10), 0, 0);

            // Chip: fondo verde lima, esquinas redondeadas, padding interno
            LinearLayout chipPrecio = new LinearLayout(this);
            chipPrecio.setLayoutParams(prP);
            chipPrecio.setOrientation(LinearLayout.HORIZONTAL);
            chipPrecio.setGravity(Gravity.CENTER_VERTICAL);
            chipPrecio.setPadding(dpToPx(12), dpToPx(6), dpToPx(14), dpToPx(6));
            GradientDrawable bgChip = new GradientDrawable();
            bgChip.setColor(getResources().getColor(R.color.verde_lima, getTheme()));
            bgChip.setCornerRadius(dpToPx(20));
            chipPrecio.setBackground(bgChip);

            // Símbolo € pequeño
            TextView tvEuro = new TextView(this);
            tvEuro.setText("€");
            tvEuro.setTextColor(getResources().getColor(R.color.fondo_oscuro, getTheme()));
            tvEuro.setTextSize(13f);
            tvEuro.setTypeface(null, Typeface.BOLD);
            tvEuro.setPadding(0, 0, dpToPx(4), 0);
            chipPrecio.addView(tvEuro);

            // Valor del precio
            String precioValor = precio.replace("€", "").trim();
            TextView tvValor = new TextView(this);
            tvValor.setText(precioValor + " €");
            tvValor.setTextColor(getResources().getColor(R.color.fondo_oscuro, getTheme()));
            tvValor.setTextSize(20f);
            tvValor.setTypeface(null, Typeface.BOLD);
            chipPrecio.addView(tvValor);

            infoLayout.addView(chipPrecio);
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

        // Botones editar/eliminar para anuncios propios
        long autorId = anuncio.get("autor_id") instanceof Long ? (Long) anuncio.get("autor_id") : -1L;
        long anuncioId = anuncio.get("id") instanceof Long ? (Long) anuncio.get("id") : -1L;
        if (autorId == miId && anuncioId != -1) {
            LinearLayout.LayoutParams acP = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            acP.setMargins(dpToPx(16), dpToPx(4), dpToPx(16), dpToPx(4));
            View accionesView = crearFilaAccionesAnuncio(anuncioId, "PALA", desc, anuncio);
            accionesView.setLayoutParams(acP);
            infoLayout.addView(accionesView);
        } else if (autorId != -1 && autorId != miId) {
            // Fila de acciones para palas ajenas
            final long fAutorId = autorId;
            LinearLayout filaCliente = new LinearLayout(this);
            filaCliente.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams filaP = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            filaP.setMargins(dpToPx(16), dpToPx(10), dpToPx(16), dpToPx(4));
            filaCliente.setLayoutParams(filaP);

            // Botón Hacer oferta (precio diferente al pedido)
            MaterialButton btnOferta = crearBotonSecundario("💸  Hacer oferta");
            LinearLayout.LayoutParams ofP = new LinearLayout.LayoutParams(0, dpToPx(44), 1f);
            ofP.setMarginEnd(dpToPx(6)); btnOferta.setLayoutParams(ofP);
            btnOferta.setTextSize(12f);
            btnOferta.setOnClickListener(v -> mostrarDialogoHacerOferta(
                    fAutorId,
                    strSafe(anuncio.get("nombre")),
                    strSafe(anuncio.get("apellidos")),
                    strSafe(anuncio.get("categoria_actual")),
                    "PALA", strSafe(anuncio.get("descripcion"))));

            // Botón Contactar con el vendedor
            MaterialButton btnChat = crearBotonPrimario("💬  Contactar");
            LinearLayout.LayoutParams chatP = new LinearLayout.LayoutParams(0, dpToPx(44), 1f);
            chatP.setMarginStart(dpToPx(6)); btnChat.setLayoutParams(chatP);
            btnChat.setTextSize(12f);
            btnChat.setOnClickListener(v -> {
                Intent intent = new Intent(ZonaActivity.this, ConversacionActivity.class);
                intent.putExtra("receptor_id", fAutorId);
                intent.putExtra("receptor_nombre",    strSafe(anuncio.get("nombre")));
                intent.putExtra("receptor_apellidos", strSafe(anuncio.get("apellidos")));
                intent.putExtra("receptor_categoria", strSafe(anuncio.get("categoria_actual")));
                startActivity(intent);
            });

            filaCliente.addView(btnOferta);
            filaCliente.addView(btnChat);
            infoLayout.addView(filaCliente);
        }

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

        // Frame que apila la inicial (fallback) y la foto real, una sobre la otra
        FrameLayout avatarFrame = new FrameLayout(this);
        avatarFrame.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        avatarFrame.setBackgroundColor(
                getResources().getColor(R.color.fondo_tarjeta_claro, getTheme()));

        // Capa 1: inicial del nombre, visible por defecto mientras no hay foto cargada
        TextView tvIni = new TextView(this);
        tvIni.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        tvIni.setGravity(Gravity.CENTER);
        tvIni.setText(!nombre.isEmpty() ? String.valueOf(nombre.charAt(0)).toUpperCase() : "?");
        tvIni.setTextColor(getResources().getColor(R.color.verde_lima, getTheme()));
        tvIni.setTextSize(14f); tvIni.setTypeface(null, Typeface.BOLD);

        // Capa 2: foto real del autor; cargarFotoSegura la hace visible si la URI es válida
        ImageView imgFotoAvatar = new ImageView(this);
        imgFotoAvatar.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        imgFotoAvatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imgFotoAvatar.setVisibility(View.GONE);

        avatarFrame.addView(tvIni);
        avatarFrame.addView(imgFotoAvatar);
        avatarCard.addView(avatarFrame);
        header.addView(avatarCard);

        // Cargamos la foto del autor usando el helper de BaseDrawerActivity.
        // Si la URI es inválida o fue revocada, muestra la inicial en su lugar (sin crash).
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
            if (marcaSeleccionada[0].isEmpty()) {
                Toast.makeText(this, "Selecciona primero la marca", Toast.LENGTH_SHORT).show();
                return;
            }
            String[] modelosMarca = MODELOS_POR_MARCA.getOrDefault(
                    marcaSeleccionada[0], new String[0]);
            // Construimos lista con todos los modelos de la marca + opción manual al final
            List<String> listaModelos = new ArrayList<>(Arrays.asList(modelosMarca));
            listaModelos.add("✏️  Otro (escribir a mano)");
            mostrarDialogoLista("📋 Modelo de " + marcaSeleccionada[0], listaModelos, item -> {
                if (item.startsWith("✏️")) {
                    // Modelo no listado → entrada manual en diálogo oscuro
                    mostrarDialogoInputOscuro(
                            "✏️  Modelo personalizado",
                            "Escribe el nombre exacto del modelo:",
                            "Ej: Hack 04 Edición Limitada",
                            texto -> {
                                modeloSeleccionado[0] = texto;
                                btnModelo.setText("📋 " + texto);
                            });
                } else {
                    modeloSeleccionado[0] = item;
                    btnModelo.setText("📋 " + item);
                }
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

        // Creamos 3 slots de foto iguales distribuidos con peso 1f cada uno
        for (int slot = 0; slot < 3; slot++) {
            final int s = slot;   // captura efectiva para el lambda
            LinearLayout slotLayout = new LinearLayout(this);
            slotLayout.setOrientation(LinearLayout.VERTICAL);
            slotLayout.setGravity(Gravity.CENTER_HORIZONTAL);
            // Peso 1f: los tres slots se reparten el ancho disponible por igual
            LinearLayout.LayoutParams slP = new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            slP.setMarginEnd(slot < 2 ? dpToPx(8) : 0);
            slotLayout.setLayoutParams(slP);

            // Tarjeta cuadrada de 90dp que actúa de marco para la miniatura
            MaterialCardView previewCard = new MaterialCardView(this);
            LinearLayout.LayoutParams pcP = new LinearLayout.LayoutParams(dpToPx(90), dpToPx(90));
            previewCard.setLayoutParams(pcP);
            previewCard.setRadius(dpToPx(10));
            previewCard.setCardElevation(dpToPx(2));
            previewCard.setStrokeColor(0xFF333333); previewCard.setStrokeWidth(dpToPx(1));

            // FrameLayout permite superponer el icono ➕ y la imagen de preview
            FrameLayout previewFrame = new FrameLayout(this);
            previewFrame.setLayoutParams(new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
            previewFrame.setBackgroundColor(0xFF1A1A1A);

            // Icono de placeholder: visible hasta que el usuario elige una foto
            TextView tvPlaceholder = new TextView(this);
            tvPlaceholder.setLayoutParams(new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
            tvPlaceholder.setGravity(Gravity.CENTER);
            tvPlaceholder.setText("➕");
            tvPlaceholder.setTextSize(24f);
            tvPlaceholder.setTextColor(0xFF555555);

            // ImageView de preview: oculto hasta que se selecciona una foto para este slot
            ImageView imgPrev = new ImageView(this);
            imgPrev.setLayoutParams(new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
            imgPrev.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imgPrev.setVisibility(View.GONE);
            // Guardamos la referencia en el array para actualizarlo desde el launcher
            imgPreviewsPala[s] = imgPrev;

            previewFrame.addView(tvPlaceholder);
            previewFrame.addView(imgPrev);
            previewCard.addView(previewFrame);

            // Al tocar el cuadro, marcamos el slot activo y abrimos el picker de imágenes.
            // El launcher usa slotFotoActual para saber a qué slot asignar el resultado.
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

        // ── Precio ──────────────────────────────────────────────────────────
        addSpacer(form, 10);
        form.addView(crearLabel("💶  Precio (€)  *obligatorio*"));
        EditText etPrecio = crearEditText("Ej: 120", true);
        form.addView(etPrecio);

        // ── Descripción ─────────────────────────────────────────────────────
        addSpacer(form, 10);
        form.addView(crearLabel(getString(R.string.zona_desc_opcional)));
        EditText etDesc = crearEditTextMultiline(
                "Ej: Pala poco usada, excelente estado. Golpeo potente y buena salida de bola. " +
                "Ideal para jugadores de nivel 3ª en adelante.");
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
            String precio  = etPrecio.getText().toString().trim();

            // Precio obligatorio
            if (precio.isEmpty()) {
                Toast.makeText(this, "Indica el precio de la pala", Toast.LENGTH_SHORT).show();
                return;
            }

            // Si no se escribió nombre libre, usamos el modelo o la marca como identificador
            String nombreFinal = nomPala.isEmpty()
                    ? (modelo.isEmpty() ? marca : modelo)
                    : nomPala;
            if (nombreFinal.isEmpty() && marca.isEmpty()) {
                Toast.makeText(this, "Indica al menos la marca o nombre de la pala",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            // Obtenemos el estado seleccionado por su posición en el RadioGroup
            int idxE = rgEstado.indexOfChild(
                    rgEstado.findViewById(rgEstado.getCheckedRadioButtonId()));
            String estadoText = idxE >= 0 ? estados[idxE] : estados[2];
            String descText = etDesc.getText().toString().trim();

            // Construimos la descripción con el formato de tags @@CLAVE:valor para la magic card.
            // agregarTarjetaPala() usa extraerTag() para leer cada campo al renderizar.
            StringBuilder sb = new StringBuilder();
            sb.append("@@NOMBRE:").append(nombreFinal).append("\n");
            if (!marca.isEmpty())  sb.append("@@MARCA:").append(marca).append("\n");
            if (!modelo.isEmpty()) sb.append("@@MODELO:").append(modelo).append("\n");
            sb.append("@@PRECIO:").append(precio).append("€\n");
            sb.append("@@ESTADO:").append(estadoText).append("\n");
            if (!descText.isEmpty()) sb.append("@@DESC:").append(descText).append("\n");

            // Las URIs de las 3 fotos se separan con "|"; se omiten las que el usuario no rellenó
            StringBuilder fotos = new StringBuilder();
            for (String uri : fotosUrisPala) {
                if (uri != null) { if (fotos.length() > 0) fotos.append("|"); fotos.append(uri); }
            }
            if (fotos.length() > 0) sb.append("@@FOTOS:").append(fotos);

            // Persistimos en SQLite y actualizamos la lista de anuncios
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
            // Localidad obligatoria: sin ella no podemos filtrar por provincia
            if (ciudadSel[0].isEmpty()) {
                Toast.makeText(this, "Selecciona la localidad", Toast.LENGTH_SHORT).show();
                return;
            }
            String categoria = etCategoria.getText().toString().trim();
            String jugadores = etJugadores.getText().toString().trim();
            String desc      = etDesc.getText().toString().trim();

            // Construimos el texto del anuncio con emojis de sección para la tarjeta estándar
            StringBuilder sb = new StringBuilder();
            if (!categoria.isEmpty()) sb.append("🏆 ").append(categoria).append("\n");
            // Si no indicaron jugadores buscados, asumimos 1 por defecto
            sb.append("👥 ").append(jugadores.isEmpty() ? "1" : jugadores)
              .append(" jugador(es) buscado(s)\n");
            sb.append("📍 ").append(ciudadSel[0]).append(", ").append(provinciaActual).append("\n");
            if (!pistaSel[0].isEmpty()) sb.append("🏟️ ").append(pistaSel[0]).append("\n");

            // Iteramos los miembros añadidos dinámicamente.
            // Cada fila tiene en su tag un EditText[]{nombre, categoria} (ver crearFilaMiembro).
            for (int i = 0; i < llMiembros.getChildCount(); i++) {
                View fila = llMiembros.getChildAt(i);
                if (fila.getTag() instanceof EditText[]) {
                    EditText[] fields = (EditText[]) fila.getTag();
                    String nom = fields[0].getText().toString().trim();
                    String cat = fields[1].getText().toString().trim();
                    // Solo incluimos el miembro si tiene nombre; la categoría es opcional
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

    // ─────────────────────────────────────────────────────────────────────────
    // Editar y eliminar anuncios propios
    // ─────────────────────────────────────────────────────────────────────────

    // Crea una fila horizontal con botones ✏️ Editar y 🗑️ Eliminar para anuncios del propio usuario.
    // Solo se añade a tarjetas cuyo autor_id == miId.
    private View crearFilaAccionesAnuncio(long anuncioId, String tipo,
                                           String descripcionActual,
                                           Map<String, Object> anuncio) {
        LinearLayout fila = new LinearLayout(this);
        fila.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams fp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        fp.setMargins(0, dpToPx(10), 0, 0);
        fila.setLayoutParams(fp);

        // Botón Editar
        MaterialButton btnEditar = crearBotonSecundario("✏️  Editar");
        LinearLayout.LayoutParams ep = new LinearLayout.LayoutParams(0, dpToPx(36), 1f);
        ep.setMarginEnd(dpToPx(8)); btnEditar.setLayoutParams(ep);
        btnEditar.setTextSize(12f);
        btnEditar.setOnClickListener(v -> mostrarEditorAnuncio(anuncioId, tipo, anuncio));

        // Botón Eliminar (rojo)
        MaterialButton btnEliminar = new MaterialButton(this);
        btnEliminar.setText("🗑️  Eliminar");
        btnEliminar.setTextColor(Color.WHITE);
        btnEliminar.setBackgroundTintList(ColorStateList.valueOf(0xFFD32F2F));
        btnEliminar.setCornerRadius(dpToPx(12));
        btnEliminar.setTextSize(12f);
        LinearLayout.LayoutParams dp2 = new LinearLayout.LayoutParams(0, dpToPx(36), 1f);
        btnEliminar.setLayoutParams(dp2);
        btnEliminar.setOnClickListener(v -> confirmarEliminarAnuncio(anuncioId));

        fila.addView(btnEditar);
        fila.addView(btnEliminar);
        return fila;
    }

    // Muestra confirmación antes de eliminar un anuncio.
    private void confirmarEliminarAnuncio(long anuncioId) {
        LinearLayout outer = crearDialogLayout();
        outer.addView(crearTituloDialog("🗑️  Eliminar anuncio"));

        TextView tvMsg = new TextView(this);
        tvMsg.setText("¿Seguro que quieres eliminar este anuncio? Esta acción no se puede deshacer.");
        tvMsg.setTextColor(0xFFCCCCCC);
        tvMsg.setTextSize(14f);
        tvMsg.setLineSpacing(dpToPx(2), 1f);
        outer.addView(tvMsg);

        LinearLayout llBtn = crearFilaBotones();
        MaterialButton btnC = crearBotonSecundario("Cancelar");
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0, dpToPx(46), 1f);
        cp.setMarginEnd(dpToPx(8)); btnC.setLayoutParams(cp);

        MaterialButton btnOk = new MaterialButton(this);
        btnOk.setText("Eliminar");
        btnOk.setTextColor(Color.WHITE);
        btnOk.setBackgroundTintList(ColorStateList.valueOf(0xFFD32F2F));
        btnOk.setCornerRadius(dpToPx(12));
        LinearLayout.LayoutParams op = new LinearLayout.LayoutParams(0, dpToPx(46), 1f);
        op.setMarginStart(dpToPx(8)); btnOk.setLayoutParams(op);
        llBtn.addView(btnC); llBtn.addView(btnOk);
        outer.addView(llBtn);

        final AlertDialog[] d = { mostrarDialog(outer) };
        btnC.setOnClickListener(v -> { if (d[0] != null) d[0].dismiss(); });
        btnOk.setOnClickListener(v -> {
            if (d[0] != null) d[0].dismiss();
            if (db.eliminarAnuncio(anuncioId)) {
                Toast.makeText(this, "Anuncio eliminado", Toast.LENGTH_SHORT).show();
                cargarAnuncios();
            } else {
                Toast.makeText(this, "Error al eliminar", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Abre el editor apropiado según el tipo de anuncio.
    private void mostrarEditorAnuncio(long anuncioId, String tipo, Map<String, Object> anuncio) {
        String desc = strSafe(anuncio.get("descripcion"));
        switch (tipo) {
            case "PALA":
                mostrarEditorAnuncioPala(anuncioId, desc);
                break;
            default:
                // PARTIDA, CLASE_OFRECER, CLASE_SOLICITAR → editor de texto libre
                mostrarEditorAnuncioTexto(anuncioId, tipo, desc);
                break;
        }
    }

    // Editor para PALA: permite editar el nombre, precio y descripción libre.
    private void mostrarEditorAnuncioPala(long anuncioId, String desc) {
        String nombreActual = extraerTag(desc, "NOMBRE");
        String precioActual = extraerTag(desc, "PRECIO").replace("€", "").trim();
        String descActual   = extraerTag(desc, "DESC");

        LinearLayout outer = crearDialogLayout();
        outer.addView(crearTituloDialog("✏️  Editar anuncio de pala"));

        outer.addView(crearLabel("Nombre de la pala:"));
        addSpacer(outer, 6);
        EditText etNombre = crearEditText("Ej: Bullpadel Hack 04", false);
        etNombre.setText(nombreActual);
        outer.addView(etNombre);

        addSpacer(outer, 10);
        outer.addView(crearLabel("Precio (€):"));
        addSpacer(outer, 6);
        EditText etPrecio = crearEditText("Ej: 150", false);
        etPrecio.setText(precioActual);
        etPrecio.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        outer.addView(etPrecio);

        addSpacer(outer, 10);
        outer.addView(crearLabel("Descripción (opcional):"));
        addSpacer(outer, 6);
        EditText etDesc = crearEditTextMultiline("Ej: Pala poco usada, excelente estado. Precio negociable.");
        etDesc.setText(descActual);
        outer.addView(etDesc);

        LinearLayout llBtn = crearFilaBotones();
        MaterialButton btnC = crearBotonSecundario("Cancelar");
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0, dpToPx(46), 1f);
        cp.setMarginEnd(dpToPx(8)); btnC.setLayoutParams(cp);
        MaterialButton btnG = crearBotonPrimario("Guardar");
        LinearLayout.LayoutParams gp = new LinearLayout.LayoutParams(0, dpToPx(46), 1f);
        gp.setMarginStart(dpToPx(8)); btnG.setLayoutParams(gp);
        llBtn.addView(btnC); llBtn.addView(btnG);
        outer.addView(llBtn);

        final AlertDialog[] d = { mostrarDialog(outer) };
        btnC.setOnClickListener(v -> { if (d[0] != null) d[0].dismiss(); });
        btnG.setOnClickListener(v -> {
            String nuevoNombre = etNombre.getText().toString().trim();
            String nuevoPrecio = etPrecio.getText().toString().trim();
            String nuevaDesc   = etDesc.getText().toString().trim();

            // Reemplazamos cada tag en el bloque de descripción estructurada
            String descFinal = desc;
            if (!nuevoNombre.isEmpty()) {
                descFinal = descFinal.contains("@@NOMBRE:")
                    ? descFinal.replaceFirst("(?s)@@NOMBRE:[^\n]*\n?", "@@NOMBRE:" + nuevoNombre + "\n")
                    : "@@NOMBRE:" + nuevoNombre + "\n" + descFinal;
            }
            if (!nuevoPrecio.isEmpty()) {
                String tagPrecio = "@@PRECIO:" + nuevoPrecio + "€\n";
                descFinal = descFinal.contains("@@PRECIO:")
                    ? descFinal.replaceFirst("(?s)@@PRECIO:[^\n]*\n?", tagPrecio)
                    : descFinal + tagPrecio;
            }
            if (descFinal.contains("@@DESC:")) {
                descFinal = descFinal.replaceFirst("(?s)@@DESC:[^\n]*\n?",
                        nuevaDesc.isEmpty() ? "" : "@@DESC:" + nuevaDesc + "\n");
            } else if (!nuevaDesc.isEmpty()) {
                descFinal = descFinal + "@@DESC:" + nuevaDesc + "\n";
            }
            guardarEdicionAnuncio(d, anuncioId, descFinal);
        });
    }

    // Editor genérico para PARTIDA y CLASES: texto libre editable en multilínea.
    private void mostrarEditorAnuncioTexto(long anuncioId, String tipo, String desc) {
        LinearLayout outer = crearDialogLayout();
        String tituloEditor = "PARTIDA".equals(tipo) ? "✏️  Editar partida"
                            : "CLASE_OFRECER".equals(tipo) ? "✏️  Editar clase ofrecida"
                            : "✏️  Editar clase buscada";
        outer.addView(crearTituloDialog(tituloEditor));
        outer.addView(crearLabel("Descripción del anuncio:"));
        addSpacer(outer, 8);

        EditText etDesc = crearEditTextMultiline("Escribe aquí el contenido del anuncio…");
        etDesc.setText(desc);
        etDesc.setMaxLines(8);
        LinearLayout.LayoutParams ep = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        ep.setMargins(0, 0, 0, dpToPx(4)); etDesc.setLayoutParams(ep);
        outer.addView(etDesc);

        LinearLayout llBtn = crearFilaBotones();
        MaterialButton btnC = crearBotonSecundario("Cancelar");
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0, dpToPx(46), 1f);
        cp.setMarginEnd(dpToPx(8)); btnC.setLayoutParams(cp);
        MaterialButton btnG = crearBotonPrimario("Guardar");
        LinearLayout.LayoutParams gp = new LinearLayout.LayoutParams(0, dpToPx(46), 1f);
        gp.setMarginStart(dpToPx(8)); btnG.setLayoutParams(gp);
        llBtn.addView(btnC); llBtn.addView(btnG);
        outer.addView(llBtn);

        final AlertDialog[] d = { mostrarDialog(outer) };
        btnC.setOnClickListener(v -> { if (d[0] != null) d[0].dismiss(); });
        btnG.setOnClickListener(v -> guardarEdicionAnuncio(d, anuncioId,
                etDesc.getText().toString().trim()));
    }

    // Diálogo "Hacer oferta": el usuario cliente introduce un precio y se abre ConversacionActivity
    // con el mensaje de oferta pre-rellenado para que lo envíe (o lo edite) al vendedor.
    private void mostrarDialogoHacerOferta(long receptorId, String nombre, String apellidos,
                                            String categoria, String tipo, String desc) {
        // Nombre del ítem para personalizar el mensaje
        String itemNombre = "PALA".equals(tipo) ? extraerTag(desc, "NOMBRE") : "";
        String precioAnuncio = "PALA".equals(tipo) ? extraerTag(desc, "PRECIO") : "";

        LinearLayout outer = crearDialogLayout();
        outer.addView(crearTituloDialog("💸  Hacer una oferta"));

        TextView tvInfo = new TextView(this);
        String infoText = itemNombre.isEmpty()
                ? "Escribe la cantidad que ofreces por este anuncio:"
                : "Escribe la cantidad que ofreces por la pala \"" + itemNombre + "\""
                  + (precioAnuncio.isEmpty() ? ":" : " (precio pedido: " + precioAnuncio + "):");
        tvInfo.setText(infoText);
        tvInfo.setTextColor(0xFFCCCCCC);
        tvInfo.setTextSize(13f);
        tvInfo.setLineSpacing(dpToPx(2), 1f);
        LinearLayout.LayoutParams infoP = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        infoP.setMargins(0, 0, 0, dpToPx(12));
        tvInfo.setLayoutParams(infoP);
        outer.addView(tvInfo);

        outer.addView(crearLabel("Tu oferta (€):"));
        addSpacer(outer, 6);
        EditText etOferta = crearEditText("Ej: 120", false);
        etOferta.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        outer.addView(etOferta);

        LinearLayout llBtn = crearFilaBotones();
        MaterialButton btnC = crearBotonSecundario("Cancelar");
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0, dpToPx(46), 1f);
        cp.setMarginEnd(dpToPx(8)); btnC.setLayoutParams(cp);
        MaterialButton btnOk = crearBotonPrimario("Enviar oferta");
        LinearLayout.LayoutParams op = new LinearLayout.LayoutParams(0, dpToPx(46), 1f);
        op.setMarginStart(dpToPx(8)); btnOk.setLayoutParams(op);
        llBtn.addView(btnC); llBtn.addView(btnOk);
        outer.addView(llBtn);

        final AlertDialog[] d = { mostrarDialog(outer) };
        btnC.setOnClickListener(v -> { if (d[0] != null) d[0].dismiss(); });
        btnOk.setOnClickListener(v -> {
            String cantidadStr = etOferta.getText().toString().trim();
            if (cantidadStr.isEmpty()) {
                Toast.makeText(this, "Introduce una cantidad", Toast.LENGTH_SHORT).show();
                return;
            }
            if (d[0] != null) d[0].dismiss();

            // Construimos el mensaje de oferta pre-rellenado
            String msgOferta;
            if (!itemNombre.isEmpty()) {
                msgOferta = "Hola, me interesa tu pala \"" + itemNombre
                        + "\". Te ofrezco " + cantidadStr + " €. ¿Está disponible?";
            } else {
                msgOferta = "Hola, te hago una oferta de " + cantidadStr + " € por tu anuncio. ¿Te interesa?";
            }

            Intent intent = new Intent(ZonaActivity.this, ConversacionActivity.class);
            intent.putExtra("receptor_id", receptorId);
            intent.putExtra("receptor_nombre", nombre);
            intent.putExtra("receptor_apellidos", apellidos);
            intent.putExtra("receptor_categoria", categoria);
            intent.putExtra("mensaje_inicial", msgOferta);
            startActivity(intent);
        });
    }

    // Guarda la edición en BD, cierra el diálogo y recarga la lista.
    private void guardarEdicionAnuncio(AlertDialog[] d, long anuncioId, String nuevaDesc) {
        if (db.actualizarAnuncio(anuncioId, nuevaDesc)) {
            if (d[0] != null) d[0].dismiss();
            Toast.makeText(this, "Anuncio actualizado", Toast.LENGTH_SHORT).show();
            cargarAnuncios();
        } else {
            Toast.makeText(this, "Error al guardar los cambios", Toast.LENGTH_SHORT).show();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
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

    // Muestra un diálogo buscable con todos los municipios de la provincia.
    // La carga es asíncrona (Thread): mientras descarga desactiva el botón.
    // Un EditText de búsqueda filtra la lista en tiempo real para facilitar la navegación
    // en provincias con muchos municipios (p.ej. Salamanca tiene más de 300).
    private void mostrarSelectorLocalidad(String provincia, MaterialButton btn,
                                           OnItemSelected callback) {
        if (provincia == null || provincia.isEmpty()) {
            Toast.makeText(this, "Selecciona primero una provincia", Toast.LENGTH_SHORT).show();
            return;
        }

        // Primero consultamos los datos locales (sin red, instantáneo)
        List<String> municipiosLocales = getMunicipiosLocales(provincia);
        if (!municipiosLocales.isEmpty()) {
            mostrarDialogoListaBuscable(
                    "📍  Ciudad / Pueblo — " + provincia,
                    municipiosLocales,
                    item -> {
                        btn.setText("📍 " + item);
                        callback.onSelected(item);
                    });
            return;
        }

        // Fallback asíncrono: INE API si la provincia no está en el mapa local
        String textoOrig = btn.getText().toString();
        btn.setText("⏳ Cargando municipios…"); btn.setEnabled(false);
        new Thread(() -> {
            List<String> municipios = fetchMunicipiosINE(provincia);
            runOnUiThread(() -> {
                btn.setEnabled(true); btn.setText(textoOrig);
                if (municipios.isEmpty()) {
                    mostrarDialogoInputOscuro(
                            "📍  Sin conexión",
                            "No se pudo cargar la lista. Escribe tu localidad:",
                            "Ej: Salamanca, Leganés, Marbella…",
                            texto -> { btn.setText("📍 " + texto); callback.onSelected(texto); });
                } else {
                    mostrarDialogoListaBuscable(
                            "📍  Ciudad / Pueblo — " + provincia,
                            municipios,
                            item -> {
                                btn.setText("📍 " + item);
                                callback.onSelected(item);
                            });
                }
            });
        }).start();
    }

    // Diálogo oscuro con lista buscable: muestra todos los ítems y los filtra en tiempo real.
    // Usa el mismo estilo oscuro (crearDialogLayout + mostrarDialog) que el resto de formularios.
    // Imprescindible para provincias con muchos municipios (Salamanca, Ávila, Soria…).
    private void mostrarDialogoListaBuscable(String titulo, List<String> items,
                                              OnItemSelected callback) {
        // Envolvemos todo en el layout oscuro de la app
        LinearLayout outer = crearDialogLayout();
        outer.addView(crearTituloDialog(titulo));

        // Campo de búsqueda con estilo oscuro
        EditText etBuscar = crearEditText("🔍  Buscar municipio…", false);
        LinearLayout.LayoutParams etP = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(44));
        etP.setMargins(0, 0, 0, dpToPx(10));
        etBuscar.setLayoutParams(etP);
        outer.addView(etBuscar);

        // ListView oscuro con altura fija para no desbordarse en pantallas pequeñas
        android.widget.ListView listView = new android.widget.ListView(this);
        listView.setBackgroundColor(0xFF161616);
        listView.setDividerHeight(1);
        listView.setDivider(new android.graphics.drawable.ColorDrawable(0xFF2A2A2A));
        listView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(300)));
        outer.addView(listView);

        // Adaptador con layout personalizado (texto blanco sobre fondo oscuro)
        final List<String> listaActual = new ArrayList<>(items);
        android.widget.BaseAdapter adapter = new android.widget.BaseAdapter() {
            @Override public int getCount() { return listaActual.size(); }
            @Override public String getItem(int pos) { return listaActual.get(pos); }
            @Override public long getItemId(int pos) { return pos; }
            @Override public android.view.View getView(int pos, android.view.View cv,
                                                        android.view.ViewGroup parent) {
                TextView tv = (cv instanceof TextView) ? (TextView) cv : new TextView(ZonaActivity.this);
                tv.setText(listaActual.get(pos));
                tv.setTextColor(0xFFDDDDDD);
                tv.setTextSize(14f);
                tv.setPadding(dpToPx(14), dpToPx(12), dpToPx(14), dpToPx(12));
                tv.setBackgroundColor(0xFF161616);
                return tv;
            }
        };
        listView.setAdapter(adapter);

        // Botón cancelar al pie
        LinearLayout llBtn = crearFilaBotones();
        MaterialButton btnCancelar = crearBotonSecundario("Cancelar");
        btnCancelar.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(46)));
        llBtn.addView(btnCancelar);
        outer.addView(llBtn);

        final AlertDialog[] dialogRef = { mostrarDialog(outer) };
        btnCancelar.setOnClickListener(v -> { if (dialogRef[0] != null) dialogRef[0].dismiss(); });

        // Al tocar un ítem: cierra el diálogo y notifica al callback
        listView.setOnItemClickListener((parent, v, pos, id) -> {
            if (dialogRef[0] != null) dialogRef[0].dismiss();
            callback.onSelected(listaActual.get(pos));
        });

        // Filtrado en tiempo real: reconstruye listaActual y refresca el adaptador
        etBuscar.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {
                String q = s.toString().toLowerCase(Locale.getDefault()).trim();
                listaActual.clear();
                if (q.isEmpty()) {
                    listaActual.addAll(items);
                } else {
                    for (String item : items) {
                        if (item.toLowerCase(Locale.getDefault()).contains(q)) listaActual.add(item);
                    }
                }
                adapter.notifyDataSetChanged();
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });
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
                    // Sin resultados de OSM: entrada manual en diálogo oscuro
                    mostrarDialogoPistaManual(btn, callback);
                } else {
                    // Añadimos opción de escritura manual al final de la lista
                    pistas.add("✏️  Escribir manualmente…");
                    mostrarDialogoListaBuscable("🏟️ Pistas en " + ciudad, pistas, item -> {
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

    // Diálogo oscuro de entrada manual de pista cuando OSM no devuelve resultados.
    private void mostrarDialogoPistaManual(MaterialButton btn, OnItemSelected callback) {
        mostrarDialogoInputOscuro(
                "🏟️  Sin resultados en OSM",
                "Escribe el nombre de la pista o club:",
                "Ej: Club Pádel Central, Pistas Municipales…",
                texto -> { btn.setText("🏟️ " + texto); callback.onSelected(texto); });
    }

    // Diálogo oscuro reutilizable para introducir texto libre.
    // Usa el mismo tema oscuro que el resto de formularios de la app.
    private void mostrarDialogoInputOscuro(String titulo, String label,
                                            String hint, OnItemSelected callback) {
        LinearLayout outer = crearDialogLayout();
        outer.addView(crearTituloDialog(titulo));
        if (label != null && !label.isEmpty()) outer.addView(crearLabel(label));
        addSpacer(outer, 8);
        EditText et = crearEditText(hint, false);
        outer.addView(et);

        LinearLayout llBtn = crearFilaBotones();
        MaterialButton btnC = crearBotonSecundario("Cancelar");
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0, dpToPx(46), 1f);
        cp.setMarginEnd(dpToPx(8)); btnC.setLayoutParams(cp);
        MaterialButton btnOk = crearBotonPrimario("OK");
        LinearLayout.LayoutParams op = new LinearLayout.LayoutParams(0, dpToPx(46), 1f);
        op.setMarginStart(dpToPx(8)); btnOk.setLayoutParams(op);
        llBtn.addView(btnC); llBtn.addView(btnOk);
        outer.addView(llBtn);

        final AlertDialog[] d = { mostrarDialog(outer) };
        btnC.setOnClickListener(v -> { if (d[0] != null) d[0].dismiss(); });
        btnOk.setOnClickListener(v -> {
            String texto = et.getText().toString().trim();
            if (!texto.isEmpty()) {
                if (d[0] != null) d[0].dismiss();
                callback.onSelected(texto);
            } else {
                Toast.makeText(this, "Escribe algo primero", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Devuelve la lista local de municipios con pádel para la provincia dada.
    // Sin llamada de red: respuesta instantánea desde el mapa estático MUNICIPIOS_CON_PADEL.
    private List<String> getMunicipiosLocales(String provincia) {
        List<String> lista = MUNICIPIOS_CON_PADEL.get(provincia);
        if (lista == null) return new ArrayList<>();
        List<String> resultado = new ArrayList<>(lista);
        Collections.sort(resultado, (a, b) -> a.compareToIgnoreCase(b));
        return resultado;
    }

    // Obtiene la lista de municipios de una provincia usando la API oficial del INE.
    // Endpoint: https://servicios.ine.es/wstempus/js/ES/MUNICIPIOS/{cod_provincia}
    // Respuesta: JSON array  [{"Id":"28001","Nombre":"Acebeda (La)"},...]
    // Ejecutar en hilo de fondo; devuelve lista vacía si falla la red.
    // Si el INE no devuelve resultados (p.ej. sin conexión) cae al fallback de Overpass.
    private List<String> fetchMunicipiosINE(String provincia) {
        List<String> result = new ArrayList<>();

        // 1. Intentar la API oficial del INE ────────────────────────────────────
        String codigoProv = CODIGO_INE.get(provincia);
        if (codigoProv != null) {
            try {
                // El INE requiere el código con cero inicial para provincias 01-09
                String urlStr = "https://servicios.ine.es/wstempus/js/ES/MUNICIPIOS/" + codigoProv;
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(12000);
                conn.setReadTimeout(20000);
                conn.setRequestProperty("User-Agent", "PadelDart-TFG/1.0");
                conn.setRequestProperty("Accept", "application/json");
                if (conn.getResponseCode() == 200) {
                    StringBuilder sb = new StringBuilder();
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(conn.getInputStream(), "UTF-8"));
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line);
                    reader.close();

                    // La respuesta es un array JSON: [{Id, Nombre}, ...]
                    JSONArray arr = new JSONArray(sb.toString());
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject obj = arr.getJSONObject(i);
                        // El campo "Nombre" viene como "Municipio, El" → lo normalizamos
                        String nombre = obj.optString("Nombre", "").trim();
                        // Eliminamos el código de provincia del inicio del Nombre si lo incluye
                        // y normalizamos la forma "Apellido, El" → "El Apellido"
                        nombre = normalizarNombreMunicipio(nombre);
                        if (!nombre.isEmpty() && !result.contains(nombre)) result.add(nombre);
                    }
                    Collections.sort(result, (a, b) -> a.compareToIgnoreCase(b));
                    if (!result.isEmpty()) return result;  // éxito: devolvemos ya
                }
                conn.disconnect();
            } catch (Exception ignored) {}
        }

        // 2. Fallback: Overpass/OSM si el INE no respondió ─────────────────────
        try {
            String nombreOSM = NOMBRE_OSM_PROVINCIA.getOrDefault(provincia, provincia);
            String query = "[out:json][timeout:30];" +
                    "area[\"name\"=\"" + nombreOSM.replace("\"", "\\\"") + "\"]" +
                    "[\"admin_level\"=\"6\"]->.p;" +
                    "rel[\"admin_level\"=\"8\"](area.p);" +
                    "out tags;";
            String urlStr = "https://overpass-api.de/api/interpreter?data=" +
                    URLEncoder.encode(query, "UTF-8");
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(35000);
            conn.setRequestProperty("User-Agent", "PadelDart-TFG/1.0");
            if (conn.getResponseCode() == 200) {
                StringBuilder sb = new StringBuilder();
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), "UTF-8"));
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();
                JSONObject response = new JSONObject(sb.toString());
                JSONArray elements  = response.getJSONArray("elements");
                for (int i = 0; i < elements.length(); i++) {
                    JSONObject el = elements.getJSONObject(i);
                    if (!el.has("tags")) continue;
                    JSONObject tags = el.getJSONObject("tags");
                    String nombre = tags.optString("name:es", tags.optString("name", "")).trim();
                    if (!nombre.isEmpty() && !result.contains(nombre)) result.add(nombre);
                }
                Collections.sort(result, (a, b) -> a.compareToIgnoreCase(b));
            }
            conn.disconnect();
        } catch (Exception ignored) {}

        return result;
    }

    // Convierte el formato INE "Apellido, El" → "El Apellido" para mejor legibilidad.
    // También elimina el sufijo bilingüe "Petrer/Petrel" → "Petrer" (queda el nombre principal).
    // Si el nombre no tiene coma ni barra, lo devuelve tal cual.
    private String normalizarNombreMunicipio(String nombre) {
        if (nombre == null) return "";

        // Paso 1: eliminar sufijo bilingüe con "/" (p.ej. "Petrer/Petrel" → "Petrer")
        int barra = nombre.indexOf('/');
        if (barra > 0) nombre = nombre.substring(0, barra).trim();

        // Paso 2: convertir formato INE "Principal, Artículo" → "Artículo Principal"
        int coma = nombre.lastIndexOf(',');
        if (coma < 0) return nombre;
        String principal = nombre.substring(0, coma).trim();
        String articulo  = nombre.substring(coma + 1).trim();
        // Artículos comunes en español (el, la, los, las, lo, de, del…)
        if (!articulo.isEmpty() && articulo.length() <= 5) {
            return articulo + " " + principal;
        }
        return nombre;
    }

    // Busca instalaciones de pádel en la ciudad mediante la API Overpass (OpenStreetMap).
    // IMPORTANTE: la declaración del área (->.a) debe ir ANTES del bloque unión ();
    //             ponerla dentro del unión es un error de sintaxis de Overpass QL.
    // Intenta primero buscar el área por nombre de ciudad; si no hay resultados,
    // amplía la búsqueda con el tag place=city/town/village como filtro adicional.
    private List<String> fetchPistasOverpass(String ciudad) {
        List<String> pistas = new ArrayList<>();
        String c = ciudad.replace("\"", "\\\"");

        // Query principal: area por nombre exacto → busca sport=padel y variantes
        String q1 = "[out:json][timeout:25];" +
                "area[\"name\"=\"" + c + "\"]->.a;" +
                "(" +
                "nwr[\"sport\"=\"padel\"](area.a);" +
                "nwr[\"sport\"~\"padel\",i](area.a);" +
                "nwr[\"leisure\"=\"pitch\"][\"sport\"=\"padel\"](area.a);" +
                "nwr[\"leisure\"=\"sports_centre\"][\"sport\"~\"padel\",i](area.a);" +
                ");" +
                "out center tags;";
        pistas.addAll(ejecutarQueryOverpass(q1));

        // Fallback si la query principal no encontró nada:
        // busca el área filtrando también por tipo de lugar (ciudad/pueblo/municipio)
        if (pistas.isEmpty()) {
            String q2 = "[out:json][timeout:30];" +
                    "area[\"name\"=\"" + c + "\"][\"place\"~\"city|town|village|municipality\"]->.a;" +
                    "nwr[\"sport\"~\"padel\",i](area.a);" +
                    "out center tags;";
            pistas.addAll(ejecutarQueryOverpass(q2));
        }

        Collections.sort(pistas, (a, b) -> a.compareToIgnoreCase(b));
        return pistas;
    }

    // Lanza una query Overpass contra la API pública y extrae los nombres de las instalaciones.
    // Devuelve lista vacía si falla la red, si hay timeout o si el JSON es inválido.
    private List<String> ejecutarQueryOverpass(String query) {
        List<String> nombres = new ArrayList<>();
        try {
            String urlStr = "https://overpass-api.de/api/interpreter?data=" +
                    URLEncoder.encode(query, "UTF-8");
            HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setConnectTimeout(15_000);
            conn.setReadTimeout(30_000);
            conn.setRequestProperty("User-Agent", "PadelDart-TFG/1.0");
            if (conn.getResponseCode() != 200) { conn.disconnect(); return nombres; }

            StringBuilder sb = new StringBuilder();
            try (BufferedReader r = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), "UTF-8"))) {
                String ln;
                while ((ln = r.readLine()) != null) sb.append(ln);
            }
            conn.disconnect();

            JSONArray elements = new JSONObject(sb.toString()).optJSONArray("elements");
            if (elements == null) return nombres;
            for (int i = 0; i < elements.length(); i++) {
                JSONObject el = elements.getJSONObject(i);
                if (!el.has("tags")) continue;
                JSONObject tags = el.getJSONObject("tags");
                String name = tags.optString("name", "").trim();
                if (!name.isEmpty() && !nombres.contains(name)) nombres.add(name);
            }
        } catch (Exception ignored) {}
        return nombres;
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

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers de construcción de UI para los formularios
    // ─────────────────────────────────────────────────────────────────────────

    // Contenedor raíz de todos los diálogos personalizados.
    // Fondo casi negro con esquinas redondeadas para el tema oscuro de la app.
    private LinearLayout crearDialogLayout() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dpToPx(24), dpToPx(24), dpToPx(24), dpToPx(20));
        GradientDrawable fondo = new GradientDrawable();
        fondo.setColor(0xFF0D0D0D); fondo.setCornerRadius(dpToPx(24));
        layout.setBackground(fondo);
        return layout;
    }

    // Título verde en negrita que aparece en la cabecera de cada diálogo de formulario.
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

    // Etiqueta secundaria para los campos del formulario (verde lima, 12sp, negrita).
    private TextView crearLabel(String texto) {
        TextView tv = new TextView(this);
        tv.setText(texto);
        tv.setTextColor(getResources().getColor(R.color.verde_lima_suave, getTheme()));
        tv.setTextSize(12f); tv.setTypeface(null, Typeface.BOLD);
        return tv;
    }

    // Campo de texto con borde redondeado oscuro; numeric=true activa el teclado numérico.
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

    // Campo de texto multilínea para descripciones largas (2–4 líneas visibles).
    private EditText crearEditTextMultiline(String hint) {
        EditText et = new EditText(this);
        et.setHint(hint); et.setHintTextColor(0xFF666666);
        et.setTextColor(getResources().getColor(R.color.white, getTheme()));
        // Permitimos crecer hasta 4 líneas sin desbordar el diálogo
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

    // Botón principal: fondo verde lima, texto oscuro, esquinas redondeadas.
    // Usado para la acción afirmativa (Publicar, Siguiente, OK).
    private MaterialButton crearBotonPrimario(String texto) {
        MaterialButton btn = new MaterialButton(this);
        btn.setText(texto); btn.setTextColor(getResources().getColor(R.color.fondo_oscuro, getTheme()));
        btn.setBackgroundTintList(ColorStateList.valueOf(
                getResources().getColor(R.color.verde_lima, getTheme())));
        btn.setCornerRadius(dpToPx(12)); btn.setTextSize(14f);
        btn.setTypeface(null, Typeface.BOLD);
        return btn;
    }

    // Botón secundario: fondo oscuro neutro, texto gris.
    // Usado para Cancelar o acciones no destructivas alternativas.
    private MaterialButton crearBotonSecundario(String texto) {
        MaterialButton btn = new MaterialButton(this);
        btn.setText(texto); btn.setTextColor(getResources().getColor(R.color.texto_gris, getTheme()));
        btn.setBackgroundTintList(ColorStateList.valueOf(
                getResources().getColor(R.color.fondo_tarjeta_claro, getTheme())));
        btn.setCornerRadius(dpToPx(12)); btn.setTextSize(13f);
        return btn;
    }

    // Fila horizontal que contiene los dos botones de acción del formulario.
    // weightSum=2 permite distribuir los botones a partes iguales con weight=1f cada uno.
    private LinearLayout crearFilaBotones() {
        LinearLayout ll = new LinearLayout(this);
        ll.setOrientation(LinearLayout.HORIZONTAL); ll.setWeightSum(2f);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        p.setMargins(0, dpToPx(16), 0, 0); ll.setLayoutParams(p);
        return ll;
    }

    // Muestra el diálogo con fondo transparente para que se vea la forma redondeada del layout.
    // Sin setBackgroundDrawable(TRANSPARENT) el sistema pinta un rectángulo blanco por encima.
    private AlertDialog mostrarDialog(LinearLayout layout) {
        AlertDialog dialog = new AlertDialog.Builder(this).setView(layout).create();
        dialog.show();
        if (dialog.getWindow() != null)
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        return dialog;
    }

    // Inserta un espacio vertical vacío de altura dp dentro de un LinearLayout vertical.
    private void addSpacer(LinearLayout parent, int dp) {
        View spacer = new View(this);
        spacer.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(dp)));
        parent.addView(spacer);
    }

    // Chip pequeño de color con texto redondeado (marca/modelo en la magic card de PALA).
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

    // Tarjeta estándar con fondo de color de app, esquinas y sombra.
    // marginBottom permite separar visualmente las tarjetas en el listado.
    private MaterialCardView crearCard(int marginBottom) {
        MaterialCardView card = new MaterialCardView(this);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        p.setMargins(0, 0, 0, marginBottom); card.setLayoutParams(p);
        card.setRadius(dpToPx(12)); card.setCardElevation(dpToPx(4));
        card.setCardBackgroundColor(getResources().getColor(R.color.fondo_tarjeta, getTheme()));
        return card;
    }

    // Layout vertical interior de una tarjeta con padding vertical simétrico.
    private LinearLayout crearInnerLayout(int paddingV) {
        LinearLayout inner = new LinearLayout(this);
        inner.setOrientation(LinearLayout.VERTICAL);
        inner.setPadding(dpToPx(16), paddingV, dpToPx(16), paddingV);
        return inner;
    }

    // TextView de fecha alineado a la derecha en formato "dd MMM yyyy · HH:mm".
    // El timestamp es Unix milisegundos tal como lo almacena DatabaseHelper.
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

    // Devuelve la etiqueta visible para el badge de tipo de anuncio.
    private String textoTipo(String tipo) {
        switch (tipo != null ? tipo : "") {
            case "PARTIDA":         return "PARTIDA";
            case "PALA":            return "PALA";
            case "CLASE_OFRECER":   return "DAR CLASES";
            case "CLASE_SOLICITAR": return "BUSCO CLASES";
            default:                return tipo != null ? tipo : "?";
        }
    }

    // Devuelve el color de fondo del badge según el tipo de anuncio.
    // Los colores están coordinados con los RadioButtons del selector de tipo.
    private int colorTipo(String tipo) {
        switch (tipo != null ? tipo : "") {
            case "PARTIDA":         return 0xFFFF5252;   // rojo
            case "PALA":            return 0xFF2196F3;   // azul
            case "CLASE_OFRECER":   return 0xFFFF9800;   // naranja
            case "CLASE_SOLICITAR": return 0xFF9C27B0;   // morado
            default:                return 0xFF666666;
        }
    }

    // Muestra un mensaje de estado centrado en el área de anuncios
    // (por ejemplo: "Aún no hay anuncios" o "No hay resultados para este filtro").
    private void mostrarMensaje(String msg) {
        TextView tv = new TextView(this);
        tv.setText(msg);
        tv.setTextColor(getResources().getColor(R.color.texto_gris, getTheme()));
        tv.setTextSize(14f); tv.setGravity(Gravity.CENTER);
        tv.setPadding(dpToPx(16), dpToPx(48), dpToPx(16), dpToPx(16));
        llAnuncios.addView(tv);
    }

    // Convierte Object a String de forma segura, devolviendo "" si el valor no es String.
    private String strSafe(Object obj) {
        return (obj instanceof String) ? (String) obj : "";
    }

    // Convierte dp a píxeles usando la densidad de pantalla del dispositivo.
    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}
