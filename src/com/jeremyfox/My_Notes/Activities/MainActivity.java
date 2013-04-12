package com.jeremyfox.My_Notes.Activities;

import android.app.*;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.*;
import android.widget.*;
import com.jeremyfox.My_Notes.Adapters.NotesAdapter;
import com.jeremyfox.My_Notes.Classes.BasicNote;
import com.jeremyfox.My_Notes.Dialogs.NewNoteDialog;
import com.jeremyfox.My_Notes.Helpers.PrefsHelper;
import com.jeremyfox.My_Notes.Interfaces.NetworkCallback;
import com.jeremyfox.My_Notes.Managers.AnalyticsManager;
import com.jeremyfox.My_Notes.Managers.NetworkManager;
import com.jeremyfox.My_Notes.Managers.NotesManager;
import com.jeremyfox.My_Notes.R;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * The Main activity.
 */
public class MainActivity extends Activity {

    private NotesManager notesManager;
    private ViewFlipper viewFlipper;
    private GridView gridView;
    public static Activity ACTIVITY;
    private static final int DEFAULT_HOME_VIEW = 0;
    private static final int NOTES_VIEW = 1;
    private static final int NEW_NOTE_REQUEST_CODE = 1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        this.ACTIVITY = this;
        this.gridView = (GridView)findViewById(R.id.gridview);
        this.viewFlipper = (ViewFlipper)findViewById(R.id.ViewFlipper);

        String user_id = PrefsHelper.getPref(this, getString(R.string.user_id));
        if (null == user_id || user_id.length() == 0) {
            AnalyticsManager.getInstance().fireEvent("new user", null);
            registerWIthAPI();
        } else {
            AnalyticsManager.getInstance().fireEvent("returning user", null);
            createGridView();
        }
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

        createGridView();
    }

    @Override
    public void onDestroy() {
        AnalyticsManager.getInstance().fireEvent("application shutdown", null);
        AnalyticsManager.getInstance().flushEvents();
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menu) {
        switch (menu.getItemId()) {
            case R.id.new_note:
                NotesAdapter notesAdapter = (NotesAdapter)this.gridView.getAdapter();
                if (null != notesAdapter) notesAdapter.setShouldIncrementCounter(true);
                AnalyticsManager.getInstance().fireEvent("selected new note option", null);
                Intent newNoteIntent = new Intent(this, NewNoteActivity.class);
                startActivityForResult(newNoteIntent, NEW_NOTE_REQUEST_CODE);
                break;

            case R.id.sync_notes:
                AnalyticsManager.getInstance().fireEvent("selected sync notes option", null);
                createGridView();
                break;
        }
        return true;
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
                 * Save the new note then update the list view
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
                        createGridView();
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
     * Used to register the device with the API
     */
    private void registerWIthAPI() {
        NetworkManager networkManager = NetworkManager.getInstance();
        networkManager.executePostRequest(MainActivity.this, NetworkManager.API_HOST + "/users.json", null, new NetworkCallback() {
            @Override
            public void onSuccess(Object json) {
                try {
                    String unique_id = ((JSONObject)json).getString(getString(R.string.unique_id));
                    PrefsHelper.setPref(getBaseContext(), getString(R.string.user_id), unique_id);
                    AnalyticsManager.getInstance().registerSuperProperty("user API key", unique_id);
                    createGridView();
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
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Sorry!")
                        .setMessage("We were unable to register you with the API at this time. Please try again later by simply relaunching the application.")
                        .setNegativeButton("Ok", null)
                        .create()
                        .show();

            }
        });
    }

    /**
     * Creates and displays the grid view of notes
     */
    private void createGridView() {
        final ProgressDialog dialog = showLoadingDialog();

        if (null == this.notesManager) {
            this.notesManager = NotesManager.getInstance();
        }

        this.notesManager.retrieveNotesFromAPI(MainActivity.this, new NetworkCallback() {
            @Override
            public void onSuccess(Object json) {
                final GridView grid = MainActivity.this.gridView;
                grid.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
                grid.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {

                    @Override
                    public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
                        NotesAdapter notesAdapter = (NotesAdapter)grid.getAdapter();
                        int count = grid.getCheckedItemCount();
                        if (count > 0) {
                            notesAdapter.setShouldIncrementCounter(false);
                        } else {
                            notesAdapter.setShouldIncrementCounter(true);
                        }
                        mode.setTitle(count + " selected");
                        BasicNote note = (BasicNote)grid.getItemAtPosition(position);
                        note.setSelected(checked);
                        grid.invalidateViews();
                    }

                    @Override
                    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.trash:
                                int numNotesSelectedForDelete = deleteSelectedNotes();
                                HashMap deletedmap = new HashMap<String, String>();
                                deletedmap.put("selected for delete", Integer.toString(numNotesSelectedForDelete));
                                AnalyticsManager.getInstance().fireEvent("deleted notes", deletedmap);
                                mode.finish();
                                return true;
                            case R.id.edit:
                                int numNotesSelectedForEdit = editSelectedNotes();
                                HashMap editedMap = new HashMap<String, String>();
                                editedMap.put("selected for edit", Integer.toString(numNotesSelectedForEdit));
                                AnalyticsManager.getInstance().fireEvent("edited notes", editedMap);
                                return true;
                            default:
                                Log.d("check", "mark");
                                return false;
                        }
                    }

                    @Override
                    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                        MenuInflater inflater = mode.getMenuInflater();
                        inflater.inflate(R.menu.context_menu, menu);
                        return true;
                    }

                    @Override
                    public void onDestroyActionMode(ActionMode mode) {
                    }

                    @Override
                    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                        return false;
                    }
                });

                setGridViewItems();

                grid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        JSONArray notes = MainActivity.this.notesManager.getNotes();
                        BasicNote note = null;
                        try {
                            note = (BasicNote)notes.get(position);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        Intent noteDetailsIntent = new Intent(MainActivity.this, NoteDetailsActivity.class);
                        noteDetailsIntent.putExtra("title", note.getTitle());
                        noteDetailsIntent.putExtra("details", note.getDetails());
                        noteDetailsIntent.putExtra("id", note.getRecordID());
                        startActivity(noteDetailsIntent, null);
                        AnalyticsManager.getInstance().fireEvent("opened a note", null);
                    }
                });

                dialog.dismiss();
            }

            @Override
            public void onFailure(int statusCode) {
                dialog.dismiss();

                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Error")
                        .setMessage("Couldn't load your notes. Please check your network connection and try again.")
                        .setNegativeButton("Ok", null)
                        .create()
                        .show();

            }
        });
    }

    /**
     * Updates the gird view
     */
    private void setGridViewItems() {
        JSONArray jsonArray = this.notesManager.getNotes();
        if (jsonArray.length() > 0) {
            ArrayList<BasicNote> notes = new ArrayList<BasicNote>(jsonArray.length());
            for (int i=0; i<jsonArray.length(); i++) {
                try {
                    BasicNote note = (BasicNote)jsonArray.get(i);
                    notes.add(note);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            NotesAdapter notesAdapter = new NotesAdapter(MainActivity.this, R.id.title, notes);
            this.gridView.setAdapter(notesAdapter);
            this.viewFlipper.setDisplayedChild(NOTES_VIEW);
            AnalyticsManager.getInstance().fireEvent("showed notes view", null);
        } else {
            NotesAdapter notesAdapter = new NotesAdapter(MainActivity.this, R.id.title, new ArrayList<BasicNote>());
            this.gridView.setAdapter(notesAdapter);
            this.viewFlipper.setDisplayedChild(DEFAULT_HOME_VIEW);
            AnalyticsManager.getInstance().fireEvent("showed default home view", null);
        }

        this.gridView.invalidateViews();
    }

    /**
     * Deletes the selected notes from the API
     * @return int the total number of notes that were deleted
     */
    private int deleteSelectedNotes() {
        int count = 0;
        NotesAdapter notesAdapter = (NotesAdapter)this.gridView.getAdapter();
        notesAdapter.setShouldIncrementCounter(true);
        SparseBooleanArray checked = this.gridView.getCheckedItemPositions();
        for (int i = 0; i < this.gridView.getCount(); i++) {
            if (checked.get(i)) {
                count++;
                final BasicNote note = (BasicNote)this.gridView.getItemAtPosition(i);
                this.notesManager.deleteNote(MainActivity.this, note, new NetworkCallback() {
                    @Override
                    public void onSuccess(Object json) {
                        MainActivity.this.notesManager.removeNote(note);
                        setGridViewItems();
                        Toast.makeText(MainActivity.this, "Selected Notes Deleted", Toast.LENGTH_SHORT).show();
                        AnalyticsManager.getInstance().fireEvent("successfully deleted notes from API", null);
                    }

                    @Override
                    public void onFailure(int statusCode) {
                        Toast.makeText(MainActivity.this, "ERROR: Selected Notes Not Deleted", Toast.LENGTH_SHORT).show();
                        HashMap map = new HashMap<String, String>();
                        map.put("status_code", Integer.toString(statusCode));
                        AnalyticsManager.getInstance().fireEvent("error deleting notes from API", map);
                    }
                });
            }
        }
        return count;
    }

    /**
     * Edits the selected notes
     * @return int the total number of notes that were edited
     */
    private int editSelectedNotes() {
        int count = 0;
        SparseBooleanArray checked = this.gridView.getCheckedItemPositions();
        for (int i = 0; i < this.gridView.getCount(); i++) {
            if (checked.get(i)) {
                count++;
                final BasicNote note = (BasicNote)this.gridView.getItemAtPosition(i);
                this.notesManager.editNote(this, note, new NetworkCallback() {
                    @Override
                    public void onSuccess(Object json) {
                        createGridView();
                        Toast.makeText(MainActivity.this, getString(R.string.noteSaved), Toast.LENGTH_SHORT).show();
                        AnalyticsManager.getInstance().fireEvent("successfully edited notes from API", null);
                    }

                    @Override
                    public void onFailure(int statusCode) {
                        Toast.makeText(MainActivity.this, "Error: Note Not Saved. Please Check Your Network Connection and Try Again.", Toast.LENGTH_LONG).show();
                        HashMap map = new HashMap<String, String>();
                        map.put("status_code", Integer.toString(statusCode));
                        AnalyticsManager.getInstance().fireEvent("error editing notes from API", map);
                    }
                });
            }
        }

        return count;
    }

    /**
     * Shows the loading spinner dialog
     * @return ProgressDialog the progress dialog that will be displayed while loading notes from the API
     */
    private ProgressDialog showLoadingDialog() {
        ProgressDialog dialog = new ProgressDialog(this);
        dialog.setMessage("Loading Notes...");
        dialog.setCancelable(false);
        dialog.show();
        AnalyticsManager.getInstance().fireEvent("showed loading dialog", null);
        return dialog;
    }

}
