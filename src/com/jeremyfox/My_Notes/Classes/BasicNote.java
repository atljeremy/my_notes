package com.jeremyfox.My_Notes.Classes;

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
    private int recordId;

    public BasicNote(String title, String details, int recordId) {
        this.title = title;
        this.details = details;
        this.recordId = recordId;
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

    public int getRecordId() {
        return recordId;
    }

    public void setRecordId(int recordId) {
        this.recordId = recordId;
    }
}
