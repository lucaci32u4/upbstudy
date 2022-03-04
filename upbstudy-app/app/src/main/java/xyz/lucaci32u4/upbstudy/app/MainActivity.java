package xyz.lucaci32u4.upbstudy.app;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.os.Handler;
import android.os.Looper;
import android.view.View;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.DatePicker;
import android.widget.EditText;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        DatePicker datePicker = findViewById(R.id.date_pickker);
        Calendar calendar = Calendar.getInstance();
        datePicker.init(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH), (view, year, monthOfYear, dayOfMonth) -> {
            // nothing.
        });

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(view -> {
            SharedPreferences sp = getSharedPreferences("apiConfig", Context.MODE_PRIVATE);
            String apiKey = sp.getString("apiKey", "");
            String apiUrl = sp.getString("apiUrl", "");
            if (apiKey.isEmpty() || apiUrl.isEmpty()) {
                Snackbar.make(view, "Please fill in API details in settings page", Snackbar.LENGTH_LONG).show();
                return;
            }
            String to = ((EditText)findViewById(R.id.hour_to)).getText().toString();
            String from = ((EditText)findViewById(R.id.hour_from)).getText().toString();
            if (to.isEmpty() || from.isEmpty()) {
                Snackbar.make(view, "Please fill arrive and leave time", Snackbar.LENGTH_LONG).show();
                return;
            }
            String reservationText = String.format("%02d.%02d.%d %s-%s", datePicker.getDayOfMonth(), datePicker.getMonth(), datePicker.getYear(), to, from);
            String load;
            try {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("dateTime", reservationText);
                jsonObject.put("apiKey", apiKey);
                load = jsonObject.toString();
            } catch (JSONException e) {
                e.printStackTrace();
                Snackbar.make(view, "JSON Error", Snackbar.LENGTH_LONG).show();
                return;
            }

            new AlertDialog.Builder(this)
                    .setTitle("Please check dates")
                    .setMessage(reservationText)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("OK", (dialog, which) -> {
                        ProgressDialog loading = ProgressDialog.show(this, "Sending", "Please wait...", true);
                        new Thread(() -> {
                            String answer = callBackend(apiUrl, load);
                            new Handler(Looper.getMainLooper()).post(() -> {
                                clearSelections();
                                loading.dismiss();
                                if (answer == null){
                                    Snackbar.make(view, "Reservartion send", Snackbar.LENGTH_LONG).show();
                                    finish();
                                } else {
                                    Intent intent = new Intent(this, ErrorActivity.class);
                                    intent.putExtra("error", answer);
                                    startActivity(intent);
                                }
                            });
                        }).start();
                    }).show();
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    private void clearSelections() {
        ((EditText)findViewById(R.id.hour_to)).setText("");
        ((EditText)findViewById(R.id.hour_from)).setText("");
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private static String callBackend(String url, String json) {
        OkHttpClient client = new OkHttpClient.Builder()
                .callTimeout(20, TimeUnit.SECONDS)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .header("Content-Type", "application/json")
                .post(RequestBody.create(json, MediaType.parse("application/json; charset=utf-8")))
                .build();
        try {
            Response response = client.newCall(request).execute();
            int code = response.code();
            if (code != 200) {
                ResponseBody rb = response.body();
                return "Http error. Got code " + code + (rb == null ? "." : " with body:\n" + rb.string());
            }
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            sw.append("Network error:\n");
            e.printStackTrace(new PrintWriter(sw));
            return sw.toString();
        }
        return null;
    }

}