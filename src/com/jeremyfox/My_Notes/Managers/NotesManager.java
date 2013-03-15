package com.jeremyfox.My_Notes.Managers;

import android.content.Context;
import android.content.res.AssetManager;
import com.jeremyfox.My_Notes.Classes.BasicNote;
import com.jeremyfox.My_Notes.R;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

/**
 * Created with IntelliJ IDEA.
 * User: jeremy
 * Date: 3/10/13
 * Time: 8:38 PM
 */
public class NotesManager {

    private JSONArray notes = new JSONArray();

    /**
     * Instantiates a new Notes manager.
     *
     * @throws JSONException the jSON exception
     */
    public NotesManager(Context context) throws JSONException, IOException {
        createJSON(context);
    }

    /**
     * Gets notes object.
     *
     * @return the notes object
     */
    public JSONArray getNotes() {
        return this.notes;
    }

    private void createJSON(Context context) throws JSONException, IOException {

        AssetManager manager = context.getAssets();
        InputStream notesFile = manager.open(context.getString(R.string.notesJson));
        byte[] data = new byte[notesFile.available()];

        if (null != data) {
            notesFile.read(data);
            notesFile.close();

            String notesString = new String(data);
            JSONObject notesObject = new JSONObject(notesString);
            JSONArray notesArray = notesObject.getJSONArray(context.getString(R.string.notes));
            if (null != notesArray && notesArray.length() > 0) {
                for (int i=0; i<notesArray.length(); i++) {
                    JSONObject currentNote = notesArray.getJSONObject(i);
                    Iterator iterator = currentNote != null ? currentNote.keys() : null;
                    while (iterator != null ? iterator.hasNext() : false) {
                        String title = (String)iterator.next();
                        String details = currentNote.getString(title);

                        BasicNote basicNote = new BasicNote(title, details);

                        this.notes.put(basicNote);
                    }
                }
            }
        }
    }
}
