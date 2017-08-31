package com.example.aleksandra.encryptapplication;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.content.Intent;
import android.widget.Button;
import android.widget.EditText;

import org.json.JSONException;
import org.json.JSONObject;

import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class ChooseLoginActivity extends AppCompatActivity implements View.OnClickListener {
    EditText nicknameField;
    public static String nickname="";
    final public static String setNickname = "set username";
    String jsonArrayObject;

    private Socket mSocket;

    private void attemptLogin() {
        // Reset errors.
        nicknameField.setError(null);

        // Store values at the time of the login attempt.
        String username = nicknameField.getText().toString().trim();

        // Check for a valid username.
        if (TextUtils.isEmpty(username)) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            nicknameField.setError("To pole jest wymagane!");
            nicknameField.requestFocus();
            return;
        }

        // perform the user login attempt.
        mSocket.emit(setNickname, username);
    }

    private Emitter.Listener onLogin = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            EncryptAppSocket app = (EncryptAppSocket) getApplication();
            app.setUsername(nickname);
            Intent intent = new Intent(ChooseLoginActivity.this, ServerStatsActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        }
    };

    private Emitter.Listener usernameUsed = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    if (!isFinishing()) {
                        new AlertDialog.Builder(new ContextThemeWrapper(ChooseLoginActivity.this, android.R.style.Theme_DeviceDefault_Dialog_NoActionBar))
                                .setMessage("Nazwa jest już zajęta. Wybierz inną.")
                                .setCancelable(false)
                                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        // Whatever...
                                    }
                                }).create().show();
                    }
                }
            });
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_login);
        getWindow().getDecorView().setBackgroundColor(Color.rgb(51, 51, 55));
        EncryptAppSocket app = (EncryptAppSocket) getApplication();
        mSocket = app.getSocket();
        nicknameField = (EditText) this.findViewById(R.id.nicknameEdit);
        nicknameField.setTextColor(Color.rgb(255, 255, 255));
        mSocket.connect();
        Button button = (Button) this.findViewById(R.id.welcomeButton);
        button.setOnClickListener(this);
        mSocket.on("arrayOfUsers", onLogin);
        mSocket.on("usernameUsed", usernameUsed);
    }

    @Override
    public void onClick(View v) {
        attemptLogin();
    }
}
