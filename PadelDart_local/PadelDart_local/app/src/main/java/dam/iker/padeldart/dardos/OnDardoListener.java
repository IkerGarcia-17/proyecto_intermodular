package dam.iker.padeldart.dardos;

// Interfaz de callbacks para el sistema de puntuación de dardos.
// La implementa la Activity del juego para reaccionar a cada evento
// sin que el teclado necesite conocer la Activity directamente.
public interface OnDardoListener {

    // Se dispara cada vez que el jugador marca una puntuación con un dardo.
    // puntos: valor final del dardo (ya multiplicado por doble/triple si procede).
    void onPuntuacionMarcada(int puntos);

    // Se dispara cuando un jugador completa su turno (3 dardos).
    // totalTurno: suma de los 3 dardos; busted: true si se pasó de 0.
    void onTurnoCompletado(int totalTurno, boolean busted);

    // Se dispara cuando alguien llega exactamente a 0 y gana la partida.
    // ganadorNombre: nombre del jugador ganador para mostrarlo en pantalla.
    void onPartidaTerminada(String ganadorNombre);
}
