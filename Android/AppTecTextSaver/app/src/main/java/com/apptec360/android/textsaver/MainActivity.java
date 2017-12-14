package com.apptec360.android.textsaver;

import android.app.AlertDialog;
import android.app.ProgressDialog;

import com.android.volley.Cache;
import com.android.volley.Network;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.*;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    ProgressDialog pd = null;
    private static boolean firstStart = true;

    private EditText urlEditText = null;
    private EditText secretEditText = null;
    private EditText textEditText = null;

    private long lastSave = 0L;

    RequestQueue mRequestQueue;

    private final String tag = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Store pointers to view objects
        urlEditText = findViewById(R.id.url);
        secretEditText = findViewById(R.id.secret);
        textEditText = findViewById(R.id.textfield);

        // Instantiate the cache: 1 MB
        Cache cache = new DiskBasedCache(getCacheDir(), 1024 * 1024);

        // Set up the network to use HttpURLConnection as the HTTP client.
        Network network = new BasicNetwork(new HurlStack());

        // Instantiate the RequestQueue with the cache and network.
        mRequestQueue = new RequestQueue(cache, network);

        // Start the queue
        mRequestQueue.start();
        Log.d(tag, "started the request queue");

        // Progress Dialog
        pd = new ProgressDialog(this);
        pd.setTitle("Loading");
    }

    private void initListeners(){
        Button save = findViewById(R.id.saveButton);
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                long now = System.currentTimeMillis();

                if (now - lastSave > 2000) {
                    mySave(
                            urlEditText.getText().toString(),
                            secretEditText.getText().toString(),
                            textEditText.getText().toString()
                    );

                    lastSave = now;
                }
            }
        });

        Button reload = findViewById(R.id.reloadButton);
        reload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                myLoad(urlEditText.getText().toString(), secretEditText.getText().toString());
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        initListeners();
        restoreTextFields();
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (pd.isShowing()) {
            pd.dismiss();
        }

        saveTextFields();
    }

    private void myLoad(String url, String secret) {
        pd.show();

        HashMap<String, String> params = new HashMap<String, String>();
        params.put("secret", secret);

        String uri = Uri.parse(url)
                .buildUpon()
                .appendQueryParameter("secret", secret)
                .build().toString();

        JsonObjectRequest jsObjRequest = new JsonObjectRequest
                (Request.Method.GET, uri, new JSONObject(params), new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            textEditText.setText(response.getString("story"));
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Log.e(tag, "malformed response json object: " + e.toString());
                        }
                        pd.dismiss();
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        error.printStackTrace();
                        Log.e(tag, "http request error: " + error.toString());
                        if(error.networkResponse.data!=null) {
                            try {
                                String body = new String(error.networkResponse.data,"UTF-8");
                                Log.e(tag, "http request message: " + body);
                                try {
                                    JSONObject responseObj = new JSONObject(body);
                                    showMessage(
                                            "Error Code " + responseObj.getInt("code"),
                                            responseObj.getString("message")
                                    );
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            } catch (UnsupportedEncodingException e) {
                                e.printStackTrace();
                            }
                        }
                        pd.dismiss();
                    }
                });

        mRequestQueue.add(jsObjRequest);
    }

    private void mySave(String url, String secret, String text) {
        pd.show();

        HashMap<String, String> params = new HashMap<String, String>();
        params.put("secret", secret);
        params.put("story", text);

        String uri = Uri.parse(url)
                .buildUpon()
                .appendQueryParameter("secret", secret)
                .appendQueryParameter("story", text)
                .build().toString();

        JsonObjectRequest jsObjRequest = new JsonObjectRequest
                (Request.Method.POST, uri, new JSONObject(params), new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        textEditText.setText("");
                        pd.dismiss();
                        showMessage("Save successful", "The text has been successfully saved.");
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        error.printStackTrace();
                        Log.e(tag, "http request error: " + error.toString());
                        if(error.networkResponse.data!=null) {
                            try {
                                String body = new String(error.networkResponse.data,"UTF-8");
                                Log.e(tag, "http request message: " + body);
                                try {
                                    JSONObject responseObj = new JSONObject(body);
                                    showMessage(
                                            "Error Code " + responseObj.getInt("code"),
                                            responseObj.getString("message")
                                    );
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            } catch (UnsupportedEncodingException e) {
                                e.printStackTrace();
                            }
                        }
                        pd.dismiss();
                    }
                });

        mRequestQueue.add(jsObjRequest);
    }

    private void restoreTextFields() {
        EditText urlField = findViewById(R.id.url);
        EditText secretField = findViewById(R.id.secret);

        SharedPreferences sp = getPreferences(MODE_PRIVATE);
        String urlString = sp.getString("url", "");
        String secret = sp.getString("secret", "");

        urlField.setText(urlString);
        secretField.setText(secret);
    }

    private void saveTextFields() {
        EditText urlField = findViewById(R.id.url);
        EditText secretField = findViewById(R.id.secret);
        String urlString = urlField.getText().toString();
        String secret = secretField.getText().toString();
        SharedPreferences sp = getPreferences(MODE_PRIVATE);
        sp.edit().putString("url", urlString).putString("secret", secret).apply();
    }

    private void showMessage(String title, String message) {
        new AlertDialog.Builder(MainActivity.this)
                .setTitle(title)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                }).show();
    }
}
