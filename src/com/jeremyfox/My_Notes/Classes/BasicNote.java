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

    public BasicNote(String title, String details) {
        this.title = title;
        this.details = details;
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
}
