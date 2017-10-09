package taipei.sean.telegram.botplayground.activity;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;

import org.apache.commons.io.FilenameUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import taipei.sean.telegram.botplayground.FavStructure;
import taipei.sean.telegram.botplayground.InstantComplete;
import taipei.sean.telegram.botplayground.R;
import taipei.sean.telegram.botplayground.SeanAdapter;
import taipei.sean.telegram.botplayground.SeanDBHelper;

public class FileDownloadActivity extends AppCompatActivity {
    final private int _dbVer = 4;
    private SeanDBHelper db;
    private String _token;
    final private Context context = this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_download);

        db = new SeanDBHelper(this, "data.db", null, _dbVer);

        Intent intent = getIntent();
        Uri uri = intent.getData();
        if (null != uri) {
            String path = uri.getPath();

            Pattern p = Pattern.compile("/file/bot(" + getString(R.string.bot_token_regex) + ")/.+");
            Matcher m = p.matcher(path);
            if (m.matches()) {
                _token = m.group(1);
                db.insertToken(_token, _token);
            } else {
                Log.e("fd", "no token with intent");
                finish();
                return;
            }
        } else {
            try {
                Bundle bundle = getIntent().getExtras();
                _token = bundle.getString("token");
            } catch (NullPointerException e) {
                Log.e("fd", "bundle error", e);
                finish();
            }
        }

        final InstantComplete fileIdView = (InstantComplete) findViewById(R.id.file_download_file_id);
        final Button submitButton = (Button) findViewById(R.id.file_download_submit);

        final List<FavStructure> favs = db.getFavs("file_id");

        ArrayList<String> favList = new ArrayList<>();
        for (FavStructure fav : favs)
            favList.add(fav.value);

        final SeanAdapter<String> adapter = new SeanAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, favList);
        fileIdView.setAdapter(adapter);

        if (null != uri) {
            String path = uri.getPath();

            Pattern p = Pattern.compile("/file/bot" + getString(R.string.bot_token_regex) + "/(.+)");
            Matcher m = p.matcher(path);
            if (m.matches()) {
                String file_path = m.group(1);
                db.updateParam("file_id", file_path);
            }
        }

        String file_id = db.getParam("file_id");
        fileIdView.setText(file_id);


        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String fileStr = fileIdView.getText().toString();
                if (fileStr.contains("/"))
                    getFilePath(null, fileStr);
                else
                    getFileId(fileStr);
            }
        });
    }

    private void getFileId(final String fileId) {
        Log.d("fd", "getFile " + fileId);

        db.insertFav("file_id", fileId, getResources().getString(R.string.file_downloader));

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("file_id", fileId);
        } catch (JSONException e) {
            Log.e("fd", "getFile", e);
            return;
        }
        final String json = jsonObject.toString();

        final String url = "https://api.telegram.org/bot" + _token + "/getFile";


        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                String response = "";
                try {
                    final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

                    RequestBody body = RequestBody.create(JSON, json);
                    Request request = new Request.Builder()
                            .url(url)
                            .post(body)
                            .build();
                    OkHttpClient client = new OkHttpClient();
                    Response resp = client.newCall(request).execute();
                    response = resp.body().string();
                } catch (final MalformedURLException e) {
                    Log.e("fd", "Malformed URL", e);
                    showError(e.getLocalizedMessage());
                    return;
                } catch (final IOException e) {
                    Log.e("fd", "IO", e);
                    showError(e.getLocalizedMessage());
                    return;
                } catch (final NullPointerException e) {
                    Log.e("fd", "Null Pointer", e);
                    showError(e.getLocalizedMessage());
                    return;
                }

                final String filePath;
                try {
                    JSONObject jsonObject = new JSONObject(response);
                    if (!jsonObject.getBoolean("ok")) {
                        Log.w("fd", "getFile response ok=false");
                        showError("getFile response ok=false");
                        return;
                    }
                    JSONObject result = jsonObject.getJSONObject("result");
                    filePath = result.getString("file_path");
                } catch (JSONException e) {
                    Log.e("fd", "getFile", e);
                    showError(e.getLocalizedMessage());
                    return;
                }
                getFilePath(fileId, filePath);
            }
        });
        thread.start();
    }

    private void getFilePath(String fileId, String filePath) {
        if (null == fileId) {
            fileId = filePath.replaceAll("/", "-");
        }

        final URL url;
        try {
            url = new URL("https://api.telegram.org/file/bot" + _token + "/" + filePath);
        } catch (MalformedURLException e) {
            Log.e("fd", "getFile", e);
            showError(e.getLocalizedMessage());
            return;
        }

        final File downloadDir = new File(Environment.getExternalStorageDirectory() + "/TeleBot");
        if (!downloadDir.exists()) {
            if (!downloadDir.mkdir()) {
                Log.e("fd", "Fail to make directory");
                showError("Fail to make directory");
                return;
            }
        } else if (!downloadDir.isDirectory()) {
            Log.e("fd", "Directory is file");
            showError("Directory is file");
            return;
        }
        final String extName = FilenameUtils.getExtension(filePath);
        final boolean noExt = (null == extName || extName.length() == 0);
        final String fileName;
        if (noExt)
            fileName = fileId;
        else

            fileName = fileId + "." + extName;
        final File file = new File(downloadDir, fileName);

        final String mime;
        if (noExt)
            mime = "*/*";
        else
            mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extName);

        if (file.exists()) {
            Log.d("fd", "File already exists");
        } else {
            Log.d("fd", "Start download " + file.toString());
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    OkHttpClient client = new OkHttpClient();
                    Request request = new Request.Builder().url(url)
                            .build();
                    Response resp;
                    try {
                        resp = client.newCall(request).execute();
                    } catch (IOException e) {
                        Log.e("fd", "IO", e);
                        showError(e.getLocalizedMessage());
                        return;
                    }

                    if (resp.code() != 200) {
                        Log.e("fd", "Resp Code " + resp.code());
                        try {
                            showError("HTTP " + resp.code() + " " + resp.message() + "\n" + resp.body().string());
                        } catch (NullPointerException e) {
                            Log.e("fd", "Null Pointer", e);
                        } catch (IOException e) {
                            Log.e("fd", "IO", e);
                        }
                        return;
                    }

                    if (null == resp.body())

                    {
                        Log.e("fd", "Resp Body null");
                        showError("File Empty");
                        return;
                    }

                    InputStream in = resp.body().byteStream();
                    FileOutputStream fos;
                    try

                    {
                        fos = new FileOutputStream(file);
                    } catch (
                            FileNotFoundException e)

                    {
                        Log.e("fd", "File Not Found", e);
                        showError(e.getLocalizedMessage());
                        return;
                    }

                    BufferedInputStream bis = new BufferedInputStream(in);
                    try

                    {

                        int current;
                        while ((current = bis.read()) != -1) {
                            fos.write(current);
                        }
                        fos.close();
                    } catch (
                            IOException e)

                    {
                        Log.e("fd", "IO", e);
                        showError(e.getLocalizedMessage());
                        return;
                    }
                    resp.body().close();

                    String authority = context.getApplicationContext().getPackageName() + ".provider";
                    Uri fileURI = FileProvider.getUriForFile(context, authority, file);
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(fileURI, mime);
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                    startActivity(intent);
                }
            });
            thread.start();
        }
    }

    private void showError(final String str) {
        final InstantComplete view = (InstantComplete) findViewById(R.id.file_download_file_id);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                view.setError(str);
            }
        });
    }
}
