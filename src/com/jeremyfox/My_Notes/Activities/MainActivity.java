package com.jeremyfox.My_Notes.Activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;
import com.jeremyfox.My_Notes.Adapters.NotesAdapter;
import com.jeremyfox.My_Notes.Classes.BasicNote;
import com.jeremyfox.My_Notes.Fragments.NoteDetailsFragment;
import com.jeremyfox.My_Notes.Fragments.NotesListFragment;
import com.jeremyfox.My_Notes.Helpers.PrefsHelper;
import com.jeremyfox.My_Notes.Interfaces.NetworkCallback;
import com.jeremyfox.My_Notes.Interfaces.Note;
import com.jeremyfox.My_Notes.Managers.AnalyticsManager;
import com.jeremyfox.My_Notes.Managers.NetworkManager;
import com.jeremyfox.My_Notes.Managers.NotesManager;
import com.jeremyfox.My_Notes.R;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

/**
 * The Main activity.
 */
public class MainActivity extends Activity implements NotesListFragment.NotesListListener, NoteDetailsFragment.NoteDetailsListener {

    public static Activity ACTIVITY;
    private static final int NEW_NOTE_REQUEST_CODE = 1;
    private GridView gridView;
    private Note note;
    private NotesListFragment notesListFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.notes_list_fragment);

        this.ACTIVITY = this;
        this.gridView = (GridView)findViewById(R.id.gridview);
        this.notesListFragment = (NotesListFragment)getFragmentManager().findFragmentById(R.id.notes_list_fragment);
    }

    @Override
    public void onStart() {
        super.onStart();
        AnalyticsManager.getInstance().fireEvent("application started", null);
    }

    @Override
    public void onResume() {
        super.onResume();
        AnalyticsManager.getInstance().fireEvent("application resumed", null);
    }

    @Override
    public void onDestroy() {
        AnalyticsManager.getInstance().fireEvent("application shutdown", null);
        AnalyticsManager.getInstance().flushEvents();
        super.onDestroy();
    }

    @Override
    public void newNoteAction() {
        NotesAdapter notesAdapter = (NotesAdapter)this.gridView.getAdapter();
        if (null != notesAdapter) notesAdapter.setShouldIncrementCounter(true);
        AnalyticsManager.getInstance().fireEvent("selected new note option", null);
        Intent newNoteIntent = new Intent(this, NewNoteActivity.class);
        startActivityForResult(newNoteIntent, NEW_NOTE_REQUEST_CODE);
    }

    @Override
    public void registerWithAPI(final NetworkCallback callback) {
        NetworkManager networkManager = NetworkManager.getInstance();
        networkManager.executePostRequest(MainActivity.this, NetworkManager.API_HOST + "/users.json", null, new NetworkCallback() {
            @Override
            public void onSuccess(Object json) {
                try {
                    String unique_id = ((JSONObject)json).getString(getString(R.string.unique_id));
                    PrefsHelper.setPref(getBaseContext(), getString(R.string.user_id), unique_id);
                    AnalyticsManager.getInstance().registerSuperProperty("user API key", unique_id);
                    callback.onSuccess(null);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                AnalyticsManager.getInstance().fireEvent("successful API registration", null);
            }

            @Override
            public void onFailure(int statusCode) {
                HashMap map = new HashMap<String, String>();
                map.put("status_code", Integer.toString(statusCode));
                AnalyticsManager.getInstance().fireEvent("failed API registration", map);
                callback.onFailure(statusCode);
            }
        });
    }

    @Override
    public void editNote(int recordID, final TextView title, final TextView details) {
        Note note = NotesManager.getInstance().getNote(recordID);
        if (null != note) {
            NotesManager.getInstance().editNote(this, note, new NetworkCallback() {
                @Override
                public void onSuccess(Object json) {
                    String newTitle = null;
                    String newDetails = null;
                    try {
                        newTitle = ((JSONObject) json).getString(getString(R.string.titleKey));
                        newDetails = ((JSONObject) json).getString(getString(R.string.detailsKey));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    if (null != newTitle && null != newDetails) {
                        title.setText(newTitle);
                        details.setText(newDetails);
                    }

                    MainActivity.this.notesListFragment.createGridView();
                }

                @Override
                public void onFailure(int statusCode) {
                    Toast.makeText(MainActivity.this, getString(R.string.error_saving_note), Toast.LENGTH_LONG);
                }
            });
        } else {
            Toast.makeText(this, getString(R.string.unexpected_error), Toast.LENGTH_LONG);
        }
    }

    @Override
    public void shareNote(Intent shareIntent) {
        startActivity(Intent.createChooser(shareIntent, "Share via"));
    }

    @Override
    public void setNote(Note note) {
        this.note = note;
    }

    @Override
    public void deleteNote(int recordID) {
        AnalyticsManager.getInstance().fireEvent("delete note from note details view", null);
        final ProgressDialog dialog = showLoadingDialog();
        final Note note = NotesManager.getInstance().getNote(recordID);
        NotesManager.getInstance().deleteNote(this, note, new NetworkCallback() {
            @Override
            public void onSuccess(Object json) {
                NotesManager.getInstance().removeNote(note);
                MainActivity.this.notesListFragment.setGridViewItems();
                Toast.makeText(MainActivity.this, "Note Deleted", Toast.LENGTH_SHORT).show();
                AnalyticsManager.getInstance().fireEvent("successfully deleted note from API", null);
                dialog.dismiss();
            }

            @Override
            public void onFailure(int statusCode) {
                dialog.dismiss();
                Toast.makeText(MainActivity.this, getString(R.string.error_deleting_note), Toast.LENGTH_LONG);
                HashMap map = new HashMap<String, String>();
                map.put("status_code", Integer.toString(statusCode));
                AnalyticsManager.getInstance().fireEvent("error deleting note from API", map);
            }
        });
    }

    @Override
    public void dismissNote() {

    }

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
            AnalyticsManager.getInstance().fireEvent("opened a note", null);
        }
    }

    /**
     * Receives the result of the NewNoteActivity
     * @param requestCode
     * @param resultCode
     * @param data
     */
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == NEW_NOTE_REQUEST_CODE) {
            if(resultCode == RESULT_OK){
                /**
                 * Save the new note then update the grid view
                 */
                String title = data.getStringExtra(getString(R.string.titleKey));
                String details = data.getStringExtra(getString(R.string.detailsKey));

                JSONObject innerParams = new JSONObject();
                JSONObject params = new JSONObject();
                try {
                    innerParams.put("title", title);
                    innerParams.put("details", details);
                    params.put("note", innerParams);
                    params.put(getString(R.string.unique_id), PrefsHelper.getPref(MainActivity.this, getString(R.string.user_id)));
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                NetworkManager networkManager = NetworkManager.getInstance();
                networkManager.executePostRequest(MainActivity.this, NetworkManager.API_HOST + "/notes.json", params, new NetworkCallback() {
                    @Override
                    public void onSuccess(Object json) {
                        MainActivity.this.notesListFragment.createGridView();
                        Toast.makeText(MainActivity.this, getString(R.string.noteSaved), Toast.LENGTH_SHORT).show();
                        AnalyticsManager.getInstance().fireEvent("new note created successfully", null);
                    }

                    @Override
                    public void onFailure(int statusCode) {
                        Toast.makeText(MainActivity.this, "Error: Note Not Saved. Please Try Again.", Toast.LENGTH_LONG).show();
                        HashMap map = new HashMap<String, String>();
                        map.put("status_code", Integer.toString(statusCode));
                        AnalyticsManager.getInstance().fireEvent("error saving new note to API", map);
                    }
                });
            }
        }
    }

    /**
     * Shows the loading spinner dialog
     * @return ProgressDialog the progress dialog that will be displayed while loading notes from the API
     */
    private ProgressDialog showLoadingDialog() {
        ProgressDialog dialog = new ProgressDialog(MainActivity.this);
        dialog.setMessage("Loading Notes...");
        dialog.setCancelable(false);
        dialog.show();
        AnalyticsManager.getInstance().fireEvent("showed loading dialog", null);
        return dialog;
    }
}
