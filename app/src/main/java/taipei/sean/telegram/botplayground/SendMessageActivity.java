package taipei.sean.telegram.botplayground;

import android.os.Bundle;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

public class SendMessageActivity extends AppCompatActivity {
    private String _token;
    private TelegramAPI _api;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_message);

        try {
            Bundle bundle = getIntent().getExtras();
            _token = bundle.getString("token");
        } catch (NullPointerException e) {
            Log.e("caller", "bundle error", e);
            finish();
        }

        _api = new TelegramAPI(this, _token);

        final Button submitButton = (Button) findViewById(R.id.send_message_submit);
        final Button checkJsonButton = (Button) findViewById(R.id.send_message_check);
        final EditText jsonView = (EditText) findViewById(R.id.send_message_data);
        final TextView resultView = (TextView) findViewById(R.id.send_message_result);

        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendMessage();
            }
        });
        checkJsonButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                _api.checkJson(jsonView, resultView);
            }
        });
    }

    public void sendMessage() {
        final EditText textView = (EditText) findViewById(R.id.send_message_text);
        final EditText chatView = (EditText) findViewById(R.id.send_message_chat);
        TextInputEditText dataView = (TextInputEditText) findViewById(R.id.send_message_data);
        TextView resultView = (TextView) findViewById(R.id.send_message_result);

        String messageText = textView.getText().toString();
        String chatId = chatView.getText().toString();
        String optionsJson = dataView.getText().toString();

        JSONObject json;

        try {
            json = new JSONObject(optionsJson);
        } catch (JSONException e) {
            Log.e("api", "sendMessage", e);
            return;
        }

        if (json.isNull("text")) {
            try {
                json.put("text", messageText);
            } catch (JSONException e) {
                Log.e("api", "sendMessage", e);
                return;
            }
        }

        if (json.isNull("chat_id")) {
            try {
                json.put("chat_id", chatId);
            } catch (JSONException e) {
                Log.e("api", "sendMessage", e);
                return;
            }
        }

        String jsonStr = json.toString();

        _api.callApi("sendMessage", resultView, jsonStr);
    }
}
