package taipei.sean.telegram.botplayground;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.support.annotation.Nullable;
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
    public JSONObject latestResponse;

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
        SpannableStringBuilder jsonSpannable = new SpannableStringBuilder(prettyJson);
        jsonColor(jsonSpannable);

        SpannableString methodSpannable = new SpannableString(method);
        methodSpannable.setSpan(new ForegroundColorSpan(Color.MAGENTA), 0, method.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        SpannedString resultText = (SpannedString) TextUtils.concat(methodSpannable, jsonSpannable);
        resultView.setText(resultText);

        final String url = _apiBaseUrl + "/" + method;

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                String respStr = "";
                try {
                    final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

                    RequestBody body = RequestBody.create(JSON, json);
                    Request request = new Request.Builder()
                            .url(url)
                            .post(body)
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
                jsonColor(jsonSpannable);
                Handler handler = new Handler(_context.getMainLooper());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        resultView.setText(jsonSpannable);
                    }
                });


                if (null != json) {
                    try {
                        latestResponse = new JSONObject(json);
                        boolean status = latestResponse.getBoolean("ok");
                        if (status) {

                            Pattern p = Pattern.compile("\"file_id\": \"([^\"]+)\"");
                            Matcher m = p.matcher(json);
                            while (m.find()) {
                                String fileId = m.group(1);
                                db.insertFav("file_id", fileId, method);
                            }
                        }
                    } catch (JSONException e) {
                        Log.e("api", "parse fav", e);
                    }
                }
            }
        });
        thread.start();
    }

    public static SpannableStringBuilder jsonColor(SpannableStringBuilder spannable) {
        String string = spannable.toString();
        int pos, posB, posE;
        for (pos = posB = 0, posE = string.indexOf("\n"); posE != -1; pos = posB = posE + 1, posE = string.indexOf("\n", posB)) {   // Missed first line
            while (spannable.charAt(pos) == ' ')   // intend space
                ++pos;

            if (spannable.charAt(pos) == '"') {   // string key
                int posT = pos;

                do ++pos;   // key, didn't consider about escape
                while (pos != posE && (spannable.charAt(pos) != '"' ||  spannable.charAt(pos) != '\n'));

                spannable.setSpan(new ForegroundColorSpan(Color.rgb(0x79, 0x5d, 0xa3)), posT, ++pos, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                if (spannable.charAt(pos) == ':') {
                    spannable.setSpan(new ForegroundColorSpan(Color.BLACK), pos, ++pos, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);   // :
                    ++pos;   // space after ":"
                }
            }


            if (spannable.charAt(pos) == '"') {   // string value
                int posT = pos;
                do ++pos;   // value, didn't consider about escape
                while (spannable.charAt(pos) != '"');

                spannable.setSpan(new ForegroundColorSpan(Color.rgb(0xdf, 0x50, 0)), posT, ++pos, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);   // "
            } else if (spannable.charAt(pos) == 't')   // true
                spannable.setSpan(new ForegroundColorSpan(Color.rgb(0, 0x86, 0xb3)), pos, pos + 4, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            else if (spannable.charAt(pos) == 'f')   // false
                spannable.setSpan(new ForegroundColorSpan(Color.rgb(0, 0x86, 0xb3)), pos, pos + 5, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            else if (spannable.charAt(pos) == 'n')   // null
                spannable.setSpan(new ForegroundColorSpan(Color.rgb(0, 0x86, 0xb3)), pos, ++pos + 4, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            else if (spannable.charAt(pos) == '[')   // array
                spannable.setSpan(new ForegroundColorSpan(Color.BLACK), pos, ++pos, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            else if (spannable.charAt(pos) == '{')   // object
                spannable.setSpan(new ForegroundColorSpan(Color.BLACK), pos, ++pos, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            else if (Character.isDigit(spannable.charAt(pos)) || spannable.charAt(pos) == '-') {   // signed number
                int posT = pos;
                do ++pos;
                while (Character.isDigit(spannable.charAt(pos)));
                spannable.setSpan(new ForegroundColorSpan(Color.rgb(0, 0x80, 0x80)), posT, pos, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }


            if (posE > 1) {
                pos = posE - 1;  // pos before newline
                if (spannable.charAt(pos) == ',')
                    spannable.setSpan(new ForegroundColorSpan(Color.BLACK), pos, ++pos, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                else if (spannable.charAt(pos) == ']')   // array
                    spannable.setSpan(new ForegroundColorSpan(Color.BLACK), pos, ++pos, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                else if (spannable.charAt(pos) == '}')   // object
                    spannable.setSpan(new ForegroundColorSpan(Color.BLACK), pos, ++pos, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        if (spannable.length() > 2)  // more than 2 line
            spannable.setSpan(new ForegroundColorSpan(Color.BLACK), spannable.length() - 1, spannable.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);   // latest "}"

        return spannable;
    }
}

