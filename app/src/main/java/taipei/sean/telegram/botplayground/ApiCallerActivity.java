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

public class ApiCallerActivity extends AppCompatActivity {
    final public String[] botApiMethodsList = new String[]{
            "getUpdates",
            "setWebhook",
            "deleteWebhook",
            "getWebhookInfo",
            "getMe",
            "sendMessage",
            "forwardMessage",
            "sendPhoto",
            "sendAudio",
            "sendDocument",
            "sendVideo",
            "sendVoice",
            "sendVideoNote",
            "sendLocation",
            "sendVenue",
            "sendContact",
            "sendChatAction",
            "getUserProfilePhotos",
            "getFile",
            "kickChatMember",
            "unbanChatMember",
            "restrictChatMember",
            "promoteChatMember",
            "exportChatInviteLink",
            "setChatPhoto",
            "deleteChatPhoto",
            "setChatTitle",
            "setChatDescription",
            "pinChatMessage",
            "unpinChatMessage",
            "leaveChat",
            "getChat",
            "getChatAdministrators",
            "getChatMembersCount",
            "getChatMember",
            "editMessageText",
            "editMessageCaption",
            "editMessageReplyMarkup",
            "deleteMessage",
            "sendSticker",
            "getStickerSet",
            "uploadStickerFile",
            "createNewStickerSet",
            "addStickerToSet",
            "setStickerPositionInSet",
            "deleteStickerFromSet",
            "answerInlineQuery",
            "sendInvoice",
            "answerShippingQuery",
            "answerPreCheckoutQuery",
            "sendGame",
            "setGameScore",
            "getGameHighScores"
    };
    private String _token;
    private TelegramAPI _api;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_api_caller);

        try {
            Bundle bundle = getIntent().getExtras();
            _token = bundle.getString("token");
        } catch (NullPointerException e) {
            Log.e("caller", "bundle error", e);
            finish();
        }

        _api = new TelegramAPI(this, _token);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, botApiMethodsList);
        AutoCompleteTextView textView = (AutoCompleteTextView)
                findViewById(R.id.api_caller_method);
        textView.setAdapter(adapter);


        final Button submitButton = (Button) findViewById(R.id.api_caller_submit);
        final Button checkJsonButton = (Button) findViewById(R.id.api_caller_check);
        final EditText jsonView = (EditText) findViewById(R.id.api_caller_data);
        final TextView resultView = (TextView) findViewById(R.id.api_caller_result);

        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                submit();
            }
        });
        checkJsonButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                _api.checkJson(jsonView, resultView);
            }
        });

        submit();   // default getMe
    }

    private void submit() {
        AutoCompleteTextView methodView = (AutoCompleteTextView) findViewById(R.id.api_caller_method);
        TextInputEditText dataView = (TextInputEditText) findViewById(R.id.api_caller_data);
        TextView resultView = (TextView) findViewById(R.id.api_caller_result);

        String method = methodView.getText().toString();
        String json = dataView.getText().toString();

        _api.callApi(method, resultView, json);
    }
}
