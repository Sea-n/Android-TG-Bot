package taipei.sean.telegram.botplayground.activity;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;

import taipei.sean.telegram.botplayground.R;
import taipei.sean.telegram.botplayground.adapter.AboutAdapter;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        JSONArray json;

        try {
            InputStream is = getAssets().open("about.json");

            int size = is.available();
            byte[] buffer = new byte[size];

            if (is.read(buffer) < 0)
                return;

            is.close();
            String jsonStr = new String(buffer, "UTF-8");

            try {
                json = new JSONArray(jsonStr);
            } catch (JSONException e) {
                Log.e("about", "parse", e);
                return;
            }
        } catch (IOException e) {
            Log.e("about", "get", e);
            return;
        }


        final RecyclerView aboutView = (RecyclerView) findViewById(R.id.about_list);
        final AboutAdapter adapter = new AboutAdapter(this);
        aboutView.setAdapter(adapter);
        aboutView.setLayoutManager(new StaggeredGridLayoutManager(1, StaggeredGridLayoutManager.VERTICAL));
        aboutView.setItemViewCacheSize(json.length());


        try {
            for (int index=0; index<json.length(); index++) {
                JSONObject value = json.getJSONObject(index);
                adapter.addData(value);
            }
        } catch (JSONException e) {
            Log.e("caller", "parse", e);
        }

    }
}
