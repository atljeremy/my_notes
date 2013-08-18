package com.jeremyfox.My_Notes.Interfaces;

/**
 * Created with IntelliJ IDEA.
 * User: jeremy
 * Date: 3/18/13
 * Time: 9:30 PM
 */
public interface NetworkCallback {

    /**
     * On success callback.
     */
    public void onSuccess(Object json);

    /**
     * On failure callback.
     */
    public void onFailure(int statusCode);

}
