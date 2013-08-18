package com.jeremyfox.My_Notes.Interfaces;

import android.os.Parcelable;

/**
 * Created with IntelliJ IDEA.
 * User: jeremy
 * Date: 3/14/13
 * Time: 4:55 PM
 */
public interface Note extends Parcelable {

    public static final String ID_KEY         = "id";
    public static final String TITLE_KEY      = "title";
    public static final String DETAILS_KEY    = "details";
    public static final String CREATED_AT_KEY = "note";
    public static final String UPDATED_AT_KEY = "note";
    public static final String NOTE_KEY       = "note";

    public void setTitle(String title);
    public String getTitle();

    public void setDetails(String details);
    public String getDetails();

    public void setSelected(boolean selected);
    public boolean isSelected();

    public void setId(int id);
    public int getId();

    public void setUserId(int userId);
    public int getUserId();

    public void setCreatedAt(String createdAt);
    public String getCreatedAt();

    public void setUpdatedAt(String updatedAt);
    public String getUpdatedAt();
}
