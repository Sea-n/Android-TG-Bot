package taipei.sean.telegram.botplayground.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Objects;

import taipei.sean.telegram.botplayground.InstantComplete;
import taipei.sean.telegram.botplayground.PWRTelegramAPI;
import taipei.sean.telegram.botplayground.R;
import taipei.sean.telegram.botplayground.SeanAdapter;
import taipei.sean.telegram.botplayground.SeanDBHelper;
import taipei.sean.telegram.botplayground.adapter.ApiCallerAdapter;

public class PWRTelegramActivity extends AppCompatActivity {
    final private int _dbVer = 4;
    private SeanDBHelper db;
    private String _token;
    private int _type;
    private PWRTelegramAPI _api;
    private JSONObject apiMethods;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pwrtelegram);

        ActionBar actionBar = getSupportActionBar();
        if (null != actionBar)
            actionBar.setDisplayHomeAsUpEnabled(true);


        try {
            Bundle bundle = getIntent().getExtras();
            _token = bundle.getString("token");
            _type = bundle.getInt("type");
        } catch (NullPointerException e) {
            Log.e("pwrt", "bundle error", e);
            finish();
        }

        if (_type == 2) {
            Intent mIntent = new Intent(PWRTelegramActivity.this, MadelineActivity.class);
            mIntent.putExtra("token", _token);
            mIntent.putExtra("type", _type);
            startActivity(mIntent);
            finish();
        }

        db = new SeanDBHelper(this, "data.db", null, _dbVer);

        _api = new PWRTelegramAPI(this, _token, _type);

        final InstantComplete methodView = (InstantComplete) findViewById(R.id.pwrtelegram_method);
        final Button submitButton = (Button) findViewById(R.id.pwrtelegram_submit);


        final ArrayList<String> botApiMethodsList = new ArrayList<String>() {};
        apiMethods = loadMethods();


        final SeanAdapter<String> adapter = new SeanAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, botApiMethodsList);
        methodView.setAdapter(adapter);

        methodView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void afterTextChanged(Editable editable) {
                updateMethod();
            }
        });

        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                submit();
            }
        });

        String method = db.getParam("_method_pwrt");
        if (apiMethods.has(method))
            methodView.setText(method);
        updateMethod();
    }

    private void updateMethod() {
        final InstantComplete methodView = (InstantComplete) findViewById(R.id.pwrtelegram_method);
        final RecyclerView paramList = (RecyclerView) findViewById(R.id.pwrtelegram_inputs);
        final String method = methodView.getText().toString();

        JSONObject methodData;
        JSONObject paramData;

        if (!apiMethods.has(method)) {
            methodView.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0);
            ViewGroup.LayoutParams layoutParams = paramList.getLayoutParams();
            layoutParams.height = 0;
            paramList.setLayoutParams(layoutParams);
            return;
        }

        try {
            methodData = apiMethods.getJSONObject(method);
        } catch (JSONException e) {
            Log.e("pwrt", apiMethods.toString(), e);
            return;
        }

        if (methodData.has("description")) {
            try {
                final String desc = methodData.getString("description");
                methodView.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View view) {
                        Snackbar.make(view, desc, Snackbar.LENGTH_LONG).show();
                        return false;
                    }
                });
            } catch (JSONException e) {
                Log.e("pwrt", "method description", e);
            }
        }

        if (methodData.has("params"))
            try {
                paramData = methodData.getJSONObject("params");
            } catch (JSONException e) {
                Log.e("pwrt", "method description", e);
                return;
            }
        else {
            Log.e("pwrt", "No params: " + method);
            return;
        }

        final ApiCallerAdapter apiCallerAdapter = new ApiCallerAdapter(getApplicationContext());

        try {
            Iterator<String> temp = paramData.keys();
            while (temp.hasNext()) {
                String key = temp.next();
                JSONObject value = paramData.getJSONObject(key);
                apiCallerAdapter.addData(key, value);
            }
        } catch (JSONException e) {
            Log.e("pwrt", "parse", e);
        }

        paramList.setAdapter(apiCallerAdapter);
        paramList.setLayoutManager(new StaggeredGridLayoutManager(1, StaggeredGridLayoutManager.VERTICAL));
        paramList.setItemViewCacheSize(paramData.length());

        db.updateParam("_method_pwrt", method);

        paramList.post(new Runnable() {
            @Override
            public void run() {
                apiCallerAdapter.fitView(paramList);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.pwrtelegram, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.pwrt_menu_madeline:
                Intent mIntent = new Intent(PWRTelegramActivity.this, MadelineActivity.class);
                mIntent.putExtra("token", _token);
                mIntent.putExtra("type", _type);
                startActivity(mIntent);
                break;
            case R.id.pwrt_menu_info:
                Uri infoUri = Uri.parse("https://t.me/PWRTelegram");
                Intent iIntent = new Intent(Intent.ACTION_VIEW, infoUri);
                startActivity(iIntent);
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void submit() {
        final InstantComplete methodView = (InstantComplete) findViewById(R.id.pwrtelegram_method);
        final RecyclerView paramList = (RecyclerView) findViewById(R.id.pwrtelegram_inputs);
        final TextView resultView = (TextView) findViewById(R.id.pwrtelegram_result);

        String method = methodView.getText().toString();

        JSONObject jsonObject = new JSONObject();

        final ApiCallerAdapter paramAdapter = (ApiCallerAdapter) paramList.getAdapter();
        final int paramHeight = paramList.getHeight();
        if (null == paramAdapter || paramHeight == 0) {
            _api.callApi(method, resultView, jsonObject);
            return;
        }

        final int inputCount = paramAdapter.getItemCount();
        for (int i=0; i<inputCount; i++) {
            TextInputLayout textInputLayout = (TextInputLayout) paramAdapter.getViewByPos(i);
            InstantComplete textInputEditText = (InstantComplete) textInputLayout.getEditText();
            if (null == textInputEditText) {
                Log.w("pwrt", "edit text null");
                continue;
            }
            CharSequence hint = textInputLayout.getHint();
            if (null == hint) {
                Log.w("pwrt", "hint null");
                continue;
            }
            String name = hint.toString();
            CharSequence valueChar = textInputEditText.getText();
            if (null == valueChar) {
                Log.w("pwrt", "value char null");
                continue;
            }
            String value = valueChar.toString();

            if (Objects.equals(value, "")) {
                Log.w("pwrt", "value empty");
                continue;
            }

            try {
                jsonObject.put(name, value);
                db.insertFav(name, value, method);
            } catch (JSONException e) {
                Log.e("pwrt", "json", e);
            }
        }

        _api.callApi(method, resultView, jsonObject);
    }

    public JSONObject loadMethods() {
        final String lang = getString(R.string.lang_code);
        String jsonStr;   // Temporary variable for read JSON file
        JSONObject json;

        /* Begin of read PWRTelegram Methods file */
        try {
            InputStream is = getAssets().open("pwrtelegram-methods.json");

            int size = is.available();
            byte[] buffer = new byte[size];

            if (is.read(buffer) < 0)
                return null;

            is.close();
            jsonStr = new String(buffer, "UTF-8");
        } catch (IOException e) {
            Log.e("pwrt", "get", e);
            return null;
        }

        try {
            json = new JSONObject(jsonStr);
        } catch (JSONException e) {
            Log.e("pwrt", "parse", e);
            return null;
        }
        /* End of read PWRTelegram Methods file */

        /* Begin of read Telegram Methods file */
        try {
            InputStream is = getAssets().open("api-methods.json");

            int size = is.available();
            byte[] buffer = new byte[size];

            if (is.read(buffer) < 0)
                return null;

            is.close();
            jsonStr = new String(buffer, "UTF-8");
        } catch (IOException e) {
            Log.e("pwrt", "get", e);
            return null;
        }

        try {
            JSONObject originalMethods = new JSONObject(jsonStr);
            Iterator<String> methods = originalMethods.keys();
            while (methods.hasNext()) {
                String methodName = methods.next();
                if (!json.has(methodName)) {   // if PWRTelegram Methods list does not exist this method
                    JSONObject method = originalMethods.getJSONObject(methodName);
                    json.put(methodName, method);   // Add to list
                }
            }
        } catch (JSONException e) {
            Log.e("pwrt", "parse", e);
            return null;
        }
        /* End of read Telegram Methods file */

        /* Begin of read Telegram Description l10n file */
        try {
            InputStream is = getAssets().open("api-methods-" + lang + ".json");

            int size = is.available();
            byte[] buffer = new byte[size];

            if (is.read(buffer) < 0)
                return null;

            is.close();
            jsonStr = new String(buffer, "UTF-8");
        } catch (IOException e) {
            Log.e("pwrt", "get locale", e);
            return null;
        }

        try {
            JSONObject localeJson = new JSONObject(jsonStr);

            Iterator<String> methods = localeJson.keys();
            while (methods.hasNext()) {
                String methodName = methods.next();
                JSONObject method = json.getJSONObject(methodName);
                JSONObject localeMethod = localeJson.getJSONObject(methodName);

                if (localeMethod.has("description")) {
                    String methodDesc = localeMethod.getString("description");
                    method.put("description", methodDesc);
                }

                if (localeMethod.has("params")) {
                    JSONObject params = method.getJSONObject("params");
                    JSONObject localeParams = localeMethod.getJSONObject("params");

                    Iterator<String> paramNames = localeParams.keys();
                    while (paramNames.hasNext()) {
                        String paramName = paramNames.next();
                        JSONObject param = params.getJSONObject(paramName);
                        String localeParam = localeParams.getString(paramName);

                        param.put("description", localeParam);
                    }
                }
            }
        } catch (JSONException e) {
            Log.e("pwrt", "parse locale", e);
            return null;
        }
        /* End of read Telegram Description l10n file */

        /* Begin of read PWRTelegram Description l10n file (Over-write original description) */
        try {
            InputStream is = getAssets().open("pwrtelegram-methods-" + lang + ".json");

            int size = is.available();
            byte[] buffer = new byte[size];

            if (is.read(buffer) < 0)
                return null;

            is.close();
            jsonStr = new String(buffer, "UTF-8");
        } catch (IOException e) {
            Log.e("pwrt", "get locale", e);
            return null;
        }

        try {
            JSONObject localeJson = new JSONObject(jsonStr);

            Iterator<String> methods = localeJson.keys();
            while (methods.hasNext()) {
                String methodName = methods.next();
                JSONObject method = json.getJSONObject(methodName);
                JSONObject localeMethod = localeJson.getJSONObject(methodName);

                if (localeMethod.has("description")) {
                    String methodDesc = localeMethod.getString("description");
                    method.put("description", methodDesc);
                }

                if (localeMethod.has("params")) {
                    JSONObject params = method.getJSONObject("params");
                    JSONObject localeParams = localeMethod.getJSONObject("params");

                    Iterator<String> paramNames = localeParams.keys();
                    while (paramNames.hasNext()) {
                        String paramName = paramNames.next();
                        JSONObject param = params.getJSONObject(paramName);
                        String localeParam = localeParams.getString(paramName);

                        param.put("description", localeParam);
                    }
                }
            }
        } catch (JSONException e) {
            Log.e("pwrt", "parse locale", e);
            return null;
        }
        /* End of read PWRTelegram Description l10n file */

        return json;
    }
}
