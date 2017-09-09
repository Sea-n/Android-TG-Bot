package taipei.sean.telegram.botplayground;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
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
                "note TEXT, " +
                "type INTEGER NOT NULL)");

        db.execSQL("CREATE TABLE main.favorites " +
                "(_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "kind TEXT NOT NULL, " +
                "value TEXT NOT NULL, " +
                "name TEXT)");

        db.execSQL("CREATE TABLE main.params " +
                "(_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "kind TEXT NOT NULL, " +
                "value TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        switch (oldVersion) {
            case 0:
                try {
                    db.execSQL("CREATE TABLE IF NOT EXISTS main.tokens " +
                            "(_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "token TEXT NOT NULL, " +
                            "name TEXT NOT NULL, " +
                            "note TEXT)");
                } catch (SQLiteException e) {
                    Log.e("db", "onUpgrade", e);
                }
            case 1:
                try {
                    db.execSQL("CREATE TABLE IF NOT EXISTS main.favorites " +
                            "(_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "kind TEXT NOT NULL, " +
                            "value TEXT NOT NULL, " +
                            "name TEXT)");
                } catch (SQLiteException e) {
                    Log.e("db", "onUpgrade", e);
                }
            case 2:
                try {
                    db.execSQL("ALTER TABLE main.tokens ADD COLUMN type INTEGER DEFAULT 0;");
                } catch (SQLiteException e) {
                    Log.e("db", "onUpgrade", e);
                }
            case 3:
                try {
                    db.execSQL("CREATE TABLE IF NOT EXISTS main.params " +
                            "(_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "kind TEXT NOT NULL, " +
                            "value TEXT)");
                } catch (SQLiteException e) {
                    Log.e("db", "onUpgrade", e);
                }
        }
    }

    public List<BotStructure> getBots() {
        SQLiteDatabase db = getWritableDatabase();

        List<BotStructure> result = new ArrayList<BotStructure>() {
        };

        Cursor cursor = db.rawQuery("SELECT * FROM tokens", null);

        while (cursor.moveToNext()) {
            BotStructure item = new BotStructure();
            item._id = cursor.getLong(0);
            item.token = cursor.getString(1);
            item.name = cursor.getString(2);
            item.note = cursor.getString(3);
            item.type = cursor.getInt(4);
            result.add(item);
        }

        cursor.close();
        return result;
    }

    public BotStructure getBot(long id) {
        SQLiteDatabase db = getWritableDatabase();

        Cursor cursor = db.rawQuery("SELECT * FROM tokens WHERE _id = ?", new String[]{id + ""});

        cursor.moveToNext();
        BotStructure result = new BotStructure();

        if (cursor.getCount() == 0) {
            Log.w("db", "no token with id " + id);
            cursor.close();
            return null;
        }

        try {
            result._id = cursor.getLong(0);
            result.token = cursor.getString(1);
            result.name = cursor.getString(2);
            result.note = cursor.getString(3);
            result.type = cursor.getInt(4);
        } catch (RuntimeException e) {
            Log.w("db", "Getting data error " + id, e);
            cursor.close();
            return null;
        }

        cursor.close();
        return result;
    }

    public List<FavStructure> getFavs(@Nullable String kind) {
        SQLiteDatabase db = getWritableDatabase();

        List<FavStructure> result = new ArrayList<FavStructure>() {
        };
        Cursor cursor;

        if (null == kind) {
            cursor = db.rawQuery("SELECT * FROM favorites", null);
        } else {
            cursor = db.rawQuery("SELECT * FROM favorites WHERE kind = ?", new String[]{kind});
        }

        while (cursor.moveToNext()) {
            FavStructure item = new FavStructure();
            item._id = cursor.getLong(0);
            item.kind = cursor.getString(1);
            item.value = cursor.getString(2);
            item.name = cursor.getString(3);
            result.add(item);
        }

        cursor.close();
        return result;
    }

    public FavStructure getFav(long id) {
        SQLiteDatabase db = getWritableDatabase();

        FavStructure result = new FavStructure();
        Cursor cursor;

        cursor = db.rawQuery("SELECT * FROM favorites WHERE _id = ?", new String[]{id + ""});
        cursor.moveToNext();

        result._id = cursor.getLong(0);
        result.kind = cursor.getString(1);
        result.value = cursor.getString(2);
        result.name = cursor.getString(3);

        cursor.close();
        return result;
    }

    public String getParam(String kind) {
        SQLiteDatabase db = getWritableDatabase();

        Cursor cursor = db.rawQuery("SELECT * FROM params WHERE kind = ?;", new String[]{kind});
        int count = cursor.getCount();
        if (count == 0) {
            cursor.close();
            return "";
        } else {
            cursor.moveToNext();
            String value = cursor.getString(2);
            cursor.close();
            return value;
        }
    }

    public long insertBot(ContentValues values) {
        SQLiteDatabase db = getWritableDatabase();
        return db.insert("tokens", null, values);
    }

    public long insertFav(String kind, String value, @Nullable String name) {
        SQLiteDatabase db = getWritableDatabase();

        Cursor cursor = db.rawQuery("SELECT * FROM favorites WHERE kind = ? AND value = ?;", new String[]{kind, value});
        int count = cursor.getCount();
        cursor.close();
        if (count > 0) {
            Log.d("db", "Fav already exists");
            return -1;
        }

        ContentValues values = new ContentValues();
        values.put("kind", kind);
        values.put("value", value);
        if (null != name) {
            values.put("name", name);
        }
        return db.insert("favorites", null, values);
    }

    public long updateBot(long id, ContentValues values) {
        SQLiteDatabase db = getWritableDatabase();
        return db.update("tokens", values, "_id = ?", new String[]{id + ""});
    }

    public long updateFav(long id, String kind, String value, @Nullable String name) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("kind", kind);
        values.put("value", value);
        if (null != name)
            values.put("name", name);
        return db.update("favorites", values, "_id = ?", new String[]{id + ""});
    }

    public long updateParam(String kind, String value) {
        SQLiteDatabase db = getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("kind", kind);
        values.put("value", value);

        Cursor cursor = db.rawQuery("SELECT * FROM params WHERE kind = ?;", new String[]{kind});
        int count = cursor.getCount();
        cursor.close();
        if (count == 0) {
            Log.d("db", "param insert " + values);
            return db.insert("params", null, values);
        } else {
            Log.d("db", "param update " + values);
            return db.update("params", values, "kind = ?", new String[]{kind + ""});
        }
    }

    public long deleteBot(long id) {
        SQLiteDatabase db = getWritableDatabase();
        return db.delete("tokens", "_id = ?", new String[]{id + ""});
    }

    public boolean copyDatabase(File srcDB, File dstDB) throws IOException {
        close();
        if (srcDB.exists()) {
            Log.d("db", "copying" + srcDB + dstDB);
            FileChannel src = new FileInputStream(srcDB).getChannel();
            FileChannel dst = new FileOutputStream(dstDB).getChannel();
            dst.transferFrom(src, 0, src.size());
            src.close();
            dst.close();
            getWritableDatabase().close();
            return true;
        } else {
            Log.w("db", srcDB + " does not exists");
        }
        return false;
    }
}
