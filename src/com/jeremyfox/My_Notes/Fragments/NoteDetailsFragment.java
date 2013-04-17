package com.jeremyfox.My_Notes.Fragments;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import com.jeremyfox.My_Notes.Interfaces.Note;
import com.jeremyfox.My_Notes.Managers.AnalyticsManager;
import com.jeremyfox.My_Notes.Managers.NotesManager;
import com.jeremyfox.My_Notes.R;
import org.json.JSONArray;
import org.json.JSONException;

/**
 * Created with IntelliJ IDEA.
 * User: jeremy
 * Date: 4/14/13
 * Time: 12:55 PM
 */
public class NoteDetailsFragment extends Fragment {

    public interface NoteDetailsListener {
        public void dismissNote();
        public void deleteNote(int recordID);
        public void editNote(int recordID, TextView title, TextView details);
        public void shareNote(Intent shareIntent);
        public void setNote(Note note);
    }

    private NoteDetailsListener listener;
    private Button dismissNoteButton;
    private Button editNoteButton;
    private TextView noteTitle;
    private TextView noteDetails;
    private String title;
    private String details;
    private int recordID;

    public static NoteDetailsFragment newInstance(int index) {
        NoteDetailsFragment f = new NoteDetailsFragment();
        Bundle args = new Bundle();
        args.putInt("index", index);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        setNoteDetails(args);
    }

    @Override
    public View onCreateView(LayoutInflater inflator, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflator, container, savedInstanceState);

        ScrollView scrollView = (ScrollView) inflator.inflate(R.layout.note_details, container, false);

        this.noteTitle = (TextView)scrollView.findViewById(R.id.note_title);
        this.noteDetails = (TextView)scrollView.findViewById(R.id.note_details);
        this.dismissNoteButton = (Button)scrollView.findViewById(R.id.dismiss_note_button);
        this.editNoteButton = (Button)scrollView.findViewById(R.id.edit_note_button);

        Typeface typeface = Typeface.createFromAsset(getActivity().getAssets(), "fonts/Dakota-Regular.ttf");
        this.noteTitle.setTypeface(typeface);
        this.noteDetails.setTypeface(typeface);
        this.dismissNoteButton.setTypeface(typeface);
        this.editNoteButton.setTypeface(typeface);

        this.noteTitle.setText(this.title);
        this.noteDetails.setText(this.details);

        this.dismissNoteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AnalyticsManager.getInstance().fireEvent("dismiss note", null);
                listener.dismissNote();
            }
        });
        this.editNoteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AnalyticsManager.getInstance().fireEvent("edit note from note details view", null);
                listener.editNote(NoteDetailsFragment.this.recordID, NoteDetailsFragment.this.noteTitle, NoteDetailsFragment.this.noteDetails);
            }
        });

        return scrollView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            listener = (NoteDetailsListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement NoteDetailsListener");
        }
    }

    public int getShownIndex() {
        int shownIndex;
        Bundle args = getArguments();
        if (null != args) {
            shownIndex = args.getInt("index", 0);
        } else {
            shownIndex = 0;
        }
        return shownIndex;
    }

    public void setNoteDetails(Bundle args) {
        Note note = NotesManager.getInstance().getFirstNote();
        listener.setNote(note);
        if (null != args) {
            int index = args.getInt("index", 0);
            try {
                JSONArray notes = NotesManager.getInstance().getNotes();
                if (null != notes && notes.length() > 0 && notes.length() > index) {
                    note = (Note) notes.get(index);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        if (null != note) {
            this.title = note.getTitle();
            this.details = note.getDetails();
            this.recordID = note.getRecordID();
        }
    }

}
