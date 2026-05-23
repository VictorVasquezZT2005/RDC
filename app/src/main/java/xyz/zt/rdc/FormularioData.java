package xyz.zt.rdc;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Modelo para el formulario completo.
 */
@Entity(tableName = "formularios")
public class FormularioData {
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    public String nombreDirectorSello = "";
    public String fechaCreacion = "";
}
