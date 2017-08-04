package taipei.sean.telegram.botplayground;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.EditText;
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

public class TelegramAPI {
    final private String _apiBaseUrl = "https://api.telegram.org/bot";
    private Context _context;
    private String _token;

    public TelegramAPI(Context context, String token) {
        this._context = context;
        this._token = token;
    }

    public void callApi(String method, final TextView resultView, @Nullable final String json) {

        Log.d("api", method + json);

        RequestQueue queue = Volley.newRequestQueue(_context);
        String url = _apiBaseUrl + _token + "/" + method;


        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Gson gson = new GsonBuilder().setPrettyPrinting().create();
                        JsonParser jp = new JsonParser();
                        JsonElement je = jp.parse(response);
                        String json = gson.toJson(je);
                        Log.d("api", "resp:" + json);

                        resultView.setText(json);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                String response = new String(error.networkResponse.data);
                Log.d("api", "error" + response);
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                JsonParser jp = new JsonParser();
                JsonElement je = jp.parse(response);
                String json = gson.toJson(je);

                resultView.setText(json);
            }
        }) {
            @Override
            public byte[] getBody() throws AuthFailureError {
                if (null == json)
                    return "{}".getBytes();
                return json.getBytes();
            }

            @Override
            public String getBodyContentType() {
                return "application/json";
            }
        };
        stringRequest.setShouldCache(false);
        queue.add(stringRequest);
    }

    public boolean checkJson(EditText jsonView, @Nullable TextView resultView) {
        try {
            String jsonData = jsonView.getText().toString();
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            JsonParser jp = new JsonParser();
            JsonElement je = jp.parse(jsonData);
            String json = gson.toJson(je);
            if (null != resultView) {
                jsonView.setText(json);
                resultView.setText(json);
            }
            return true;
        } catch (JsonSyntaxException e) {
            Log.e("main", "check error:", e);
            String error = e.getLocalizedMessage();
            if (null != resultView)
                resultView.setText(error);
        }
        return false;
    }
}
