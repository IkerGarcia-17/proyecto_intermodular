package dam.iker.padeldart;

import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

// Pantalla de chat entre dos usuarios. Los mensajes se almacenan en Firestore
// y se muestran en tiempo real mediante un SnapshotListener de Firestore.
// Burbujas: verde (enviados) y gris oscuro (recibidos).
public class ConversacionActivity extends AppCompatActivity {

    private FirebaseHelper fb;
    private SessionManager session;

    // UIDs de ambos extremos de la conversación (ahora son String, no long)
    private String miId;
    private String receptorId;

    // Referencias a las vistas de la conversación
    private LinearLayout     llMensajes;
    private ScrollView       scrollMensajes;
    private TextInputEditText etMensaje;

    // Listener en tiempo real de Firestore; hay que cancelarlo en onDestroy para evitar fugas
    private ListenerRegistration mensajesListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversacion);

        fb      = FirebaseHelper.getInstance();
        session = SessionManager.getInstance(this);
        miId    = session.getUsuarioActualId();

        // Datos del interlocutor recibidos desde ChatListActivity vía Intent extras
        receptorId = getIntent().getStringExtra("receptor_id"); // String UID de Firebase
        String nombre    = getIntent().getStringExtra("receptor_nombre");
        String apellidos = getIntent().getStringExtra("receptor_apellidos");
        String categoria = getIntent().getStringExtra("receptor_categoria");

        // Si no hay receptor válido no podemos abrir el chat
        if (receptorId == null || receptorId.isEmpty()) { finish(); return; }

        // Configuramos la cabecera con los datos del interlocutor
        TextView tvAvatar = findViewById(R.id.tvAvatarHeader);
        tvAvatar.setText(nombre != null && !nombre.isEmpty()
                ? String.valueOf(nombre.charAt(0)).toUpperCase() : "?");

        TextView tvNombre = findViewById(R.id.tvNombreInterlocutor);
        tvNombre.setText((nombre != null ? nombre : "") + " " + (apellidos != null ? apellidos : ""));

        TextView tvCategoria = findViewById(R.id.tvCategoriaInterlocutor);
        tvCategoria.setText(categoria != null ? categoria : "");

        // Enlazamos las vistas del área de mensajes y el campo de texto
        llMensajes     = findViewById(R.id.llMensajes);
        scrollMensajes = findViewById(R.id.scrollMensajes);
        etMensaje      = findViewById(R.id.etMensaje);

        findViewById(R.id.btnAtras).setOnClickListener(v -> finish());

        // Iniciamos el listener en tiempo real; se actualiza automáticamente con nuevos mensajes
        mensajesListener = fb.escucharConversacion(miId, receptorId, this::mostrarMensajes);

        // Al pulsar Enviar: guardamos el mensaje en Firestore (el listener lo mostrará)
        findViewById(R.id.btnEnviar).setOnClickListener(v -> {
            String texto = etMensaje.getText().toString().trim();
            if (texto.isEmpty()) return;

            etMensaje.setText(""); // Limpiamos el campo antes de la respuesta asíncrona
            fb.enviarMensaje(miId, receptorId, texto, ok -> {
                if (!ok) {
                    Toast.makeText(this, "Error al enviar el mensaje", Toast.LENGTH_SHORT).show();
                }
                // Si ok=true el SnapshotListener ya redibuja la lista automáticamente
            });
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Cancelamos el listener para evitar actualizaciones en una Activity ya destruida
        if (mensajesListener != null) mensajesListener.remove();
    }

    // Limpia la lista y redibuja todos los mensajes con los datos del snapshot
    private void mostrarMensajes(List<Map<String, Object>> mensajes) {
        llMensajes.removeAllViews();
        for (Map<String, Object> m : mensajes) {
            String contenido = (String) m.get("contenido");
            String emisorId  = (String) m.get("emisor_id");
            long   timestamp = m.get("timestamp") instanceof Number
                    ? ((Number) m.get("timestamp")).longValue() : 0L;
            // Comparamos los UIDs como String para saber si el mensaje es mío
            agregarBurbuja(contenido, miId.equals(emisorId), timestamp);
        }
        scrollAlFinal();
    }

    // Crea una burbuja de mensaje y la añade al contenedor.
    // esMio=true → burbuja verde a la derecha; esMio=false → gris a la izquierda.
    private void agregarBurbuja(String texto, boolean esMio, long timestamp) {
        // Wrapper para la alineación (izquierda o derecha)
        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams wrapperParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        wrapperParams.setMargins(0, 4, 0, 4);
        wrapper.setLayoutParams(wrapperParams);
        wrapper.setGravity(esMio ? Gravity.END : Gravity.START);

        // Burbuja de texto con esquinas redondeadas usando GradientDrawable
        TextView tvTexto = new TextView(this);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        // Máximo 75% de ancho de pantalla para que las burbujas no sean demasiado anchas
        textParams.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.75f);
        tvTexto.setLayoutParams(textParams);
        tvTexto.setText(texto);
        tvTexto.setTextSize(14f);
        tvTexto.setPadding(dpToPx(12), dpToPx(10), dpToPx(12), dpToPx(10));

        // Color de texto y fondo según si es mensaje enviado o recibido
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dpToPx(16));
        if (esMio) {
            tvTexto.setTextColor(getResources().getColor(R.color.fondo_oscuro, getTheme()));
            bg.setColor(getResources().getColor(R.color.verde_lima, getTheme()));
        } else {
            tvTexto.setTextColor(getResources().getColor(R.color.white, getTheme()));
            bg.setColor(getResources().getColor(R.color.fondo_tarjeta_claro, getTheme()));
        }
        tvTexto.setBackground(bg);

        // Timestamp bajo la burbuja en formato HH:mm
        TextView tvHora = new TextView(this);
        String hora = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(timestamp));
        tvHora.setText(hora);
        tvHora.setTextSize(10f);
        tvHora.setTextColor(getResources().getColor(R.color.texto_gris_suave, getTheme()));
        LinearLayout.LayoutParams horaParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        horaParams.setMargins(dpToPx(4), 2, dpToPx(4), 0);
        tvHora.setLayoutParams(horaParams);

        wrapper.addView(tvTexto);
        wrapper.addView(tvHora);
        llMensajes.addView(wrapper);
    }

    // Desplaza el scroll hasta el último mensaje enviado
    private void scrollAlFinal() {
        scrollMensajes.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        scrollMensajes.fullScroll(ScrollView.FOCUS_DOWN);
                        scrollMensajes.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    }
                });
    }

    // Convierte dp a píxeles usando la densidad de la pantalla
    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}
