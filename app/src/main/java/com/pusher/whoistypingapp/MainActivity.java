package com.pusher.whoistypingapp;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.google.gson.Gson;
import com.pusher.client.Pusher;
import com.pusher.client.channel.Channel;
import com.pusher.client.channel.SubscriptionEventListener;

import java.io.IOException;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private static final String USER_TYPING_ENDPOINT = "https://{NODE_JS_SERVER_ENDPOINT}/userTyping";
    private static final String PUSHER_API_KEY = "PUSHER_API_KEY";
    private static final String CHANNEL_NAME = "anonymous_chat";
    private static final String USER_TYPING_EVENT = "user_typing";

    Pusher pusher = new Pusher(PUSHER_API_KEY);
    OkHttpClient httpClient = new OkHttpClient();

    EditText messageEditText;
    Button sendButton;
    TimerTask clearTimerTask;
    Timer clearTimer;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextWatcher messageInputTextWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                Log.d("User Input Change", charSequence.toString());
                Request userIsTypingRequest = new Request.Builder()
                        .url(USER_TYPING_ENDPOINT)
                        .post(new FormBody.Builder()
                                .add("username", getUsername())
                                .build())
                        .build();

                httpClient.newCall(userIsTypingRequest)
                        .enqueue(new Callback() {
                            @Override
                            public void onFailure(Call call, IOException e) {
                                Log.d("Post Response", e.toString());
                            }

                            @Override
                            public void onResponse(Call call, Response response) throws IOException {
                                Log.d("Post Response", response.toString());
                            }
                        });
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        };

        messageEditText = (EditText)findViewById(R.id.messageEditText);
        messageEditText.addTextChangedListener(messageInputTextWatcher);

        SubscriptionEventListener isTypingEventListener = new SubscriptionEventListener() {
            @Override
            public void onEvent(String channel, String event, String data) {
                final WhosTyping whosTyping = new Gson().fromJson(data, WhosTyping.class);
//                if(!whosTyping.username.equals(getUsername())) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            getSupportActionBar().setSubtitle(whosTyping.username + " is typing...");
                        }
                    });
//                }

                //reset timer
                if(clearTimer != null) {
                    clearTimer.cancel();
                }
                startClearTimer();
            }
        };

        Channel pusherChannel = pusher.subscribe(CHANNEL_NAME);
        pusherChannel.bind(USER_TYPING_EVENT, isTypingEventListener);


        sendButton = (Button)findViewById(R.id.sendButton);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //send message to server and update view
            }
        });
    }

    /**
     * Starts the who's typing clear timer.
     *
     */
    private void startClearTimer() {
        clearTimerTask = new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        getSupportActionBar().setSubtitle("");
                    }
                });

            }
        };
        clearTimer = new Timer();
        long interval = 900; //0.9 seconds
        clearTimer.schedule(clearTimerTask, interval);
    }

    @Override
    protected void onResume() {
        super.onResume();
        pusher.connect();
    }

    @Override
    protected void onPause() {
        pusher.disconnect();
        super.onPause();
    }

    private String getUsername() {
        return "sweet_kitten-"+ new Random().nextInt(1000);
    }
}
