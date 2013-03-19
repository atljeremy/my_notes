package com.jeremyfox.My_Notes.Activities;

import android.app.ListActivity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.*;
import android.widget.*;
import com.jeremyfox.My_Notes.Classes.BasicNote;
import com.jeremyfox.My_Notes.Dialogs.NewNoteDialog;
import com.jeremyfox.My_Notes.Dialogs.NoteDetailsDialog;
import com.jeremyfox.My_Notes.Managers.NotesManager;
import com.jeremyfox.My_Notes.R;
import org.json.JSONArray;
import org.json.JSONException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

public class MainActivity extends ListActivity {

    private NotesManager notesManager;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        createListView();
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
                            JSONArray notes = MainActivity.this.notesManager.getNotes();
                            String dateString = DateFormat.format(getString(R.string.dateFormat), new Date()).toString();
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append(dateString);
                            stringBuilder.append(" - ");
                            stringBuilder.append(titleInput.getText().toString());
                            notes.put(new BasicNote(stringBuilder.toString(), detailsInput.getText().toString()));

                            createListView();
                            Toast.makeText(MainActivity.this, getString(R.string.noteSaved), Toast.LENGTH_SHORT);
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
        if (null == this.notesManager) {
            this.notesManager = new NotesManager(getApplicationContext());
        }

        if (null != this.notesManager){

            ListView listView = getListView();
            JSONArray jsonArray = this.notesManager.getNotes();
            if (jsonArray.length() > 0) {
                listView.setLayoutParams(getLayoutParams());
                ArrayList<String> titles = new ArrayList<String>(jsonArray.length());
                for (int i=0; i<jsonArray.length(); i++) {
                    BasicNote note;
                    try {
                        note = (BasicNote)jsonArray.get(i);
                        titles.add(note.getTitle());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                setListAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, titles));
            } else {

            }

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
