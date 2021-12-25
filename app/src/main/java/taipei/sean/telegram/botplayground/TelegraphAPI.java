package taipei.sean.telegram.botplayground;

import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.os.Handler;
import androidx.annotation.Nullable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.SpannedString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Iterator;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class TelegraphAPI {
    final private int _dbVer = 4;
    final private String _apiBaseUrl = "https://api.telegra.ph/";
    final private Context _context;
    public JSONObject latestResponse;
    private SeanDBHelper db;

    public TelegraphAPI(Context context) {
        this._context = context;
    }

    public void callApi(final String method, final TextView resultView, @Nullable JSONObject j) {
        if (null == j)
            j = new JSONObject();

        final String json = j.toString();

        db = new SeanDBHelper(_context, "data.db", null, _dbVer);

        Log.d("api", method + json);

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonParser jp = new JsonParser();
        JsonElement je = jp.parse(json);
        String prettyJson = gson.toJson(je);
        SpannableStringBuilder jsonSpannable = new SpannableStringBuilder(prettyJson);
        TelegramAPI.jsonColor(jsonSpannable);

        SpannableString methodSpannable = new SpannableString(method);
        methodSpannable.setSpan(new ForegroundColorSpan(Color.MAGENTA), 0, method.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        SpannedString resultText = (SpannedString) TextUtils.concat(methodSpannable, jsonSpannable);
        resultView.setText(resultText);

        String url = _apiBaseUrl + "/" + method + "?";

        try {
            Iterator<String> temp = j.keys();
            while (temp.hasNext()) {
                String key = temp.next();
                String value = j.get(key).toString();
                url += Uri.encode(key) + "=" + Uri.encode(value) + "&";
            }
        } catch (JSONException e) {
            Log.e("api", "parse", e);
        }

        final String finalUrl = url;
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                String respStr = "";
                String resultText = "";
                try {
                    Request request = new Request.Builder()
                            .url(finalUrl)
                            .build();
                    OkHttpClient client = new OkHttpClient();
                    Response resp = client.newCall(request).execute();
                    respStr = resp.body().string();
                } catch (final MalformedURLException e) {
                    Log.e("api", "Malformed URL", e);
                    final String finalResultText = e.getLocalizedMessage();
                    Handler handler = new Handler(_context.getMainLooper());
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            resultView.setText(finalResultText);
                        }
                    });
                } catch (final IOException e) {
                    Log.e("api", "IO", e);
                    final String finalResultText = e.getLocalizedMessage();
                    Handler handler = new Handler(_context.getMainLooper());
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            resultView.setText(finalResultText);
                        }
                    });
                } catch (final NullPointerException e) {
                    Log.e("api", "Null Pointer", e);
                    final String finalResultText = e.getLocalizedMessage();
                    Handler handler = new Handler(_context.getMainLooper());
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            resultView.setText(finalResultText);
                        }
                    });
                }

                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                JsonParser jp = new JsonParser();
                JsonElement je;
                String json = "";
                try {
                    je = jp.parse(respStr);
                    json = gson.toJson(je);
                } catch (JsonSyntaxException e) {
                    Log.e("api", "parse", e);
                    final String finalResultText = respStr;
                    Handler handler = new Handler(_context.getMainLooper());
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            resultView.setText(finalResultText);
                        }
                    });
                }
                Log.v("api", "resp:" + json);

                final SpannableStringBuilder jsonSpannable = new SpannableStringBuilder(json);
                TelegramAPI.jsonColor(jsonSpannable);
                Handler handler = new Handler(_context.getMainLooper());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        resultView.setText(jsonSpannable);
                    }
                });

                try {
                    latestResponse = new JSONObject(json);
                    boolean status = latestResponse.getBoolean("ok");
                    if (status) {
                        JSONObject result = latestResponse.getJSONObject("result");
                        if (result.has("access_token")) {
                            String token = result.getString("access_token");
                            db.insertFav("access_token", token, _context.getString(R.string.title_activity_telegraph));
                        }
                        if (result.has("path")) {
                            String token = result.getString("path");
                            db.insertFav("path", token, _context.getString(R.string.title_activity_telegraph));
                        }
                    }
                } catch (JSONException e) {
                    Log.e("api", "parse", e);
                }
            }
        });
        thread.start();
    }
}
