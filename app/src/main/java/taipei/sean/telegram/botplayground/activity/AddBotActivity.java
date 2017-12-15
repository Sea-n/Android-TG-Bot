package taipei.sean.telegram.botplayground.activity;

import android.content.ContentValues;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import taipei.sean.telegram.botplayground.BotStructure;
import taipei.sean.telegram.botplayground.R;
import taipei.sean.telegram.botplayground.SeanDBHelper;

public class AddBotActivity extends AppCompatActivity {
    final private int _dbVer = 4;
    private TextInputEditText tokenView;
    private TextInputEditText nameView;
    private SeanDBHelper db;
    private long _id = -1;

    public AddBotActivity() {
        db = new SeanDBHelper(this, "data.db", null, _dbVer);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_bot);
        setupActionBar();

        tokenView = (TextInputEditText) findViewById(R.id.add_bot_token);
        nameView = (TextInputEditText) findViewById(R.id.add_bot_name);

        Bundle bundle = getIntent().getExtras();
        if ((bundle != null) && bundle.containsKey("id")) {
            _id = bundle.getLong("id");
            Log.d("add", "start restore data");
            restoreData();
        }

        Button submitButton = (Button) findViewById(R.id.add_bot_submit);
        submitButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                addBot();
            }
        });

        tokenView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if (!b)
                    formatToken();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.add_bot, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id) {
            case R.id.action_done:
                addBot();
                break;

            default:
                Log.w("option", "Press unknown " + id);
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void setupActionBar() {
        // Show the Up button in the action bar.
        ActionBar actionBar = getSupportActionBar();
        if (null != actionBar)
            actionBar.setDisplayHomeAsUpEnabled(true);
    }

    private void restoreData() {
        BotStructure bot = db.getBot(_id);
        Log.d("add", "bot" + bot);
        nameView.setText(bot.name);
        tokenView.setText(bot.token);
    }

    private void formatToken() {
        String rawToken = tokenView.getText().toString();
        String tokenRegex = ".*?(" + getString(R.string.bot_token_regex) + ").*";
        Pattern tokenPattern = Pattern.compile(tokenRegex, Pattern.DOTALL);
        Matcher tokenMatcher = tokenPattern.matcher(rawToken);
        if (tokenMatcher.matches()) {
            String token = tokenMatcher.group(1);
            tokenView.setText(token);
        } else {
            tokenView.setError(getString(R.string.add_bot_token_invalid));
        }

    }

    private void addBot() {
        formatToken();

        String token = tokenView.getText().toString();
        String name = nameView.getText().toString();
        int type = 0;

        boolean cancel = false;
        View focusView = null;

        if (TextUtils.isEmpty(name)) {
            nameView.setError(getString(R.string.add_bot_name_invalid));
            focusView = nameView;
            cancel = true;
        }

        String tokenRegex = ".*?(" + getString(R.string.bot_token_regex) + ").*";
        Pattern tokenPattern = Pattern.compile(tokenRegex, Pattern.DOTALL);
        Matcher tokenMatcher = tokenPattern.matcher(token);
        if (!tokenMatcher.matches()) {
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
        values.put("type", type);
        if (_id > 0)
            db.updateBot(_id, values);
        else
            _id = db.insertBot(values);


        SharedPreferences settings = getSharedPreferences("data", MODE_PRIVATE);
        settings.edit()
                .putLong("currentBotId", _id)
                .apply();

        Log.d("add", "inserted bot" + _id);
        finish();
    }
}