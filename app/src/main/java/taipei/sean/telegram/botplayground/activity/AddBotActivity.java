package taipei.sean.telegram.botplayground.activity;

import android.content.ContentValues;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
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

import org.json.JSONObject;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import taipei.sean.telegram.botplayground.BotStructure;
import taipei.sean.telegram.botplayground.R;
import taipei.sean.telegram.botplayground.SeanDBHelper;

public class AddBotActivity extends AppCompatActivity {
    final private int _dbVer = 4;
    private TextInputEditText tokenView;
    private TextInputEditText nicknameView;
    private TextInputEditText infoView;
    private SeanDBHelper db;
    private long _id = -1;
    private String _token;
    private String username;
    private int userId;

    public AddBotActivity() {
        db = new SeanDBHelper(this, "data.db", null, _dbVer);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_bot);
        setupActionBar();

        tokenView = (TextInputEditText) findViewById(R.id.add_bot_token);
        nicknameView = (TextInputEditText) findViewById(R.id.add_bot_nickname);
        infoView = (TextInputEditText) findViewById(R.id.add_bot_info);

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
                    updateToken();
            }
        });

        updateToken();
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
        nicknameView.setText(bot.name);
        tokenView.setText(bot.token);
    }

    private void updateToken() {
        String rawToken = tokenView.getText().toString();
        if (rawToken.isEmpty())
            return;
        String tokenRegex = ".*?(" + getString(R.string.bot_token_regex) + ").*";
        Pattern tokenPattern = Pattern.compile(tokenRegex, Pattern.DOTALL);
        Matcher tokenMatcher = tokenPattern.matcher(rawToken);
        if (!tokenMatcher.matches()) {
            tokenView.setError(getString(R.string.add_bot_token_invalid));
            return;
        }

        _token = tokenMatcher.group(1);
        tokenView.setText(_token);

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                final Handler handler = new Handler(getMainLooper());

                try {
                    final String url = String.format("https://api.telegram.org/bot%s/getMe", _token);
                    Request request = new Request.Builder()
                            .url(url)
                            .build();
                    OkHttpClient client = new OkHttpClient();
                    Response resp = client.newCall(request).execute();
                    String respStr = resp.body().string();
                    JSONObject json = new JSONObject(respStr).getJSONObject("result");
                    final String firstname = json.getString("first_name");
                    username = json.getString("username");
                    userId = json.getInt("id");
                    final String detail = String.format("%s (@%s)", firstname, username);
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            infoView.setText(detail);
                        }
                    });
                } catch (final Exception e) {
                    Log.e("add", "err", e);
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            infoView.setText(R.string.token_unauthorized);
                            tokenView.setError(getString(R.string.token_unauthorized));
                        }
                    });
                }
            }
        });
        thread.start();
    }

    private void addBot() {
        updateToken();

        _token = tokenView.getText().toString();
        String name = nicknameView.getText().toString();
        int type = 0;

        boolean cancel = false;
        View focusView = null;

        if (TextUtils.isEmpty(name)) {
            nicknameView.setError(getString(R.string.add_bot_name_invalid));
            focusView = nicknameView;
            cancel = true;
        }

        String tokenRegex = ".*?(" + getString(R.string.bot_token_regex) + ").*";
        Pattern tokenPattern = Pattern.compile(tokenRegex, Pattern.DOTALL);
        Matcher tokenMatcher = tokenPattern.matcher(_token);
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
        values.put("token", _token);
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