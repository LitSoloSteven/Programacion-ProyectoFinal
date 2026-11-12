package ni.edu.uam.proyectofinalprogramacionappescritorio.modelo;

import jakarta.persistence.*;

/** Una fila compartida; se bloquea hasta finalizar la transaccion financiera. */
@Entity
@Table(name = "contador_recibo", schema = "latch")
public class ContadorRecibo {
    public static final String CLAVE = "RECIBOS";
    @Id
    @Column(length = 16, updatable = false)
    private String id;
    @Column(name = "ultimo_numero", nullable = false)
    private long ultimoNumero;

    protected ContadorRecibo() { }

    /** Para inicializacion de una base vacia, no para cada solicitud. */
    public static ContadorRecibo inicial() {
        ContadorRecibo contador = new ContadorRecibo();
        contador.id = CLAVE;
        return contador;
    }

    /** Invocar solo bajo bloqueo exclusivo en la transaccion del pago. */
    public long siguiente() {
        if (ultimoNumero < 0) throw new IllegalStateException("El contador esta corrupto.");
        long nuevo;
        try { nuevo = Math.addExact(ultimoNumero, 1); }
        catch (ArithmeticException e) {
            throw new IllegalStateException("Se agoto el rango de numeros de recibo.", e);
        }
        ultimoNumero = nuevo;
        return nuevo;
    }
    public long getUltimoNumero() { return ultimoNumero; }
}
