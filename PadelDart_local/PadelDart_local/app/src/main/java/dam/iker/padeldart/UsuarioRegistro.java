package dam.iker.padeldart;

import java.io.Serializable;

// Contenedor de datos que viaja entre las 4 pantallas del registro via Intent.
// Serializable es suficiente para este uso: no necesitamos Parcelable porque
// el objeto no va a pasar por IPC entre procesos distintos.
public class UsuarioRegistro implements Serializable {

    // --- Paso 1: Datos personales ---
    public String nombre;
    public String apellidos;
    public String dni;
    public String email;
    public String password;
    public String direccion;
    public String cp;

    // --- Paso 2: Perfil pádel ---
    public double nivelPadel;
    public String categoriaPadel;
    public String posicion;
    public String provincia;
    public String pistaFavorita;

    // --- Paso 3: Perfil dardos ---
    public boolean tieneDiana;
    public String tipoDiana;

    // --- Paso 4: Pago ---
    public String metodoPago;
}
