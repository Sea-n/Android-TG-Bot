package taipei.sean.telegram.botplayground;

import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

public class AddFavoriteActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_favorite);

        final Spinner spinner = (Spinner) findViewById(R.id.add_fav_kind);
        final TextInputEditText inputValue = (TextInputEditText) findViewById(R.id.add_fav_value);
        final Button submitButton = (Button) findViewById(R.id.add_fav_submit);

        ArrayAdapter adapter = ArrayAdapter.createFromResource(this, R.array.add_fav_list,
                android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (parent.getId()) {
                    case R.id.add_fav_kind:
                        Log.d("addFav", "pos"+position+" id"+id);
                        switch (position) {
                            case 0:
                                inputValue.setHint("Your Chat ID");
                                inputValue.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED);
                                break;
                            case 1:
                                inputValue.setHint("Your Message Template");
                                inputValue.setInputType(InputType.TYPE_CLASS_TEXT);
                                break;
                            default:
                                Log.d("addFav", "ois "+parent+" "+view+" "+position+" "+id);
                                break;
                        }

                        break;
                    default:
                        Log.d("addFav", "ois "+parent+" "+view+" "+position+" "+id);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int spinnerPos = spinner.getSelectedItemPosition();
                final String strVal = inputValue.getText().toString();
                switch (spinnerPos) {
                    case 0:
                        try {
                            long chatId = Long.parseLong(strVal);
                            Log.d("addFav", "chat"+chatId);
                        } catch (RuntimeException e) {
                            Log.e("addFav", "parse chat id", e);
                            inputValue.setError(getString(R.string.parse_error));
                        }
                        break;
                    case 1:
                        Log.d("addFav", "msg"+strVal);
                        break;
                    default:
                        Log.w("addFav", "Unknow pos "+strVal);
                        break;
                }
            }
        });
    }
}
