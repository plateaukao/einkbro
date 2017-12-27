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

package de.baumann.browser.Database;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;


public class Pass {

    //define static variable
    private static final int dbVersion =6;
    private static final String dbName = "pass_DB_v01.db";
    private static final String dbTable = "pass";

    private static class DatabaseHelper extends SQLiteOpenHelper {
        DatabaseHelper(Context context) {
            super(context,dbName,null, dbVersion);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS "+dbTable+" (_id INTEGER PRIMARY KEY autoincrement, pass_title, pass_content, pass_icon, pass_attachment, pass_creation, UNIQUE(pass_title))");
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

    public Pass(Context context) {
        this.c = context;
    }
    public void open() throws SQLException {
        DatabaseHelper dbHelper = new DatabaseHelper(c);
        sqlDb = dbHelper.getWritableDatabase();
    }

    //insert data
    @SuppressWarnings("SameParameterValue")
    public void insert(String pass_title, String pass_content, String pass_icon, String pass_attachment, String pass_creation) {
        if(!isExist(pass_title)) {
            sqlDb.execSQL("INSERT INTO pass (pass_title, pass_content, pass_icon, pass_attachment, pass_creation) VALUES('" + pass_title + "','" + pass_content + "','" + pass_icon + "','" + pass_attachment + "','" + pass_creation + "')");
        }
    }
    //check entry already in database or not
    public boolean isExist(String pass_title){
        String query = "SELECT pass_title FROM pass WHERE pass_title='"+pass_title+"' LIMIT 1";
        @SuppressLint("Recycle") Cursor row = sqlDb.rawQuery(query, null);
        return row.moveToFirst();
    }

    //edit data
    public void update(int id,String pass_title,String pass_content,String pass_icon,String pass_attachment, String pass_creation) {
        sqlDb.execSQL("UPDATE "+dbTable+" SET pass_title='"+pass_title+"', pass_content='"+pass_content+"', pass_icon='"+pass_icon+"', pass_attachment='"+pass_attachment+"', pass_creation='"+pass_creation+"'   WHERE _id=" + id);
    }

    //delete data
    public void delete(int id) {
        sqlDb.execSQL("DELETE FROM "+dbTable+" WHERE _id="+id);
    }


    //fetch data
    public Cursor fetchAllData() {
        String[] columns = new String[]{"_id", "pass_title", "pass_content", "pass_icon","pass_attachment","pass_creation"};
        return sqlDb.query(dbTable, columns, null, null, null, null, "pass_title" + " COLLATE NOCASE ASC;");
    }
}