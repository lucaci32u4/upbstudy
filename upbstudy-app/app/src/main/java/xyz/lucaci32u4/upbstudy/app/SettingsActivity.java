package xyz.lucaci32u4.upbstudy.app;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.EditText;

import com.google.android.material.snackbar.Snackbar;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        SharedPreferences sp = getSharedPreferences("apiConfig", Context.MODE_PRIVATE);
        ((EditText)findViewById(R.id.edit_url)).setText(sp.getString("apiUrl", "https://your-api.xyz"));
        ((EditText)findViewById(R.id.edit_key)).setText(sp.getString("apiKey", "super-secret-key"));
        findViewById(R.id.edit_cancel).setOnClickListener(v -> finish());
        findViewById(R.id.edit_save).setOnClickListener(v -> {
            save();
            Snackbar.make(v, "Saved", Snackbar.LENGTH_SHORT).show();
            finish();
        });
    }


    private void save() {
        SharedPreferences sp = getSharedPreferences("apiConfig", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString("apiUrl", ((EditText)findViewById(R.id.edit_url)).getText().toString());
        editor.putString("apiKey", ((EditText)findViewById(R.id.edit_key)).getText().toString());
        editor.apply();
    }

}