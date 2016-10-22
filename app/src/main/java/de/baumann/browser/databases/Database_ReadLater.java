/*
    This file is part of the Browser WebApp.

    Browser WebApp is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Browser WebApp is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with the Browser webview app.

    If not, see <http://www.gnu.org/licenses/>.
 */

package de.baumann.browser.databases;

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;

import java.util.ArrayList;

public class Database_ReadLater extends SQLiteOpenHelper {
    public Database_ReadLater(Context context)
            throws NameNotFoundException {
        super(context,
                "readLater.db",
                null,
                context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        createTable(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    private void createTable(SQLiteDatabase db) {
        db.execSQL(
                "CREATE TABLE bookmarks (" +
                        "seqno NUMBER NOT NULL, " +
                        "title TEXT NOT NULL, " +
                        "url TEXT NOT NULL, " +
                        "PRIMARY KEY(seqno))"
        );
    }

    public void loadInitialData() {
        int seqno = 0;

        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();

        SQLiteStatement stmt = db.compileStatement("INSERT INTO bookmarks VALUES(?, ?, ?)");
        stmt.bindLong(1, seqno);
        stmt.bindString(2, "Startpage");
        stmt.bindString(3, "https://www.startpage.de");
        stmt.executeInsert();

        db.setTransactionSuccessful();
        db.endTransaction();
        db.close();
    }

    public int getRecordCount() {
        SQLiteDatabase db = getReadableDatabase();

        int ret = 0;

        String sql = "SELECT COUNT(*) FROM bookmarks";
        Cursor c = db.rawQuery(sql, null);
        if (c.getCount() > 0) {
            c.moveToFirst();
            ret = c.getInt(0);
        }
        c.close();
        db.close();

        return ret;
    }

    public void getBookmarks(ArrayList<String[]> data) {
        SQLiteDatabase db = getReadableDatabase();

        String sql = "SELECT seqno,title,url FROM bookmarks ORDER BY seqno";
        Cursor c = db.rawQuery(sql, null);
        c.moveToFirst();
        for (int i = 0; i < c.getCount(); i++) {
            String[] strAry = {c.getString(0), c.getString(1), c.getString(2)};
            data.add(strAry);
            c.moveToNext();
        }
        c.close();
        db.close();
    }

    public void addBookmark(String title, String url) {
        int seqno;

        SQLiteDatabase db = getWritableDatabase();

        String sql = "SELECT MAX(seqno) FROM bookmarks";
        Cursor c = db.rawQuery(sql, null);
        c.moveToFirst();
        seqno = c.getInt(0) + 1;

        db.beginTransaction();

        SQLiteStatement stmt = db.compileStatement("INSERT INTO bookmarks VALUES(?, ?, ?)");
        stmt.bindLong(1, seqno);
        stmt.bindString(2, title);
        stmt.bindString(3, url);
        stmt.executeInsert();

        db.setTransactionSuccessful();
        db.endTransaction();
        db.close();

        c.close();
    }

    public void deleteBookmark(int seqno) {

        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();

        SQLiteStatement stmt = db.compileStatement("DELETE FROM bookmarks WHERE seqno = ?");
        stmt.bindLong(1, seqno);
        stmt.execute();

        db.setTransactionSuccessful();
        db.endTransaction();
        db.close();
    }
}