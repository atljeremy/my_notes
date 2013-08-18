package com.jeremyfox.My_Notes.Interfaces;

import java.util.Date;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: jfox
 * Date: 8/17/13
 * Time: 3:34 PM
 */
public interface User {

    public static final String ID_KEY              = "id";
    public static final String API_TOKEN_KEY       = "apiToken";
    public static final String EMAIL_ADDRESS_KEY   = "email";
    public static final String LAST_SYNC_DATE_KEY  = "lastSyncDate";

    public int getId();
    public void setId(int id);

    public String getApiToken();
    public void setApiToken(String apiToken);

    public String getEmailAddress();
    public void setEmailAddress(String email);

    public String getLastSyncDate();
    public void setLastSyncDate(String lastSyncDate);

    public List<Note> getNotes();
    public void setNotes(List<Note> notes);

}
