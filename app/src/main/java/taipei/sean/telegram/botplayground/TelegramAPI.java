package taipei.sean.telegram.botplayground;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.SpannedString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TelegramAPI {
    final private int _dbVer = 4;
    final private String _apiBaseUrl;
    final private Context _context;
    private SeanDBHelper db;
    public JSONObject latestResponse;

    public TelegramAPI(Context context, String token) {
        this._context = context;
        _apiBaseUrl = "https://api.telegram.org/bot" + token;
    }

    public void callApi(final String method, final TextView resultView, @Nullable JSONObject j) {
        final JSONObject json;
        if (null == j)
            json = new JSONObject();
        else
            json = j;

        final String jsonStr = json.toString();

        db = new SeanDBHelper(_context, "data.db", null, _dbVer);

        Log.d("api", method + jsonStr);

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonParser jp = new JsonParser();
        JsonElement je = jp.parse(jsonStr);
        String prettyJson = gson.toJson(je);
        SpannableStringBuilder jsonSpannable = new SpannableStringBuilder(prettyJson);
        jsonColor(jsonSpannable);

        SpannableString methodSpannable = new SpannableString(method);
        methodSpannable.setSpan(new ForegroundColorSpan(Color.MAGENTA), 0, method.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        SpannedString resultText = (SpannedString) TextUtils.concat(methodSpannable, jsonSpannable);
        resultView.setText(resultText);

        final String url = _apiBaseUrl + "/" + method;

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                String respStr = "";
                try {
                    MultipartUtility multipart = new MultipartUtility(_context, url);

                    Iterator<String> keys = json.keys();
                    while (keys.hasNext()) {
                        final String key = keys.next();

                        try {
                            Object object = json.get(key);
                            String value = object.toString();
                            if (value.startsWith("content://")) {
                                try {
                                    Uri uri = Uri.parse(value);
                                    String path = getPath(uri);
                                    File file = new File(path);
                                    multipart.addFilePart(key, file);
                                } catch (Exception e) {
                                    Log.e("api", "file", e);
                                    multipart.addFormField(key, value);
                                }
                            } else {
                                multipart.addFormField(key, value);
                            }
                        } catch (JSONException e) {
                            Log.e("api", "parsing", e);
                        }
                    }
                    multipart.addFormField("_sender", "Awesome Telegram Bot");
                    respStr = multipart.finish();
                } catch (final Exception e) {
                    Log.e("api", "send request", e);
                    final String finalResultText = e.getLocalizedMessage();
                    Handler handler = new Handler(_context.getMainLooper());
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            resultView.setText(finalResultText);
                        }
                    });
                }

                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                JsonParser jp = new JsonParser();
                JsonElement je;
                String json = "";
                try {
                    je = jp.parse(respStr);
                    json = gson.toJson(je);
                } catch (JsonSyntaxException e) {
                    Log.e("api", "parse", e);
                    final String finalResultText = respStr;
                    Handler handler = new Handler(_context.getMainLooper());
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            resultView.setText(finalResultText);
                        }
                    });
                }
                Log.v("api", "resp:" + json);

                final SpannableStringBuilder jsonSpannable = new SpannableStringBuilder(json);
                jsonColor(jsonSpannable);
                Handler handler = new Handler(_context.getMainLooper());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        resultView.setText(jsonSpannable);
                    }
                });


                if (null != json) {
                    try {
                        latestResponse = new JSONObject(json);
                        boolean status = latestResponse.getBoolean("ok");
                        if (status) {

                            Pattern p = Pattern.compile("\"file_id\": \"([^\"]+)\"");
                            Matcher m = p.matcher(json);
                            while (m.find()) {
                                String fileId = m.group(1);
                                db.insertFav("file_id", fileId, method);
                            }
                        }
                    } catch (JSONException e) {
                        Log.e("api", "parse fav", e);
                    }
                }
            }
        });
        thread.start();
    }

    public static SpannableStringBuilder jsonColor(SpannableStringBuilder spannable) {
        String string = spannable.toString();
        int pos, posB, posE;
        for (pos = posB = 0, posE = string.indexOf("\n"); posE != -1; pos = posB = posE + 1, posE = string.indexOf("\n", posB)) {   // Missed first line
            while (spannable.charAt(pos) == ' ')   // intend space
                ++pos;

            if (spannable.charAt(pos) == '"') {   // string key
                int posT = pos;

                do ++pos;   // key, didn't consider about escape
                while (pos != posE && (spannable.charAt(pos) != '"' || spannable.charAt(pos) != '\n'));

                spannable.setSpan(new ForegroundColorSpan(Color.rgb(0x79, 0x5d, 0xa3)), posT, ++pos, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                if (spannable.charAt(pos) == ':') {
                    spannable.setSpan(new ForegroundColorSpan(Color.BLACK), pos, ++pos, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);   // :
                    ++pos;   // space after ":"
                }
            }


            if (spannable.charAt(pos) == '"') {   // string value
                int posT = pos;
                do ++pos;   // value, didn't consider about escape
                while (spannable.charAt(pos) != '"');

                spannable.setSpan(new ForegroundColorSpan(Color.rgb(0xdf, 0x50, 0)), posT, ++pos, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);   // "
            } else if (spannable.charAt(pos) == 't')   // true
                spannable.setSpan(new ForegroundColorSpan(Color.rgb(0, 0x86, 0xb3)), pos, pos + 4, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            else if (spannable.charAt(pos) == 'f')   // false
                spannable.setSpan(new ForegroundColorSpan(Color.rgb(0, 0x86, 0xb3)), pos, pos + 5, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            else if (spannable.charAt(pos) == 'n')   // null
                spannable.setSpan(new ForegroundColorSpan(Color.rgb(0, 0x86, 0xb3)), pos, ++pos + 4, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            else if (spannable.charAt(pos) == '[')   // array
                spannable.setSpan(new ForegroundColorSpan(Color.BLACK), pos, ++pos, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            else if (spannable.charAt(pos) == '{')   // object
                spannable.setSpan(new ForegroundColorSpan(Color.BLACK), pos, ++pos, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            else if (Character.isDigit(spannable.charAt(pos)) || spannable.charAt(pos) == '-') {   // signed number
                int posT = pos;
                do ++pos;
                while (Character.isDigit(spannable.charAt(pos)));
                spannable.setSpan(new ForegroundColorSpan(Color.rgb(0, 0x80, 0x80)), posT, pos, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }


            if (posE > 1) {
                pos = posE - 1;  // pos before newline
                if (spannable.charAt(pos) == ',')
                    spannable.setSpan(new ForegroundColorSpan(Color.BLACK), pos, ++pos, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                else if (spannable.charAt(pos) == ']')   // array
                    spannable.setSpan(new ForegroundColorSpan(Color.BLACK), pos, ++pos, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                else if (spannable.charAt(pos) == '}')   // object
                    spannable.setSpan(new ForegroundColorSpan(Color.BLACK), pos, ++pos, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        if (spannable.length() > 2)  // more than 2 line
            spannable.setSpan(new ForegroundColorSpan(Color.BLACK), spannable.length() - 1, spannable.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);   // latest "}"

        return spannable;
    }


    private String getPath(final Uri uri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && DocumentsContract.isDocumentUri(_context, uri)) {   // DocumentProvider
            if ("com.android.externalstorage.documents".equals(uri.getAuthority())) {   // ExternalStorageProvider
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }

            } else if ("com.android.providers.downloads.documents".equals(uri.getAuthority())) {   // DownloadsProvider

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(_context, contentUri, null, null);
            } else if ("com.android.providers.media.documents".equals(uri.getAuthority())) {   // MediaProvider
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{
                        split[1]
                };

                return getDataColumn(_context, contentUri, selection, selectionArgs);
            }
        } else if ("content".equalsIgnoreCase(uri.getScheme())) {   // MediaStore (and general)

            // Return the remote address
            if ("com.google.android.apps.photos.content".equals(uri.getAuthority()))
                return uri.getLastPathSegment();

            return getDataColumn(_context, uri, null, null);
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {   // File
            return uri.getPath();
        }

        return null;
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context       The context.
     * @param uri           The Uri to query.
     * @param selection     (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    public String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }
}

