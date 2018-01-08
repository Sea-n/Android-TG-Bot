package taipei.sean.telegram.botplayground.activity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.Switch;

import taipei.sean.telegram.botplayground.R;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        final Switch uploadView = (Switch) findViewById(R.id.upload);
        final Switch screenshotView = (Switch) findViewById(R.id.screenshot);
        final Spinner copyView = (Spinner) findViewById(R.id.copy);
        final Spinner shareView = (Spinner) findViewById(R.id.share);

        ArrayAdapter copyAdapter = ArrayAdapter.createFromResource(this, R.array.setting_copy_options, android.R.layout.simple_spinner_item);
        copyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        copyView.setAdapter(copyAdapter);

        ArrayAdapter shareAdapter = ArrayAdapter.createFromResource(this, R.array.setting_share_options, android.R.layout.simple_spinner_item);
        shareAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        shareView.setAdapter(shareAdapter);

        final SharedPreferences preferences = getSharedPreferences("data", MODE_PRIVATE);
        final boolean upload = preferences.getBoolean("upload_payload", true);
        final boolean screenshot = preferences.getBoolean("take_screenshot", false);
        final int copy = preferences.getInt("copy_action", 0);
        final int share = preferences.getInt("share_intent", 0);

        uploadView.setChecked(upload);
        screenshotView.setChecked(screenshot);
        copyView.setSelection(copy);
        shareView.setSelection(share);


        uploadView.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (!b) {
                    int copy = copyView.getSelectedItemPosition();
                    if (copy == 0 || copy == 1)
                        copyView.setSelection(2);

                    int share = shareView.getSelectedItemPosition();
                    if (share == 0 || share == 2)
                        shareView.setSelection(3);
                }

                preferences.edit()
                        .putBoolean("upload_payload", b)
                        .apply();
            }
        });

        screenshotView.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (!b) {
                    int share = shareView.getSelectedItemPosition();
                    if (share == 1 || share == 2)
                        shareView.setSelection(3);
                }

                preferences.edit()
                        .putBoolean("take_screenshot", b)
                        .apply();
            }
        });

        copyView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if (i == 0 || i == 1)
                    uploadView.setChecked(true);

                preferences.edit()
                        .putInt("copy_action", i)
                        .apply();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        shareView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if (i == 0 || i == 2)
                    uploadView.setChecked(true);

                if (i == 1 || i == 2)
                    screenshotView.setChecked(true);

                preferences.edit()
                        .putInt("share_intent", i)
                        .apply();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        preferences.registerOnSharedPreferenceChangeListener(new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
                Snackbar.make(uploadView, "Saved", Snackbar.LENGTH_SHORT).show();
            }
        });
    }
}
