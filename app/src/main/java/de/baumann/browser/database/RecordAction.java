package de.baumann.browser.database;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import androidx.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import de.baumann.browser.unit.RecordUnit;
import de.baumann.browser.view.GridItem;

public class RecordAction {
    private SQLiteDatabase database;
    private final RecordHelper helper;

    public RecordAction(Context context) {
        this.helper = new RecordHelper(context);
    }
    public void open(boolean rw) { database = rw ? helper.getWritableDatabase() : helper.getReadableDatabase(); }
    public void close() {
        helper.close();
    }

    public void addHistory(Record record) {
        if (record == null
                || record.getTitle() == null
                || record.getTitle().trim().isEmpty()
                || record.getURL() == null
                || record.getURL().trim().isEmpty()
                || record.getTime() < 0L) {
            return;
        }

        ContentValues values = new ContentValues();
        values.put(RecordUnit.COLUMN_TITLE, record.getTitle().trim());
        values.put(RecordUnit.COLUMN_URL, record.getURL().trim());
        values.put(RecordUnit.COLUMN_TIME, record.getTime());
        database.insert(RecordUnit.TABLE_HISTORY, null, values);
    }

    public void addDomain(String domain, String table) {
        if (domain == null || domain.trim().isEmpty()) { return; }
        ContentValues values = new ContentValues();
        values.put(RecordUnit.COLUMN_DOMAIN, domain.trim());
        database.insert(table, null, values);
    }

    public boolean addGridItem(GridItem item) {
        if (item == null
                || item.getTitle() == null
                || item.getTitle().trim().isEmpty()
                || item.getURL() == null
                || item.getURL().trim().isEmpty()
                || item.getFilename() == null
                || item.getFilename().trim().isEmpty()
                || item.getOrdinal() < 0) {
            return false;
        }
        ContentValues values = new ContentValues();
        values.put(RecordUnit.COLUMN_TITLE, item.getTitle().trim());
        values.put(RecordUnit.COLUMN_URL, item.getURL().trim());
        values.put(RecordUnit.COLUMN_FILENAME, item.getFilename().trim());
        values.put(RecordUnit.COLUMN_ORDINAL, item.getOrdinal());
        database.insert(RecordUnit.TABLE_GRID, null, values);
        return true;
    }

    public void updateGridItem(GridItem item) {
        if (item == null
                || item.getTitle() == null
                || item.getTitle().trim().isEmpty()
                || item.getURL() == null
                || item.getURL().trim().isEmpty()
                || item.getFilename() == null
                || item.getFilename().trim().isEmpty()
                || item.getOrdinal() < 0) {
            return;
        }
        ContentValues values = new ContentValues();
        values.put(RecordUnit.COLUMN_TITLE, item.getTitle().trim());
        values.put(RecordUnit.COLUMN_URL, item.getURL().trim());
        values.put(RecordUnit.COLUMN_FILENAME, item.getFilename().trim());
        values.put(RecordUnit.COLUMN_ORDINAL, item.getOrdinal());
        database.update(RecordUnit.TABLE_GRID, values, RecordUnit.COLUMN_URL + "=?", new String[] {item.getURL()});
    }


    public boolean checkHistory(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        Cursor cursor = database.query(
                RecordUnit.TABLE_HISTORY,
                new String[] {RecordUnit.COLUMN_URL},
                RecordUnit.COLUMN_URL + "=?",
                new String[] {url.trim()},
                null,
                null,
                null
        );
        if (cursor != null) {
            boolean result = cursor.moveToFirst();
            cursor.close();

            return result;
        }
        return false;
    }

    public boolean checkDomain(String domain, String table) {
        if (domain == null || domain.trim().isEmpty()) {
            return false;
        }
        Cursor cursor = database.query(
                table,
                new String[] {RecordUnit.COLUMN_DOMAIN},
                RecordUnit.COLUMN_DOMAIN + "=?",
                new String[] {domain.trim()},
                null,
                null,
                null
        );
        if (cursor != null) {
            boolean result = cursor.moveToFirst();
            cursor.close();
            return result;
        }
        return false;
    }

    public boolean checkGridItem(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }
        Cursor cursor = database.query(
                RecordUnit.TABLE_GRID,
                new String[] {RecordUnit.COLUMN_URL},
                RecordUnit.COLUMN_URL + "=?",
                new String[] {url.trim()},
                null,
                null,
                null
        );
        if (cursor != null) {
            boolean result = cursor.moveToFirst();
            cursor.close();
            return result;
        }
        return false;
    }

    public void deleteHistoryItemByURL (String domain) {
        if (domain == null || domain.trim().isEmpty()) { return; }
        database.execSQL("DELETE FROM "+ RecordUnit.TABLE_HISTORY + " WHERE " + RecordUnit.COLUMN_URL + " = " + "\"" + domain.trim() + "\"");
    }

    public void deleteHistoryItem(Record record) {
        if (record == null || record.getTime() <= 0) { return; }
        database.execSQL("DELETE FROM "+ RecordUnit.TABLE_HISTORY + " WHERE " + RecordUnit.COLUMN_TIME + " = " + record.getTime());
    }

    public void deleteGridItem(GridItem item) {
        if (item == null || item.getURL() == null || item.getURL().trim().isEmpty()) { return; }
        database.execSQL("DELETE FROM " + RecordUnit.TABLE_GRID + " WHERE " + RecordUnit.COLUMN_URL + " = " + "\"" + item.getURL().trim() + "\"");
    }

    public void deleteDomain(String domain, String table) {
        if (domain == null || domain.trim().isEmpty()) { return; }
        database.execSQL("DELETE FROM "+ table + " WHERE " + RecordUnit.COLUMN_DOMAIN + " = " + "\"" + domain.trim() + "\"");
    }

    public void clearHome() {
        database.execSQL("DELETE FROM " + RecordUnit.TABLE_GRID);
    }
    public void clearHistory() {
        database.execSQL("DELETE FROM " + RecordUnit.TABLE_HISTORY);
    }
    public void clearDomains() {
        database.execSQL("DELETE FROM " + RecordUnit.TABLE_WHITELIST);
    }
    public void clearDomainsJS() {
        database.execSQL("DELETE FROM " + RecordUnit.TABLE_JAVASCRIPT);
    }
    public void clearDomainsCookie() {database.execSQL("DELETE FROM " + RecordUnit.TABLE_COOKIE);}

    private Record getRecord(Cursor cursor) {
        Record record = new Record();
        record.setTitle(cursor.getString(0));
        record.setURL(cursor.getString(1));
        record.setTime(cursor.getLong(2));
        return record;
    }

    private GridItem getGridItem(Cursor cursor) {
        GridItem item = new GridItem();
        item.setTitle(cursor.getString(0));
        item.setURL(cursor.getString(1));
        item.setFilename(cursor.getString(2));
        item.setOrdinal(cursor.getInt(3));
        return item;
    }

    public List<Record> listEntries (Activity activity, boolean listAll) {
        List<Record> list = new ArrayList<>();
        Cursor cursor;

        if (listAll) {
            //add startSite
            cursor = database.query(
                    RecordUnit.TABLE_GRID,
                    new String[] {
                            RecordUnit.COLUMN_TITLE,
                            RecordUnit.COLUMN_URL,
                            RecordUnit.COLUMN_FILENAME,
                            RecordUnit.COLUMN_ORDINAL
                    },
                    null,
                    null,
                    null,
                    null,
                    RecordUnit.COLUMN_ORDINAL
            );

            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                list.add(getRecord(cursor));
                cursor.moveToNext();
            }
            cursor.close();

            //add bookmarks
            BookmarkList db = new BookmarkList(activity);
            db.open();
            cursor = db.fetchAllForSearch();
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                list.add(getRecord(cursor));
                cursor.moveToNext();
            }
            cursor.close();
        }

        //add history
        cursor = database.query(
                RecordUnit.TABLE_HISTORY,
                new String[] {
                        RecordUnit.COLUMN_TITLE,
                        RecordUnit.COLUMN_URL,
                        RecordUnit.COLUMN_TIME
                },
                null,
                null,
                null,
                null,
                RecordUnit.COLUMN_TIME + " asc"
        );

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            list.add(getRecord(cursor));
            cursor.moveToNext();
        }
        cursor.close();

        return list;
    }

    public List<String> listDomains(String table) {
        List<String> list = new ArrayList<>();
        Cursor cursor = database.query(
                table,
                new String[] {RecordUnit.COLUMN_DOMAIN},
                null,
                null,
                null,
                null,
                RecordUnit.COLUMN_DOMAIN
        );
        if (cursor == null) {
            return list;
        }
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            list.add(cursor.getString(0));
            cursor.moveToNext();
        }
        cursor.close();
        return list;
    }

    public List<GridItem> listGrid(Context context) {

        List<GridItem> list = new LinkedList<>();
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        String sortBy = Objects.requireNonNull(sp.getString("sort_startSite", "ordinal"));

        Cursor cursor;
        cursor = database.query(
                RecordUnit.TABLE_GRID,
                new String[] {
                        RecordUnit.COLUMN_TITLE,
                        RecordUnit.COLUMN_URL,
                        RecordUnit.COLUMN_FILENAME,
                        RecordUnit.COLUMN_ORDINAL
                },
                null,
                null,
                null,
                null,
                sortBy
        );
        if (cursor == null) {
            return list;
        }
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            list.add(getGridItem(cursor));
            cursor.moveToNext();
        }
        cursor.close();
        return list;
    }
}
