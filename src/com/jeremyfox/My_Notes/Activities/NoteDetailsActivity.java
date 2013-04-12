package com.jeremyfox.My_Notes.Activities;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
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
public class NoteDetailsActivity extends Activity {

    private NotesManager notesManager;
    private String title;
    private String details;
    private int recordID;
    private TextView noteTitle;
    private TextView noteDetails;
    private Button dismissNoteButton;
    private Button editNoteButton;
    private Dialog dialog;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.note_details);

        getActionBar().setDisplayHomeAsUpEnabled(true);

        this.notesManager = NotesManager.getInstance();

        Bundle extras = this.getIntent().getExtras();
        this.title = extras.getString(getString(R.string.titleKey));
        this.details = extras.getString(getString(R.string.detailsKey));
        this.recordID = extras.getInt(getString(R.string.idKey));

        this.noteTitle = (TextView)findViewById(R.id.note_title);
        this.noteDetails = (TextView)findViewById(R.id.note_details);
        this.dismissNoteButton = (Button)findViewById(R.id.dismiss_note_button);
        this.editNoteButton = (Button)findViewById(R.id.edit_note_button);

        Typeface typeface = Typeface.createFromAsset(getAssets(), "fonts/Dakota-Regular.ttf");
        this.dismissNoteButton.setTypeface(typeface);
        this.editNoteButton.setTypeface(typeface);
        this.noteTitle.setTypeface(typeface);
        this.noteDetails.setTypeface(typeface);

        this.dismissNoteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AnalyticsManager.getInstance().fireEvent("dismiss note", null);
                finish();
            }
        });
        this.editNoteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AnalyticsManager.getInstance().fireEvent("edit note from note details view", null);
                editNote();
            }
        });

        this.noteTitle.setText(this.title);
        this.noteDetails.setText(this.details);
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
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, this.title);
                shareIntent.putExtra(Intent.EXTRA_TITLE, this.title);
                shareIntent.putExtra(Intent.EXTRA_TEXT, this.details);
                startActivity(Intent.createChooser(shareIntent, "Share via"));
                break;

            case R.id.note_details_trash:
                AnalyticsManager.getInstance().fireEvent("delete note from note details view", null);
                this.dialog = showLoadingDialog();
                deleteNote();
                break;

            case android.R.id.home:
                finish();
                return true;
        }
        return true;
    }

    private void editNote() {
        Note note = this.notesManager.getNote(this.recordID);
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
                        NoteDetailsActivity.this.title = newTitle;
                        NoteDetailsActivity.this.details = newDetails;
                        NoteDetailsActivity.this.noteTitle.setText(newTitle);
                        NoteDetailsActivity.this.noteDetails.setText(newDetails);
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

    private void deleteNote() {
        Note note = this.notesManager.getNote(this.recordID);
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

    private ProgressDialog showLoadingDialog() {
        ProgressDialog dialog = new ProgressDialog(this);
        dialog.setMessage(getString(R.string.deleting_note));
        dialog.setCancelable(false);
        dialog.show();
        AnalyticsManager.getInstance().fireEvent("showed deleting note dialog", null);
        return dialog;
    }

}