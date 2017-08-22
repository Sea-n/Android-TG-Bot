package taipei.sean.telegram.botplayground.activity;

import android.os.Bundle;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import taipei.sean.telegram.botplayground.FavStructure;
import taipei.sean.telegram.botplayground.R;
import taipei.sean.telegram.botplayground.SeanDBHelper;

public class AddFavoriteActivity extends AppCompatActivity {
    final private int _dbVer = 4;
    private long id = -1;
    private SeanDBHelper db;

    public AddFavoriteActivity() {
        db = new SeanDBHelper(this, "data.db", null, _dbVer);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_favorite);

        final TextInputEditText kindView = (TextInputEditText) findViewById(R.id.add_fav_kind);
        final TextInputEditText valueInput = (TextInputEditText) findViewById(R.id.add_fav_value);
        final TextInputEditText nameInput = (TextInputEditText) findViewById(R.id.add_fav_name);
        final Button submitButton = (Button) findViewById(R.id.add_fav_submit);

        Bundle bundle = getIntent().getExtras();
        if ((bundle != null) && bundle.containsKey("id")) {
            id = bundle.getLong("id");
            FavStructure fav = db.getFav(id);

            if (null != fav) {
                kindView.setText(fav.kind);
                valueInput.setText(fav.value);
                nameInput.setText(fav.name);
            }
        }


        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String strKind = kindView.getText().toString();
                final String strVal = valueInput.getText().toString();
                final String strName = nameInput.getText().toString();

                if (id > 0)
                    db.updateFav(id, strKind, strVal, strName);
                else
                    db.insertFav(strKind, strVal, strName);

                finish();
            }
        });
    }
}
