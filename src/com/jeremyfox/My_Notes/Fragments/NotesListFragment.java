package com.jeremyfox.My_Notes.Fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.SparseBooleanArray;
import android.view.*;
import android.widget.*;
import com.jeremyfox.My_Notes.Adapters.NotesAdapter;
import com.jeremyfox.My_Notes.Classes.BasicNote;
import com.jeremyfox.My_Notes.Helpers.PrefsHelper;
import com.jeremyfox.My_Notes.Interfaces.NetworkCallback;
import com.jeremyfox.My_Notes.Managers.AnalyticsManager;
import com.jeremyfox.My_Notes.Managers.NotesManager;
import com.jeremyfox.My_Notes.R;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created with IntelliJ IDEA.
 * User: jeremy
 * Date: 4/14/13
 * Time: 12:47 PM
 */
public class NotesListFragment extends Fragment {

    public interface NotesListListener {
        public void showNoteDetails(int index, boolean dualMode);
        public void registerWithAPI();
    }

    private NotesListListener listener;
    private boolean dualMode;
    private int curCheckPosition = 0;
    private static final int DEFAULT_HOME_VIEW = 0;
    private static final int NOTES_VIEW = 1;
    private ViewFlipper viewFlipper;
    private GridView gridView;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        this.gridView = (GridView)getActivity().findViewById(R.id.gridview);
        this.viewFlipper = (ViewFlipper)getActivity().findViewById(R.id.ViewFlipper);

        View detailsFrame = getActivity().findViewById(R.id.note_details_fragment);
        dualMode = detailsFrame != null && detailsFrame.getVisibility() == View.VISIBLE;

        if (savedInstanceState != null) {
            curCheckPosition = savedInstanceState.getInt("curNote", 0);
        }

        if (dualMode) {
            GridView gridView = (GridView)getActivity().findViewById(R.id.gridview);
            gridView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
            showNoteDetails(curCheckPosition);
        }

        String user_id = PrefsHelper.getPref(getActivity(), getActivity().getString(R.string.user_id));
        if (null == user_id || user_id.length() == 0) {
            AnalyticsManager.getInstance().fireEvent("new user", null);
            listener.registerWithAPI();
        } else {
            AnalyticsManager.getInstance().fireEvent("returning user", null);
            createGridView();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("curNote", curCheckPosition);
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

    private void showNoteDetails(int index) {
        curCheckPosition = index;
        listener.showNoteDetails(index, dualMode);
    }

    /**
     * Creates the notes grid view, retrieves notes from API, then displays the grid view of notes
     */
    public void createGridView() {
        final ProgressDialog dialog = showLoadingDialog();

        NotesManager.getInstance().retrieveNotesFromAPI(getActivity(), new NetworkCallback() {
            @Override
            public void onSuccess(Object json) {
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
                        showNoteDetails(position);
                    }
                });

                dialog.dismiss();
            }

            @Override
            public void onFailure(int statusCode) {
                dialog.dismiss();

                new AlertDialog.Builder(getActivity())
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
    public void setGridViewItems() {
        JSONArray jsonArray = NotesManager.getInstance().getNotes();
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

            NotesAdapter notesAdapter = new NotesAdapter(getActivity(), R.id.title, notes);
            this.gridView.setAdapter(notesAdapter);
            this.viewFlipper.setDisplayedChild(NOTES_VIEW);
            AnalyticsManager.getInstance().fireEvent("showed notes view", null);
        } else {
            NotesAdapter notesAdapter = new NotesAdapter(getActivity(), R.id.title, new ArrayList<BasicNote>());
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
                NotesManager.getInstance().deleteNote(getActivity(), note, new NetworkCallback() {
                    @Override
                    public void onSuccess(Object json) {
                        NotesManager.getInstance().removeNote(note);
                        setGridViewItems();
                        Toast.makeText(getActivity(), "Selected Notes Deleted", Toast.LENGTH_SHORT).show();
                        AnalyticsManager.getInstance().fireEvent("successfully deleted notes from API", null);
                    }

                    @Override
                    public void onFailure(int statusCode) {
                        Toast.makeText(getActivity(), "ERROR: Selected Notes Not Deleted", Toast.LENGTH_SHORT).show();
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
                NotesManager.getInstance().editNote(getActivity(), note, new NetworkCallback() {
                    @Override
                    public void onSuccess(Object json) {
                        createGridView();
                        Toast.makeText(getActivity(), getString(R.string.noteSaved), Toast.LENGTH_SHORT).show();
                        AnalyticsManager.getInstance().fireEvent("successfully edited notes from API", null);
                    }

                    @Override
                    public void onFailure(int statusCode) {
                        Toast.makeText(getActivity(), "Error: Note Not Saved. Please Check Your Network Connection and Try Again.", Toast.LENGTH_LONG).show();
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
        ProgressDialog dialog = new ProgressDialog(getActivity());
        dialog.setMessage("Loading Notes...");
        dialog.setCancelable(false);
        dialog.show();
        AnalyticsManager.getInstance().fireEvent("showed loading dialog", null);
        return dialog;
    }

}