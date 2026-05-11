package dam.iker.padeldart.dardos;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import java.util.Map;
import java.util.Random;

import dam.iker.padeldart.DatabaseHelper;
import dam.iker.padeldart.LoginActivity;
import dam.iker.padeldart.R;
import dam.iker.padeldart.SessionManager;

// Partida completa de 501. Implementa OnDardoListener para separar la lógica
// de puntuación de la lógica de UI, igual que haría un componente externo.
public class PartidaDardosActivity extends AppCompatActivity implements OnDardoListener {

    // ── Estado del juego ─────────────────────────────────────────────────────
    // "pvp" = dos humanos, "pve" = humano vs IA
    private String modo;

    // Puntuación restante de cada jugador (empieza en 501)
    private int puntosJ1 = 501;
    private int puntosJ2 = 501;

    // turnoActual: 1 = le toca al J1, 2 = le toca al J2/CPU
    private int turnoActual = 1;

    // Dardos lanzados en el turno actual (máximo 3)
    private int[] dartosTurno = new int[3];
    private int dartoIndex    = 0;  // índice del siguiente hueco libre

    // ── Estadísticas para el promedio por dardo ──────────────────────────────
    private int dartosJ1 = 0, sumaJ1 = 0;
    private int dartosJ2 = 0, sumaJ2 = 0;

    // Modo del teclado visible: 0=Simple, 1=Doble, 2=Triple
    private int modoTeclado = 0;

    // ── Vistas del marcador ──────────────────────────────────────────────────
    private TextView tvNombreJ1, tvNombreJ2;
    private TextView tvPuntosJ1, tvPuntosJ2;
    private TextView tvPromedioJ1, tvPromedioJ2;
    private TextView tvTotalTurno;
    private TextView tvDardo1, tvDardo2, tvDardo3;
    private LinearLayout panelJ1, panelJ2;

    // ── Teclados y botonera ──────────────────────────────────────────────────
    private View tecladoSimple, tecladoDoble, tecladoTriple;
    private MaterialButton btnModoSimple, btnModoDoble, btnModoTriple;
    private MaterialButton btnDeshacer, btnConfirmarTurno;

    // Handler para los delays visuales del turno de la IA
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random  random  = new Random();

    // ── Ciclo de vida ────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Protección de ruta: si no hay sesión activa, volvemos al login
        SessionManager session = SessionManager.getInstance(this);
        if (!session.haySesionActiva()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_partida_dardos);

        // Leemos el modo elegido en DardosActivity; "pvp" como fallback seguro
        modo = getIntent().getStringExtra("modo");
        if (modo == null) modo = "pvp";

        // Iniciamos con nombre por defecto; Firestore lo actualizará en cuanto responda
        initVistas("Jugador 1");
        initListenersTeclados();
        actualizarUI();

        // Carga asíncrona del nombre real del usuario logueado desde Firestore
        String uid = session.getUsuarioActualId();
        dam.iker.padeldart.FirebaseHelper.getInstance().obtenerUsuario(uid, usuario -> {
            if (usuario != null && tvNombreJ1 != null) {
                Object nombre = usuario.get(DatabaseHelper.COL_NOMBRE);
                if (nombre instanceof String && !((String) nombre).isEmpty()) {
                    tvNombreJ1.setText((String) nombre);
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Cancelar cualquier Runnable pendiente de la IA al salir de la pantalla
        handler.removeCallbacksAndMessages(null);
    }

    // ── Inicialización ───────────────────────────────────────────────────────

    // Enlaza las vistas del layout y configura los listeners de la botonera.
    private void initVistas(String nombreJ1) {
        tvNombreJ1    = findViewById(R.id.tvNombreJ1);
        tvNombreJ2    = findViewById(R.id.tvNombreJ2);
        tvPuntosJ1    = findViewById(R.id.tvPuntosJ1);
        tvPuntosJ2    = findViewById(R.id.tvPuntosJ2);
        tvPromedioJ1  = findViewById(R.id.tvPromedioJ1);
        tvPromedioJ2  = findViewById(R.id.tvPromedioJ2);
        tvTotalTurno  = findViewById(R.id.tvTotalTurno);
        tvDardo1      = findViewById(R.id.tvDardo1);
        tvDardo2      = findViewById(R.id.tvDardo2);
        tvDardo3      = findViewById(R.id.tvDardo3);
        panelJ1       = findViewById(R.id.panelJ1);
        panelJ2       = findViewById(R.id.panelJ2);

        tecladoSimple = findViewById(R.id.tecladoSimple);
        tecladoDoble  = findViewById(R.id.tecladoDoble);
        tecladoTriple = findViewById(R.id.tecladoTriple);

        btnModoSimple     = findViewById(R.id.btnModoSimple);
        btnModoDoble      = findViewById(R.id.btnModoDoble);
        btnModoTriple     = findViewById(R.id.btnModoTriple);
        btnDeshacer       = findViewById(R.id.btnDeshacer);
        btnConfirmarTurno = findViewById(R.id.btnConfirmarTurno);

        // Nombres de los jugadores: J2 es "CPU" en modo PvE
        tvNombreJ1.setText(nombreJ1);
        tvNombreJ2.setText("pve".equals(modo) ? "CPU" : "Jugador 2");

        // Cambio de teclado activo
        btnModoSimple.setOnClickListener(v -> cambiarModoTeclado(0));
        btnModoDoble.setOnClickListener(v  -> cambiarModoTeclado(1));
        btnModoTriple.setOnClickListener(v -> cambiarModoTeclado(2));

        // Deshacer retira el último dardo registrado del turno actual
        btnDeshacer.setOnClickListener(v -> deshacerUltimoDardo());

        // Confirmar cierra el turno aunque no se hayan lanzado los 3 dardos
        btnConfirmarTurno.setOnClickListener(v -> confirmarTurno());
    }

    // Registra listeners en cada botón de los tres teclados.
    // Los IDs son globales: Android busca en todo el árbol de vistas.
    private void initListenersTeclados() {

        // ── Teclado Simple (1-20, Bull=50, 25, Miss=0) ───────────────────────
        int[] idsS = { R.id.btn_s1,  R.id.btn_s2,  R.id.btn_s3,  R.id.btn_s4,  R.id.btn_s5,
                       R.id.btn_s6,  R.id.btn_s7,  R.id.btn_s8,  R.id.btn_s9,  R.id.btn_s10,
                       R.id.btn_s11, R.id.btn_s12, R.id.btn_s13, R.id.btn_s14, R.id.btn_s15,
                       R.id.btn_s16, R.id.btn_s17, R.id.btn_s18, R.id.btn_s19, R.id.btn_s20 };
        for (int i = 0; i < idsS.length; i++) {
            final int val = i + 1;  // sectores 1-20
            findViewById(idsS[i]).setOnClickListener(v -> registrarDardo(val));
        }
        // Zonas especiales fuera del rango 1-20
        findViewById(R.id.btn_bull).setOnClickListener(v -> registrarDardo(50));
        findViewById(R.id.btn_25).setOnClickListener(v  -> registrarDardo(25));
        findViewById(R.id.btn_miss).setOnClickListener(v -> registrarDardo(0));

        // ── Teclado Doble (D1=2 … D20=40, D-Bull=50) ────────────────────────
        int[] idsD = { R.id.btn_d1,  R.id.btn_d2,  R.id.btn_d3,  R.id.btn_d4,  R.id.btn_d5,
                       R.id.btn_d6,  R.id.btn_d7,  R.id.btn_d8,  R.id.btn_d9,  R.id.btn_d10,
                       R.id.btn_d11, R.id.btn_d12, R.id.btn_d13, R.id.btn_d14, R.id.btn_d15,
                       R.id.btn_d16, R.id.btn_d17, R.id.btn_d18, R.id.btn_d19, R.id.btn_d20 };
        for (int i = 0; i < idsD.length; i++) {
            final int val = (i + 1) * 2;  // D1=2, D2=4, …, D20=40
            findViewById(idsD[i]).setOnClickListener(v -> registrarDardo(val));
        }
        findViewById(R.id.btn_dbull).setOnClickListener(v -> registrarDardo(50));

        // ── Teclado Triple (T1=3 … T20=60) ───────────────────────────────────
        int[] idsT = { R.id.btn_t1,  R.id.btn_t2,  R.id.btn_t3,  R.id.btn_t4,  R.id.btn_t5,
                       R.id.btn_t6,  R.id.btn_t7,  R.id.btn_t8,  R.id.btn_t9,  R.id.btn_t10,
                       R.id.btn_t11, R.id.btn_t12, R.id.btn_t13, R.id.btn_t14, R.id.btn_t15,
                       R.id.btn_t16, R.id.btn_t17, R.id.btn_t18, R.id.btn_t19, R.id.btn_t20 };
        for (int i = 0; i < idsT.length; i++) {
            final int val = (i + 1) * 3;  // T1=3, T2=6, …, T20=60
            findViewById(idsT[i]).setOnClickListener(v -> registrarDardo(val));
        }
    }

    // ── Lógica de puntuación ─────────────────────────────────────────────────

    // Añade un dardo al turno actual y actualiza los indicadores visuales.
    // Si ya se lanzaron 3, el nuevo dardo se ignora (el botón confirmar ya habrá disparado).
    private void registrarDardo(int puntos) {
        if (dartoIndex >= 3) return;

        // Notificamos via la interfaz (útil si se conecta un observador externo)
        onPuntuacionMarcada(puntos);

        dartosTurno[dartoIndex] = puntos;
        dartoIndex++;
        actualizarSlotsDardos();
        actualizarTotalTurno();

        // Tras el tercer dardo se confirma automáticamente con un pequeño delay visual
        if (dartoIndex == 3) {
            handler.postDelayed(this::confirmarTurno, 700);
        }
    }

    // Retira el último dardo registrado en el turno, recuperando el slot.
    private void deshacerUltimoDardo() {
        if (dartoIndex == 0) return;
        dartoIndex--;
        dartosTurno[dartoIndex] = 0;
        actualizarSlotsDardos();
        actualizarTotalTurno();
    }

    // Aplica el turno al marcador: descuenta si no hay bust, o anula si hay bust.
    // Bust = quedar con puntuación negativa o en 1 (no se puede cerrar con 1).
    private void confirmarTurno() {
        // Cancelar el autoconfirm si el usuario pulsó el botón antes del delay
        handler.removeCallbacksAndMessages(null);

        int total          = calcularTotalTurno();
        int puntosActuales = (turnoActual == 1) ? puntosJ1 : puntosJ2;
        int nuevo          = puntosActuales - total;

        if (nuevo < 0 || nuevo == 1) {
            // Bust: el turno queda anulado, el marcador no cambia
            Toast.makeText(this, "¡Pasado! Turno anulado", Toast.LENGTH_SHORT).show();
            onTurnoCompletado(total, true);
        } else if (nuevo == 0) {
            // Victoria: el jugador llega exactamente a cero
            if (turnoActual == 1) { puntosJ1 = 0; dartosJ1 += dartoIndex; sumaJ1 += total; }
            else                  { puntosJ2 = 0; dartosJ2 += dartoIndex; sumaJ2 += total; }
            actualizarUI();
            onPartidaTerminada(turnoActual == 1
                    ? tvNombreJ1.getText().toString()
                    : tvNombreJ2.getText().toString());
        } else {
            // Turno normal: descontamos y pasamos al otro jugador
            if (turnoActual == 1) { puntosJ1 = nuevo; dartosJ1 += dartoIndex; sumaJ1 += total; }
            else                  { puntosJ2 = nuevo; dartosJ2 += dartoIndex; sumaJ2 += total; }
            onTurnoCompletado(total, false);
        }
    }

    // Suma los puntos de los dardos ya lanzados en el turno actual.
    private int calcularTotalTurno() {
        int t = 0;
        for (int i = 0; i < dartoIndex; i++) t += dartosTurno[i];
        return t;
    }

    // ── Callbacks de OnDardoListener ─────────────────────────────────────────

    @Override
    public void onPuntuacionMarcada(int puntos) {
        // Punto de extensión: aquí se podría reproducir un sonido o animar la diana
    }

    @Override
    public void onTurnoCompletado(int totalTurno, boolean busted) {
        // Alternamos el turno entre J1 y J2
        turnoActual = (turnoActual == 1) ? 2 : 1;
        resetTurno();
        actualizarUI();

        // Si es PvE y ahora toca a la CPU, ejecutamos el turno automático
        if ("pve".equals(modo) && turnoActual == 2) {
            handler.postDelayed(this::ejecutarTurnoIA, 900);
        }
    }

    @Override
    public void onPartidaTerminada(String ganadorNombre) {
        // Bloqueamos los teclados ocultando el keyboard activo mientras el diálogo está visible
        tecladoSimple.setVisibility(View.GONE);
        tecladoDoble.setVisibility(View.GONE);
        tecladoTriple.setVisibility(View.GONE);

        // Diálogo de fin de partida con opciones para repetir o salir
        new AlertDialog.Builder(this)
                .setTitle("Partida terminada")
                .setMessage("¡" + ganadorNombre + " gana!\n\n"
                        + tvNombreJ1.getText() + ": avg " + formatAvg(sumaJ1, dartosJ1) + " pts/dardo\n"
                        + tvNombreJ2.getText() + ": avg " + formatAvg(sumaJ2, dartosJ2) + " pts/dardo")
                .setPositiveButton("Jugar de nuevo", (d, w) -> reiniciarPartida())
                .setNegativeButton("Salir", (d, w) -> finish())
                .setCancelable(false)
                .show();
    }

    // ── Turno de la IA ───────────────────────────────────────────────────────

    // Simula los 3 lanzamientos de la CPU con un delay entre cada uno para
    // que el jugador pueda seguir visualmente lo que hace la IA.
    private void ejecutarTurnoIA() {
        // Pre-calculamos los 3 dardos antes de empezar los delays
        int[] dartosIA    = new int[3];
        int   acumulado   = 0;
        int   numDartos   = 0;

        for (int i = 0; i < 3; i++) {
            int pendiente = puntosJ2 - acumulado;
            int val       = generarDardoIA(pendiente);
            int resultado = pendiente - val;

            // Bust de la IA: detenemos la generación y marcamos bust en esa tirada
            if (resultado < 0 || resultado == 1) {
                dartosIA[i] = val;
                numDartos   = i + 1;
                acumulado  += val;
                break;
            }
            dartosIA[i] = val;
            acumulado  += val;
            numDartos   = i + 1;

            // Victoria de la IA: no hace falta lanzar más dardos
            if (resultado == 0) break;
        }

        // Copiamos al campo temporal antes de los lambdas: los lambdas no pueden
        // capturar la variable local dartosIA[] directamente en Android (no es effectively final).
        System.arraycopy(dartosIA, 0, dartosIA_temp, 0, 3);

        final int totalDartos = numDartos;
        final int totalFinal  = acumulado;

        // Mostramos cada dardo uno a uno con delay de 600 ms entre ellos
        for (int i = 0; i < totalDartos; i++) {
            final int idx = i;
            final int val = dartosIA[i];
            handler.postDelayed(() -> mostrarDardoIA(idx, val), (idx + 1) * 600L);
        }

        // Tras mostrar todos, aplicamos el resultado con un delay extra
        handler.postDelayed(() -> aplicarResultadoIA(totalFinal, totalDartos),
                (totalDartos + 1) * 600L + 300L);
    }

    // Rellena el slot visual del dardo idx con el valor lanzado por la IA.
    private void mostrarDardoIA(int idx, int valor) {
        TextView[] slots = { tvDardo1, tvDardo2, tvDardo3 };
        if (idx < slots.length) {
            slots[idx].setText(String.valueOf(valor));
            // Color azul para distinguir los dardos de la IA de los del jugador
            slots[idx].setTextColor(Color.parseColor("#2196F3"));
        }
        // Actualizamos el total parcial que se muestra en el centro del marcador
        tvTotalTurno.setText(String.valueOf(sumarHasta(dartosIA_temp, idx + 1)));
    }

    // Almacén temporal para que mostrarDardoIA pueda acceder a los valores.
    // Se inicializa en ejecutarTurnoIA y se descarta al aplicar el resultado.
    private int[] dartosIA_temp = new int[3];

    // Evalúa el resultado del turno de la IA y actualiza el estado del juego.
    private void aplicarResultadoIA(int total, int numDartos) {
        int nuevo = puntosJ2 - total;

        if (nuevo == 0) {
            // Victoria de la IA
            puntosJ2 = 0;
            dartosJ2 += numDartos;
            sumaJ2   += total;
            actualizarUI();
            onPartidaTerminada(tvNombreJ2.getText().toString());
        } else if (nuevo < 0 || nuevo == 1) {
            // Bust: la IA no descuenta nada
            Toast.makeText(this, "¡CPU pasada!", Toast.LENGTH_SHORT).show();
            turnoActual = 1;
            resetTurno();
            actualizarUI();
        } else {
            // Turno normal: descontamos y devolvemos el control al jugador
            puntosJ2  = nuevo;
            dartosJ2 += numDartos;
            sumaJ2   += total;
            turnoActual = 1;
            resetTurno();
            actualizarUI();
        }
    }

    // Genera un valor de dardo razonablemente inteligente para la IA.
    // En los últimos tramos intenta acabar; en el resto prefiere valores altos.
    private int generarDardoIA(int pendiente) {
        if (pendiente <= 0) return 0;

        // Si puede acabar con un doble, lo intenta con un 35 % de probabilidad
        if (pendiente <= 40 && pendiente % 2 == 0 && random.nextInt(100) < 35) {
            return pendiente;
        }

        // Con poca puntuación restante, lanza valores pequeños para no pasarse
        if (pendiente <= 10) {
            return Math.max(0, random.nextInt(pendiente));
        }

        // En rango normal prefiere T20(60) > T19(57) > T18(54) > valores medianos
        int[] buenos = { 60, 57, 54, 45, 40, 38, 36, 26, 24, 20, 19, 18 };
        for (int v : buenos) {
            if (v < pendiente && random.nextInt(100) < 55) return v;
        }
        // Fallback: sector aleatorio del 1 al 20
        return random.nextInt(20) + 1;
    }

    // Suma los primeros 'hasta' elementos de un array de dardos.
    private int sumarHasta(int[] arr, int hasta) {
        int s = 0;
        for (int i = 0; i < hasta && i < arr.length; i++) s += arr[i];
        return s;
    }

    // ── Actualización de UI ──────────────────────────────────────────────────

    // Sincroniza todo el marcador con el estado actual del juego.
    private void actualizarUI() {
        tvPuntosJ1.setText(String.valueOf(puntosJ1));
        tvPuntosJ2.setText(String.valueOf(puntosJ2));
        tvPromedioJ1.setText("Avg: " + formatAvg(sumaJ1, dartosJ1));
        tvPromedioJ2.setText("Avg: " + formatAvg(sumaJ2, dartosJ2));

        // Color de fondo del panel activo: verde oscuro sutil, el inactivo más oscuro
        int activo   = Color.parseColor("#1B2E1B");
        int inactivo = Color.parseColor("#2A2A2A");
        panelJ1.setBackgroundColor(turnoActual == 1 ? activo : inactivo);
        panelJ2.setBackgroundColor(turnoActual == 2 ? activo : inactivo);

        // El teclado solo es táctil cuando le toca al jugador humano
        boolean esTurnoHumano = (turnoActual == 1) || "pvp".equals(modo);
        actualizarVisibilidadTeclado(esTurnoHumano);
        btnConfirmarTurno.setEnabled(esTurnoHumano && dartoIndex > 0);
        btnDeshacer.setEnabled(esTurnoHumano && dartoIndex > 0);
    }

    // Muestra el teclado correspondiente al modo activo y oculta los otros.
    private void actualizarVisibilidadTeclado(boolean habilitado) {
        tecladoSimple.setVisibility(habilitado && modoTeclado == 0 ? View.VISIBLE : View.GONE);
        tecladoDoble.setVisibility(habilitado  && modoTeclado == 1 ? View.VISIBLE : View.GONE);
        tecladoTriple.setVisibility(habilitado && modoTeclado == 2 ? View.VISIBLE : View.GONE);
    }

    // Rellena los slots de dardos con sus valores o los vacía si aún no se lanzaron.
    private void actualizarSlotsDardos() {
        TextView[] slots = { tvDardo1, tvDardo2, tvDardo3 };
        for (int i = 0; i < 3; i++) {
            if (i < dartoIndex) {
                slots[i].setText(String.valueOf(dartosTurno[i]));
                slots[i].setTextColor(Color.parseColor("#94D500"));  // verde lima
            } else {
                slots[i].setText("—");
                slots[i].setTextColor(Color.parseColor("#A0A0A0"));  // gris
            }
        }
    }

    private void actualizarTotalTurno() {
        tvTotalTurno.setText(String.valueOf(calcularTotalTurno()));
    }

    // Cambia el teclado activo y actualiza los estilos de los botones de modo.
    private void cambiarModoTeclado(int nuevo) {
        modoTeclado = nuevo;

        // Resaltamos el botón del modo activo y dejamos los otros como outlined
        boolean esTurnoHumano = (turnoActual == 1) || "pvp".equals(modo);
        actualizarVisibilidadTeclado(esTurnoHumano);

        // El botón activo tiene fondo sólido; los inactivos solo borde
        btnModoSimple.setTextColor(Color.parseColor(modoTeclado == 0 ? "#121212" : "#FFFFFF"));
        btnModoDoble.setTextColor(Color.parseColor("#2196F3"));
        btnModoTriple.setTextColor(Color.parseColor("#FF9800"));
    }

    // Reinicia todos los contadores para jugar una partida nueva.
    private void reiniciarPartida() {
        puntosJ1 = puntosJ2 = 501;
        turnoActual = 1;
        dartosJ1 = sumaJ1 = dartosJ2 = sumaJ2 = 0;
        modoTeclado = 0;
        resetTurno();
        actualizarUI();
    }

    // Borra el turno actual (slots + índice) y refresca los indicadores.
    private void resetTurno() {
        dartosTurno   = new int[3];
        dartosIA_temp = new int[3];
        dartoIndex    = 0;
        actualizarSlotsDardos();
        actualizarTotalTurno();
    }

    // Formatea el promedio por dardo para mostrarlo en una línea: "X.X"
    private String formatAvg(int suma, int count) {
        if (count == 0) return "0.0";
        return String.format("%.1f", (float) suma / count);
    }
}
