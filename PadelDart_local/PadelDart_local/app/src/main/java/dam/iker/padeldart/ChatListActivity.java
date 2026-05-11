package dam.iker.padeldart;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.card.MaterialCardView;

import java.util.List;
import java.util.Map;

// Lista de usuarios para iniciar conversación privada. Extiende BaseDrawerActivity.
// En local mostramos todos los usuarios registrados en la BD SQLite.
public class ChatListActivity extends BaseDrawerActivity {

    private DatabaseHelper db;
    private SessionManager session;
    private LinearLayout llUsuarios;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_list);

        db      = DatabaseHelper.getInstance(this);
        session = SessionManager.getInstance(this);

        llUsuarios = findViewById(R.id.llUsuarios);
        findViewById(R.id.btnAtras).setOnClickListener(v -> finish());

        // Cargamos la lista de otros usuarios (excluyendo al propio usuario logueado)
        long miId = session.getUsuarioActualId();
        List<Map<String, Object>> usuarios = db.obtenerTodosUsuariosMenos(miId);

        if (usuarios.isEmpty()) {
            // Si no hay más usuarios registrados, avisamos al usuario
            mostrarMensajeVacio();
        } else {
            // Creamos una tarjeta por cada usuario encontrado
            for (Map<String, Object> u : usuarios) {
                añadirTarjetaUsuario(u);
            }
        }
    }

    // Crea y añade una tarjeta de usuario al contenedor de forma programática.
    // Esto evita tener que usar un RecyclerView + Adapter para listas simples.
    private void añadirTarjetaUsuario(Map<String, Object> usuario) {
        long   uid       = (Long)   usuario.get(DatabaseHelper.COL_ID);
        String nombre    = (String) usuario.get(DatabaseHelper.COL_NOMBRE);
        String apellidos = (String) usuario.get(DatabaseHelper.COL_APELLIDOS);
        String categoria = (String) usuario.get(DatabaseHelper.COL_CATEGORIA);
        String provincia = (String) usuario.get(DatabaseHelper.COL_PROVINCIA);

        // --- Tarjeta contenedora ---
        MaterialCardView card = new MaterialCardView(this);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(16, 6, 16, 6);
        card.setLayoutParams(cardParams);
        card.setRadius(dpToPx(12));
        card.setCardElevation(dpToPx(3));
        card.setCardBackgroundColor(getResources().getColor(R.color.fondo_tarjeta, getTheme()));
        card.setClickable(true);
        card.setFocusable(true);

        // --- Layout horizontal dentro de la tarjeta ---
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dpToPx(16), dpToPx(14), dpToPx(16), dpToPx(14));

        // --- Avatar circular con la primera letra del nombre ---
        MaterialCardView avatar = new MaterialCardView(this);
        LinearLayout.LayoutParams avatarParams = new LinearLayout.LayoutParams(dpToPx(44), dpToPx(44));
        avatar.setLayoutParams(avatarParams);
        avatar.setRadius(dpToPx(22));
        avatar.setCardElevation(0f);
        avatar.setCardBackgroundColor(getResources().getColor(R.color.verde_lima, getTheme()));

        TextView tvInicial = new TextView(this);
        tvInicial.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        tvInicial.setGravity(Gravity.CENTER);
        tvInicial.setTextSize(18f);
        tvInicial.setTextColor(getResources().getColor(R.color.fondo_oscuro, getTheme()));
        tvInicial.setTypeface(null, android.graphics.Typeface.BOLD);
        // La inicial es la primera letra del nombre en mayúscula
        tvInicial.setText(nombre != null && !nombre.isEmpty()
                ? String.valueOf(nombre.charAt(0)).toUpperCase() : "?");

        avatar.addView(tvInicial);
        row.addView(avatar);

        // --- Columna de texto: nombre y categoría ---
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams colParams = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        colParams.setMarginStart(dpToPx(14));
        col.setLayoutParams(colParams);

        TextView tvNombre = new TextView(this);
        tvNombre.setText((nombre != null ? nombre : "") + " " + (apellidos != null ? apellidos : ""));
        tvNombre.setTextColor(getResources().getColor(R.color.white, getTheme()));
        tvNombre.setTextSize(15f);
        tvNombre.setTypeface(null, android.graphics.Typeface.BOLD);

        // Mostramos categoría y provincia juntas como subtítulo
        TextView tvInfo = new TextView(this);
        String info = (categoria != null ? categoria : "Sin categoría") +
                      (provincia != null ? "  ·  " + provincia : "");
        tvInfo.setText(info);
        tvInfo.setTextColor(getResources().getColor(R.color.texto_gris, getTheme()));
        tvInfo.setTextSize(12f);

        col.addView(tvNombre);
        col.addView(tvInfo);
        row.addView(col);

        // Flecha indicativa de que el elemento es pulsable
        TextView tvArrow = new TextView(this);
        tvArrow.setText("›");
        tvArrow.setTextColor(getResources().getColor(R.color.verde_lima, getTheme()));
        tvArrow.setTextSize(24f);
        row.addView(tvArrow);

        card.addView(row);
        llUsuarios.addView(card);

        // Al pulsar la tarjeta abrimos la conversación con ese usuario
        card.setOnClickListener(v -> abrirConversacion(uid, nombre, apellidos, categoria));
    }

    // Lanza la pantalla de conversación pasando los datos del interlocutor como extras
    private void abrirConversacion(long userId, String nombre, String apellidos, String categoria) {
        Intent intent = new Intent(this, ConversacionActivity.class);
        intent.putExtra("receptor_id",       userId);
        intent.putExtra("receptor_nombre",   nombre != null ? nombre : "");
        intent.putExtra("receptor_apellidos", apellidos != null ? apellidos : "");
        intent.putExtra("receptor_categoria", categoria != null ? categoria : "");
        startActivity(intent);
    }

    // Muestra un texto informativo si no hay otros usuarios en la BD
    private void mostrarMensajeVacio() {
        TextView tv = new TextView(this);
        tv.setText("Aún no hay otros jugadores registrados.\nRegistra más cuentas para empezar a chatear.");
        tv.setTextColor(getResources().getColor(R.color.texto_gris, getTheme()));
        tv.setTextSize(15f);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(dpToPx(32), dpToPx(48), dpToPx(32), dpToPx(16));
        llUsuarios.addView(tv);
    }

    // Convierte dp a píxeles usando la densidad de la pantalla actual
    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}
