package xyz.zt.rdc;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Modelo para una fila individual de la tabla.
 */
@Entity(tableName = "registros_filas")
public class RegistroFila {
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    public int formularioId; // Relación con el formulario padre
    public int numeroFila; // 1-8
    
    public String fecha = "";
    
    // Deméritos (D)
    public int dA;
    public int dB;
    public int dC;
    public int dD;
    
    // Redención (R)
    public int rA;
    public int rB;
    public int rC;
    
    // Reconocimiento (RC)
    public int rcA;
    public int rcB;
    
    public String nombreFirmaRegistra = "";
    public String responsableRedencion = "";
    public String firmaEstudiante = "";
}
