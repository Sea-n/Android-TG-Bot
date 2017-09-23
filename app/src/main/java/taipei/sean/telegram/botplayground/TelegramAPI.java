package taipei.sean.telegram.botplayground;

import android.content.Context;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class TelegramAPI {
    final private int _dbVer = 4;
    final private String _apiBaseUrl;
    final private Context _context;
    private SeanDBHelper db;

    public TelegramAPI(Context context, String token) {
        this._context = context;
        _apiBaseUrl = "https://api.telegram.org/bot" + token;
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
        String resultText = method + prettyJson;
        resultView.setText(resultText);

        final String url = _apiBaseUrl + "/" + method;

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                String response = "";
                String resultText = "";
                try {
                    final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

                    RequestBody body = RequestBody.create(JSON, json);
                    Request request = new Request.Builder()
                            .url(url)
                            .post(body)
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

                try {
                    JSONObject jsonObject = new JSONObject(json);
                    boolean status = jsonObject.getBoolean("ok");
                    if (status) {

                        Pattern p = Pattern.compile("\"file_id\": \"([^\"]+)\"");
                        Matcher m = p.matcher(json);
                        while (m.find()) {
                            String fileId = m.group(1);
                            db.insertFav("file_id", fileId, method);
                        }
                    }
                } catch (JSONException e) {
                    Log.e("api", "parse", e);
                    resultText += response;
                }

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
