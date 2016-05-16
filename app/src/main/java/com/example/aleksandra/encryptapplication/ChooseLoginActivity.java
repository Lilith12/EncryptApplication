package com.example.aleksandra.encryptapplication;

import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.content.Intent;
import android.widget.Button;
import android.widget.EditText;

public class ChooseLoginActivity extends AppCompatActivity implements View.OnClickListener {
    EditText nicknameField;
    public static String nickname="";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_login);
        getWindow().getDecorView().setBackgroundColor(Color.rgb(51, 51, 55));
        nicknameField = (EditText) this.findViewById(R.id.nicknameEdit);
        nicknameField.setTextColor(Color.rgb(255, 255, 255));
        Button button = (Button) this.findViewById(R.id.welcomeButton);
        button.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        nickname = nicknameField.getText().toString();
        Intent intent = new Intent(this, ServerStatsActivity.class);
        intent.putExtra("nick", nickname);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }
}
