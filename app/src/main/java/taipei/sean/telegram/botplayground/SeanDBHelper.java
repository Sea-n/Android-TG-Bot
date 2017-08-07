package taipei.sean.telegram.botplayground;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class SeanDBHelper extends SQLiteOpenHelper {

    public SeanDBHelper(Context context, String name,
                        SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }


    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE main.tokens " +
                "(_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "token TEXT NOT NULL, " +
                "name TEXT NOT NULL, " +
                "note TEXT)");
    }

    public List<BotStructure> getBots() {
        SQLiteDatabase db = getWritableDatabase();

        List<BotStructure> result = new ArrayList<BotStructure>() {
        };

        Cursor cursor = db.rawQuery("SELECT * FROM tokens", null);

        while (cursor.moveToNext()) {
            BotStructure item = new BotStructure();
            item._id = cursor.getInt(0);
            item.token = cursor.getString(1);
            item.name = cursor.getString(2);
            item.note = cursor.getString(3);
            result.add(item);
        }

        cursor.close();
        Log.d("db", "Total" + result.size());
        return result;
    }

    public BotStructure getBot(long id) {
        SQLiteDatabase db = getWritableDatabase();

        Cursor cursor = db.rawQuery("SELECT * FROM tokens WHERE _id = ?", new String[]{id + ""});

        cursor.moveToNext();
        BotStructure result = new BotStructure();

        if (cursor.getColumnCount() == 0) {
            Log.w("db", "no token with id " + id);
            return result;
        }

        try {
            result._id = cursor.getInt(0);
            result.token = cursor.getString(1);
            result.name = cursor.getString(2);
            result.note = cursor.getString(3);
        } catch (RuntimeException e) {
            Log.w("db", "Getting data error" + id);
            return new BotStructure();
        }

        cursor.close();
        return result;
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    public long insertBot(ContentValues values) {
        SQLiteDatabase db = getWritableDatabase();
        return db.insert("tokens", null, values);
    }

    public long updateBot(long id, ContentValues values) {
        SQLiteDatabase db = getWritableDatabase();
        return db.update("tokens", values, "_id=?", new String[]{id + ""});
    }

    public long deleteBot(long id) {
        SQLiteDatabase db = getWritableDatabase();
        Log.d("db", "del" + id);
        return db.delete("tokens", "_id=?", new String[]{id + ""});
    }
}
