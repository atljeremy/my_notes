package com.jeremyfox.My_Notes.Managers;

import android.content.Context;
import com.jeremyfox.My_Notes.Activities.MainActivity;
import com.jeremyfox.My_Notes.Classes.Environment;
import com.mixpanel.android.mpmetrics.MixpanelAPI;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Created with IntelliJ IDEA.
 * User: jeremy
 * Date: 3/28/13
 * Time: 12:20 PM
 */
public class AnalyticsManager {

    /**
     * The single instance of AnalyticsManager
     */
    private static final String API_TOKEN = "e8fb97ba8b60cfbc027dfb2e337cd822";
    private static final String API_TOKEN_DEBUG = "8c692559bf5c07a59ab6cf31c1975a76";

    /**
     * Fire event.
     *
     * @param eventKey the event key
     * @param propertiesMap the properties map
     */
    public static void fireEvent(Context context, String eventKey, HashMap<String, String> propertiesMap) {
        JSONObject eventProperties = null;
        if (propertiesMap != null) {
            eventProperties = new JSONObject();
            Iterator iterator = propertiesMap.entrySet().iterator();
            while (iterator.hasNext()) {
                HashMap.Entry pairs = (HashMap.Entry)iterator.next();
                String entryKey = (pairs.getKey().toString().length() > 0) ? pairs.getKey().toString() : "no_key";
                String entryValue = (pairs.getValue().toString().length() > 0) ? pairs.getValue().toString() : "no_value";
                try {
                    eventProperties.put(entryKey, entryValue);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }

        MixpanelAPI.getInstance(context, getAPIToken()).track(eventKey, eventProperties);
    }

    /**
     * Flush events.
     */
    public static void flushEvents(Context context) {
        MixpanelAPI.getInstance(context, getAPIToken()).flush();
    }

    /**
     * Register super property.
     *
     * @param key the key
     * @param property the property
     */
    public static void registerSuperProperty(Context context, String key, String property) {
        JSONObject superProperties = new JSONObject();
        try {
            superProperties.put(key, property);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        MixpanelAPI.getInstance(context, getAPIToken()).registerSuperProperties(superProperties);
    }

    private static String getAPIToken() {
        String apiToken = API_TOKEN;
        if (Environment.isDebug()) {
            apiToken = API_TOKEN_DEBUG;
        }
        return apiToken;
    }

}
