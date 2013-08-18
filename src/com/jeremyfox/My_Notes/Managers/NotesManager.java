package com.jeremyfox.My_Notes.Managers;

import android.content.Context;
import com.jeremyfox.My_Notes.Helpers.DataBaseHelper;
import com.jeremyfox.My_Notes.Models.BasicNote;
import com.jeremyfox.My_Notes.Interfaces.Note;
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
    public ArrayList<Note> getNotes(Context context, int userId, String filter, String filterWHERE) {
        DataBaseHelper db = new DataBaseHelper(context);
        return db.getNotes(userId, filter, filterWHERE);
    }

    /**
     * Sets notes.
     *
     * @param notes the notes
     */
    public void setNotes(Context context, JSONArray notes) {
        try {
            for (int i=0; i<notes.length(); i++) {
                JSONObject currentNote = notes.getJSONObject(i);

                int id           = currentNote.getInt(Note.ID_KEY);
                String title     = currentNote.getString(Note.TITLE_KEY);
                String details   = currentNote.getString(Note.DETAILS_KEY);
                String createdAt = currentNote.getString(Note.CREATED_AT_KEY);
                String updatedAt = currentNote.getString(Note.UPDATED_AT_KEY);

                BasicNote basicNote = new BasicNote(id, title, details, createdAt, updatedAt);

                NotesManager.this.notes.put(basicNote);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public Note getNote(Context context, int id) {
        DataBaseHelper db = new DataBaseHelper(context);
        db.getNote(id);
        return null;
    }

    public Note getFirstNote() {
        Note note = null;
        try {
            note = (Note)getNotes().get(0);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return note;
    }

    /**
     * Removes the supplied note from the "notes" array.
     *
     * @param note the note to remove
     * @return the boolean
     */
    public boolean removeNote(Note note) {
        boolean removed = false;
        if (null != note) {
            JSONArray newArray = new JSONArray();
            for (int i=0; i<this.notes.length(); i++) {
                try {
                    BasicNote currentNote = ((BasicNote)this.notes.get(i));
                    if (note.getRecordID() != currentNote.getRecordID()) {
                        newArray.put(currentNote);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            if (newArray.length() == (this.notes.length() - 1)) {
                removed = true;
                this.notes = newArray;
            }
        }
        return removed;
    }
}
