package com.jeremyfox.My_Notes.Managers;

import android.content.Context;
import com.jeremyfox.My_Notes.Classes.BasicNote;
import com.jeremyfox.My_Notes.Helpers.PrefsHelper;
import com.jeremyfox.My_Notes.Interfaces.NetworkCallback;
import com.jeremyfox.My_Notes.R;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

/**
 * Created with IntelliJ IDEA.
 * User: jeremy
 * Date: 3/10/13
 * Time: 8:38 PM
 */
public class NotesManager {

    private static NotesManager instance = null;
    private JSONArray notes = new JSONArray();

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
     * Synchronously retrieves notes from API.
     *
     * @param context the context
     * @return the boolean
     */
    public boolean retrieveNotesFromAPI(Context context, final NetworkCallback callback) {
        AnalyticsManager.getInstance().fireEvent("retreiving notes from API", null);
        final boolean[] requestSuccessful = {false};
        if (NetworkManager.isConnected(context)) {
            NetworkManager networkManager = NetworkManager.getInstance();
            String user_id = PrefsHelper.getPref(context, context.getString(R.string.user_id));
            String query = NetworkManager.API_HOST + "/notes.json?unique_id=" + user_id;
            networkManager.executeGetRequest(context, query, new NetworkCallback() {
                @Override
                public void onSuccess(Object json) {
                    if (null != json) {
                        JSONArray jsonArray = (JSONArray)json;
                        try {
                            if (null != jsonArray && jsonArray.length() > 0) {
                                requestSuccessful[0] = true;
                                NotesManager.this.notes = new JSONArray();
                                for (int i=0; i<jsonArray.length(); i++) {
                                    JSONObject currentNote = jsonArray.getJSONObject(i);
                                    String title = currentNote.getString("title");
                                    String details = currentNote.getString("details");
                                    int recordId = currentNote.getInt("id");

                                    BasicNote basicNote = new BasicNote(title, details, recordId);
                                    NotesManager.this.notes.put(basicNote);
                                }
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    AnalyticsManager.getInstance().fireEvent("successfully retrieved notes from API", null);
                    callback.onSuccess(json);
                }

                @Override
                public void onFailure(int statusCode) {
                    HashMap map = new HashMap<String, String>();
                    map.put("status_code", Integer.toString(statusCode));
                    AnalyticsManager.getInstance().fireEvent("error saving new note to API", map);
                    callback.onFailure(statusCode);
                }
            });
        }

        return requestSuccessful[0];
    }

    /**
     * Gets notes object.
     *
     * @return the notes object
     */
    public JSONArray getNotes() {
        return this.notes;
    }

    public boolean removeNote(BasicNote note) {
        boolean removed = false;
        if (null != note) {
            JSONArray newArray = new JSONArray();
            for (int i=0; i<this.notes.length(); i++) {
                try {
                    BasicNote currentNote = ((BasicNote)this.notes.get(i));
                    if (note.getRecordId() != currentNote.getRecordId()) {
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
