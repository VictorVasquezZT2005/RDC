package xyz.zt.rdc;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {FormularioData.class, RegistroFila.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {
    public abstract FormularioDao formularioDao();

    private static AppDatabase instance;

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(),
                    AppDatabase.class, "rdc_database")
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return instance;
    }
}
