package com.example.redassistant;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.api.client.json.JsonString;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.dialogflow.v2.DetectIntentResponse;
import com.google.cloud.dialogflow.v2.QueryInput;
import com.google.cloud.dialogflow.v2.SessionName;
import com.google.cloud.dialogflow.v2.SessionsClient;
import com.google.cloud.dialogflow.v2.SessionsSettings;
import com.google.cloud.dialogflow.v2.TextInput;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int USER = 10001;
    private static final int BOT = 10002;
    private static final int OPTIONS = 10003;
    int payload=0;


    private String uuid = UUID.randomUUID().toString();
    private LinearLayout chatLayout;
    private EditText queryEditText;
    ArrayList options= new ArrayList();

    ;

    // Java V2
    private SessionsClient sessionsClient;
    private SessionName session;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.mytoolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        final ScrollView scrollview = findViewById(R.id.chatScrollView);
        scrollview.post(() -> scrollview.fullScroll(ScrollView.FOCUS_DOWN));

        chatLayout = findViewById(R.id.chatLayout);


        queryEditText = findViewById(R.id.queryEditText);
        queryEditText.setOnKeyListener((view, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                switch (keyCode) {
                    case KeyEvent.KEYCODE_DPAD_CENTER:
                    case KeyEvent.KEYCODE_ENTER:
                        sendMessage();
                        return true;
                    default:
                        break;
                }
            }
            return false;
        });

        // Android client for older V1 --- recommend not to use this
//        initChatbot();

        // Java V2
        initV2Chatbot();





    }

//    public void buttonClicked(View view){
//        String text = view.getContext().getText(0).toString();
//        Log.d(TAG,"button clicked"+text);
//    }

    private void initV2Chatbot() {
        try {
            InputStream stream = getResources().openRawResource(R.raw.credentials);
            GoogleCredentials credentials = GoogleCredentials.fromStream(stream);
            String projectId = ((ServiceAccountCredentials)credentials).getProjectId();

            SessionsSettings.Builder settingsBuilder = SessionsSettings.newBuilder();
            SessionsSettings sessionsSettings = settingsBuilder.setCredentialsProvider(FixedCredentialsProvider.create(credentials)).build();
            sessionsClient = SessionsClient.create(sessionsSettings);
            session = SessionName.of(projectId, uuid);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendMessage() {
        String msg = queryEditText.getText().toString();
        if (msg.trim().isEmpty()) {
            Toast.makeText(MainActivity.this, "Please enter your query!", Toast.LENGTH_LONG).show();
        } else {
            showTextView(msg, USER);
            queryEditText.setText("");

            // Java V2
            QueryInput queryInput = QueryInput.newBuilder().setText(TextInput.newBuilder().setText(msg).setLanguageCode("en-US")).build();
            new RequestJavaV2Task(MainActivity.this, session, sessionsClient, queryInput).execute();
        }
    }



    public void callbackV2(DetectIntentResponse response) {
        payload=0;
        int temp=0;
        if(response.getQueryResult().getFulfillmentMessagesCount()>1){
         payload =Integer.parseInt(response.getQueryResult().getFulfillmentMessages(1).getPayload().
                getFieldsMap().get("numbers").getListValue().getValues(0).getStringValue());
        temp=1;}
        if (response != null  ) {
            // process aiResponse here


            String botReply = response.getQueryResult().getFulfillmentText();
                 if(payload!=0) {
                     if (response.getQueryResult().getFulfillmentMessagesCount() > 1) {
                         for (int i = 0; i < Integer.parseInt(response.getQueryResult().getFulfillmentMessages(temp).getPayload().
                                 getFieldsMap().get("numbers").getListValue().getValues(0).getStringValue()); i++)
                             options.add(i, response.getQueryResult().getFulfillmentMessages(1).getPayload().
                                     getFieldsMap().get("choices").getListValue().getValues(i).getStringValue());
                     }
                 }
            /*List<String> list = new ArrayList<String>();
            JSONArray array = new JSONArray(response.getField());
            for(int i = 0 ; i < array.length() ; i++){
                list.add(array.getJSONObject(i).getString("interestKey"));
            }*/
            Log.d(TAG,"choices: " + response.getQueryResult());
            Log.d(TAG, "V2 Bot Reply: " + botReply);
            showTextView(botReply, BOT);
            if(payload!=0) {
                for (int i = 0; i < Integer.parseInt(response.getQueryResult().getFulfillmentMessages(temp).getPayload().
                        getFieldsMap().get("numbers").getListValue().getValues(0).getStringValue()); i++)
                    showTextView(options.get(i).toString(), OPTIONS);
            }



        } else {
            Log.d(TAG, "Bot Reply: Null");
            showTextView("There was some communication issue. Please Try again!", BOT);
        }
    }

    private void showTextView(String message, int type) {
        FrameLayout layout;
        switch (type) {

            case USER:
                layout = getUserLayout();
                break;
            case BOT:
                layout = getBotLayout();
                break;
            case OPTIONS:
                layout=getOptionsLayout();
                break;
            default:
                layout = getBotLayout();
                break;
        }
        layout.setFocusableInTouchMode(true);
        chatLayout.addView(layout); // move focus to text view to automatically make it scroll up if softfocus
        if(type != OPTIONS) {
            TextView tv = layout.findViewById(R.id.chatMsg);
            tv.setText(message);
        } else {
            TextView optionss = layout.findViewById(R.id.chatopt);
            optionss.setOnClickListener(new View.OnClickListener(){
                @Override

                public void onClick(View v){
                    String name= optionss.getText().toString();
                    queryEditText.setText("");
                    Log.d(TAG,"girdi");

                    // Java V2
                    QueryInput queryInput = QueryInput.newBuilder().setText(TextInput.newBuilder().setText(name).setLanguageCode("en-US")).build();
                    new RequestJavaV2Task(MainActivity.this, session, sessionsClient, queryInput).execute();
                }


            });
            optionss.setText(message.toLowerCase(Locale.ROOT));

        }

        layout.requestFocus();
        queryEditText.requestFocus(); // change focus back to edit text to continue typing
    }

    FrameLayout getUserLayout() {
        LayoutInflater inflater = LayoutInflater.from(MainActivity.this);
        return (FrameLayout) inflater.inflate(R.layout.user_msg_layout, null);
    }

    FrameLayout getBotLayout() {
        LayoutInflater inflater = LayoutInflater.from(MainActivity.this);
        return (FrameLayout) inflater.inflate(R.layout.bot_msg_layout, null);
    }
    FrameLayout getOptionsLayout() {
        LayoutInflater inflater = LayoutInflater.from(MainActivity.this);
        return (FrameLayout) inflater.inflate(R.layout.bot_options_layout, null);
    }

}