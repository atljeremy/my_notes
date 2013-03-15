package com.jeremyfox.My_Notes.Dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.widget.EditText;
import android.widget.LinearLayout;
import com.jeremyfox.My_Notes.R;

/**
 * Created with IntelliJ IDEA.
 * User: jeremy
 * Date: 3/14/13
 * Time: 8:34 PM
 * To change this template use File | Settings | File Templates.
 */
public class NewNoteDialog {

    private AlertDialog.Builder builder;
    private AlertDialog dialog;
    private EditText titleInput;
    private EditText detailsInput;

    public NewNoteDialog(Activity activity, EditText titleInput, EditText detailsInput, DialogInterface.OnClickListener saveListener){
        this.titleInput = titleInput;
        this.detailsInput = detailsInput;
        this.builder = new AlertDialog.Builder(activity);
        this.builder.setTitle(activity.getString(R.string.createNewNote))
                    .setPositiveButton(activity.getString(R.string.save), saveListener)
                    .setNegativeButton(activity.getString(R.string.cancel), null);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        LinearLayout linearLayout = new LinearLayout(this.builder.getContext());
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setLayoutParams(lp);
        linearLayout.addView(this.titleInput, lp);
        linearLayout.addView(this.detailsInput, lp);

        this.builder.setView(linearLayout);

        this.dialog = this.builder.create();
    }

    public void showDialog() {
        if (null != this.builder) {
            this.builder.show();
        }
    }

    public void hideDialog() {
        if (null != this.dialog) {
            this.dialog.dismiss();
        }
    }

    public EditText getTitleInput() {
        return titleInput;
    }

    public void setTitleInput(EditText titleInput) {
        this.titleInput = titleInput;
    }

    public EditText getDetailsInput() {
        return detailsInput;
    }

    public void setDetailsInput(EditText detailsInput) {
        this.detailsInput = detailsInput;
    }
}
