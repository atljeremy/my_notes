package com.jeremyfox.My_Notes.Activities;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;
import com.jeremyfox.My_Notes.Adapters.NotesAdapter;
import com.jeremyfox.My_Notes.Models.BasicNote;
import com.jeremyfox.My_Notes.Classes.MyNotesAPIResultReceiver;
import com.jeremyfox.My_Notes.Classes.ResponseObject;
import com.jeremyfox.My_Notes.Dialogs.NewNoteDialog;
import com.jeremyfox.My_Notes.Fragments.NoteDetailsFragment;
import com.jeremyfox.My_Notes.Fragments.NotesListFragment;
import com.jeremyfox.My_Notes.Helpers.PrefsHelper;
import com.jeremyfox.My_Notes.Interfaces.NetworkCallback;
import com.jeremyfox.My_Notes.Interfaces.Note;
import com.jeremyfox.My_Notes.Managers.AnalyticsManager;
import com.jeremyfox.My_Notes.Managers.NetworkManager;
import com.jeremyfox.My_Notes.Managers.NotesManager;
import com.jeremyfox.My_Notes.R;
import com.jeremyfox.My_Notes.Services.MyNotesAPIService;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * The Main activity.
 */
public class MainActivity extends Activity implements NotesListFragment.NotesListListener, NoteDetailsFragment.NoteDetailsListener, MyNotesAPIResultReceiver.Receiver {

    private static final int NEW_NOTE_REQUEST_CODE = 1;
    private GridView gridView;
    private NotesListFragment notesListFragment;
    private MyNotesAPIResultReceiver receiver;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.notes_list_fragment);

        this.gridView = (GridView)findViewById(R.id.gridview);
        this.notesListFragment = (NotesListFragment)getFragmentManager().findFragmentById(R.id.notes_list_fragment);
    }

    @Override
    public void onStart() {
        super.onStart();
        AnalyticsManager.fireEvent(this, "application started", null);
    }

    @Override
    public void onResume() {
        super.onResume();
        AnalyticsManager.fireEvent(this, "application resumed", null);
    }

    @Override
    public void onDestroy() {
        AnalyticsManager.fireEvent(this, "application shutdown", null);
        AnalyticsManager.flushEvents(this);
        super.onDestroy();
    }

    @Override
    public void newNoteAction() {
        NotesAdapter notesAdapter = (NotesAdapter)this.gridView.getAdapter();
        if (null != notesAdapter) notesAdapter.setShouldIncrementCounter(true);
        AnalyticsManager.fireEvent(this, "selected new note option", null);
        Intent newNoteIntent = new Intent(this, NewNoteActivity.class);
        startActivityForResult(newNoteIntent, NEW_NOTE_REQUEST_CODE);
    }

    @Override
    public void registerWithAPI(final NetworkCallback callback) {
        NetworkManager networkManager = NetworkManager.getInstance();
        String url = NetworkManager.getInstance().getApiHost() + "/users.json";
        networkManager.executePostRequest(MainActivity.this, url, null, new NetworkCallback() {
            @Override
            public void onSuccess(Object json) {
                try {
                    String unique_id = ((JSONObject)json).getString(getString(R.string.unique_id));
                    PrefsHelper.setPref(getBaseContext(), getString(R.string.user_id), unique_id);
                    AnalyticsManager.registerSuperProperty(MainActivity.this, "user API key", unique_id);
                    callback.onSuccess(null);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                AnalyticsManager.fireEvent(MainActivity.this, "successful API registration", null);
            }

            @Override
            public void onFailure(int statusCode) {
                HashMap map = new HashMap<String, String>();
                map.put("status_code", Integer.toString(statusCode));
                AnalyticsManager.fireEvent(MainActivity.this, "failed API registration", map);
                callback.onFailure(statusCode);
            }
        });
    }

    /**
     * MyNotesAPIService GET notes request
     */
    @Override
    public void requestNotesFromAPI() {
        if (this.receiver == null) {
            this.receiver = new MyNotesAPIResultReceiver(new Handler());
            this.receiver.setReceiver(this);
        }
        final Intent intent = new Intent(Intent.ACTION_SYNC, null, this, MyNotesAPIService.class);
        intent.putExtra("receiver", this.receiver);
        intent.putExtra("action", MyNotesAPIService.GET_NOTES);
        startService(intent);
    }

    /**
     * MyNotesAPIService POST new note request
     * @param title the title
     * @param details the details
     */
    public void saveNoteToAPI(String title, String details) {
        if (this.receiver == null) {
            this.receiver = new MyNotesAPIResultReceiver(new Handler());
            this.receiver.setReceiver(this);
        }
        final Intent intent = new Intent(Intent.ACTION_SYNC, null, this, MyNotesAPIService.class);
        intent.putExtra("title", title);
        intent.putExtra("details", details);
        intent.putExtra("receiver", this.receiver);
        intent.putExtra("action", MyNotesAPIService.SAVE_NOTE);
        startService(intent);
    }

    /**
     * MyNotesAPIService DELETE notes request
     * @param notesArray
     */
    @Override
    public void deleteNotes(ArrayList<Note> notesArray) {
        if (this.receiver == null) {
            this.receiver = new MyNotesAPIResultReceiver(new Handler());
            this.receiver.setReceiver(this);
        }
        final Intent intent = new Intent(Intent.ACTION_SYNC, null, this, MyNotesAPIService.class);
        intent.putParcelableArrayListExtra("notesArray", notesArray);
        intent.putExtra("receiver", this.receiver);
        intent.putExtra("action", MyNotesAPIService.DELETE_NOTES);
        startService(intent);
    }

    /**
     * MyNotesAPIService PUT edited note request
     * @param recordID the record iD
     * @param title the title
     * @param details the details
     */
    public void updateNoteToAPI(int recordID, String title, String details) {
        if (this.receiver == null) {
            this.receiver = new MyNotesAPIResultReceiver(new Handler());
            this.receiver.setReceiver(this);
        }
        final Intent intent = new Intent(Intent.ACTION_SYNC, null, this, MyNotesAPIService.class);
        intent.putExtra("title", title);
        intent.putExtra("details", details);
        intent.putExtra("recordID", recordID);
        intent.putExtra("receiver", this.receiver);
        intent.putExtra("action", MyNotesAPIService.EDIT_NOTES);
        startService(intent);
    }

    /**
     * Called from the MyNotesAPIService to report the status of the request (RUNNING, FINISHED, ERROR)
     *
     * @param resultCode The status of the request. can be STATUS_RUNNING, STATUS_FINISHED, or STATUS_ERROR
     * @param resultData The result data
     */
    @Override
    public void onReceiveResult(int resultCode, Bundle resultData) {
        ResponseObject responseObject;
        int action = resultData.getInt("action");
        switch (resultCode) {
            case MyNotesAPIService.STATUS_RUNNING:
                Log.d("MainActivity", "STATUS_RUNNING");
                break;

            case MyNotesAPIService.STATUS_FINISHED:
                responseObject = (ResponseObject)resultData.getSerializable("result");
                switch (action) {
                    case MyNotesAPIService.GET_NOTES:
                        if (responseObject.getStatus() == ResponseObject.RequestStatus.STATUS_SUCCESS) {
                            if (responseObject.getObject() instanceof JSONArray) {
                                JSONArray array = (JSONArray)responseObject.getObject();
                                NotesManager.getInstance().setNotes(this, array);
                                MainActivity.this.notesListFragment.createGridView();
                                AnalyticsManager.fireEvent(this, "successfully retrieved notes from API", null);
                            }
                        } else {
                            MainActivity.this.notesListFragment.showLoadingError();
                            AnalyticsManager.fireEvent(this, "error retrieving notes from API", null);
                        }
                        break;

                    case MyNotesAPIService.SAVE_NOTE:
                        if (responseObject.getStatus() == ResponseObject.RequestStatus.STATUS_SUCCESS) {
                            if (responseObject.getObject() instanceof JSONObject) {
                                MainActivity.this.notesListFragment.requestNotes();
                                Toast.makeText(MainActivity.this, getString(R.string.noteSaved), Toast.LENGTH_SHORT).show();
                                AnalyticsManager.fireEvent(this, "new note created successfully", null);
                            }
                        } else {
                            MainActivity.this.notesListFragment.showSavingError();
                            AnalyticsManager.fireEvent(this, "error saving new note to API", null);
                        }
                        break;

                    case MyNotesAPIService.EDIT_NOTES:
                        NoteDetailsFragment.FRAGMENT.dismissDialog();
                        if (responseObject.getStatus() == ResponseObject.RequestStatus.STATUS_SUCCESS) {
                            if (responseObject.getObject() instanceof JSONObject) {
                                JSONObject jsonObject = (JSONObject)responseObject.getObject();
                                String title = null;
                                String details = null;
                                try {
                                    title = jsonObject.getString("title");
                                    details = jsonObject.getString("details");
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }

                                if (null != title && null != details) {
                                    NoteDetailsFragment.FRAGMENT.updateCurrentNote(title, details);
                                }

                                Toast.makeText(MainActivity.this, getString(R.string.notesUpdated), Toast.LENGTH_SHORT).show();
                                AnalyticsManager.fireEvent(this, "note updated successfully", null);
                            }
                        } else {
                            NoteDetailsFragment.FRAGMENT.showLoadingError();
                            AnalyticsManager.fireEvent(this, "error updating note to API", null);
                        }
                        break;

                    case MyNotesAPIService.DELETE_NOTES:
                        if (responseObject.getStatus() == ResponseObject.RequestStatus.STATUS_SUCCESS) {
                            MainActivity.this.notesListFragment.setGridViewItems();
                            Toast.makeText(MainActivity.this, getString(R.string.notesDeleted), Toast.LENGTH_SHORT).show();
                            AnalyticsManager.fireEvent(this, "successfully deleted note from API", null);
                        } else {
                            MainActivity.this.notesListFragment.showSavingError();
                            AnalyticsManager.fireEvent(this, "error deleting note from API", null);
                        }
                        break;
                }
                Log.d("MainActivity", "STATUS_FINISHED");
                break;

            case MyNotesAPIService.STATUS_ERROR:
                Log.d("MainActivity", "STATUS_ERROR");
                MainActivity.this.notesListFragment.showLoadingError();
                break;

            default:
                Log.d("default", "default");
                break;
        }
    }

    @Override
    public void editNote(int recordID, final TextView title, final TextView details) {
        final Note note = NotesManager.getInstance().getNote(recordID);
        if (null != note) {
            final EditText titleInput = new EditText(this);
            titleInput.setText(note.getTitle());
            final EditText detailsInput = new EditText(this);
            detailsInput.setText(note.getDetails());

            NewNoteDialog newNoteDialog = new NewNoteDialog(this, getString(R.string.editNote), titleInput, detailsInput, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {

                    boolean titleEmpty = titleInput.getText().toString().length() == 0;
                    boolean detailsEmpty = detailsInput.getText().toString().length() == 0;
                    if (titleEmpty || detailsEmpty) {
                        Toast.makeText(MainActivity.this, getString(R.string.allFeildsRequired), Toast.LENGTH_SHORT).show();
                    } else {
                        final String title = titleInput.getText().toString();
                        final String details = detailsInput.getText().toString();
                        updateNoteToAPI(note.getRecordID(), title, details);
                    }
                }
            });
            newNoteDialog.showDialog();
        } else {
            Toast.makeText(this, getString(R.string.unexpected_error), Toast.LENGTH_LONG);
        }
    }

    @Override
    public void shareNote(Intent shareIntent) {
        startActivity(Intent.createChooser(shareIntent, "Share via"));
    }

    @Override
    public void setNote(Note note) {}

    @Override
    public void deleteNote(int recordID) {
        AnalyticsManager.fireEvent(this, "delete note from note details view", null);
        final Note note = NotesManager.getInstance().getNote(recordID);
        ArrayList<Note> notesArray = new ArrayList<Note>();
        notesArray.add(note);
        deleteNotes(notesArray);
    }

    @Override
    public void dismissNote() {}

    @Override
    public void showNoteDetails(int index, boolean dualMode) {

        if (dualMode) {
            // We can display everything in-place with fragments, so update
            // the list to highlight the selected item and show the data.
            this.gridView.setItemChecked(index, true);

            // Check what fragment is currently shown, replace if needed.
            NoteDetailsFragment details = (NoteDetailsFragment)getFragmentManager().findFragmentById(R.id.note_details_fragment);
            if (details == null || details.getShownIndex() != index) {
                // Make new fragment to show this selection.
                details = NoteDetailsFragment.newInstance(index);

                // Execute a transaction, replacing any existing fragment
                // with this one inside the frame.
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                ft.replace(R.id.note_details_fragment, details);
                ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
                ft.commit();
            }

        } else {
            // Otherwise we need to launch a new activity to display
            // the dialog fragment with selected text.
            JSONArray notes = NotesManager.getInstance().getNotes();
            BasicNote note = null;
            try {
                note = (BasicNote)notes.get(index);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            Intent noteDetailsIntent = new Intent(MainActivity.this, NoteDetailsActivity.class);
            noteDetailsIntent.putExtra("title", note.getTitle());
            noteDetailsIntent.putExtra("details", note.getDetails());
            noteDetailsIntent.putExtra("id", note.getRecordID());
            noteDetailsIntent.putExtra("index", index);
            startActivity(noteDetailsIntent, null);
            AnalyticsManager.fireEvent(this, "opened a note", null);
        }
    }

    /**
     * Receives the result of the NewNoteActivity
     * @param requestCode
     * @param resultCode
     * @param data
     */
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode == RESULT_OK){
            if (requestCode == NEW_NOTE_REQUEST_CODE) {
                String title = data.getStringExtra(Note.TITLE_KEY);
                String details = data.getStringExtra(Note.DETAILS_KEY);
                saveNoteToAPI(title, details);
            }
        }
    }
}
