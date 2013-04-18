package com.jeremyfox.My_Notes.Managers;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.widget.EditText;
import android.widget.Toast;
import com.jeremyfox.My_Notes.Classes.BasicNote;
import com.jeremyfox.My_Notes.Dialogs.NewNoteDialog;
import com.jeremyfox.My_Notes.Helpers.PrefsHelper;
import com.jeremyfox.My_Notes.Interfaces.NetworkCallback;
import com.jeremyfox.My_Notes.Interfaces.Note;
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
     * Asynchronously retrieves notes from API.
     *
     * @param context the context
     * @param callback the callback
     * @return the boolean
     */
    public boolean retrieveNotesFromAPI(Context context, final NetworkCallback callback) {
        AnalyticsManager.getInstance().fireEvent("retreiving notes from API", null);
        final boolean[] requestSuccessful = {false};
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

    public Note getNote(int recordID) {
        for (int i=0; i<getNotes().length(); i++) {
            Note currentNote = null;
            try {
                currentNote = (Note)getNotes().get(i);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            if (currentNote.getRecordID() == recordID) {
                return currentNote;
            }
        }
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
     * Remove note.
     *
     * @param note the note
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

    public void deleteNote(final Activity activity, final Note note, final NetworkCallback callback) {
        NetworkManager networkManager = NetworkManager.getInstance();
        String url = NetworkManager.API_HOST+"/notes/"+note.getRecordID()+".json?unique_id="+PrefsHelper.getPref(activity, activity.getString(R.string.user_id));
        networkManager.executeDeleteRequest(activity, url, null, new NetworkCallback() {
            @Override
            public void onSuccess(Object json) {
                removeNote(note);
                callback.onSuccess(null);
            }

            @Override
            public void onFailure(int statusCode) {
                callback.onFailure(statusCode);
            }
        });
    }

    /**
     * Edit note.
     *
     * @param activity the activity
     * @param note the note
     * @param callback the callback
     */
    public void editNote(final Activity activity, final Note note, final NetworkCallback callback) {
        final EditText titleInput = new EditText(activity);
        titleInput.setText(note.getTitle());
        final EditText detailsInput = new EditText(activity);
        detailsInput.setText(note.getDetails());

        NewNoteDialog newNoteDialog = new NewNoteDialog(activity, activity.getString(R.string.editNote), titleInput, detailsInput, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                boolean titleEmpty = titleInput.getText().toString().length() == 0;
                boolean detailsEmpty = detailsInput.getText().toString().length() == 0;
                if (titleEmpty || detailsEmpty) {
                    Toast.makeText(activity, activity.getString(R.string.allFeildsRequired), Toast.LENGTH_SHORT).show();
                } else {
                    final String title = titleInput.getText().toString();
                    final String details = detailsInput.getText().toString();

                    JSONObject innerParams = new JSONObject();
                    JSONObject params = new JSONObject();
                    try {
                        innerParams.put("title", title);
                        innerParams.put("details", details);
                        params.put("note", innerParams);
                        params.put(activity.getString(R.string.unique_id), PrefsHelper.getPref(activity, activity.getString(R.string.user_id)));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    NetworkManager networkManager = NetworkManager.getInstance();
                    networkManager.executePutRequest(activity, NetworkManager.API_HOST + "/notes/" + note.getRecordID() + ".json", params, new NetworkCallback() {
                        @Override
                        public void onSuccess(Object json) {
                            JSONObject jsonObject = new JSONObject();
                            try {
                                jsonObject.put("title", title);
                                jsonObject.put("details", details);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            callback.onSuccess(jsonObject);
                        }

                        @Override
                        public void onFailure(int statusCode) {
                            callback.onFailure(statusCode);
                        }
                    });

                }
            }
        });
        newNoteDialog.showDialog();
    }
}
