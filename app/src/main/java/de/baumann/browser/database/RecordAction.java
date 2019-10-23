package de.baumann.browser.database;

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

    public void open(boolean rw) {
        database = rw ? helper.getWritableDatabase() : helper.getReadableDatabase();
    }

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

    public void addDomain(String domain) {
        if (domain == null || domain.trim().isEmpty()) {
            return;
        }

        ContentValues values = new ContentValues();
        values.put(RecordUnit.COLUMN_DOMAIN, domain.trim());
        database.insert(RecordUnit.TABLE_WHITELIST, null, values);

    }

    public void addDomainJS(String domain) {
        if (domain == null || domain.trim().isEmpty()) {
            return;
        }

        ContentValues values = new ContentValues();
        values.put(RecordUnit.COLUMN_DOMAIN, domain.trim());
        database.insert(RecordUnit.TABLE_JAVASCRIPT, null, values);

    }

    public void addDomainCookie(String domain) {
        if (domain == null || domain.trim().isEmpty()) {
            return;
        }

        ContentValues values = new ContentValues();
        values.put(RecordUnit.COLUMN_DOMAIN, domain.trim());
        database.insert(RecordUnit.TABLE_COOKIE, null, values);

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

    public void deleteHistoryOld(String domain) {
        if (domain == null || domain.trim().isEmpty()) {
            return;
        }

        database.execSQL("DELETE FROM "+ RecordUnit.TABLE_HISTORY + " WHERE " + RecordUnit.COLUMN_URL + " = " + "\"" + domain.trim() + "\"");
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

    public boolean checkDomain(String domain) {
        if (domain == null || domain.trim().isEmpty()) {
            return false;
        }

        Cursor cursor = database.query(
                RecordUnit.TABLE_WHITELIST,
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

    public boolean checkDomainJS(String domain) {
        if (domain == null || domain.trim().isEmpty()) {
            return false;
        }

        Cursor cursor = database.query(
                RecordUnit.TABLE_JAVASCRIPT,
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

    public boolean checkDomainCookie(String domain) {
        if (domain == null || domain.trim().isEmpty()) {
            return false;
        }

        Cursor cursor = database.query(
                RecordUnit.TABLE_COOKIE,
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

    public void deleteHistory(Record record) {
        if (record == null || record.getTime() <= 0) {
            return;
        }

        database.execSQL("DELETE FROM "+ RecordUnit.TABLE_HISTORY + " WHERE " + RecordUnit.COLUMN_TIME + " = " + record.getTime());
    }

    public void deleteDomain(String domain) {
        if (domain == null || domain.trim().isEmpty()) {
            return;
        }

        database.execSQL("DELETE FROM "+ RecordUnit.TABLE_WHITELIST + " WHERE " + RecordUnit.COLUMN_DOMAIN + " = " + "\"" + domain.trim() + "\"");
    }

    public void deleteDomainJS(String domain) {
        if (domain == null || domain.trim().isEmpty()) {
            return;
        }

        database.execSQL("DELETE FROM "+ RecordUnit.TABLE_JAVASCRIPT + " WHERE " + RecordUnit.COLUMN_DOMAIN + " = " + "\"" + domain.trim() + "\"");
    }

    public void deleteDomainCookie(String domain) {
        if (domain == null || domain.trim().isEmpty()) {
            return;
        }

        database.execSQL("DELETE FROM "+ RecordUnit.TABLE_COOKIE + " WHERE " + RecordUnit.COLUMN_DOMAIN + " = " + "\"" + domain.trim() + "\"");
    }

    public void deleteGridItem(GridItem item) {
        if (item == null || item.getURL() == null || item.getURL().trim().isEmpty()) {
            return;
        }

        database.execSQL("DELETE FROM " + RecordUnit.TABLE_GRID + " WHERE " + RecordUnit.COLUMN_URL + " = " + "\"" + item.getURL().trim() + "\"");
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

    public List<Record> listHistory() {
        List<Record> list = new ArrayList<>();

        Cursor cursor = database.query(
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

        if (cursor == null) {
            return list;
        }

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            list.add(getRecord(cursor));
            cursor.moveToNext();
        }
        cursor.close();

        return list;
    }

    public List<String> listDomains() {
        List<String> list = new ArrayList<>();

        Cursor cursor = database.query(
                RecordUnit.TABLE_WHITELIST,
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

    public List<String> listDomainsJS() {
        List<String> list = new ArrayList<>();

        Cursor cursor = database.query(
                RecordUnit.TABLE_JAVASCRIPT,
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

    public List<String> listDomainsCookie() {
        List<String> list = new ArrayList<>();

        Cursor cursor = database.query(
                RecordUnit.TABLE_COOKIE,
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
        List<GridItem> list2 = new LinkedList<>();
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);

        switch (Objects.requireNonNull(sp.getString("sort_startSite", "ordinal"))) {
            case "ordinal":
                Cursor cursor = database.query(
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

            case "title": {
                Cursor cursor2 = database.query(
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
                        RecordUnit.COLUMN_TITLE
                );
                if (cursor2 == null) {
                    return list2;
                }
                cursor2.moveToFirst();
                while (!cursor2.isAfterLast()) {
                    list2.add(getGridItem(cursor2));
                    cursor2.moveToNext();
                }
                cursor2.close();
                return list2;
            }
        }
        return null;
    }
}
