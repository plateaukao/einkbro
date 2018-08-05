package de.baumann.browser.Database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import de.baumann.browser.Unit.RecordUnit;
import de.baumann.browser.View.GridItem;

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

    public void addBookmark(Record record) {
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
        database.insert(RecordUnit.TABLE_BOOKMARKS, null, values);

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

    public void updateBookmark(Record record) {
        if (record == null
                || record.getTitle() == null
                || record.getTitle().trim().isEmpty()
                || record.getURL() == null
                || record.getURL().trim().isEmpty()
                || record.getTime() < 0) {
            return;
        }

        ContentValues values = new ContentValues();
        values.put(RecordUnit.COLUMN_TITLE, record.getTitle().trim());
        values.put(RecordUnit.COLUMN_URL, record.getURL().trim());
        values.put(RecordUnit.COLUMN_TIME, record.getTime());
        database.update(RecordUnit.TABLE_BOOKMARKS, values, RecordUnit.COLUMN_TIME + "=?", new String[] {String.valueOf(record.getTime())});

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

    public boolean checkBookmark(Record record) {
        if (record == null || record.getURL() == null || record.getURL().trim().isEmpty()) {
            return false;
        }

        Cursor cursor = database.query(
                RecordUnit.TABLE_BOOKMARKS,
                new String[] {RecordUnit.COLUMN_URL},
                RecordUnit.COLUMN_URL + "=?",
                new String[] {record.getURL().trim()},
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

    public boolean checkBookmark(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }

        Cursor cursor = database.query(
                RecordUnit.TABLE_BOOKMARKS,
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

    public void deleteBookmark(Record record) {
        if (record == null || record.getURL() == null || record.getURL().trim().isEmpty()) {
            return;
        }

        database.execSQL("DELETE FROM " + RecordUnit.TABLE_BOOKMARKS + " WHERE " + RecordUnit.COLUMN_URL + " = " + "\"" + record.getURL().trim() + "\"");
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

    public void clearBookmarks() {
        database.execSQL("DELETE FROM " + RecordUnit.TABLE_BOOKMARKS);
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

    public void clearGrid() {
        database.execSQL("DELETE FROM " + RecordUnit.TABLE_GRID);
    }

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

    public List<Record> listBookmarks() {
        List<Record> list = new ArrayList<>();

        Cursor cursor = database.query(
                RecordUnit.TABLE_BOOKMARKS,
                new String[] {
                        RecordUnit.COLUMN_TITLE,
                        RecordUnit.COLUMN_URL,
                        RecordUnit.COLUMN_TIME
                },
                null,
                null,
                null,
                null,
                RecordUnit.COLUMN_TIME + " desc"
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
                RecordUnit.COLUMN_TIME + " desc"
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

    public List<GridItem> listGrid() {
        List<GridItem> list = new LinkedList<>();

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
    }
}
