package com.jeremyfox.My_Notes.Activities;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.jeremyfox.My_Notes.Fragments.NoteDetailsFragment;
import com.jeremyfox.My_Notes.Interfaces.NetworkCallback;
import com.jeremyfox.My_Notes.Interfaces.Note;
import com.jeremyfox.My_Notes.Managers.AnalyticsManager;
import com.jeremyfox.My_Notes.Managers.NotesManager;
import com.jeremyfox.My_Notes.R;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.NoRouteToHostException;

/**
 * Created with IntelliJ IDEA.
 * User: jeremy
 * Date: 4/9/13
 * Time: 5:01 PM
 */
public class NoteDetailsActivity extends Activity implements NoteDetailsFragment.NoteDetailsListener {

    private NotesManager notesManager;
    private Dialog dialog;
    private Note note;

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

        this.notesManager = NotesManager.getInstance();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.note_details_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menu) {
        switch (menu.getItemId()) {
            case R.id.share_note:
                AnalyticsManager.getInstance().fireEvent("share note from note details view", null);
                Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, this.note.getTitle());
                shareIntent.putExtra(Intent.EXTRA_TITLE, this.note.getTitle());
                shareIntent.putExtra(Intent.EXTRA_TEXT, this.note.getDetails());
                startActivity(Intent.createChooser(shareIntent, "Share via"));
                break;

            case R.id.note_details_trash:
                AnalyticsManager.getInstance().fireEvent("delete note from note details view", null);
                this.dialog = showLoadingDialog();
                deleteNote(this.note.getRecordID());
                break;

            case android.R.id.home:
                finish();
                return true;
        }
        return true;
    }

    @Override
    public void editNote(int recordID, final TextView title, final TextView details) {
        Note note = this.notesManager.getNote(recordID);
        if (null != note) {
            this.notesManager.editNote(this, note, new NetworkCallback() {
                @Override
                public void onSuccess(Object json) {
                    String newTitle = null;
                    String newDetails = null;
                    try {
                        newTitle = ((JSONObject)json).getString(getString(R.string.titleKey));
                        newDetails = ((JSONObject)json).getString(getString(R.string.detailsKey));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    if (null != newTitle && null != newDetails) {
                        title.setText(newTitle);
                        details.setText(newDetails);
                    }
                }

                @Override
                public void onFailure(int statusCode) {
                    Toast.makeText(NoteDetailsActivity.this, getString(R.string.error_saving_note), Toast.LENGTH_LONG);
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
        this.dialog = showLoadingDialog();
        Note note = this.notesManager.getNote(recordID);
        this.notesManager.deleteNote(this, note, new NetworkCallback() {
            @Override
            public void onSuccess(Object json) {
                NoteDetailsActivity.this.dialog.dismiss();
                finish();
            }

            @Override
            public void onFailure(int statusCode) {
                NoteDetailsActivity.this.dialog.dismiss();
                Toast.makeText(NoteDetailsActivity.this, getString(R.string.error_deleting_note), Toast.LENGTH_LONG);
            }
        });
    }

    @Override
    public void dismissNote() {
        finish();
    }

    private ProgressDialog showLoadingDialog() {
        ProgressDialog dialog = new ProgressDialog(this);
        dialog.setMessage(getString(R.string.deleting_note));
        dialog.setCancelable(false);
        dialog.show();
        AnalyticsManager.getInstance().fireEvent("showed deleting note dialog", null);
        return dialog;
    }

}