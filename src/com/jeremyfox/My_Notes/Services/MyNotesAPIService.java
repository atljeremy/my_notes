package com.jeremyfox.My_Notes.Services;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.util.Log;
import com.jeremyfox.My_Notes.Classes.ResponseObject;
import com.jeremyfox.My_Notes.Helpers.DataBaseHelper;
import com.jeremyfox.My_Notes.Helpers.PrefsHelper;
import com.jeremyfox.My_Notes.Interfaces.Note;
import com.jeremyfox.My_Notes.Interfaces.User;
import com.jeremyfox.My_Notes.Managers.NetworkManager;
import com.jeremyfox.My_Notes.Managers.NotesManager;
import com.jeremyfox.My_Notes.R;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: jeremy
 * Date: 4/21/13
 * Time: 7:16 PM
 */
public class MyNotesAPIService extends IntentService {

    public static final int GET_NOTES       = 111;
    public static final int SAVE_NOTE       = 222;
    public static final int DELETE_NOTES    = 333;
    public static final int EDIT_NOTES      = 444;
    public static final int STATUS_RUNNING  = 555;
    public static final int STATUS_FINISHED = 666;
    public static final int STATUS_ERROR    = 777;
    public static final String RECEIVER_KEY = "receiver";
    public static final String ACTION_KEY   = "action";
    public static final String RESULT_KEY   = "result";

    public MyNotesAPIService() {
        super("MyNotesAPIService");
    }

    protected void onHandleIntent(Intent intent) {
        Bundle extras = intent.getExtras();
        final ResultReceiver receiver = extras.getParcelable(RECEIVER_KEY);
        int action = intent.getIntExtra(ACTION_KEY, GET_NOTES);
        Bundle resultBundle = new Bundle();

        receiver.send(STATUS_RUNNING, Bundle.EMPTY);

        switch (action) {
            case GET_NOTES:
                getNotes(extras, resultBundle, receiver);
                break;

            case SAVE_NOTE:
                saveNote(extras, resultBundle, receiver);
                break;

            case DELETE_NOTES:
                deleteNotes(extras, resultBundle, receiver);
                break;

            case EDIT_NOTES:
                editNotes(extras, resultBundle, receiver);
                break;

            default:
                receiver.send(STATUS_ERROR, resultBundle);
                break;

        }

        this.stopSelf();
    }

    private void getNotes(Bundle extras, Bundle resultBundle, ResultReceiver receiver) {
        String userToken = extras.getString(User.API_TOKEN_KEY);
        String query     = NetworkManager.getInstance().getApiHost() + "/notes.json?unique_id=" + userToken;
        String response  = processRequest(query, null, NetworkManager.RequestType.GET);
        Object result    = processResult(response);
        ResponseObject.RequestStatus status = (null != result) ? ResponseObject.RequestStatus.STATUS_SUCCESS : ResponseObject.RequestStatus.STATUS_FAILED;

        resultBundle.putSerializable(RESULT_KEY, new ResponseObject(result, status));
        resultBundle.putInt(ACTION_KEY, GET_NOTES);
        receiver.send(STATUS_FINISHED, resultBundle);
    }

    private void saveNote(Bundle extras, Bundle resultBundle, ResultReceiver receiver) {
        try {
            String title           = extras.getString(Note.TITLE_KEY);
            String details         = extras.getString(Note.DETAILS_KEY);
            int id                 = extras.getInt(Note.ID_KEY);
            String userToken       = extras.getString(User.API_TOKEN_KEY);
            JSONObject innerParams = new JSONObject();
            JSONObject params      = new JSONObject();

            innerParams.put(Note.TITLE_KEY, title);
            innerParams.put(Note.DETAILS_KEY, details);
            params.put(Note.NOTE_KEY, innerParams);
            params.put(User.API_TOKEN_KEY, userToken);

            String query    = NetworkManager.getInstance().getApiHost() + "/notes.json";
            String response = processRequest(query, params, NetworkManager.RequestType.POST);
            Object result   = processResult(response);
            ResponseObject.RequestStatus status = (null != result) ? ResponseObject.RequestStatus.STATUS_SUCCESS : ResponseObject.RequestStatus.STATUS_FAILED;

            JSONObject jsonObject = new JSONObject();
            jsonObject.put(Note.ID_KEY, id);
            jsonObject.put(Note.API_ID_KEY, (result != null) ? Integer.parseInt(((JSONObject)result).getString(Note.ID_KEY)) : -1 );
            resultBundle.putSerializable(RESULT_KEY, new ResponseObject(jsonObject, status));
            resultBundle.putInt(ACTION_KEY, SAVE_NOTE);
            receiver.send(STATUS_FINISHED, resultBundle);
        } catch(JSONException e) {
            resultBundle.putString(Intent.EXTRA_TEXT, e.toString());
            receiver.send(STATUS_ERROR, resultBundle);
        }
    }

    private void deleteNotes(Bundle extras, Bundle resultBundle, ResultReceiver receiver) {
        ResponseObject.RequestStatus status = ResponseObject.RequestStatus.STATUS_FAILED;
        Object result = null;
        ArrayList<Note> notesArray = extras.getParcelableArrayList("notesArray");
        String userToken = extras.getString(User.API_TOKEN_KEY);

        ArrayList<Note> notesNotDeleted = new ArrayList<Note>();

        for (Note note : notesArray) {
            String query = NetworkManager.getInstance().getApiHost()+"/notes/"+note.getAPINoteId()+".json?unique_id="+userToken;
            String response = processRequest(query, null, NetworkManager.RequestType.DELETE);
            result = processResult(response);
            status = (null != result) ? ResponseObject.RequestStatus.STATUS_SUCCESS : ResponseObject.RequestStatus.STATUS_FAILED;
            if (status != ResponseObject.RequestStatus.STATUS_SUCCESS) {
                notesNotDeleted.add(note);
            }
        }

        resultBundle.putParcelableArrayList("notesArray", notesArray);
        resultBundle.putParcelableArrayList("notDeletedNotesArray", notesNotDeleted);
        resultBundle.putSerializable(RESULT_KEY, new ResponseObject(result, status));
        resultBundle.putInt(ACTION_KEY, DELETE_NOTES);
        receiver.send(STATUS_FINISHED, resultBundle);
    }

    private void editNotes(Bundle extras, Bundle resultBundle, ResultReceiver receiver) {
        try {
            int id                 = extras.getInt(Note.ID_KEY);
            String title           = extras.getString(Note.TITLE_KEY);
            String details         = extras.getString(Note.DETAILS_KEY);
            int noteAPIId          = extras.getInt(Note.API_ID_KEY);
            String userToken       = extras.getString(User.API_TOKEN_KEY);
            JSONObject innerParams = new JSONObject();
            JSONObject params      = new JSONObject();

            innerParams.put(Note.TITLE_KEY, title);
            innerParams.put(Note.DETAILS_KEY, details);
            params.put(Note.NOTE_KEY, innerParams);
            params.put(User.API_TOKEN_KEY, userToken);

            String query    = NetworkManager.getInstance().getApiHost() + "/notes/" + noteAPIId + ".json";
            String response = processRequest(query, params, NetworkManager.RequestType.PUT);
            Object result   = processResult(response);
            ResponseObject.RequestStatus status = (null != result) ? ResponseObject.RequestStatus.STATUS_SUCCESS : ResponseObject.RequestStatus.STATUS_FAILED;

            JSONObject jsonObject = new JSONObject();
            jsonObject.put(Note.TITLE_KEY, title);
            jsonObject.put(Note.DETAILS_KEY, details);
            jsonObject.put(Note.ID_KEY, id);
            resultBundle.putSerializable(RESULT_KEY, new ResponseObject(jsonObject, status));
            resultBundle.putInt(ACTION_KEY, EDIT_NOTES);
            receiver.send(STATUS_FINISHED, resultBundle);
        } catch(JSONException e) {
            resultBundle.putString(Intent.EXTRA_TEXT, e.toString());
            receiver.send(STATUS_ERROR, resultBundle);
        }
    }

    protected String processRequest(String url, JSONObject params, NetworkManager.RequestType requestType) {

        StringBuilder stringBuilder = new StringBuilder();
        if(NetworkManager.isConnected(this) && null != url && url.length() > 0) {
            HttpRequestBase httpRequestBase = NetworkManager.getInstance().createHttpRequest(url, params, requestType);

            HttpClient httpClient = new DefaultHttpClient();
            try {
                HttpResponse response = httpClient.execute(httpRequestBase);
                StatusLine statusLine = response.getStatusLine();
                int statusCode = statusLine.getStatusCode();
                boolean received200Status = (statusCode == NetworkManager.SUCCESS_STATUS);
                boolean received201Status = (statusCode == NetworkManager.SUCCESS_RECORD_CREATED_STATUS);
                boolean received204Status = (statusCode == NetworkManager.SUCCESS_RECORD_DELETED_STATUS);
                boolean received304Status = (statusCode == NetworkManager.NOT_MODIFIED_STATUS);

                if (received200Status || received201Status || received204Status || received304Status) {

                    HttpEntity entity = response.getEntity();
                    if (null != entity) {
                        InputStream inputStream = entity.getContent();
                        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                        String line;

                        while ((line = reader.readLine()) != null) {
                            stringBuilder.append(line);
                        }

                        inputStream.close();
                    }
                }
            } catch (IOException e) {
                Log.d("processRequest", e.getLocalizedMessage());
            }
        }

        return stringBuilder.toString();
    }

    protected Object processResult(String result) {
        Object response = null;
        final String JSON_ARRAY_OPEN_BRACKET = "[";
        boolean isJsonArray = false;

        if (null == result || result.length() == 0) {
            response = "{}";
        } else {
            int index = JSON_ARRAY_OPEN_BRACKET.indexOf(result.charAt(0));
            if (index >= 0) {
                isJsonArray = true;
            }
        }

        try {
            if (isJsonArray) {
                response = new JSONArray(result);
            } else {
                response = new JSONObject(result);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return response;
    }

}