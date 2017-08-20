package taipei.sean.telegram.botplayground;

import android.content.Context;
import android.util.Log;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

public class PWRTelegramAPI {
    final private String _apiBaseUrl;
    final private Context _context;
    final private String _token;

    public PWRTelegramAPI(Context context, String token, int type) {
        final String _apiBaseUrlNormal = "https://api.pwrtelegram.xyz/bot";
        final String _apiBaseUrlDeep = "https://deepapi.pwrtelegram.xyz/bot";
        final String _apiBaseUrlUser = "https://api.pwrtelegram.xyz/user";

        this._context = context;
        this._token = token;
        switch (type) {
            case 1:
                _apiBaseUrl = _apiBaseUrlDeep;
                break;
            case 2:
                _apiBaseUrl = _apiBaseUrlUser;
                break;
            default:
                _apiBaseUrl = _apiBaseUrlNormal;
        }
    }

    public void callApi(String method, final TextView resultView, final JSONObject params) {
        Log.d("api", method + params);

        RequestQueue queue = Volley.newRequestQueue(_context);
        String url = _apiBaseUrl + _token + "/" + method;

        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Gson gson = new GsonBuilder().setPrettyPrinting().create();
                        JsonParser jp = new JsonParser();
                        JsonElement je;
                        try {
                            je = jp.parse(response);
                        } catch (JsonSyntaxException e) {
                            Log.e("papi", "parse", e);
                            resultView.setText(response);
                            return;
                        }
                        String json = gson.toJson(je);
                        Log.d("papi", "resp: " + json);

                        resultView.setText(json);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                if (null == error.networkResponse)
                    return;
                String response = new String(error.networkResponse.data);
                Log.d("papi", "error" + response);
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                JsonParser jp = new JsonParser();
                JsonElement je;
                try {
                    je = jp.parse(response);
                } catch (JsonSyntaxException e) {
                    Log.e("papi", "parse", e);
                    resultView.setText(response);
                    return;
                }
                String json = gson.toJson(je);

                resultView.setText(json);
            }
        }) {
            @Override
            public byte[] getBody() throws AuthFailureError {
                String postData = "";
                try {
                    Iterator<String> temp = params.keys();
                    while (temp.hasNext()) {
                        String key = temp.next();
                        String value = params.get(key).toString();
                        postData += key + "=" + value + "&";
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                return postData.getBytes();
            }
        };
        stringRequest.setShouldCache(false);
        queue.add(stringRequest);
    }
}
