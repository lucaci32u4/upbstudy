package xyz.lucaci32u4.upbstudy.app;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.TextView;

public class ErrorActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_error);
        String error = getIntent().getStringExtra("error");
        ((TextView)findViewById(R.id.show_error)).setText(error == null ? "" : error);
    }
}