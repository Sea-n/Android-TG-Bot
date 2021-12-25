package taipei.sean.telegram.botplayground.activity;

import android.os.Bundle;
import android.os.Handler;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.webkit.URLUtil;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import taipei.sean.telegram.botplayground.FavStructure;
import taipei.sean.telegram.botplayground.InstantComplete;
import taipei.sean.telegram.botplayground.R;
import taipei.sean.telegram.botplayground.SeanAdapter;
import taipei.sean.telegram.botplayground.SeanDBHelper;

public class WebhookActivity extends AppCompatActivity {
    final private int _dbVer = 4;
    private SeanDBHelper db;
    private JSONObject payloads;
    private ArrayList<JSONObject> payloadArray;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webhook);

        db = new SeanDBHelper(this, "data.db", null, _dbVer);

        final InstantComplete urlView = (InstantComplete) findViewById(R.id.webhook_url);
        final Button submitButton = (Button) findViewById(R.id.webhook_submit);


        List<FavStructure> urls = db.getFavs("url");
        ArrayList<String> urlList = new ArrayList<>();
        for (int i = 0; i < urls.size(); i++)
            urlList.add(urls.get(i).value);
        SeanAdapter<String> urlAdapter = new SeanAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, urlList);
        urlView.setAdapter(urlAdapter);

        urlView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                String url = editable.toString();
                db.updateParam("url", url);
            }
        });

        String url = db.getParam("url");
        if (!URLUtil.isValidUrl(url))
            url = "https://httpbin.org/post";

        urlView.setText(url);

        final String defaultPayloadStr = "{'default':{'update_id':10000,'message':{'date':1441645532,'chat':{'last_name':'Test Lastname','id':1111111,'first_name':'Test','username':'Test'},'message_id':1365,'from':{'last_name':'Test Lastname','id':1111111,'first_name':'Test','username':'Test'},'text':'/start'}}}".replace("'", "\"");
        try {
            payloads = new JSONObject(defaultPayloadStr);
            initPayloadList();
        } catch (JSONException e) {
            Log.e("rc", "default json", e);
        }

        final FirebaseRemoteConfig remoteConfig = FirebaseRemoteConfig.getInstance();
        remoteConfig.fetch(69).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                remoteConfig.fetchAndActivate();
                String payloadsStr = remoteConfig.getString("webhook_debug_payloads");
                if (payloadsStr.isEmpty()) {
                    Log.e("rc", "no payloads");
                    return;
                }

                try {
                    payloads = new JSONObject(payloadsStr);
                    initPayloadList();
                } catch (JSONException e) {
                    Log.e("rc", "json", e);
                }
            }
        });

        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                submit();
            }
        });
    }

    private void initPayloadList() {
        final Spinner payloadSpinner = (Spinner) findViewById(R.id.webhook_payload);
        ArrayList<String> payloadNameArray = new ArrayList<>();
        payloadArray = new ArrayList<>();

        try {
            Iterator<String> names = payloads.keys();
            while (names.hasNext()) {
                String payloadName = names.next();
                JSONObject payload = payloads.getJSONObject(payloadName);
                payloadArray.add(payload);
                payloadNameArray.add(payloadName);
            }
        } catch (JSONException e) {
            Log.e("caller", "parse locale", e);
        }

        ArrayAdapter<String> payloadAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, payloadNameArray);
        payloadSpinner.setAdapter(payloadAdapter);

    }

    private void submit() {
        final InstantComplete urlView = (InstantComplete) findViewById(R.id.webhook_url);
        final Spinner payloadSpinner = (Spinner) findViewById(R.id.webhook_payload);
        final TextView resultView = (TextView) findViewById(R.id.webhook_result);

        final String url = urlView.getText().toString();

        final int pos = payloadSpinner.getSelectedItemPosition();
        final String payloadName = (String) payloadSpinner.getSelectedItem();
        final JSONObject payload = payloadArray.get(pos);
        final String payloadStr = payload.toString();

        db.insertFav("url", url, payloadName);

        resultView.setText(payloadStr);
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
                    RequestBody requestBody = RequestBody.create(JSON, payloadStr);
                    Request request = new Request.Builder()
                            .url(url)
                            .post(requestBody)
                            .header("User-Agent", "Awesome Telegram Bot (+https://tg.sean.taipei)")
                            .build();
                    OkHttpClient client = new OkHttpClient();
                    Response resp = client.newCall(request).execute();
                    final String respStr = resp.body().string();

                    Handler handler = new Handler(getMainLooper());
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            resultView.setText(respStr);
                        }
                    });
                } catch (final Exception e) {
                    Log.e("webhook", "share", e);
                    Handler handler = new Handler(getMainLooper());
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            final String errorMsg = e.getLocalizedMessage();
                            resultView.setText(errorMsg);
                        }
                    });
                }
            }
        });
        thread.start();
    }
}
