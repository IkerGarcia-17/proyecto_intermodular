package dam.iker.padeldart;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.RadioButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

import com.google.android.material.card.MaterialCardView;

// Pantalla de ajustes: permite cambiar el idioma entre español e inglés.
// Usa AppCompatDelegate.setApplicationLocales() que funciona desde API 24
// y persiste el idioma elegido entre sesiones (no hace falta guardar en prefs).
public class SettingsActivity extends AppCompatActivity {

    private static final String PREFS_SETTINGS = "padeldart_settings";
    private static final String KEY_LANG        = "idioma";  // "es" o "en"

    private RadioButton rbEspanol, rbEnglish;
    private MaterialCardView cardEspanol, cardEnglish;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        rbEspanol  = findViewById(R.id.rbEspanol);
        rbEnglish  = findViewById(R.id.rbEnglish);
        cardEspanol = findViewById(R.id.cardEspanol);
        cardEnglish = findViewById(R.id.cardEnglish);

        // Marcamos el idioma actualmente guardado en preferencias
        String idiomaActual = leerIdioma();
        seleccionarIdioma("en".equals(idiomaActual) ? "en" : "es");

        // Pulsar la tarjeta completa actúa igual que pulsar el radio button
        cardEspanol.setOnClickListener(v -> seleccionarIdioma("es"));
        cardEnglish.setOnClickListener(v -> seleccionarIdioma("en"));
        rbEspanol.setOnClickListener(v  -> seleccionarIdioma("es"));
        rbEnglish.setOnClickListener(v  -> seleccionarIdioma("en"));

        // Botón Atrás de la cabecera
        findViewById(R.id.btnAtrasSettings).setOnClickListener(v -> finish());

        // Guardar aplica el cambio de locale y reinicia la app para que surta efecto
        findViewById(R.id.btnGuardarSettings).setOnClickListener(v -> aplicarCambioIdioma());
    }

    // Actualiza los radio buttons y el borde de las tarjetas según la selección
    private void seleccionarIdioma(String lang) {
        boolean esEspanol = "es".equals(lang);
        rbEspanol.setChecked(esEspanol);
        rbEnglish.setChecked(!esEspanol);

        // Resaltamos la tarjeta activa con el borde verde corporativo
        cardEspanol.setStrokeWidth(esEspanol ? 2 : 1);
        cardEnglish.setStrokeWidth(esEspanol ? 1 : 2);
    }

    // Persiste la elección y cambia el locale del sistema de forma inmediata
    private void aplicarCambioIdioma() {
        String lang = rbEnglish.isChecked() ? "en" : "es";
        guardarIdioma(lang);

        // LocaleListCompat crea la lista de locales compatibles con todas las versiones de Android
        LocaleListCompat appLocale = LocaleListCompat.forLanguageTags(lang);
        AppCompatDelegate.setApplicationLocales(appLocale);

        Toast.makeText(this, getString(R.string.settings_reiniciando), Toast.LENGTH_SHORT).show();
        finish();
    }

    // Lee el idioma guardado; devuelve "es" por defecto si no hay preferencia guardada
    private String leerIdioma() {
        SharedPreferences prefs = getSharedPreferences(PREFS_SETTINGS, MODE_PRIVATE);
        return prefs.getString(KEY_LANG, "es");
    }

    // Guarda la preferencia de idioma en SharedPreferences para recordarla entre sesiones
    private void guardarIdioma(String lang) {
        getSharedPreferences(PREFS_SETTINGS, MODE_PRIVATE)
                .edit().putString(KEY_LANG, lang).apply();
    }
}
