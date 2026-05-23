package xyz.zt.rdc;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface FormularioDao {
    @Insert
    long insertFormulario(FormularioData formulario);

    @Update
    void updateFormulario(FormularioData formulario);

    @Insert
    void insertFilas(List<RegistroFila> filas);

    @Query("SELECT * FROM formularios ORDER BY id DESC")
    List<FormularioData> getAllFormularios();

    @Query("SELECT * FROM registros_filas WHERE formularioId = :formularioId ORDER BY numeroFila ASC")
    List<RegistroFila> getFilasByFormulario(int formularioId);
}
