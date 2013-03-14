package com.jeremyfox.My_Notes.Managers;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;
import com.jeremyfox.My_Notes.R;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: jeremy
 * Date: 3/10/13
 * Time: 8:38 PM
 */
public class NotesManager {

    private JSONArray notes;

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
    public JSONArray getNotesObject() {
        return this.notes;
    }

    private void createJSON(Context context) throws JSONException, IOException {

        AssetManager manager = context.getAssets();
        InputStream notesFile = manager.open("notes.json");
        byte[] data = new byte[notesFile.available()];

        if (null != data) {
            notesFile.read(data);
            notesFile.close();

            String notesString = new String(data);
            JSONObject notesObject = new JSONObject(notesString);
            JSONArray notesArray = notesObject.getJSONArray("notes");
            if (null != notesArray) {
                this.notes = notesArray;
            }
        }
    }
}
