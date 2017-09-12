package taipei.sean.telegram.botplayground;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.support.annotation.Nullable;
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
    final private String _apiBaseUrl = "https://api.telegra.ph/";
    final private Context _context;

    public TelegraphAPI(Context context) {
        this._context = context;
    }

    public void callApi(final String method, final TextView resultView, @Nullable JSONObject j) {
        if (null == j)
            j = new JSONObject();

        final String json = j.toString();

        Log.d("api", method + json);

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonParser jp = new JsonParser();
        JsonElement je = jp.parse(json);
        String prettyJson = gson.toJson(je);
        String resultText = method + prettyJson;
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
                String response = "";
                String resultText = "";
                try {
                    Request request = new Request.Builder()
                            .url(finalUrl)
                            .build();
                    OkHttpClient client = new OkHttpClient();
                    Response resp = client.newCall(request).execute();
                    response = resp.body().string();
                } catch (final MalformedURLException e) {
                    Log.e("api", "Malformed URL", e);
                    resultText += e.getLocalizedMessage();
                } catch (final IOException e) {
                    Log.e("api", "IO", e);
                    resultText += e.getLocalizedMessage();
                } catch (final NullPointerException e) {
                    Log.e("api", "Null Pointer", e);
                    resultText += e.getLocalizedMessage();
                }

                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                JsonParser jp = new JsonParser();
                JsonElement je;
                String json = "";
                try {
                    je = jp.parse(response);
                    json = gson.toJson(je);
                } catch (JsonSyntaxException e) {
                    Log.e("api", "parse", e);
                    resultText += response;
                }
                Log.d("api", "resp:" + json);

                resultText += json;

                final String finalResultText = resultText;
                Handler handler = new Handler(_context.getMainLooper());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        resultView.setText(finalResultText);
                    }
                });
            }
        });
        thread.start();
    }
}
