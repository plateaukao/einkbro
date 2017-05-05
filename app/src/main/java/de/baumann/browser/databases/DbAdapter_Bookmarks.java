/*
    This file is part of the HHS Moodle WebApp.

    HHS Moodle WebApp is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    HHS Moodle WebApp is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with the Diaspora Native WebApp.

    If not, see <http://www.gnu.org/licenses/>.
 */

package de.baumann.browser.databases;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.preference.PreferenceManager;

import de.baumann.browser.R;


public class DbAdapter_Bookmarks {

    //define static variable
    private static final int dbVersion =6;
    private static final String dbName = "bookmarks_DB_v01.db";
    private static final String dbTable = "bookmarks";

    private static class DatabaseHelper extends SQLiteOpenHelper {
        DatabaseHelper(Context context) {
            super(context,dbName,null, dbVersion);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS "+dbTable+" (_id INTEGER PRIMARY KEY autoincrement, bookmarks_title, bookmarks_content, bookmarks_icon, bookmarks_attachment, bookmarks_creation, UNIQUE(bookmarks_content))");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS "+dbTable);
            onCreate(db);
        }
    }

    //establish connection with SQLiteDataBase
    private final Context c;
    private SQLiteDatabase sqlDb;

    public DbAdapter_Bookmarks(Context context) {
        this.c = context;
    }
    public void open() throws SQLException {
        DatabaseHelper dbHelper = new DatabaseHelper(c);
        sqlDb = dbHelper.getWritableDatabase();
    }

    //insert data
    @SuppressWarnings("SameParameterValue")
    public void insert(String bookmarks_title, String bookmarks_content, String bookmarks_icon, String bookmarks_attachment, String bookmarks_creation) {
        if(!isExist(bookmarks_title)) {
            sqlDb.execSQL("INSERT INTO bookmarks (bookmarks_title, bookmarks_content, bookmarks_icon, bookmarks_attachment, bookmarks_creation) VALUES('" + bookmarks_title + "','" + bookmarks_content + "','" + bookmarks_icon + "','" + bookmarks_attachment + "','" + bookmarks_creation + "')");
        }
    }
    //check entry already in database or not
    public boolean isExist(String bookmarks_content){
        String query = "SELECT bookmarks_content FROM bookmarks WHERE bookmarks_content='"+bookmarks_content+"' LIMIT 1";
        @SuppressLint("Recycle") Cursor row = sqlDb.rawQuery(query, null);
        return row.moveToFirst();
    }
    //check entry already in database or not
    @SuppressWarnings("SameParameterValue")
    public boolean isExistFav(String bookmarks_attachment){
        String query = "SELECT bookmarks_attachment FROM bookmarks WHERE bookmarks_attachment='"+bookmarks_attachment+"' LIMIT 1";
        @SuppressLint("Recycle") Cursor row = sqlDb.rawQuery(query, null);
        return row.moveToFirst();
    }
    //edit data
    public void update(int id,String bookmarks_title,String bookmarks_content,String bookmarks_icon,String bookmarks_attachment, String bookmarks_creation) {
        sqlDb.execSQL("UPDATE "+dbTable+" SET bookmarks_title='"+bookmarks_title+"', bookmarks_content='"+bookmarks_content+"', bookmarks_icon='"+bookmarks_icon+"', bookmarks_attachment='"+bookmarks_attachment+"', bookmarks_creation='"+bookmarks_creation+"'   WHERE _id=" + id);
    }

    //delete data
    public void delete(int id) {
        sqlDb.execSQL("DELETE FROM "+dbTable+" WHERE _id="+id);
    }


    //fetch data
    public Cursor fetchAllData(Context context) {

        PreferenceManager.setDefaultValues(context, R.xml.user_settings, false);
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);

        String[] columns = new String[]{"_id", "bookmarks_title", "bookmarks_content", "bookmarks_icon","bookmarks_attachment","bookmarks_creation"};

        if (sp.getString("sortDBB", "title").equals("title")) {
            return sqlDb.query(dbTable, columns, null, null, null, null, "bookmarks_title" + " COLLATE NOCASE ASC;");
        } else if (sp.getString("sortDBB", "title").equals("create")) {
            String orderBy = "bookmarks_creation" + "," +
                    "bookmarks_title" + " COLLATE NOCASE ASC;";
            return sqlDb.query(dbTable, columns, null, null, null, null, orderBy);
        }

        return null;
    }

    //fetch data by filter
    public Cursor fetchDataByFilter(String inputText,String filterColumn) throws SQLException {
        Cursor row;
        String query = "SELECT * FROM "+dbTable;
        if (inputText == null  ||  inputText.length () == 0)  {
            row = sqlDb.rawQuery(query, null);
        }else {
            query = "SELECT * FROM "+dbTable+" WHERE "+filterColumn+" like '%"+inputText+"%'";
            row = sqlDb.rawQuery(query, null);
        }
        if (row != null) {
            row.moveToFirst();
        }
        return row;
    }
}