package com.jeremyfox.My_Notes.Managers;

import android.content.Context;
import com.jeremyfox.My_Notes.Helpers.DataBaseHelper;
import com.jeremyfox.My_Notes.Models.BasicNote;
import com.jeremyfox.My_Notes.Interfaces.Note;
import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: jeremy
 * Date: 3/10/13
 * Time: 8:38 PM
 */
public class NotesManager {

    private static NotesManager instance = null;

    /**
     * Instantiates a new Notes manager.
     *
     * @throws JSONException the jSON exception
     */
    protected NotesManager() {
    }

    /**
     * Gets instance.
     *
     * @return the instance
     */
    public static NotesManager getInstance() {
        if(instance == null) {
            instance = new NotesManager();
        }
        return instance;
    }

    /**
     * Gets notes object.
     *
     * @return the notes object
     */
    public ArrayList<Note> getNotes(Context context, int userId, String filter, String filterWHERE, String sort) {
        DataBaseHelper db = new DataBaseHelper(context);
        return db.getNotes(userId, filter, filterWHERE, sort);
    }

    public void addNote(Context context, Note note) {
        DataBaseHelper db = new DataBaseHelper(context);
        note.setUserId(db.getCurrentUser(null, null, null).getId());
        if (note.getCreatedAt() == null || note.getCreatedAt().length() == 0) {
            String date = String.valueOf(new DateTime());
            note.setCreatedAt(date);
        }
        if (note.getUpdatedAt() == null || note.getUpdatedAt().length() == 0) {
            note.setUpdatedAt(note.getCreatedAt());
        }
        if (note.getAPINoteId() <= 0) {
            note.setAPINoteId(-1);
        }
        db.addNote(note);
    }

    /**
     * Sets notes.
     *
     * @param notes the notes
     */
    public void addNotes(Context context, JSONArray notes) {
        DataBaseHelper db = new DataBaseHelper(context);
        int userId = db.getCurrentUser(null, null, null).getId();

        /**
         * First check to see if there are local Notes that have not been synced to the API yet
         * If so, set them aside so they can be re-added to the database after all notes from the
         * API have been stored
         */
        String filterValue = Note.UNSYNCED_NOTE;
        String filterWHERE = "AND "+DataBaseHelper.NOTE_API_ID+" = ?";
        List<Note> unsyncedNotes = db.getNotes(userId, filterValue, filterWHERE, null);

        /**
         * Next, dump all notes currently in the Notes Table
         */
        db.clearNotesTable();

        /**
         * Then store all notes that came back from the API in the Notes Table
         */
        try {
            for (int i=0; i<notes.length(); i++) {
                JSONObject currentNote = notes.getJSONObject(i);

                int apiNoteID    = currentNote.getInt(Note.ID_KEY); // This is from the API JSON response, "id" is the API's Note ID
                String title     = currentNote.getString(Note.TITLE_KEY);
                String details   = currentNote.getString(Note.DETAILS_KEY);
                String createdAt = currentNote.getString(Note.CREATED_AT_KEY);
                String updatedAt = currentNote.getString(Note.UPDATED_AT_KEY);

                BasicNote basicNote = new BasicNote();
                basicNote.setAPINoteId(apiNoteID);
                basicNote.setCreatedAt(createdAt);
                basicNote.setUpdatedAt(updatedAt);
                basicNote.setTitle(title);
                basicNote.setDetails(details);
                basicNote.setUserId(userId);

                db.addNote(basicNote);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        /**
         * last, append any un-synced notes to the Notes Table
         */
        if (unsyncedNotes != null && unsyncedNotes.size() > 0) {
            for (Note note : unsyncedNotes) {
                db.addNote(note);
            }
        }
    }

    public Note getNote(Context context, int id) {
        DataBaseHelper db = new DataBaseHelper(context);
        return db.getNote(id);
    }

    public Note getFirstNote(Context context) {
        return new DataBaseHelper(context).getFirstNote();
    }

    public void updateNote(Context context, int id, String[] columns, String[] values) {
        DataBaseHelper db = new DataBaseHelper(context);
        db.updateNote(id, columns, values);
    }

    /**
     * Removes the supplied note from the Notes Table.
     *
     * @param note the note to remove
     * @return the boolean
     */
    public boolean removeNote(Context context, Note note) {
        boolean removed = false;
        if (note != null) {
            DataBaseHelper db = new DataBaseHelper(context);
            removed = db.deleteNote(note);
        }
        return removed;
    }
}
