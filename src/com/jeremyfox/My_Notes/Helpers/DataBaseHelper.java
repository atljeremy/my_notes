package com.jeremyfox.My_Notes.Helpers;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import com.jeremyfox.My_Notes.Interfaces.Note;
import com.jeremyfox.My_Notes.Interfaces.User;
import com.jeremyfox.My_Notes.Models.BasicNote;
import com.jeremyfox.My_Notes.Models.BasicUser;
import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: jfox
 * Date: 8/11/13s
 * Time: 9:38 AM
 */
public class DataBaseHelper extends SQLiteOpenHelper {

    /**
     * All Database versions
     *
     * New database versions MUST be created as new constant matching
     * the style of the versions below
     *
     * The onUpgrade method utilizes each version in a switch statement to
     * ensure proper migration from one version to the next
     */
    private static final int DB_VERSION_ONE          = 1;
    //private static final int DB_VERSION_TWO          = 2; // This is an example of the next version

    private static String DATABASE_NAME              = "MyNotes";

    public static final String USERS_TABLE          = "users";
    public static final String USER_ID              = "id";           // INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL
    public static final String USER_API_TOKEN       = "apiToken";     // TEXT
    public static final String USER_EMAIL_ADDRESS   = "email";        // TEXT
    public static final String USER_LAST_SYNC_DATE  = "lastSyncDate"; // TEXT
    public static final String USER_API_ID          = "apiUserId";    // INTEGER

    public static final String NOTES_TABLE          = "results";
    public static final String NOTE_ID              = "id";        // INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL
    public static final String NOTE_TITLE           = "title";     // TEXT
    public static final String NOTE_DETAILS         = "details";   // TEXT
    public static final String NOTE_CREATED_AT      = "createdAt"; // TEXT
    public static final String NOTE_UPDATED_AT      = "updatedAt"; // TEXT
    public static final String NOTE_API_ID          = "apiNoteId"; // INTEGER
    public static final String NOTE_USER_ID         = "userId";    // INTEGER

    public DataBaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DB_VERSION_ONE);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String usersSql = "CREATE TABLE IF NOT EXISTS " + USERS_TABLE + "" +
                "("+USER_ID+" INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                ""+USER_API_TOKEN+" TEXT, " +
                ""+USER_EMAIL_ADDRESS+" TEXT, " +
                ""+USER_LAST_SYNC_DATE+" TEXT, " +
                ""+USER_API_ID+" INTEGER)";

        String notesSql = "CREATE TABLE " + NOTES_TABLE + "" +
                "("+NOTE_ID+" INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                ""+NOTE_TITLE+" TEXT, " +
                ""+NOTE_DETAILS+" TEXT, " +
                ""+NOTE_CREATED_AT+" TEXT, " +
                ""+NOTE_UPDATED_AT+" TEXT, " +
                ""+NOTE_API_ID+" INTEGER, " +
                ""+NOTE_USER_ID+" INTEGER)";

        db.execSQL(usersSql);
        db.execSQL(notesSql);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        /**
         * Switch statement for migrating the database in proper order.
         *
         * VERY IMPORTANT: Each case must NOT 'break;'. This would cause the migration to
         * fail because it would skip over any additional db versions during migration.
         */
        switch (oldVersion) {
            case DB_VERSION_ONE: // Migrating from version 1 to version 2
                db.execSQL(""); // Placeholder
            //case DB_VERSION_TWO:
                // Code for migrating from version 2 to version 3
        }
    }

    /*******************************************************************
     * SELECT
     *******************************************************************/

    public BasicUser getUser(int id, String notesFilter, String notesFilterWHERE) {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(USERS_TABLE, new String[] {USER_ID}, USER_ID + "=?", new String[] { String.valueOf(id) }, null, null, null, null);
        if (cursor != null) {
            cursor.moveToFirst();
        }

        BasicUser user = null;
        try {
            int userId          = cursor.getInt(0);
            String apiToken     = cursor.getString(1);
            String email        = cursor.getString(2);
            String lastSyncDate = cursor.getString(3);
            int apiUserId       = cursor.getInt(4);

            user = new BasicUser(userId, apiToken, email, lastSyncDate, apiUserId);
            ArrayList<Note> notes = getNotes(user.getId(), notesFilter, notesFilterWHERE);

            user.setNotes(notes);

        } catch (CursorIndexOutOfBoundsException e) {
            // Tried to access an index that doesn't exist in the cursor
        }

        return user;
    }

    public BasicUser getCurrentUser() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM "+USERS_TABLE+" WHERE "+USER_ID+" = (SELECT MAX("+USER_ID+") FROM "+USERS_TABLE+");", null);
        if (cursor != null) {
            cursor.moveToLast();
        }

        BasicUser user = null;
        try {
            int userId          = cursor.getInt(0);
            String apiToken     = cursor.getString(1);
            String email        = cursor.getString(2);
            String lastSyncDate = cursor.getString(3);
            int apiUserId       = cursor.getInt(4);

            user = new BasicUser(userId, apiToken, email, lastSyncDate, apiUserId);
            ArrayList<Note> notes = getNotes(user.getId(), null, null);

            user.setNotes(notes);

        } catch (CursorIndexOutOfBoundsException e) {
            // Tried to access an index that doesn't exist in the cursor
        }

        return user;
    }

    public List<User> getAllUsers() {
        List<User> userList = new ArrayList<User>();
        String selectQuery = "SELECT * FROM " + USERS_TABLE;
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);
        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {
                int userId          = cursor.getInt(0);
                String apiToken     = cursor.getString(1);
                String email        = cursor.getString(2);
                String lastSyncDate = cursor.getString(3);
                int apiUserId       = cursor.getInt(4);

                BasicUser user = new BasicUser(userId, apiToken, email, lastSyncDate, apiUserId);
                ArrayList<Note> notes = getNotes(userId, null, null);
                user.setNotes(notes);

                userList.add(user);

            } while (cursor.moveToNext());
        }

        return userList;
    }



    public ArrayList<Note> getNotes(int userId, String filter, String filterWHERE) {
        ArrayList<Note> notesList = new ArrayList<Note>();
        SQLiteDatabase db = this.getWritableDatabase();
        String[] columns = {
                NOTE_ID,
                NOTE_TITLE,
                NOTE_DETAILS,
                NOTE_CREATED_AT,
                NOTE_UPDATED_AT,
                NOTE_API_ID,
                NOTE_USER_ID
        };
        String[] whereArguments = { String.valueOf(userId) };
        Cursor cursor = db.query(NOTES_TABLE, columns, NOTE_USER_ID + "= ?", whereArguments, null, null, null);
        if (filter != null && filterWHERE != null) {
            whereArguments = new String[] { String.valueOf(userId), filter };
            cursor = db.query(NOTES_TABLE, columns, NOTE_USER_ID + "= ? " + filterWHERE, whereArguments, null, null, null);
        }

        if (cursor.moveToFirst()) {
            do {
                try {
                    int noteId           = cursor.getInt(0);
                    String noteTitle     = cursor.getString(1);
                    String noteDetails   = cursor.getString(2);
                    String noteCreatedAt = cursor.getString(3);
                    String noteUpdatedAt = cursor.getString(4);
                    int apiNoteId        = cursor.getInt(5);
                    int noteUserId       = cursor.getInt(6);

                    notesList.add(new BasicNote(noteId, noteTitle, noteDetails, noteCreatedAt, noteUpdatedAt, apiNoteId, noteUserId));

                } catch (CursorIndexOutOfBoundsException e) {
                    // Tried to access an index that doesn't exist in the cursor
                }

            } while (cursor.moveToNext());
        }

        return notesList;
    }

    public Note getNote(int id) {
        SQLiteDatabase db = this.getReadableDatabase();

        String[] columns = {
                NOTE_ID,
                NOTE_TITLE,
                NOTE_DETAILS,
                NOTE_CREATED_AT,
                NOTE_UPDATED_AT,
                NOTE_API_ID,
                NOTE_USER_ID
        };
        Cursor cursor = db.query(NOTES_TABLE, columns, NOTE_ID + "=?", new String[] { String.valueOf(id) }, null, null, null, null);
        if (cursor != null) {
            cursor.moveToFirst();
        }

        BasicNote note = null;
        try {
            int noteId           = cursor.getInt(0);
            String noteTitle     = cursor.getString(1);
            String noteDetails   = cursor.getString(2);
            String noteCreatedAt = cursor.getString(3);
            String noteUpdatedAt = cursor.getString(4);
            int apiNoteId        = cursor.getInt(5);
            int noteUserId       = cursor.getInt(6);

            note = new BasicNote(noteId, noteTitle, noteDetails, noteCreatedAt, noteUpdatedAt, apiNoteId, noteUserId);

        } catch (CursorIndexOutOfBoundsException e) {
            // Tried to access an index that doesn't exist in the cursor
        }

        return note;
    }

    public BasicNote getFirstNote() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM "+NOTES_TABLE+" WHERE "+NOTE_ID+" = (SELECT MIN("+NOTE_ID+") FROM "+NOTES_TABLE+");", null);
        if (cursor != null) {
            cursor.moveToFirst();
        }

        BasicNote note = null;
        try {
            int noteId           = cursor.getInt(0);
            String noteTitle     = cursor.getString(1);
            String noteDetails   = cursor.getString(2);
            String noteCreatedAt = cursor.getString(3);
            String noteUpdatedAt = cursor.getString(4);
            int apiNoteId        = cursor.getInt(5);
            int noteUserId       = cursor.getInt(6);

            note = new BasicNote(noteId, noteTitle, noteDetails, noteCreatedAt, noteUpdatedAt, apiNoteId, noteUserId);

        } catch (CursorIndexOutOfBoundsException e) {
            // Tried to access an index that doesn't exist in the cursor
        }

        return note;
    }

    /*******************************************************************
     * INSERT
     *******************************************************************/

    public void addUser(User user) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(USER_API_TOKEN, user.getApiToken());
        values.put(USER_EMAIL_ADDRESS, user.getEmailAddress());
        values.put(USER_LAST_SYNC_DATE, String.valueOf(new DateTime()));
        values.put(USER_API_ID, user.getAPIUserId());
        long searchId = db.insert(USERS_TABLE, null, values);
        db.close();
        user.setId(Integer.parseInt(String.valueOf(searchId)));
    }

    public void addNote(Note note) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(NOTE_TITLE,      note.getTitle());
        values.put(NOTE_DETAILS,    note.getDetails());
        values.put(NOTE_API_ID,     note.getAPINoteId());
        values.put(NOTE_CREATED_AT, note.getCreatedAt());
        values.put(NOTE_UPDATED_AT, note.getUpdatedAt());
        values.put(NOTE_USER_ID,    note.getUserId());
        long noteId = db.insert(NOTES_TABLE, null, values);
        note.setId((int) noteId);
        Log.d("DataBaseHelper", "New Note ID: " + String.valueOf(noteId));
        db.close();
    }

    /*******************************************************************
     * UPDATE
     *******************************************************************/

    public void updateNote(int id, String[] columns, String[] values) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        for (int i = 0; i < columns.length; i++) {
            String column = columns[i];
            String value = values[i];
            contentValues.put(column, value);
        }
        db.update(NOTES_TABLE, contentValues, NOTE_ID + " = ?", new String[] { String.valueOf(id) });
    }

    /*******************************************************************
     * DELETE
     *******************************************************************/

    public void clearNotesTable() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM " + NOTES_TABLE);
    }

    public boolean deleteNote(Note note) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(NOTES_TABLE, NOTE_ID + " = " + note.getId(), null) > 0;
    }

}