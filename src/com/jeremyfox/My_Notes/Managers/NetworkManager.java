package com.jeremyfox.My_Notes.Managers;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.util.Log;
import com.jeremyfox.My_Notes.Interfaces.NetworkCallback;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created with IntelliJ IDEA.
 * User: jeremy
 * Date: 3/18/13
 * Time: 9:01 PM
 */
public class NetworkManager {

    /**
     * The constant FAILURE_UNKNOWN_STATUS. Used to determine if a network request failed without returning a status code.
     */
    public static int FAILURE_UNKNOWN_STATUS = -1;
    /**
     * The constant SUCCESS_STATUS. Used to tell the callback.onFailure(statusCode) was and to compare against returned status codes.
     */
    public static int SUCCESS_STATUS = 200;
    /**
     * The constant NOT_MODIFIED_STATUS. Used to tell the callback.onFailure(statusCode) was and to compare against returned status codes.
     */
    public static int NOT_MODIFIED_STATUS = 304;

    private enum RequestType {

        /**
         * Specifies a GET request.
         */
        GET,

        /**
         * Specifies a POST request.
         */
        POST
    };

    /**
     * Is connected helper method. Use this method to determine network connectivity.
     *
     * @param context the context
     * @return the boolean
     */
    public static boolean isConnected(Context context) {

        boolean isConnected = false;

        ConnectivityManager connectivityManager = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = connectivityManager.getActiveNetworkInfo();
        if(ni!=null){
            if(ni.isConnected()){
                isConnected = true;
            }
        }

        return isConnected;
    }

    /**
     * Execute get request.
     *
     * @param url the url
     * @param callback the callback
     */
    public void executeGetRequest(String url, NetworkCallback callback) {
        NetworkManager.executeRequest(url, RequestType.GET, callback);
    }

    /**
     * Execute post request.
     *
     * @param url the url
     * @param callback the callback
     */
    public void executePostRequest(String url, NetworkCallback callback) {
        NetworkManager.executeRequest(url, RequestType.POST, callback);
    }

    /**
     * Execute request.
     *
     * @param url the url
     * @param requestType the RequestType
     * @param callback the callback
     */
    private static void executeRequest(String url, RequestType requestType, NetworkCallback callback) {

        if (null != url || null != callback) {

            HttpRequestBase httpRequest;
            switch (requestType) {
                case POST:
                    httpRequest = new HttpPost(url);
                    break;

                case GET:
                default:
                    httpRequest = new HttpGet(url);
                    break;
            }

            new NetworkAsyncTask().execute(httpRequest, callback);
        }
    }
}

class NetworkAsyncTask extends AsyncTask<Object, Integer, String> {

    private NetworkCallback callback;

    protected String doInBackground(Object... params) {

        HttpRequestBase httpRequestBase = (HttpRequestBase)params[0];
        this.callback = (NetworkCallback)params[1];

        StringBuilder stringBuilder = new StringBuilder();
        HttpClient httpClient = new DefaultHttpClient();
        try {
            HttpResponse response = httpClient.execute((HttpRequestBase)params[0]);
            StatusLine statusLine = response.getStatusLine();
            int statusCode = statusLine.getStatusCode();
            boolean received200Status = (statusCode == NetworkManager.SUCCESS_STATUS);
            boolean received304Status = (statusCode == NetworkManager.NOT_MODIFIED_STATUS);

            if (received200Status || received304Status) {

                HttpEntity entity = response.getEntity();
                InputStream inputStream = entity.getContent();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                String line;

                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line);
                }

                inputStream.close();

            }
        } catch (Exception e) {
            Log.d("executeGetRequest", e.getLocalizedMessage());
        }

        return stringBuilder.toString();
    }

    protected void onPostExecute(String result) {
        if (null != result && result.length() > 0 && null != this.callback) {
            try {
                this.callback.onSuccess(new JSONObject(result));
            } catch (JSONException e) {
                e.printStackTrace();
                this.callback.onFailure(NetworkManager.FAILURE_UNKNOWN_STATUS);
            }
        } else {
            this.callback.onFailure(NetworkManager.FAILURE_UNKNOWN_STATUS);
        }
    }
}
