package taipei.sean.telegram.botplayground;

import android.os.Bundle;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class SendMessageActivity extends AppCompatActivity {
    final private int _dbVer = 2;
    private SeanDBHelper db;
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

        db = new SeanDBHelper(this, "data.db", null, _dbVer);

        final AutoCompleteTextView chatView = (AutoCompleteTextView) findViewById(R.id.send_message_chat);
        final AutoCompleteTextView textView = (AutoCompleteTextView) findViewById(R.id.send_message_text);
        final Button submitButton = (Button) findViewById(R.id.send_message_submit);
        final Button checkJsonButton = (Button) findViewById(R.id.send_message_check);
        final EditText jsonView = (EditText) findViewById(R.id.send_message_data);
        final TextView resultView = (TextView) findViewById(R.id.send_message_result);


        List<FavStructure> chats = db.getFavs("chat_id");
        String[] chatList = new String[chats.size()];
        for (int i=0; i<chats.size(); i++)
            chatList[i] = chats.get(i).value;
        String[] chatListU = new HashSet<>(Arrays.asList(chatList)).toArray(new String[0]);
        ArrayAdapter<String> chatAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, chatListU);
        chatView.setAdapter(chatAdapter);


        List<FavStructure> msg = db.getFavs("msg");
        String[] msgList = new String[msg.size()];
        for (int i=0; i<msg.size(); i++)
            msgList[i] = msg.get(i).value;
        String[] msgListU = new HashSet<>(Arrays.asList(msgList)).toArray(new String[0]);
        ArrayAdapter<String> msgAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, msgListU);
        textView.setAdapter(msgAdapter);

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
        final AutoCompleteTextView chatView = (AutoCompleteTextView) findViewById(R.id.send_message_chat);
        final AutoCompleteTextView textView = (AutoCompleteTextView) findViewById(R.id.send_message_text);
        final TextInputEditText dataView = (TextInputEditText) findViewById(R.id.send_message_data);
        final TextView resultView = (TextView) findViewById(R.id.send_message_result);

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

        db.insertFav("chat_id", chatId, getLocalClassName());
        db.insertFav("msg", messageText, getLocalClassName());
    }
}
