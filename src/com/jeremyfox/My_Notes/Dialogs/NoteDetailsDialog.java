package com.jeremyfox.My_Notes.Dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import com.jeremyfox.My_Notes.R;

/**
 * Created with IntelliJ IDEA.
 * User: jeremy
 * Date: 3/14/13
 * Time: 7:03 PM
 */
public class NoteDetailsDialog {

    private AlertDialog.Builder builder;
    private AlertDialog dialog;

    public NoteDetailsDialog(Activity activity, String title, String details){
        this.builder = new AlertDialog.Builder(activity);
        this.builder.setTitle(title).setMessage(details).setPositiveButton(activity.getString(R.string.dismiss), null);
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

}