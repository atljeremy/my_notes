package com.jeremyfox.My_Notes.Activities;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.jeremyfox.My_Notes.Classes.MyNotesAPIResultReceiver;
import com.jeremyfox.My_Notes.Classes.ResponseObject;
import com.jeremyfox.My_Notes.Dialogs.NewNoteDialog;
import com.jeremyfox.My_Notes.Fragments.NoteDetailsFragment;
import com.jeremyfox.My_Notes.Helpers.DataBaseHelper;
import com.jeremyfox.My_Notes.Interfaces.Note;
import com.jeremyfox.My_Notes.Interfaces.User;
import com.jeremyfox.My_Notes.Managers.AnalyticsManager;
import com.jeremyfox.My_Notes.Managers.NotesManager;
import com.jeremyfox.My_Notes.R;
import com.jeremyfox.My_Notes.Services.MyNotesAPIService;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: jeremy
 * Date: 4/9/13
 * Time: 5:01 PM
 */
public class NoteDetailsActivity extends Activity implements NoteDetailsFragment.NoteDetailsListener, MyNotesAPIResultReceiver.Receiver {

    private NoteDetailsFragment noteDetailsFragment;
    private Note note;
    private MyNotesAPIResultReceiver receiver;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.note_details_fragment);
        Bundle extras = getIntent().getExtras();

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // If the screen is now in landscape mode, show the
            // details in-line with the list so we don't need this activity.
            finish();
            return;
        }

        if (savedInstanceState == null) {
            // During initial setup, plug in the details fragment.
            NoteDetailsFragment details = new NoteDetailsFragment();
            details.setArguments(extras);
            getFragmentManager().beginTransaction().add(android.R.id.content, details).commit();
        }

        getActionBar().setDisplayHomeAsUpEnabled(true);

        this.noteDetailsFragment = (NoteDetailsFragment)getFragmentManager().findFragmentById(R.id.note_details_fragment);
    }

    @Override
    public void editNote(final Note note, final TextView title, final TextView details) {
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
                        Toast.makeText(NoteDetailsActivity.this, getString(R.string.allFeildsRequired), Toast.LENGTH_SHORT).show();
                    } else {
                        final String title = titleInput.getText().toString();
                        final String details = detailsInput.getText().toString();
                        updateNoteToAPI(note.getId(), note.getAPINoteId(), title, details);
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
    public void setNote(Note note) {
        this.note = note;
    }

    @Override
    public void deleteNote(Note note) {
        AnalyticsManager.fireEvent(this, "delete note from note details view", null);
        ArrayList<Note> notesArray = new ArrayList<Note>();
        notesArray.add(note);
        deleteNotes(notesArray);
    }

    @Override
    public void dismissNote() {
        finish();
    }

    /**
     * MyNotesAPIService DELETE notes request
     * @param notesArray
     */
    public void deleteNotes(ArrayList<Note> notesArray) {
        if (this.receiver == null) {
            this.receiver = new MyNotesAPIResultReceiver(new Handler());
            this.receiver.setReceiver(this);
        }
        final Intent intent = new Intent(Intent.ACTION_SYNC, null, this, MyNotesAPIService.class);
        DataBaseHelper db = new DataBaseHelper(this);
        intent.putExtra(User.API_TOKEN_KEY, db.getCurrentUser().getApiToken());
        intent.putParcelableArrayListExtra("notesArray", notesArray);
        intent.putExtra(MyNotesAPIService.RECEIVER_KEY, this.receiver);
        intent.putExtra(MyNotesAPIService.ACTION_KEY, MyNotesAPIService.DELETE_NOTES);
        startService(intent);
    }

    /**
     * MyNotesAPIService PUT edited note request
     */
    public void updateNoteToAPI(int id, int apiId, String title, String details) {
        if (this.receiver == null) {
            this.receiver = new MyNotesAPIResultReceiver(new Handler());
            this.receiver.setReceiver(this);
        }
        final Intent intent = new Intent(Intent.ACTION_SYNC, null, this, MyNotesAPIService.class);
        DataBaseHelper db = new DataBaseHelper(this);
        intent.putExtra(User.API_TOKEN_KEY, db.getCurrentUser().getApiToken());
        intent.putExtra(Note.TITLE_KEY, title);
        intent.putExtra(Note.DETAILS_KEY, details);
        intent.putExtra(Note.ID_KEY, id);
        intent.putExtra(Note.API_ID_KEY, apiId);
        intent.putExtra(MyNotesAPIService.RECEIVER_KEY, this.receiver);
        intent.putExtra(MyNotesAPIService.ACTION_KEY, MyNotesAPIService.EDIT_NOTES);
        startService(intent);
    }

    @Override
    public void onReceiveResult(int resultCode, Bundle resultData) {
        ResponseObject responseObject;
        int action = resultData.getInt(MyNotesAPIService.ACTION_KEY);
        switch (resultCode) {
            case MyNotesAPIService.STATUS_RUNNING:
                Log.d("MainActivity", "STATUS_RUNNING");
                break;

            case MyNotesAPIService.STATUS_FINISHED:
                responseObject = (ResponseObject)resultData.getSerializable(MyNotesAPIService.RESULT_KEY);
                switch (action) {
                    case MyNotesAPIService.GET_NOTES:
                        break;

                    case MyNotesAPIService.SAVE_NOTE:
                        break;

                    case MyNotesAPIService.EDIT_NOTES:
                        NoteDetailsActivity.this.noteDetailsFragment.dismissDialog();
                        if (responseObject.getStatus() == ResponseObject.RequestStatus.STATUS_SUCCESS) {
                            if (responseObject.getObject() instanceof JSONObject) {
                                JSONObject jsonObject = (JSONObject)responseObject.getObject();
                                String title = null;
                                String details = null;
                                int id = -1;
                                try {
                                    title = jsonObject.getString(Note.TITLE_KEY);
                                    details = jsonObject.getString(Note.DETAILS_KEY);
                                    id = jsonObject.getInt(Note.ID_KEY);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }

                                if (null != title && null != details && -1 != id) {
                                    String[] columns = new String[] { DataBaseHelper.NOTE_TITLE, DataBaseHelper.NOTE_DETAILS };
                                    String[] values = new String[] { title, details };
                                    NotesManager.getInstance().updateNote(NoteDetailsActivity.this, id, columns, values);
                                    NoteDetailsActivity.this.noteDetailsFragment.updateCurrentNote(title, details);
                                    dismissNote();
                                }

                                Toast.makeText(NoteDetailsActivity.this, getString(R.string.notesUpdated), Toast.LENGTH_SHORT).show();
                                AnalyticsManager.fireEvent(this, "note updated successfully", null);
                            }
                        } else {
                            NoteDetailsActivity.this.noteDetailsFragment.showLoadingError();
                            AnalyticsManager.fireEvent(this, "error updating note to API", null);
                        }
                        break;

                    case MyNotesAPIService.DELETE_NOTES:
                        NoteDetailsActivity.this.noteDetailsFragment.dismissDialog();
                        ArrayList<Note> notesArray = resultData.getParcelableArrayList("notesArray");
                        ArrayList<Note> notDeletedNotes = resultData.getParcelableArrayList("notDeletedNotesArray");
                        boolean allNotesDeleted = true;

                        if (notesArray != null && notesArray.size() > 0) {
                            for (Note note : notesArray) {
                                NotesManager.getInstance().removeNote(this, note);
                            }
                        }

                        if (notDeletedNotes != null && notDeletedNotes.size() > 0) {
                            allNotesDeleted = false;
                        }

                        if (allNotesDeleted) {
                            dismissNote();
                            AnalyticsManager.fireEvent(this, "successfully deleted note from API", null);
                        } else {
                            Toast.makeText(NoteDetailsActivity.this, getString(R.string.error_deleting_notes), Toast.LENGTH_LONG).show();
                            AnalyticsManager.fireEvent(this, "error deleting note(s) from API", null);
                        }
                        break;
                }
                Log.d("MainActivity", "STATUS_FINISHED");
                break;

            case MyNotesAPIService.STATUS_ERROR:
                Log.d("MainActivity", "STATUS_ERROR");
                NoteDetailsActivity.this.noteDetailsFragment.showLoadingError();
                break;

            default:
                Log.d("default", "default");
                break;
        }
    }
}