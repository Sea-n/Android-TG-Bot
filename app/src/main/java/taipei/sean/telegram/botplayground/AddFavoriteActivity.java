package taipei.sean.telegram.botplayground;

import android.os.StrictMode;
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
    final private int _dbVer = 2;
    private long id = -1;
    private SeanDBHelper db;

    public AddFavoriteActivity() {
        db = new SeanDBHelper(this, "data.db", null, _dbVer);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_favorite);

        final Spinner spinner = (Spinner) findViewById(R.id.add_fav_kind);
        final TextInputEditText valueInput = (TextInputEditText) findViewById(R.id.add_fav_value);
        final TextInputEditText nameInput = (TextInputEditText) findViewById(R.id.add_fav_name);
        final Button submitButton = (Button) findViewById(R.id.add_fav_submit);

        Bundle bundle = getIntent().getExtras();
        if ((bundle != null) && bundle.containsKey("id")) {
            id = bundle.getLong("id");
            FavStructure fav = db.getFav(id);

            switch (fav.kind) {
                case "chat_id":
                    spinner.setSelection(0);
                    break;
                case "msg":
                    spinner.setSelection(2);
                    break;
            }

            valueInput.setText(fav.value);
            nameInput.setText(fav.name);
        }


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
                                valueInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED);
                            case 1:
                                valueInput.setHint("Your Chat ID");
                                break;
                            case 2:
                                valueInput.setHint("Your Message Template");
                                valueInput.setInputType(InputType.TYPE_CLASS_TEXT);
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
                final String strVal = valueInput.getText().toString();
                final String strName = nameInput.getText().toString();
                switch (spinnerPos) {
                    case 0:
                    case 1:
                        if (id > 0)
                            db.updateFavChat(id, strVal, strName);
                        else
                            db.insertFavChat(strVal, strName);
                        finish();
                        break;
                    case 2:
                        if (id > 0)
                            db.updateFavMsg(id, strVal, strName);
                        else
                            db.insertFavMsg(strVal, strName);
                        finish();
                        break;
                    default:
                        Log.w("addFav", "Unknown pos "+strVal);
                        valueInput.setError(getString(R.string.add_fav_unknown_select));
                        break;
                }
            }
        });
    }
}
