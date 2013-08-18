package com.jeremyfox.My_Notes.Models;

import com.jeremyfox.My_Notes.Interfaces.Note;
import com.jeremyfox.My_Notes.Interfaces.User;

import java.util.Date;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: jfox
 * Date: 8/17/13
 * Time: 3:44 PM
 */
public class BasicUser implements User {

    private int id;
    private String apiToken;
    private String email;
    private String lastSyncDate;
    private List<Note> notes;

    public BasicUser(int id, String apiToken, String email, String lastSyncDate) {
        this.id = id;
        this.apiToken = apiToken;
        this.email = email;
        this.lastSyncDate = lastSyncDate;
    }

    @Override
    public int getId() {
        return this.id;
    }

    @Override
    public void setId(int id) {
        this.id = id;
    }

    @Override
    public String getApiToken() {
        return this.apiToken;
    }

    @Override
    public void setApiToken(String apiToken) {
        this.apiToken = apiToken;
    }

    @Override
    public String getEmailAddress() {
        return this.email;
    }

    @Override
    public void setEmailAddress(String email) {
        this.email = email;
    }

    @Override
    public String getLastSyncDate() {
        return this.lastSyncDate;
    }

    @Override
    public void setLastSyncDate(String lastSyncDate) {
        this.lastSyncDate = lastSyncDate;
    }

    @Override
    public List<Note> getNotes() {
        return this.notes;
    }

    @Override
    public void setNotes(List<Note> notes) {
        this.notes = notes;
    }
}
