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
     *
     * @return the title
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
     *
     * @return the details
     */
    public String getDetails();

    /**
     * Sets selected.
     *
     * @param selected the selected
     */
    public void setSelected(boolean selected);

    /**
     * Is selected.
     *
     * @return the boolean
     */
    public boolean isSelected();

    /**
     * Sets record iD.
     *
     * @param recordID the record iD
     */
    public void setRecordID(int recordID);

    /**
     * Gets record iD.
     *
     * @return the record iD
     */
    public int getRecordID();
}
