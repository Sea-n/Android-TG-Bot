package taipei.sean.telegram.botplayground.activity;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.text.Editable;
import android.text.Html;
import android.text.Spanned;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import taipei.sean.telegram.botplayground.InstantComplete;
import taipei.sean.telegram.botplayground.R;
import taipei.sean.telegram.botplayground.SeanAdapter;
import taipei.sean.telegram.botplayground.SeanDBHelper;
import taipei.sean.telegram.botplayground.TelegramAPI;
import taipei.sean.telegram.botplayground.adapter.ApiCallerAdapter;

public class ApiCallerActivity extends AppCompatActivity {
    final private int _dbVer = 4;
    private SeanDBHelper db;
    private String _token;
    private TelegramAPI _api;
    private JSONObject apiMethods;
    private boolean modified = true;
    private String payloadUrl;
    private boolean upload;
    private boolean screenshot;
    private int copyType;
    private int shareType;

    private static void setTextSize(Paint paint, float desiredWidth, String text) {
        float testTextSize = 48f;

        Rect bounds = new Rect();
        paint.setTextSize(testTextSize);

        paint.getTextBounds(text, 0, text.length(), bounds);
        testTextSize *= desiredWidth;
        testTextSize /= bounds.width();
        paint.setTextSize(testTextSize);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.api_caller);

        db = new SeanDBHelper(this, "data.db", null, _dbVer);

        Intent intent = getIntent();
        Uri uri = intent.getData();
        if (null != uri) {
            String path = uri.getPath();

            Pattern p = Pattern.compile("/bot(" + getString(R.string.bot_token_regex) + ")/.+");
            Matcher m = p.matcher(path);
            if (m.matches()) {
                _token = m.group(1);
                db.insertToken(_token, _token);
            } else {
                Log.e("caller", "no token with intent");
                finish();
                return;
            }
        } else {
            try {
                Bundle bundle = getIntent().getExtras();
                _token = bundle.getString("token");
            } catch (NullPointerException e) {
                Log.e("caller", "bundle error", e);
                finish();
            }
        }

        _api = new TelegramAPI(this, _token);

        final InstantComplete methodView = (InstantComplete) findViewById(R.id.api_caller_method);
        final Button submitButton = (Button) findViewById(R.id.api_caller_submit);

        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                submit();
            }
        });

        ArrayList<String> botApiMethodsList = new ArrayList<String>() {
        };
        apiMethods = loadMethods();

        if (null == apiMethods) {
            Log.e("caller", "no methods");
            return;
        }

        Iterator<String> temp = apiMethods.keys();
        while (temp.hasNext()) {
            String key = temp.next();
            botApiMethodsList.add(key);
        }

        final SeanAdapter<String> adapter = new SeanAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, botApiMethodsList);
        methodView.setAdapter(adapter);

        methodView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                modified = true;
                String method = editable.toString();
                if (apiMethods.has(method))
                    db.updateParam("_method", method);
                updateMethod();
            }
        });

        if (null != uri) {
            String path = uri.getPath();

            Pattern p = Pattern.compile("/bot" + getString(R.string.bot_token_regex) + "/([A-Za-z]+)");
            Matcher m = p.matcher(path);
            if (m.matches()) {
                String method = m.group(1);
                db.updateParam("_method", method);
            }

            Set<String> args = uri.getQueryParameterNames();
            for (Object argNameObj : args) {
                String argName = argNameObj.toString();
                String argVal = uri.getQueryParameter(argName);
                db.updateParam(argName, argVal);
            }
        }

        String method = db.getParam("_method");
        if (apiMethods.has(method))
            methodView.setText(method);

        updateMethod();

        final SharedPreferences preferences = getSharedPreferences("data", MODE_PRIVATE);
        upload = preferences.getBoolean("upload_payload", true);
        screenshot = preferences.getBoolean("take_screenshot", false);
        copyType = preferences.getInt("copy_action", 0);
        shareType = preferences.getInt("share_intent", 0);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.api_caller, menu);

        MenuItem shareButton = menu.findItem(R.id.action_share);
        if (!upload && !screenshot)
            shareButton.setVisible(false);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        final InstantComplete methodView = (InstantComplete) findViewById(R.id.api_caller_method);
        final TextView resultView = (TextView) findViewById(R.id.api_caller_result);
        final RecyclerView paramList = (RecyclerView) findViewById(R.id.api_caller_inputs);
        final ApiCallerAdapter paramAdapter = (ApiCallerAdapter) paramList.getAdapter();

        if (null != paramAdapter)
            modified |= paramAdapter.modified;

        switch (id) {
            case R.id.action_share:
                if (modified) {
                    Snackbar.make(resultView, "Please submit before share", Snackbar.LENGTH_SHORT).show();
                    break;
                }

                modified = true;
                Snackbar.make(resultView, "Processing...", Snackbar.LENGTH_SHORT).show();

                if (upload) {
                    Thread thread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                final String method = methodView.getText().toString();
                                JSONObject requestJson = new JSONObject();
                                if (null != paramAdapter)
                                    requestJson = paramAdapter.getJson(method);

                                JSONObject json = new JSONObject();
                                json.put("token", _token);
                                json.put("method", method);
                                json.put("request", requestJson);
                                json.put("response", _api.latestResponse);

                                final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
                                RequestBody requestBody = RequestBody.create(JSON, json.toString());
                                Request request = new Request.Builder()
                                        .url("https://tg.sean.taipei/create.php")
                                        .post(requestBody)
                                        .build();
                                OkHttpClient client = new OkHttpClient();
                                Response resp = client.newCall(request).execute();
                                final String respStr = resp.body().string();
                                payloadUrl = resp.header("X-payload-url");

                                Handler handler = new Handler(getMainLooper());
                                final JSONObject finalRequestJson = requestJson;
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (!screenshot) {
                                            Spanned spanned = Html.fromHtml(respStr);
                                            resultView.setText(spanned);
                                        } else
                                            Snackbar.make(resultView, respStr, Snackbar.LENGTH_LONG).show();

                                        if (null != payloadUrl) {
                                            String copyText = null;
                                            if (copyType == 0)
                                                copyText = payloadUrl;
                                            else if (copyType == 1) {
                                                try {
                                                    copyText = payloadUrl + "\n\n" + finalRequestJson.toString(2);
                                                } catch (JSONException e) {
                                                    Log.e("api", "request json", e);
                                                }
                                            }

                                            if (null != copyText) {
                                                ClipData clip = ClipData.newPlainText(getString(R.string.app_name), copyText);
                                                ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                                                clipboard.setPrimaryClip(clip);
                                                Snackbar.make(resultView, "Copied!", Snackbar.LENGTH_SHORT).show();
                                            }

                                            if (shareType == 0) {
                                                String text = String.format("My %s payload:\n%s", method, payloadUrl);
                                                Intent intent = new Intent();
                                                intent.setAction(Intent.ACTION_SEND);
                                                intent.putExtra(Intent.EXTRA_TEXT, text);
                                                intent.setType("text/plain");
                                                startActivity(Intent.createChooser(intent, "Share Payload of " + method));
                                            }
                                        }
                                    }
                                });

                                if (shareType == 2)
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            screenshot();
                                        }
                                    });
                            } catch (final Exception e) {
                                Log.e("caller", "share", e);
                                Handler handler = new Handler(getMainLooper());
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        final String errorMsg = e.getLocalizedMessage();
                                        if (!screenshot)
                                            resultView.setText(errorMsg);
                                        else
                                            Snackbar.make(resultView, errorMsg, Snackbar.LENGTH_LONG).show();
                                    }
                                });
                            }
                        }
                    });
                    thread.start();
                }
                if (screenshot) {
                    if (shareType != 2)
                        screenshot();
                }
                break;

            default:
                Log.w("option", "Press unknown " + id);
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        switch (requestCode) {
            case 87:
                if (resultCode == RESULT_OK) {
                    Uri uri = data.getData();
                    if (uri != null) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                            ContentResolver resolver = getContentResolver();
                            resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        }

                        String param = db.getParam("_file");

                        db.updateParam(param, uri.toString());
                        updateMethod();
                    }
                }
        }
    }

    private void updateMethod() {
        final InstantComplete methodView = (InstantComplete) findViewById(R.id.api_caller_method);
        final RecyclerView paramList = (RecyclerView) findViewById(R.id.api_caller_inputs);
        final String method = methodView.getText().toString();

        JSONObject methodData;
        JSONObject paramData;

        if (null == apiMethods) {
            Log.d("caller", "no methods");
            return;
        }

        if (!apiMethods.has(method)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
                methodView.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0);
            ViewGroup.LayoutParams layoutParams = paramList.getLayoutParams();
            layoutParams.height = 0;
            paramList.setLayoutParams(layoutParams);
            paramList.setAdapter(null);
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
            methodView.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.ic_star_border_black_24dp, 0);

        try {
            methodData = apiMethods.getJSONObject(method);
        } catch (JSONException e) {
            Log.e("caller", apiMethods.toString(), e);
            return;
        }

        if (methodData.has("description")) {
            try {
                final String desc = methodData.getString("description");
                methodView.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View view) {
                        Snackbar.make(view, desc, Snackbar.LENGTH_LONG).show();
                        return false;
                    }
                });
            } catch (JSONException e) {
                Log.e("caller", "method description", e);
            }
        }

        if (methodData.has("params")) {
            try {
                paramData = methodData.getJSONObject("params");
            } catch (JSONException e) {
                Log.e("caller", methodData.toString(), e);
                return;
            }
        } else {
            Log.e("caller", "No params: " + method);
            return;
        }

        final ApiCallerAdapter apiCallerAdapter = new ApiCallerAdapter(this);

        try {
            Iterator<String> temp = paramData.keys();
            while (temp.hasNext()) {
                String key = temp.next();
                JSONObject value = paramData.getJSONObject(key);
                apiCallerAdapter.addData(key, value);
            }
        } catch (JSONException e) {
            Log.e("caller", "parse", e);
        }

        paramList.setAdapter(apiCallerAdapter);
        paramList.setLayoutManager(new StaggeredGridLayoutManager(1, StaggeredGridLayoutManager.VERTICAL));
        paramList.setItemViewCacheSize(paramData.length());

        paramList.post(new Runnable() {
            @Override
            public void run() {
                apiCallerAdapter.fitView(paramList);
            }
        });
    }

    private void submit() {
        modified = false;
        final InstantComplete methodView = (InstantComplete) findViewById(R.id.api_caller_method);
        final RecyclerView paramList = (RecyclerView) findViewById(R.id.api_caller_inputs);
        final TextView resultView = (TextView) findViewById(R.id.api_caller_result);

        String method = methodView.getText().toString();

        final ApiCallerAdapter paramAdapter = (ApiCallerAdapter) paramList.getAdapter();

        if (null == paramAdapter) {
            _api.callApi(method, resultView, null);
            return;
        }

        JSONObject jsonObject = paramAdapter.getJson(method);
        paramAdapter.modified = false;

        _api.callApi(method, resultView, jsonObject);
    }

    public JSONObject loadMethods() {
        final String lang = getString(R.string.lang_code);
        JSONObject json = new JSONObject();

        try {
            InputStream is = getAssets().open("api-methods.json");

            int size = is.available();
            byte[] buffer = new byte[size];

            if (is.read(buffer) < 0)
                return null;

            is.close();
            String jsonStr = new String(buffer, "UTF-8");

            try {
                json = new JSONObject(jsonStr);
            } catch (JSONException e) {
                Log.e("caller", "parse", e);
            }
        } catch (IOException e) {
            Log.e("caller", "get", e);
        }

        try {
            InputStream is = getAssets().open("api-methods-" + lang + ".json");

            int size = is.available();
            byte[] buffer = new byte[size];

            if (is.read(buffer) < 0)
                return null;

            is.close();
            String jsonStr = new String(buffer, "UTF-8");

            try {
                JSONObject localeJson = new JSONObject(jsonStr);

                Iterator<String> methods = localeJson.keys();
                while (methods.hasNext()) {
                    String methodName = methods.next();
                    JSONObject method = json.getJSONObject(methodName);
                    JSONObject localeMethod = localeJson.getJSONObject(methodName);

                    if (localeMethod.has("description")) {
                        String methodDesc = localeMethod.getString("description");
                        method.put("description", methodDesc);
                    }

                    if (localeMethod.has("params")) {
                        JSONObject params = method.getJSONObject("params");
                        JSONObject localeParams = localeMethod.getJSONObject("params");

                        Iterator<String> paramNames = localeParams.keys();
                        while (paramNames.hasNext()) {
                            String paramName = paramNames.next();
                            JSONObject param = params.getJSONObject(paramName);
                            String localeParam = localeParams.getString(paramName);

                            param.put("description", localeParam);
                        }
                    }
                }
            } catch (JSONException e) {
                Log.e("caller", "parse locale", e);
            }
        } catch (IOException e) {
            Log.e("caller", "get locale", e);
        }

        return json;
    }

    public void screenshot() {
        final InstantComplete methodView = (InstantComplete) findViewById(R.id.api_caller_method);
        final TextView resultView = (TextView) findViewById(R.id.api_caller_result);

        LinearLayout parentLayout = (LinearLayout) findViewById(R.id.api_caller_layout);
        int width = parentLayout.getWidth();
        int height = 0;

        String method = methodView.getText().toString();
        if (!apiMethods.has(method))
            method = null;

        Bitmap paramsBitmap = null;
        if (null != method) {
            height += width * 3 / 16;   // Request Header (with method name)
            RecyclerView paramsLayout = (RecyclerView) findViewById(R.id.api_caller_inputs);
            final ApiCallerAdapter paramsAdapter = (ApiCallerAdapter) paramsLayout.getAdapter();
            if (null != paramsAdapter) {
                paramsBitmap = paramsAdapter.getScreenshot();
                if (null != paramsBitmap)
                    height += paramsBitmap.getHeight();   // Request Body
            }
        }

        Bitmap resultBitmap = null;
        LinearLayout resultWrapper = (LinearLayout) findViewById(R.id.api_caller_result_wrapper);
        if (!resultView.getText().toString().equals(getString(R.string.no_context_yet))) {
            resultBitmap = Bitmap.createBitmap(resultWrapper.getWidth(), resultWrapper.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas resultCanvas = new Canvas(resultBitmap);
            resultWrapper.draw(resultCanvas);
            height += resultBitmap.getHeight() + width * 3 / 16;
        }

        if (height == 0)
            return;

        height += width * 2 / 16;   // footer

        Bitmap finalBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas finalCanvas = new Canvas(finalBitmap);
        finalCanvas.drawColor(Color.WHITE);

        Paint titlePaint = new Paint();
        titlePaint.setColor(Color.BLACK);
        titlePaint.setTypeface(Typeface.SANS_SERIF);
        setTextSize(titlePaint, width / 2, getString(R.string.request));
        titlePaint.setTextAlign(Paint.Align.LEFT);

        Paint methodPaint = null;
        if (null != method) {
            methodPaint = new Paint();
            methodPaint.setColor(Color.DKGRAY);
            methodPaint.setTypeface(Typeface.SANS_SERIF);
            setTextSize(methodPaint, width / 3, method);
            methodPaint.setTextAlign(Paint.Align.LEFT);
        }

        Paint textPaint = new Paint();
        textPaint.setColor(Color.LTGRAY);
        textPaint.setTypeface(Typeface.SANS_SERIF);
        setTextSize(textPaint, width / 2, getString(R.string.powered_by));
        textPaint.setTextAlign(Paint.Align.LEFT);

        Paint linkPaint = new Paint();
        linkPaint.setColor(Color.BLUE);
        titlePaint.setTypeface(Typeface.SANS_SERIF);
        setTextSize(linkPaint, width / 3, getString(R.string.app_link_text));
        linkPaint.setTextAlign(Paint.Align.RIGHT);

        int offset = 0;

        if (null != method) {
            finalCanvas.drawText(getString(R.string.request), width / 32, offset + width * 2 / 16, titlePaint);
            finalCanvas.drawText("(" + method + ")", width * 9 / 16, offset + width * 2 / 16, methodPaint);
            offset += width * 3 / 16;

            if (null != paramsBitmap) {
                finalCanvas.drawBitmap(paramsBitmap, 0, offset, null);
                offset += paramsBitmap.getHeight();
            }
        }

        if (null != resultBitmap) {
            finalCanvas.drawText(getString(R.string.response), width / 32, offset + width * 2 / 16, titlePaint);
            offset += width * 3 / 16;

            finalCanvas.drawBitmap(resultBitmap, 0, offset, null);
            offset += resultBitmap.getHeight();
        }

        finalCanvas.drawText(getString(R.string.powered_by), width / 32, offset + width / 16, textPaint);
        finalCanvas.drawText(getString(R.string.app_link_text), width * 23 / 24, offset + width * 3 / 32, linkPaint);

        File dir = createDir();
        if (null == dir)
            return;
        SimpleDateFormat sdf = new SimpleDateFormat("MMMdd'T'HH:mm:ss", Locale.US);
        Date date = new Date();
        String time = sdf.format(date);
        File file = new File(dir, "screenshot-" + time + ".png");
        try {
            FileOutputStream out = new FileOutputStream(file);
            finalBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
        } catch (Exception e) {
            Log.e("caller", "new file", e);
        }

        String caption = null;
        if (shareType == 1)
            caption = getString(R.string.powered_by) + ", \n" + getString(R.string.app_link_text);
        else if (shareType == 2)
            caption = payloadUrl;

        if (null != caption) {
            String authority = getApplicationContext().getPackageName() + ".provider";
            Uri fileURI = FileProvider.getUriForFile(getApplicationContext(), authority, file);
            Intent intent = new Intent();
            intent.setType("image/png");
            intent.setAction(Intent.ACTION_SEND);
            intent.putExtra(Intent.EXTRA_TEXT, caption);
            intent.putExtra(Intent.EXTRA_STREAM, fileURI);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "Share Screenshot of " + method));
        }
    }

    private File createDir() {
        int permW = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permW == PackageManager.PERMISSION_DENIED) {
            Log.w("main", "permission WRITE_EXTERNAL_STORAGE denied");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
            return null;
        }

        final File dir = new File(Environment.getExternalStorageDirectory() + "/TeleBot");
        if (!dir.exists()) {
            if (!dir.mkdir()) {
                Log.e("main", "mkdir fail");
                Toast.makeText(this, R.string.mkdir_fail, Toast.LENGTH_LONG).show();
                return null;
            }
        } else if (!dir.isDirectory()) {
            Log.e("main", "director is file");
            Toast.makeText(this, R.string.mkdir_fail, Toast.LENGTH_LONG).show();
            return null;
        }
        return dir;
    }
}
