package com.jeremyfox.My_Notes.Managers;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: jeremy
 * Date: 3/10/13
 * Time: 8:38 PM
 */
public class NotesManager {

    private ArrayList notes = new ArrayList();

    /**
     * Instantiates a new Notes manager.
     *
     * @throws JSONException the jSON exception
     */
    public NotesManager() throws JSONException {
        createJSON();
    }

    /**
     * Gets notes object.
     *
     * @return the notes object
     */
    public ArrayList getNotesObject() {
        return this.notes;
    }

    private void createJSON() throws JSONException {
        this.notes.add(new JSONObject().put("03.12.13 - Pick up kids", "Don't forget to pick up the kids from soccer practice."));
        this.notes.add(new JSONObject().put("03.15.13 - Meeting with John", "I need to meet with John regarding a raise."));
        this.notes.add(new JSONObject().put("03.20.13 - Vacation ideas", "We could visit Disney World or go to Santa Monica."));
        this.notes.add(new JSONObject().put("04.03.13 - Passwords", "My facebook password is P@$$w0rd."));
        this.notes.add(new JSONObject().put("04.14.13 - Grocery list", "Milk, eggs, chicken, butter, cereal."));
        this.notes.add(new JSONObject().put("04.24.13 - Phone Numbers", "Michael's new phone number is 123-123-1234."));
        this.notes.add(new JSONObject().put("05.06.13 - Paper Requirements", "The paper due on May 12th has to be 100 pages."));
        this.notes.add(new JSONObject().put("05.15.13 - New phone ideas", "iPhone 5, or Samsung Galaxy 4."));
        this.notes.add(new JSONObject().put("05.23.13 - New app ideas", "An app that allows you to import videos and export photos from frames in the video."));
        this.notes.add(new JSONObject().put("06.07.13 - App Issues", "I noticed the title logo needs to be centered and slightly larger."));
    }

}
