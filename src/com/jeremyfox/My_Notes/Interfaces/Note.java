package com.jeremyfox.My_Notes.Interfaces;

/**
 * Created with IntelliJ IDEA.
 * User: jeremy
 * Date: 3/14/13
 * Time: 4:55 PM
 */
public interface Note {

    /**
     * Sets title.
     *
     * @param title the title
     */
    public void setTitle(String title);

    /**
     * Gets title.
     */
    public String getTitle();

    /**
     * Sets details.
     *
     * @param details the details
     */
    public void setDetails(String details);

    /**
     * Gets details.
     */
    public String getDetails();
}
