package com.igarcia;
// Comentario de JAVADOC /** */

/**
 * Clase para generar objetos de Videojuegos. Estos objetos están caracterizados por
 * su título, el precio y el stock que tienen. De todos los juegos queremos obtenero su precio
 * por unidad y su precio con impuestos.
 * @author Iker García Martínez
 */
public class VideoJuegos {

    /**
     * Atributo que indica el titulo del juego (expresado sin abreviaturas)
     */
    private String titulo;



    /**
     * Atributo que indica el precio del juego (se expresará en la moneda €)
     */
    private double precio;

    /**
     * Atributo que indica el stock del juego (se expresa en enteros)
     */
    private double stock;

    /**
     * Constructor sin parámetros: Contiene un Juego de ejemplo.
     */
    public VideoJuegos() {
        titulo = "VideoJuegoLife2";
        stock = 10;
        precio = 3.99;
    }

    /**
     * Constructor de uso general: Nos permite seleccionar los valores del juego (titulo, precio y stock)
     * @param titulo
     * @param precio Medido en Euros (€)
     * @param stock Medido en enteros
     */
    public VideoJuegos(String titulo, double precio,  double stock) {
        this.titulo = titulo;
        this.precio = precio;
        this.stock = stock;
    }

    /**
     * Retorna el titulo del juego
     * @return El valor del titulo
     */
    public String getTitulo() {
        return titulo;
    }

    /**
     * Método que permite cambiar el título del juego
     * @param  titulo El nuevo valor de título
     */
    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    /**
     * Retorna el precio del juego
     * @return El valor del juego en €
     */
    public double getPrecio() {
        return precio;
    }

    /**
     * Método que permite cambiar el precio del juego
     * @param precio El nuevo valor de precio
     */
    public void setPrecio(double precio) {
        this.precio = precio;
    }

    /**
     * Retorna el stock del juego
     * @return El valor de stock en enteros
     */
    public double getStock() {
        return stock;
    }

    /**
     * Método que permite cambiar el stock del juego
     * @param stock El nuebo valor de stock
     */
    public void setStock(int stock) {
        this.stock = stock;
    }

    /**
     * Retorna valor de los atributos
     * @return Retorna el título, el precio y el stock de los juegos
     */
    @Override
    public String toString() {
        return "VideoJuegos{" +
                "titulo='" + titulo + '\'' +
                ", stock=" + stock +
                ", precio=" + precio + "€" +
                '}';
    }

    /**
     * Método de cálculo para el precio por unidad
     * @return Precio en euros por unidad
     */
    public double precioUd(){return stock*precio;}

    /**
     * Método de cálculo para los impuestos de cada videojuego
     * @return Precio en euros con Impuestos añadidos
     */
    public double precioIVA(){return precio*1.21;}
}
