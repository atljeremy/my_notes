package com.jeremyfox.My_Notes.Activites;

import android.app.ListActivity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.jeremyfox.My_Notes.Managers.NotesManager;
import com.jeremyfox.My_Notes.R;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

public class MainActivity extends ListActivity {

    private EditText titleTextField;
    private EditText detailsTextField;
    private NotesManager notesManager;
    private LinearLayout layout;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        createUIElements();
    }

    /**
     * Used to create all custom UI Elements with one method call
     */
    private void createUIElements() {
        LinearLayout layout = createLinearLayout();

        TextView title = createTextview();
        title.setText(getString(R.string.notesTitle));
        createTitleTextField(layout);

        TextView details = createTextview();
        details.setText(getString(R.string.notesDetails));
        createDetailsTextField(layout);

        createButton();

        createListView();
    }

    /**
     * Custom LinearLayout
     */
    private LinearLayout createLinearLayout() {
        this.layout = new LinearLayout(this);
        this.layout.setBackgroundColor(Color.BLACK);
        this.layout.setOrientation(LinearLayout.VERTICAL);
        this.layout.setGravity(Gravity.CENTER_HORIZONTAL);

        ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT);
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(layoutParams);
        this.addContentView(this.layout, params);

        return this.layout;
    }

    /**
     * Custom TextView
     */
    private TextView createTextview() {
        TextView textView = new TextView(this);
        this.layout.addView(textView, getLayoutParams());
        return textView;
    }

    /**
     * Title Text Field
     * @param layout the layout in which to show this EditText
     */
    private void createTitleTextField(LinearLayout layout) {
        this.titleTextField = new EditText(this);
        layout.addView(this.titleTextField, getLayoutParams());
    }

    /**
     * Details Text Field
     * @param layout the layout in which to show this EditText
     */
    private void createDetailsTextField(LinearLayout layout) {
        this.detailsTextField = new EditText(this);
        layout.addView(this.detailsTextField, getLayoutParams());
    }

    /**
     * Custom Button
     */
    private void createButton() {
        Button button = new Button(this);
        button.setText("Save New Note");
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String toastMessage;

                if (MainActivity.this.titleTextField.length() > 0 && MainActivity.this.detailsTextField.length() > 0) {

                    /**
                     * Save the new note then update the list view
                     */
                    JSONArray notes = MainActivity.this.notesManager.getNotesObject();
                    Date date = new Date();
                    String dateString = android.text.format.DateFormat.format("MM.dd.yy", new java.util.Date()).toString();
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(dateString);
                    stringBuilder.append(" - ");
                    stringBuilder.append(MainActivity.this.titleTextField.getText().toString());
                    try {
                        notes.put(new JSONObject().put(stringBuilder.toString(), MainActivity.this.detailsTextField.getText().toString()));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    createListView();
                    MainActivity.this.titleTextField.setText("");
                    MainActivity.this.detailsTextField.setText("");
                    toastMessage = "Note Saved!";

                } else {
                    toastMessage = "Note Not Saved. All Fields Required.";
                }

                Toast.makeText(MainActivity.this, toastMessage, Toast.LENGTH_SHORT).show();
            }
        });

        this.layout.addView(button, getLayoutParams());
    }

    private void createListView() {
        if (null == this.notesManager) {
            try {
                this.notesManager = new NotesManager(getApplicationContext());
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (null != this.notesManager){

            ListView listView = getListView();;
            JSONArray jsonArray = this.notesManager.getNotesObject();
            if (jsonArray.length() > 0) {
                listView.setLayoutParams(getLayoutParams());
                ArrayList<String> titles = new ArrayList<String>(jsonArray.length());
                for (int i=0; i<jsonArray.length(); i++) {
                    JSONObject currentNote = null;
                    try {
                        currentNote = jsonArray.getJSONObject(i);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    Iterator iterator = currentNote.keys();
                    while (iterator.hasNext()) {
                        String key = (String)iterator.next();
                        titles.add(key);
                    }
                }
                setListAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, titles));
            } else {

            }

            ((ViewGroup)listView.getParent()).removeView(listView);
            this.layout.addView(listView, getLayoutParams());

            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    JSONArray notes = MainActivity.this.notesManager.getNotesObject();
                    JSONObject noteObject = null;
                    try {
                        noteObject = notes.getJSONObject(position);
                    } catch (JSONException e) {

                    }
                    Iterator iterator = noteObject.keys();
                    while (iterator.hasNext()) {
                        String key = (String)iterator.next();
                        if (null != key) {
                            try {
                                Toast.makeText(getApplicationContext(), noteObject.getString(key), Toast.LENGTH_LONG).show();
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            });
        }
    }

    /**
     * Used in the custom view creation methods to set the layout params
     * @return LayoutParams
     */
    private ViewGroup.LayoutParams getLayoutParams() {
        ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        return new ViewGroup.LayoutParams(layoutParams);
    }
}
