package com.jeremyfox.My_Notes.Adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import com.jeremyfox.My_Notes.Classes.BasicNote;
import com.jeremyfox.My_Notes.R;

import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: jeremy
 * Date: 3/20/13
 * Time: 10:27 PM
 */
public class NotesAdapter extends ArrayAdapter<BasicNote> {

    private ArrayList<BasicNote> notes;

    public NotesAdapter(Context context, int textViewResourceId, ArrayList<BasicNote> notes) {
        super(context, textViewResourceId, notes);
        this.notes = notes;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            LayoutInflater layoutInflater = (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = layoutInflater.inflate(R.layout.my_notes_list_item, null);
        }

        BasicNote note = notes.get(position);
        if (note != null) {
            TextView title = (TextView) view.findViewById(R.id.title);
            if (title != null) {
                title.setText(note.getTitle());
            }
        }
        return view;
    }
}