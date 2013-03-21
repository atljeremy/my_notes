package com.jeremyfox.My_Notes.Activities;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.*;
import android.widget.*;
import com.jeremyfox.My_Notes.Adapters.NotesAdapter;
import com.jeremyfox.My_Notes.Classes.BasicNote;
import com.jeremyfox.My_Notes.Dialogs.NewNoteDialog;
import com.jeremyfox.My_Notes.Dialogs.NoteDetailsDialog;
import com.jeremyfox.My_Notes.Helpers.PrefsHelper;
import com.jeremyfox.My_Notes.Interfaces.NetworkCallback;
import com.jeremyfox.My_Notes.Managers.NetworkManager;
import com.jeremyfox.My_Notes.Managers.NotesManager;
import com.jeremyfox.My_Notes.R;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * The type Main activity.
 */
public class MainActivity extends ListActivity {

    private NotesManager notesManager;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        String user_id = PrefsHelper.getPref(this, getString(R.string.user_id));
        if (null == user_id || user_id.length() == 0) {
            registerWIthAPI();
        } else {
            createListView();
        }
    }

    private void registerWIthAPI() {
        if (NetworkManager.isConnected(this)) {
            NetworkManager networkManager = NetworkManager.getInstance();
            networkManager.executePostRequest(NetworkManager.API_HOST + "/users.json", null, new NetworkCallback() {
                @Override
                public void onSuccess(JSONObject json) {
                    Log.d("MainActivity", "onSuccess");
                    try {
                        String unique_id = json.getString(getString(R.string.unique_id));
                        PrefsHelper.setPref(getBaseContext(), getString(R.string.user_id), unique_id);
                        createListView();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(int statusCode) {
                    Log.d("MainActivity", "onFailure");
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setTitle("Sorry!")
                            .setMessage("We were unable to register you with the API at this time. Please try again later by simply relaunching the application.")
                            .setNegativeButton("Ok", null)
                            .create()
                            .show();

                }
            });
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle("Offline")
                    .setMessage("You are currently not connected to the internet. Please connect to a network with internet access and relaunch the application.")
                    .setNegativeButton("Ok", null)
                    .create()
                    .show();
        }
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

                final EditText titleInput = new EditText(this);
                titleInput.setHint(getString(R.string.titleInputHint));
                final EditText detailsInput = new EditText(this);
                detailsInput.setHint(getString(R.string.detailsInputHint));

                NewNoteDialog newNoteDialog = new NewNoteDialog(this, titleInput, detailsInput, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                        boolean titleEmpty = titleInput.getText().toString().length() == 0;
                        boolean detailsEmpty = detailsInput.getText().toString().length() == 0;
                        if (titleEmpty || detailsEmpty) {
                            Toast.makeText(MainActivity.this, getString(R.string.allFeildsRequired), Toast.LENGTH_SHORT).show();
                        } else {
                            /**
                             * Save the new note then update the list view
                             */
                            String title = titleInput.getText().toString();
                            String details = detailsInput.getText().toString();

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
                            networkManager.executePostRequest(NetworkManager.API_HOST + "/notes.json", params, new NetworkCallback() {
                                @Override
                                public void onSuccess(JSONObject json) {
                                    createListView();
                                    Toast.makeText(MainActivity.this, getString(R.string.noteSaved), Toast.LENGTH_SHORT);
                                }

                                @Override
                                public void onFailure(int statusCode) {
                                    Toast.makeText(MainActivity.this, "Error: Note Not Saved. Please Try Again.", Toast.LENGTH_LONG);
                                }
                            });
                        }
                    }
                });
                newNoteDialog.showDialog();
                break;
        }
        return true;
    }

    /**
     * Sets up the listView
     */
    private void createListView() {
        AlertDialog dialog = showLoadingDialog();

        if (null == this.notesManager) {
            this.notesManager = NotesManager.getInstance();
        }
        this.notesManager.retrieveNotesFromAPI(this);

        dialog.dismiss();

        final ListView listView = getListView();
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        listView.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {

            @Override
            public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
                int count = listView.getCheckedItemCount();
                mode.setTitle(count + " selected");
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.trash:
                        deleteSelectedNotes();
                        mode.finish();
                        return true;
                    default:
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

        setListViewItems();

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                JSONArray notes = MainActivity.this.notesManager.getNotes();
                BasicNote note = null;
                try {
                    note = (BasicNote)notes.get(position);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                new NoteDetailsDialog(MainActivity.this, note.getTitle(), note.getDetails()).showDialog();
            }
        });
    }

    private void setListViewItems() {
        ListView listView = getListView();
        JSONArray jsonArray = this.notesManager.getNotes();
        if (jsonArray.length() > 0) {
            listView.setLayoutParams(getLayoutParams());
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
            setListAdapter(notesAdapter);
        } else {
            NotesAdapter notesAdapter = new NotesAdapter(MainActivity.this, R.id.title, new ArrayList<BasicNote>());
            setListAdapter(notesAdapter);
        }

        listView.invalidateViews();
    }

    private void deleteSelectedNotes() {
        ListView listView = getListView();
        SparseBooleanArray checked = listView.getCheckedItemPositions();
        for (int i = 0; i < listView.getCount(); i++) {
            if (checked.get(i)) {
                final BasicNote note = (BasicNote)listView.getItemAtPosition(i);
                NetworkManager networkManager = NetworkManager.getInstance();
                String url = NetworkManager.API_HOST+"/notes/"+note.getRecordId()+".json?unique_id="+PrefsHelper.getPref(this, getString(R.string.user_id));
                networkManager.executeDeleteRequest(url, null, new NetworkCallback() {
                    @Override
                    public void onSuccess(JSONObject json) {
                        MainActivity.this.notesManager.removeNote(note);
                        setListViewItems();
                        Toast.makeText(MainActivity.this, "Selected Notes Deleted", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(int statusCode) {
                        Toast.makeText(MainActivity.this, "Selected Notes Deleted", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
    }

    private AlertDialog showLoadingDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Loading Notes...")
                .setNegativeButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                });
        AlertDialog dialog = builder.create();
        dialog.show();
        return dialog;
    }

    /**
     * Used in the custom view creation methods to set the layout params
     * @return LayoutParams
     */
    private LinearLayout.LayoutParams getLayoutParams() {
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        return new LinearLayout.LayoutParams(layoutParams);
    }
}
