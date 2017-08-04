package taipei.sean.telegram.botplayground;

import android.content.ContentValues;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AddBotActivity extends AppCompatActivity {
    private TextInputEditText tokenView;
    private TextInputEditText nameView;
    private SeanDBHelper db;

    public AddBotActivity() {
        db = new SeanDBHelper(this, "data.db", null, 1);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_bot);
        setupActionBar();

        tokenView = (TextInputEditText) findViewById(R.id.add_bot_token);
        nameView = (TextInputEditText) findViewById(R.id.add_bot_name);

        Button submitButton = (Button) findViewById(R.id.add_bot_submit);
        submitButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                addBot();
            }
        });
    }


    private void setupActionBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            // Show the Up button in the action bar.
            ActionBar actionBar = getSupportActionBar();
            if (null != actionBar)
                actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    private void addBot() {

        // Store values at the time of the login attempt.
        String token = tokenView.getText().toString();
        String name = nameView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        if (TextUtils.isEmpty(name) || !isNameValid(name)) {
            nameView.setError(getString(R.string.add_bot_name_invalid));
            focusView = nameView;
            cancel = true;
        }

        if (TextUtils.isEmpty(token) || !isTokenValid(token)) {
            tokenView.setError(getString(R.string.add_bot_token_invalid));
            focusView = tokenView;
            cancel = true;
        }

        if (cancel) {
            focusView.requestFocus();
            return;
        }

        ContentValues values = new ContentValues();
        values.put("token", token);
        values.put("name", name);
        long id = db.insertBot(values);
        Log.d("add", "inserted bot" + id);
        finish();
    }

    private boolean isTokenValid(String token) {
        String regex = getString(R.string.bot_token_regex);
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(token);
        return matcher.matches();
    }

    private boolean isNameValid(String name) {
        return name.length() > 0;
    }
}