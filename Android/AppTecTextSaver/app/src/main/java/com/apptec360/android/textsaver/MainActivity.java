package com.apptec360.android.textsaver;

import android.app.ProgressDialog;

import com.apptec360.android.textsaver.R;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    HandlerThread handlerThread = null;
    Handler handler = null;
    Handler mainHandler = null;

    ProgressDialog pd = null;
    private static boolean firstStart = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        handlerThread = new HandlerThread("handlerThread");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
        mainHandler = new Handler();

        pd = new ProgressDialog(this);
        pd.setTitle("Loading");
    }

    private void initListeners(){
        Button save = (Button) findViewById(R.id.saveButton);
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                save();
            }
        });

        Button reload = (Button) findViewById(R.id.reloadButton);
        reload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                load();
            }
        });
    }

    private void load() {
        if(firstStart){
            firstStart = false;
            return;
        }
        LoadTask lt = new LoadTask();
        handler.post(lt);
    }

    private void save() {
        SaveTask st = new SaveTask();
        handler.post(st);
    }

    private class LoadTask implements Runnable {
        @Override
        public void run() {
            _load();
        }
    }

    private class SaveTask implements Runnable {
        @Override
        public void run() {
            _save();
        }
    }

    private class UpdateTextTask implements Runnable {

        String text = null;

        public UpdateTextTask(String text){
            this.text = text;
        }

        @Override
        public void run() {
            EditText textField = (EditText) findViewById(R.id.textfield);
            textField.setText(text);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        initListeners();

        load();

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

    private void restoreTextFields() {
        EditText urlField = (EditText) findViewById(R.id.url);
        EditText secretField = (EditText) findViewById(R.id.secret);


        SharedPreferences sp = getPreferences(MODE_PRIVATE);
        String urlString = sp.getString("url", null);
        String secret = sp.getString("secret", null);

        urlField.setText(urlString);
        secretField.setText(secret);
    }

    private void saveTextFields() {
        EditText urlField = (EditText) findViewById(R.id.url);
        EditText secretField = (EditText) findViewById(R.id.secret);
        String urlString = urlField.getText().toString();
        String secret = secretField.getText().toString();
        SharedPreferences sp = getPreferences(MODE_PRIVATE);
        sp.edit().putString("url", urlString).putString("secret", secret).commit();
    }

    private void _load() {
        pd.show();

        EditText urlField = (EditText) findViewById(R.id.url);
        EditText secretField = (EditText) findViewById(R.id.secret);

        String urlString = urlField.getText().toString();
        String secret = secretField.getText().toString();

        urlString += "?action=load&secret=" + secret;

        String answer = "null";

        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setDoOutput(true);

            InputStream is = connection.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();

            while ((answer = reader.readLine()) != null) {
                sb.append(answer);
            }

            is.close();
            connection.disconnect();
            answer = sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (!answer.equals("error")) {
            //success
           UpdateTextTask utt = new UpdateTextTask(answer);
           mainHandler.post(utt);
        }

        pd.dismiss();
    }

    private void _save() {
        pd.show();

        EditText urlField = (EditText) findViewById(R.id.url);
        EditText secretField = (EditText) findViewById(R.id.secret);
        EditText textField = (EditText) findViewById(R.id.textfield);
        String urlString = urlField.getText().toString();
        String secret = secretField.getText().toString();
        String text = textField.getText().toString();

        urlString += "?action=save&secret=" + secret;
        String postParameters = "text=" + text;

        String answer = null;

        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoInput(true);
            connection.setDoOutput(true);

            DataOutputStream wr = new DataOutputStream(connection.getOutputStream());
            wr.writeBytes(postParameters);
            wr.flush();
            wr.close();
            InputStream is = connection.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();


            while ((answer = reader.readLine()) != null) {
                sb.append(answer);
            }

            is.close();
            connection.disconnect();
            answer = sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (!answer.equals("error")) {
            //success
        }

        pd.dismiss();
    }
}
