package com.jeremyfox.My_Notes.Fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.util.SparseBooleanArray;
import android.view.*;
import android.widget.*;
import com.jeremyfox.My_Notes.Adapters.NotesAdapter;
import com.jeremyfox.My_Notes.Helpers.DataBaseHelper;
import com.jeremyfox.My_Notes.Helpers.PrefsHelper;
import com.jeremyfox.My_Notes.Interfaces.User;
import com.jeremyfox.My_Notes.Managers.NotesManager;
import com.jeremyfox.My_Notes.Models.BasicNote;
import com.jeremyfox.My_Notes.Interfaces.NetworkCallback;
import com.jeremyfox.My_Notes.Interfaces.Note;
import com.jeremyfox.My_Notes.Managers.AnalyticsManager;
import com.jeremyfox.My_Notes.Models.BasicUser;
import com.jeremyfox.My_Notes.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: jeremy
 * Date: 4/14/13
 * Time: 12:47 PM
 */
public class NotesListFragment extends Fragment {

    /**
     * The interface Notes list listener.
     */
    public interface NotesListListener {
        /**
         * Show note details.
         *
         * @param note the Note to display
         * @param dualMode the dual mode
         */
        public void showNoteDetails(Note note, int index, boolean dualMode);

        /**
         * New note action.
         */
        public void newNoteAction();

        /**
         * Register with aPI.
         *
         * @param callback the callback
         */
        public void registerWithAPI(NetworkCallback callback);

        /**
         * Request notes from aPI.
         */
        public void requestNotesFromAPI();

        /**
         * Send all un-synced notes to the API.
         */
        public void sendUnsyncedNotesToAPI();

        /**
         * Delete notes.
         *
         * @param notesArray the notes array
         */
        public void deleteNotes(ArrayList<Note> notesArray);
    }

    private NotesListListener listener;
    private boolean dualMode;
    private int currentNoteId = 0;
    private int currentGridIndex = 0;
    private static final int DEFAULT_HOME_VIEW = 0;
    private static final int NOTES_VIEW = 1;
    private ViewFlipper viewFlipper;
    private GridView gridView;
    private ProgressDialog dialog;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        this.gridView = (GridView)getActivity().findViewById(R.id.gridview);
        this.viewFlipper = (ViewFlipper)getActivity().findViewById(R.id.ViewFlipper);

        View detailsFrame = getActivity().findViewById(R.id.note_details_fragment);
        dualMode = detailsFrame != null && detailsFrame.getVisibility() == View.VISIBLE;

        if (savedInstanceState != null) {
            currentNoteId = savedInstanceState.getInt("curNote", 0);
            currentGridIndex = savedInstanceState.getInt("curGridIndex", 0);
            boolean wasShowingDialog = savedInstanceState.getBoolean("dialogVisibility");
            if (wasShowingDialog) {
                showLoadingDialog();
            }
        }

        if (dualMode) {
            GridView gridView = (GridView)getActivity().findViewById(R.id.gridview);
            gridView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
            Note note = NotesManager.getInstance().getNote(getActivity(), currentNoteId);
            if (note != null) {
                showNoteDetails(note, currentGridIndex);
            }
        }

        DataBaseHelper db = new DataBaseHelper(getActivity());
        BasicUser user = db.getCurrentUser();
        if (user == null || user.getApiToken() == null || user.getApiToken().length() == 0) {

            /**
             * First, check and see if this user was using version 1.0 of the app. '
             * If so, they probably already have an API token/User account.
             */
            String prefsToken = PrefsHelper.getPref(getActivity(), getActivity().getString(R.string.user_id));
            if (prefsToken != null && prefsToken.length() > 0) {
                /**
                 * Is an existing v1.0 user with an existing User account in the API.
                 * Create a new User row in the local db in the Users Table for this user
                 */
                user = new BasicUser();
                user.setApiToken(prefsToken);
                db.addUser(user);
                requestNotes();

            } else {
                AnalyticsManager.fireEvent(getActivity(), "new user", null);
                listener.registerWithAPI(new NetworkCallback() {

                    @Override
                    public void onSuccess(Object json) {
                        requestNotes();
                    }

                    @Override
                    public void onFailure(int statusCode) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                        builder.setTitle("Sorry!")
                                .setMessage("We were unable to register you with the API at this time. Please try again later by simply relaunching the application.")
                                .setNegativeButton("Ok", null)
                                .create()
                                .show();
                    }
                });
            }

        } else {
            AnalyticsManager.fireEvent(getActivity(), "returning user", null);
            requestNotes();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        createGridView();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("curNote", currentNoteId);
        if (dialog != null) {
            outState.putBoolean("dialogVisibility", dialog.isShowing());
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflator, ViewGroup container, Bundle savedInstanceState){
        super.onCreateView(inflator, container, savedInstanceState);

        LinearLayout view = (LinearLayout) inflator.inflate(R.layout.main, container, false);

        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            listener = (NotesListListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement NotesListListener");
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menu) {
        switch (menu.getItemId()) {
            case R.id.new_note:
                listener.newNoteAction();
                break;

            case R.id.sync_notes:
                AnalyticsManager.fireEvent(getActivity(), "selected sync notes option", null);
                showLoadingDialog();
                sendUnsyncedNotesToAPI();
                requestNotes();
                break;
        }
        return true;
    }

    private void showNoteDetails(Note note, int index) {
        currentNoteId = note.getId();
        listener.showNoteDetails(note, index, dualMode);
    }

    /**
     * Creates the notes grid view, retrieves notes from API, then displays the grid view of notes
     */
    public void createGridView() {
        final GridView grid = NotesListFragment.this.gridView;
        if (dualMode) {
            grid.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        } else {
            grid.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        }

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
                        AnalyticsManager.fireEvent(getActivity(), "deleted notes", deletedmap);
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

        setGridViewItems();

        grid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Note note = (Note) grid.getAdapter().getItem(position);
                showNoteDetails(note, position);
            }
        });

        dismissDialog();
    }

    /**
     * Request notes from the API.
     */
    public void requestNotes() {
        if (listener != null) {
            listener.requestNotesFromAPI();
        }
    }

    /**
     * Send all un-synced notes to the API.
     */
    public void sendUnsyncedNotesToAPI() {
        if (listener != null) {
            listener.sendUnsyncedNotesToAPI();
        }
    }

    /**
     * Updates the gird view
     */
    public void setGridViewItems() {
        dismissDialog();
        DataBaseHelper db = new DataBaseHelper(getActivity());
        User user = db.getCurrentUser();
        List<Note> notes = user.getNotes();
        if (user != null && notes != null && notes.size() > 0) {
            NotesAdapter notesAdapter = new NotesAdapter(getActivity(), R.id.title, notes);
            this.gridView.setAdapter(notesAdapter);
            this.viewFlipper.setDisplayedChild(NOTES_VIEW);
            AnalyticsManager.fireEvent(getActivity(), "showed notes view", null);
        } else {
            NotesAdapter notesAdapter = new NotesAdapter(getActivity(), R.id.title, new ArrayList<Note>());
            this.gridView.setAdapter(notesAdapter);
            this.viewFlipper.setDisplayedChild(DEFAULT_HOME_VIEW);
            AnalyticsManager.fireEvent(getActivity(), "showed default home view", null);
        }

        this.gridView.invalidateViews();
    }

    /**
     * Deletes the selected notes from the API
     * @return int the total number of notes that were deleted
     */
    private int deleteSelectedNotes() {
        showDeletingNotesDialog();
        ArrayList<Note> notesArray = new ArrayList<Note>();
        NotesAdapter notesAdapter = (NotesAdapter)this.gridView.getAdapter();
        notesAdapter.setShouldIncrementCounter(true);
        SparseBooleanArray checked = this.gridView.getCheckedItemPositions();
        for (int i = 0; i < this.gridView.getCount(); i++) {
            if (checked.get(i)) {
                final BasicNote note = (BasicNote)this.gridView.getItemAtPosition(i);
                notesArray.add(note);
            }
        }

        if (notesArray.size() > 0) {
            listener.deleteNotes(notesArray);
        }

        return notesArray.size();
    }

    public void dismissDialog() {
        if (null != this.dialog) {
            this.dialog.dismiss();
        }
    }

    /**
     * Shows the loading spinner dialog
     * @return ProgressDialog the progress dialog that will be displayed while loading notes from the API
     */
    private void showLoadingDialog() {
        if (null == this.dialog) this.dialog = new ProgressDialog(getActivity());
        this.dialog.setMessage("Loading Notes...");
        this.dialog.setCancelable(false);
        this.dialog.show();
        AnalyticsManager.fireEvent(getActivity(), "showed loading dialog", null);
    }

    /**
     * Show loading error.
     */
    public void showLoadingError() {
        dismissDialog();

        new AlertDialog.Builder(getActivity())
                .setTitle("Error")
                .setMessage("Couldn't sync your notes at this time. Please check your network connection and try again.")
                .setNegativeButton("Ok", null)
                .create()
                .show();
    }

    /**
     * Show loading error.
     */
    public void showSavingError() {
        dismissDialog();

        new AlertDialog.Builder(getActivity())
                .setTitle("Error")
                .setMessage("Couldn't save your note. Please check your network connection and try again.")
                .setNegativeButton("Ok", null)
                .create()
                .show();
    }

    /**
     * Show Deleting Notes dialog
     */
    private void showDeletingNotesDialog() {
        if (null == this.dialog) this.dialog = new ProgressDialog(getActivity());
        this.dialog.setMessage(getString(R.string.deleting_note));
        this.dialog.setCancelable(false);
        this.dialog.show();
        AnalyticsManager.fireEvent(getActivity(), "showed deleting note dialog", null);
    }
}
