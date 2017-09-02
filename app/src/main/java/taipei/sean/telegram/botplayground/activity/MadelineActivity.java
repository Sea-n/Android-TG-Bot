package taipei.sean.telegram.botplayground.activity;

import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
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

public class MadelineActivity extends AppCompatActivity {
    final private int _dbVer = 4;
    private SeanDBHelper db;
    private String _token;
    private int _type;
    private PWRTelegramAPI _api;
    private JSONObject apiMethods;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_madeline);

        try {
            Bundle bundle = getIntent().getExtras();
            _token = bundle.getString("token");
            _type = bundle.getInt("type");
        } catch (NullPointerException e) {
            Log.e("ml", "bundle error", e);
            finish();
        }

        db = new SeanDBHelper(this, "data.db", null, _dbVer);

        _api = new PWRTelegramAPI(this, _token, _type);

        final InstantComplete methodView = (InstantComplete) findViewById(R.id.madeline_method);
        final RecyclerView inputList = (RecyclerView) findViewById(R.id.madeline_inputs);
        final Button submitButton = (Button) findViewById(R.id.madeline_submit);


        final ArrayList<String> botApiMethodsList = new ArrayList<String>() {};
        apiMethods = loadMethods();


        Iterator<String> temp = apiMethods.keys();
        while (temp.hasNext()) {
            String key = temp.next();
            botApiMethodsList.add(key);
        }

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

        String method = db.getParam("_method_ml");
        if (apiMethods.has(method))
            methodView.setText(method);

        updateMethod();
    }

    private void updateMethod() {
        final InstantComplete methodView = (InstantComplete) findViewById(R.id.madeline_method);
        final RecyclerView paramView = (RecyclerView) findViewById(R.id.madeline_inputs);
        final String method = methodView.getText().toString();

        JSONObject methodData;
        JSONObject paramData;

        if (!apiMethods.has(method)) {
            methodView.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0);
            ViewGroup.LayoutParams layoutParams = paramView.getLayoutParams();
            layoutParams.height = 0;
            paramView.setLayoutParams(layoutParams);
            return;
        }

        try {
            methodData = apiMethods.getJSONObject(method);
        } catch (JSONException e) {
            Log.e("ml", apiMethods.toString(), e);
            return;
        }

        if (methodData.has("params")) {
            try {
                paramData = methodData.getJSONObject("params");
            } catch (JSONException e) {
                Log.e("ml", "json", e);
                return;
            }
        } else {
            Log.d("ml", "No params: " + method);
            return;
        }

        final ApiCallerAdapter apiCallerAdapter = new ApiCallerAdapter(getApplicationContext());

        try {
            JSONObject wrapValue = new JSONObject();

            Iterator<String> temp = paramData.keys();
            while (temp.hasNext()) {
                String key = temp.next();
                String type = paramData.getString(key);
                wrapValue.put("type", type);
                apiCallerAdapter.addData(key, wrapValue);
            }
        } catch (JSONException e) {
            Log.e("ml", "parse", e);
        }

        paramView.setAdapter(apiCallerAdapter);
        paramView.setLayoutManager(new StaggeredGridLayoutManager(1, StaggeredGridLayoutManager.VERTICAL));
        paramView.setItemViewCacheSize(paramData.length());

        db.updateParam("_method_ml", method);

        paramView.post(new Runnable() {
            @Override
            public void run() {
                apiCallerAdapter.fitView(paramView);
            }
        });
    }

    private void submit() {
        final InstantComplete methodView = (InstantComplete) findViewById(R.id.madeline_method);
        final RecyclerView paramList = (RecyclerView) findViewById(R.id.madeline_inputs);
        final TextView resultView = (TextView) findViewById(R.id.madeline_result);

        String method = methodView.getText().toString();

        JSONObject jsonObject = new JSONObject();
        JSONObject paramObject = new JSONObject();
        try {
            jsonObject.put("method", method);
        } catch (JSONException e) {
            Log.e("ml", "method", e);
            return;
        }

        final ApiCallerAdapter paramAdapter = (ApiCallerAdapter) paramList.getAdapter();
        final int paramHeight = paramList.getHeight();
        if (null == paramAdapter || paramHeight == 0) {
            _api.callApi("madeline", resultView, jsonObject);
            return;
        }

        final int inputCount = paramAdapter.getItemCount();
        for (int i=0; i<inputCount; i++) {
            TextInputLayout textInputLayout = (TextInputLayout) paramAdapter.getViewByPos(i);
            InstantComplete textInputEditText = (InstantComplete) textInputLayout.getEditText();
            if (null == textInputEditText) {
                Log.w("ml", "edit text null");
                continue;
            }
            CharSequence hint = textInputLayout.getHint();
            if (null == hint) {
                Log.w("ml", "hint null");
                continue;
            }
            String name = hint.toString();
            CharSequence valueChar = textInputEditText.getText();
            if (null == valueChar) {
                Log.w("ml", "value char null");
                continue;
            }
            String value = valueChar.toString();

            if (Objects.equals(value, "")) {
                Log.w("ml", "value empty");
                continue;
            }

            try {
                paramObject.put(name, value);
                db.insertFav(name, value, method);
            } catch (JSONException e) {
                Log.e("ml", "json", e);
            }
        }

        try {
            jsonObject.put("params", paramObject);
        } catch (JSONException e) {
            Log.e("ml", "method", e);
            return;
        }

        _api.callApi("madeline", resultView, jsonObject);
    }

    public JSONObject loadMethods() {
        String jsonStr;
        JSONObject json;
        try {
            InputStream is = getAssets().open("madeline-methods.json");

            int size = is.available();
            byte[] buffer = new byte[size];

            if (is.read(buffer) < 0)
                return null;

            is.close();
            jsonStr = new String(buffer, "UTF-8");
        } catch (IOException e) {
            Log.e("ml", "get", e);
            return null;
        }
        try {
            json = new JSONObject(jsonStr);
        } catch (JSONException e) {
            Log.e("ml", "parse", e);
            return null;
        }
        return json;
    }
}
