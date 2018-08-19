package com.example.aleksandra.encryptapplication;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Base64;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.example.aleksandra.encryptapplication.encrypt.RSA;
import com.example.aleksandra.encryptapplication.handlers.DatabaseHandler;

import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class ChooseLoginActivity extends AppCompatActivity implements View.OnClickListener {
    EditText nicknameField;
    private static String username = "";
    private static final String setNickname = "set username";
    private Socket mSocket;

    private void attemptLogin() {
        nicknameField.setError(null);

        username = nicknameField.getText().toString().trim();

        if (TextUtils.isEmpty(username)) {
            nicknameField.setError(getString(R.string.field_error));
            nicknameField.requestFocus();
            return;
        }

        // perform the user login attempt.
        mSocket.emit(setNickname, username,
                Base64.encodeToString(RSA.getRSAInstance().getPublicKey().getEncoded(),
                        Base64.DEFAULT));
    }

    private Emitter.Listener onLogin = args -> {
        deleteConversationHistory();
        EncryptAppSocket app = (EncryptAppSocket) getApplication();
        app.setUsername(username);
        Intent intent = new Intent(ChooseLoginActivity.this, ServerStatsActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    };

    private Emitter.Listener usernameUsed = args -> runOnUiThread(() ->
            new AlertDialog.Builder(new ContextThemeWrapper(ChooseLoginActivity.this,
                    android.R.style.Theme_DeviceDefault_Dialog_NoActionBar))
                    .setMessage(getString(R.string.username_not_available))
                    .setCancelable(false)
                    .setPositiveButton(getString(R.string.ok), (dialog, which) -> {}).create().show());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_login);
        getWindow().getDecorView().setBackgroundColor(Color.rgb(51, 51, 55));
        EncryptAppSocket app = (EncryptAppSocket) getApplication();
        mSocket = app.getSocket();
        nicknameField = this.findViewById(R.id.nicknameEdit);
        nicknameField.setTextColor(Color.rgb(255, 255, 255));
        mSocket.connect();
        Button button = this.findViewById(R.id.welcomeButton);
        button.setOnClickListener(this);
        mSocket.on("arrayOfUsers", onLogin);
        mSocket.on("usernameUsed", usernameUsed);
    }

    private void deleteConversationHistory() {
        DatabaseHandler databaseHandler = DatabaseHandler.getDatabaseHandler(this);
        databaseHandler.dropTable();
        databaseHandler.recreateTable();
    }

    @Override
    public void onClick(View v) {
        attemptLogin();
    }
}
