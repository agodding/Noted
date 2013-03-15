package com.agodding.noted.persistence;

import java.util.ArrayList;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;

import com.agodding.noted.R;
import com.agodding.noted.model.Note;
import com.agodding.noted.sync.DriveSyncService;

public class DBManager extends SQLiteOpenHelper {

	private static final int DATABASE_VERSION = 1;
	private static final String DATABASE_NAME = "database";
    private static final String NOTE_TABLE_NAME = "notes";
    
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_TITLE = "title";
    private static final String COLUMN_BODY = "body";
    private static final String COLUMN_TIMESTAMP = "timestamp";
    private static final String COLUMN_PASSWORD = "password";
    
    private static final String NOTE_TABLE_CREATE =
                "CREATE TABLE " + NOTE_TABLE_NAME + " (" +
                COLUMN_ID + " integer primary key autoincrement, " +
                COLUMN_TITLE + " text, " +
                COLUMN_BODY + " text, " +
                COLUMN_TIMESTAMP + " integer, " +
                COLUMN_PASSWORD + " text);";
    
    private static DBManager singleton;
	private SQLiteDatabase db;
	private Context context;
	
	public static DBManager getInstance(Context context) {
		if (singleton == null) {
			singleton = new DBManager(context);
		}
		singleton.onOpen();
		return singleton;
	}

    private DBManager(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        db = getWritableDatabase();
        this.context = context.getApplicationContext();
    }
	
	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(NOTE_TABLE_CREATE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase arg0, int arg1, int arg2) {
		// TODO Auto-generated method stub
	}
	
	public void onOpen() {
		db = getWritableDatabase();
	}
	
	public void onClose() {
		db.close();
	}
	public void createNote(Note note) {
		ContentValues values = new ContentValues();
		note.setTimestamp(System.currentTimeMillis());
	    populateValues(note, values);
	    note.setId(db.insert(NOTE_TABLE_NAME, null, values));
	    handleSync();
	}
	
	public Note retreiveNote(long id) {
		Cursor cursor = db.query(NOTE_TABLE_NAME, null, COLUMN_ID + " = " + String.valueOf(id), null, null, null, null);
		Note note = null;
		if (cursor.moveToFirst()) {
			note = extractNoteFromCursor(cursor);
		}
		cursor.close();
		return note;
	}
	
	public ArrayList<Note> getNotes() {
		ArrayList<Note> notes = new ArrayList<Note>();
		Cursor cursor = db.query(NOTE_TABLE_NAME, null, null, null, null, null, null);
		if (cursor.moveToFirst()) {
			while (!cursor.isAfterLast()) {
				notes.add(extractNoteFromCursor(cursor));
				cursor.moveToNext();
			}
		}
		cursor.close();
		return notes;
	}
	
	public void updateNote(Note note) {
		ContentValues values = new ContentValues();
		note.setTimestamp(System.currentTimeMillis());
		values.put(COLUMN_ID, note.getId());
	    populateValues(note, values);
	    db.update(NOTE_TABLE_NAME, values, COLUMN_ID + " = " + String.valueOf(note.getId()), null);
	    handleSync();
	}
	
	public void deleteNote(Note note) {
		db.delete(NOTE_TABLE_NAME, COLUMN_ID + " = " + String.valueOf(note.getId()), null);
		handleSync();
	}
	
	public ArrayList<Note> queryNotes(String queryString) {
		ArrayList<Note> notes = new ArrayList<Note>();
		Cursor cursor = db.query(NOTE_TABLE_NAME, null, COLUMN_TITLE + " LIKE '%" + queryString + "%' OR " + COLUMN_BODY + " LIKE '%" + queryString + "%'" , null, null, null, null);
		if (cursor.moveToFirst()) {
			while (!cursor.isAfterLast()) {
				notes.add(extractNoteFromCursor(cursor));
				cursor.moveToNext();
			}
		}
		cursor.close();
		return notes;
	}
	
	private void populateValues(Note note, ContentValues values) {
		values.put(COLUMN_TITLE, note.getTitle());
	    values.put(COLUMN_BODY, note.getBody());
	    values.put(COLUMN_PASSWORD, note.getPassword());
	    values.put(COLUMN_TIMESTAMP, note.getTimestamp());
	}

	private Note extractNoteFromCursor(Cursor cursor) {
		Note aNote = new Note();
		aNote.setId(cursor.getLong(0));
		aNote.setTitle(cursor.getString(1));
		aNote.setBody(cursor.getString(2));
		aNote.setTimestamp(cursor.getLong(3));
		aNote.setPassword(cursor.getString(4));
		return aNote;
	}
	
	private void handleSync() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
		if (prefs.getBoolean(context.getString(R.string.syncPref), false)) {
			if (prefs.getBoolean(context.getString(R.string.wifiPref), false) && !wifiConnected()) {
				return;
			}
			Intent intent = new Intent(context, DriveSyncService.class);
			context.startService(intent);
		}
	}
	
	private boolean wifiConnected() {
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
		if (activeNetwork != null && activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
			return true;
		}
		return false;
	}
}