package dam.iker.padeldart;

import android.content.Intent;
import android.os.Bundle;
import com.google.android.material.card.MaterialCardView;

// Hub principal del módulo de Pádel. Extiende BaseDrawerActivity para menú lateral.
public class PadelActivity extends BaseDrawerActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_padel);

        // Botón de volver al menú principal
        findViewById(R.id.btnAtras).setOnClickListener(v -> finish());

        // Tarjeta de chat: lista de usuarios para iniciar conversaciones privadas
        MaterialCardView cardChatAmigos = findViewById(R.id.cardChatAmigos);
        cardChatAmigos.setOnClickListener(v ->
                startActivity(new Intent(this, ChatListActivity.class)));

        // Tarjeta de zona: tablón de anuncios filtrado por provincia del usuario
        MaterialCardView cardZona = findViewById(R.id.cardZona);
        cardZona.setOnClickListener(v ->
                startActivity(new Intent(this, ZonaActivity.class)));
    }
}
