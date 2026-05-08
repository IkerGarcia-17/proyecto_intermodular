package dam.iker.padeldart.dardos;

import android.content.Intent;
import android.os.Bundle;

import com.google.android.material.card.MaterialCardView;

import dam.iker.padeldart.BaseDrawerActivity;
import dam.iker.padeldart.R;
import dam.iker.padeldart.SessionManager;
import dam.iker.padeldart.LoginActivity;

// Hub del módulo de dardos: PvP o PvE. Extiende BaseDrawerActivity para el drawer.
public class DardosActivity extends BaseDrawerActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Protección de ruta: si no hay sesión, volvemos al login
        if (!SessionManager.getInstance(this).haySesionActiva()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_dardos);

        // Tarjeta PvP: arranca la partida pasando el modo como extra del Intent
        MaterialCardView cardPvP = findViewById(R.id.cardPvP);
        cardPvP.setOnClickListener(v -> lanzarPartida("pvp"));

        // Tarjeta PvE: el segundo "jugador" lo controla la IA
        MaterialCardView cardPvE = findViewById(R.id.cardPvE);
        cardPvE.setOnClickListener(v -> lanzarPartida("pve"));
    }

    // Crea el Intent hacia PartidaDardosActivity incluyendo el modo elegido.
    // La pantalla de partida usa este extra para saber si el J2 es humano o CPU.
    private void lanzarPartida(String modo) {
        Intent intent = new Intent(this, PartidaDardosActivity.class);
        intent.putExtra("modo", modo);
        startActivity(intent);
    }
}
