package com.jeremyfox.My_Notes.Models;

import android.os.Parcel;
import android.os.Parcelable;
import com.jeremyfox.My_Notes.Interfaces.Note;

/**
 * Created with IntelliJ IDEA.
 * User: jeremy
 * Date: 3/14/13
 * Time: 5:07 PM
 */
public class BasicNote implements Note {

    private String title;
    private String details;
    private boolean selected;
    private int id;
    private int userId;
    private String createdAt;
    private String updatedAt;

    /**
     * Instantiates a new Basic note.
     *
     * @param id the record id
     * @param title the title
     * @param details the details
     * @param userId the user ID of the user in the Users database that owns this note
     */
    public BasicNote(int id, String title, String details, String createdAt, String updatedAt, int userId) {
        this.title     = title;
        this.details   = details;
        this.id        = id;
        this.selected  = false;
        this.userId    = userId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     *
     * Constructor to use when re-constructing object
     * from a parcel
     *
     * @param in a parcel from which to read this object
     */
    public BasicNote(Parcel in) {
        readFromParcel(in);
    }

    @Override
    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public String getTitle() {
        return this.title;
    }

    @Override
    public void setDetails(String details) {
        this.details = details;
    }

    @Override
    public String getDetails() {
        return this.details;
    }

    @Override
    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    @Override
    public boolean isSelected() {
        return this.selected;
    }

    @Override
    public void setId(int id) {
        this.id = id;
    }

    @Override
    public int getId() {
        return this.id;
    }

    @Override
    public void setUserId(int userId) {
        this.userId = userId;
    }

    @Override
    public int getUserId() {
        return this.userId;
    }

    @Override
    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String getCreatedAt() {
        return this.createdAt;
    }

    @Override
    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String getUpdatedAt() {
        return this.updatedAt;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * Called by the OS when parceling a BasicNote
     *
     * @param out
     * @param i
     */
    @Override
    public void writeToParcel(Parcel out, int i) {
        out.writeString(title);
        out.writeString(details);
        out.writeInt((selected) ? 1 : 0);
        out.writeInt(id);
        out.writeInt(userId);
    }

    /**
     *
     * Called from the constructor to create this
     * object from a parcel.
     *
     * @param in parcel from which to re-create object
     */
    private void readFromParcel(Parcel in) {
        title = in.readString();
        details = in.readString();
        selected = (in.readInt() != 0);
        id = in.readInt();
        userId = in.readInt();
    }

    /**
     * This field is needed for Android to be able to
     * create new objects, individually or as arrays.
     */
    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public BasicNote createFromParcel(Parcel in) {
            return new BasicNote(in);
        }

        public BasicNote[] newArray(int size) {
            return new BasicNote[size];
        }
    };
}
