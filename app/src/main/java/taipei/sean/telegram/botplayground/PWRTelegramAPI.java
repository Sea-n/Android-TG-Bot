package taipei.sean.telegram.botplayground;

import android.content.Context;
import android.os.Handler;
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

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;

public class PWRTelegramAPI {
    final private String _apiBaseUrl;
    final private Context _context;

    public PWRTelegramAPI(Context context, String token, int type) {
        final String _apiBaseUrlNormal = "https://api.pwrtelegram.xyz/bot";
        final String _apiBaseUrlDeep = "https://deepapi.pwrtelegram.xyz/bot";
        final String _apiBaseUrlUser = "https://api.pwrtelegram.xyz/user";

        this._context = context;

        String apiBaseUrl;

        switch (type) {
            case 1:
                apiBaseUrl = _apiBaseUrlDeep;
                break;
            case 2:
                apiBaseUrl = _apiBaseUrlUser;
                break;
            default:
                apiBaseUrl = _apiBaseUrlNormal;
        }
        apiBaseUrl += token;
        _apiBaseUrl = apiBaseUrl;
    }

    public void callApi(final String method, final TextView resultView, final JSONObject params) {
        Log.d("papi", method + params);

        final String url = _apiBaseUrl + "/" + method;

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                String response = "";
                String resultText = "";
                try {
                    final MediaType FORM = MediaType.parse("application/x-www-form-urlencoded; charset=utf-8");

                    String postData = "";
                    try {
                        Iterator<String> temp = params.keys();
                        while (temp.hasNext()) {
                            String key = temp.next();
                            String value = params.get(key).toString();
                            postData += key + "=" + value + "&";
                        }
                    } catch (JSONException e) {
                        Log.w("papi", "JSON", e);
                    }
                    RequestBody body = RequestBody.create(FORM, postData);
                    okhttp3.Request request = new okhttp3.Request.Builder()
                            .url(url)
                            .post(body)
                            .build();
                    OkHttpClient client = new OkHttpClient();
                    okhttp3.Response resp = client.newCall(request).execute();
                    response = resp.body().string();
                } catch (final MalformedURLException e) {
                    Log.e("papi", "Malformed URL", e);
                    resultText += e.getLocalizedMessage();
                } catch (final IOException e) {
                    Log.e("papi", "IO", e);
                    resultText += e.getLocalizedMessage();
                } catch (final NullPointerException e) {
                    Log.e("papi", "Null Pointer", e);
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
                    Log.e("papi", "parse", e);
                    resultText += response;
                }
                Log.d("papi", "resp:" + json);

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
