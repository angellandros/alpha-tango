package com.apptec360.android.textsaver;

import android.app.AlertDialog;
import android.app.ProgressDialog;

import com.android.volley.AuthFailureError;
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
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    ProgressDialog pd = null;
    private static boolean firstStart = true;

    private long lastSave = 0L;

    RequestQueue mRequestQueue;

    private final String tag = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Instantiate the cache: 1 MB
        Cache cache = new DiskBasedCache(getCacheDir(), 1024 * 1024);

        // Set up the network to use HttpURLConnection as the HTTP client.
        Network network = new BasicNetwork(new HurlStack());

        // Instantiate the RequestQueue with the cache and network.
        mRequestQueue = new RequestQueue(cache, network);
        Log.d(tag, "initialized the HTTP request queue");

        // Start the queue
        mRequestQueue.start();
        Log.d(tag, "started the request queue");

        // Progress Dialog
        pd = new ProgressDialog(this);
        pd.setTitle("Loading");
    }

    private void initListeners() {
        Log.d(tag, "initializing the listeners");
        Button save = findViewById(R.id.saveButton);
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText urlEditText = findViewById(R.id.url);
                EditText secretEditText = findViewById(R.id.secret);
                EditText textEditText = findViewById(R.id.textfield);

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
                EditText urlEditText = findViewById(R.id.url);
                EditText secretEditText = findViewById(R.id.secret);

                myLoad(urlEditText.getText().toString(), secretEditText.getText().toString());
            }
        });
        Log.d(tag, "initialized the listeners");
    }

    @Override
    protected void onResume() {
        Log.d(tag, "onResume called");
        super.onResume();
        initListeners();

        if(!firstStart) {
            restoreTextFields();
            Log.d(tag, "restored the text fields");
        } else {
            firstStart = false;
            Log.d(tag, "first time, no text field restoration");
        }
    }

    @Override
    protected void onPause() {
        Log.d(tag, "onPause called");
        super.onPause();

        if (pd.isShowing()) {
            pd.dismiss();
        }

        saveTextFields();
        Log.d(tag, "text fields saved");
    }

    private void myLoad(String url, String secret) {
        Log.d(tag, "myLoad called, with URL = " + url + ", secret = " + secret);
        pd.show();

        HashMap<String, String> params = new HashMap<>();
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
                            EditText textEditText = findViewById(R.id.textfield);
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
                        try {
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
                                        Log.d(tag, "error status code: " + error.networkResponse.statusCode);
                                        if (error.networkResponse.statusCode == 404) {
                                            showMessage("Not Found", "Please check your URL.");
                                        }
                                    }
                                } catch (UnsupportedEncodingException e) {
                                    e.printStackTrace();
                                }
                            }
                        } catch (NullPointerException e) {
                            e.printStackTrace();
                            showMessage("Unexpected Response",
                                    "Please check your URL and network connection.");
                        }

                        pd.dismiss();
                    }
                });

        mRequestQueue.add(jsObjRequest);
    }

    private void mySave(String url, String secret, String text) {
        Log.d(tag, "mySave called, with URL = " + url + ", secret = " + secret + ", text = " + text);
        pd.show();

        HashMap<String, String> params = new HashMap<>();
        params.put("secret", secret);
        params.put("story", text);

        String uri = Uri.parse(url)
                .buildUpon()
//                .appendQueryParameter("secret", secret)
//                .appendQueryParameter("story", text)
                .build().toString();
        Log.d(tag, "Uri: " + uri);

        JsonObjectRequest jsObjRequest = new JsonObjectRequest
                (Request.Method.POST, uri, new JSONObject(params), new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        Log.d(tag, "response: " + response.toString());
                        EditText textEditText = findViewById(R.id.textfield);
                        textEditText.setText("");
                        pd.dismiss();
                        showMessage("Save successful", "The text has been successfully saved.");
                    }
                }, new Response.ErrorListener() {

                    @Override
                    public void onErrorResponse(VolleyError error) {
                        try {
                            error.printStackTrace();
                            Log.e(tag, "http request error: " + error.toString());
                            if(error.networkResponse.data != null) {
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
                                        Log.d(tag, "error status code: " + error.networkResponse.statusCode);
                                        if (error.networkResponse.statusCode == 404) {
                                            showMessage("Not Found", "Please check your URL.");
                                        }
                                    }
                                } catch (UnsupportedEncodingException e) {
                                    e.printStackTrace();
                                }
                            }
                        } catch (NullPointerException e) {
                            e.printStackTrace();
                            showMessage("Unexpected Response",
                                    "Please check your URL and network connection.");
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
