package taipei.sean.telegram.botplayground.adapter;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.util.Log;
import android.util.LruCache;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import taipei.sean.telegram.botplayground.FavStructure;
import taipei.sean.telegram.botplayground.InstantComplete;
import taipei.sean.telegram.botplayground.R;
import taipei.sean.telegram.botplayground.SeanAdapter;
import taipei.sean.telegram.botplayground.SeanDBHelper;
import taipei.sean.telegram.botplayground.TelegramAPI;

public class ApiCallerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    final public static int TYPE_BOOLEAN = 1;
    final public static int TYPE_INTEGER = 2;
    final public static int TYPE_FLOAT = 4;
    final public static int TYPE_STRING = 8;
    final public static int TYPE_JSON = 16;
    final public static int TYPE_FILE = 32;
    final private Context context;
    final private int _dbVer = 4;
    RecyclerView mRecyclerView;
    private SeanDBHelper db;
    private ArrayList<JSONObject> iList;
    private ArrayList<String> iListName;
    private ArrayList<View> iListView;
    public boolean modified = false;

    public ApiCallerAdapter(Context context) {
        this.context = context;

        db = new SeanDBHelper(context, "data.db", null, _dbVer);

        iList = new ArrayList<>();
        iListView = new ArrayList<>();
        iListName = new ArrayList<>();
    }

    public void addData(String name, JSONObject data) {
        iList.add(data);
        iListName.add(name);
    }


    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);

        mRecyclerView = recyclerView;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        TextInputLayout view = new TextInputLayout(parent.getContext());
        ViewGroup.LayoutParams params = new RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        view.setLayoutParams(params);

        return new DummyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        String _type = "String";
        int type = 0;
        boolean req = false;
        String desc = "";
        int maxChar = -1;

        final JSONObject data = iList.get(position);
        final String name = iListName.get(position);
        try {
            if (data.has("type"))
                _type = data.getString("type");
            if (data.has("required"))
                req = data.getBoolean("required");
            if (data.has("description"))
                desc = data.getString("description");
            if (data.has("maxChar"))
                maxChar = data.getInt("maxChar");
            Log.d("ada", "add " + name);
        } catch (JSONException e) {
            Log.e("ada", "ada", e);
        }


        TextInputLayout textInputLayout = (TextInputLayout) holder.itemView;
        textInputLayout.setHint(name);
        ViewGroup.LayoutParams layoutParams = textInputLayout.getLayoutParams();
        layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
        textInputLayout.setLayoutParams(layoutParams);

        if (_type.contains("Boolean"))
            type |= TYPE_BOOLEAN;
        if (_type.contains("Integer"))
            type |= TYPE_INTEGER;
        if (_type.contains("Float"))
            type |= TYPE_FLOAT;
        if (_type.contains("String"))
            type |= TYPE_STRING;
        if (_type.contains("Array") || _type.contains("KeyboardMarkup") || _type.contains("MaskPosition"))
            type |= TYPE_JSON;
        if (_type.contains("InputFile"))
            type |= TYPE_FILE;

        final InstantComplete autoCompleteTextView = new InstantComplete(textInputLayout.getContext());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            final int DRAWABLE_RIGHT = 2;
            int end = 0;

            if (req) {
                end = R.drawable.ic_star_border_black_24dp;
                autoCompleteTextView.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        if (event.getAction() == MotionEvent.ACTION_UP) {
                            int drawableWidth = autoCompleteTextView.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width();
                            if (event.getRawX() >= (autoCompleteTextView.getRight() - drawableWidth)) {
                                Snackbar.make(autoCompleteTextView, R.string.field_required, Snackbar.LENGTH_SHORT).show();
                            }
                        }
                        return false;
                    }
                });
            }

            if ((type & TYPE_FILE) > 0) {
                end = R.drawable.ic_cloud_upload_black_24dp;
                autoCompleteTextView.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        if (event.getAction() == MotionEvent.ACTION_UP) {
                            int drawableWidth = autoCompleteTextView.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width();
                            if (event.getRawX() >= (autoCompleteTextView.getRight() - drawableWidth)) {
                                db.updateParam("_file", name);
                                try {
                                    Intent intent;
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                                        intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                                        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
                                    } else {
                                        intent = new Intent(Intent.ACTION_GET_CONTENT);
                                    }
                                    intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
                                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                    intent.setType("*/*");

                                    ((Activity) context).startActivityForResult(Intent.createChooser(intent, "Select a File to Upload"), 87);
                                } catch (ActivityNotFoundException e) {
                                    Log.e("ada", "no file manager", e);
                                    // Potentially direct the user to the Market with a Dialog
                                    Snackbar.make(autoCompleteTextView, "Please install a File Manager.", Snackbar.LENGTH_LONG).show();
                                }
                            }
                        }
                        return false;
                    }
                });
            }

            autoCompleteTextView.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, end, 0);
        }


        if (type == TYPE_BOOLEAN) {
            CheckBox checkBox = new CheckBox(context);
            checkBox.setTextColor(Color.BLACK);
            checkBox.setText(name);

            final String finalDesc = desc;
            checkBox.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    Snackbar.make(view, finalDesc, Snackbar.LENGTH_LONG).show();
                    return false;
                }
            });

            textInputLayout.addView(checkBox);
            iListView.add(checkBox);
        } else {
            if (type == TYPE_INTEGER) {
                autoCompleteTextView.setRawInputType(InputType.TYPE_CLASS_NUMBER);
                autoCompleteTextView.setSingleLine();
            }
            if (type == TYPE_FLOAT) {
                autoCompleteTextView.setRawInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                autoCompleteTextView.setSingleLine();
            }

            if (maxChar > 0) {
                autoCompleteTextView.setFilters(new InputFilter[]{new InputFilter.LengthFilter(maxChar)});
            }

            String text = db.getParam(name);
            if (text.equals("") && data.has("default")) {
                try {
                    text = data.getString("default");
                } catch (JSONException e) {
                    Log.e("ada", "default", e);
                }
            }
            autoCompleteTextView.setText(text);

            if ((type & TYPE_JSON) != 0) {
                TelegramAPI.jsonColor((SpannableStringBuilder) autoCompleteTextView.getEditableText());
            }


            if (data.has("maxInt")) {
                int min = 0;
                int max = -1;
                try {
                    max = data.getInt("maxInt");
                    if (data.has("minInt"))
                        min = data.getInt("minInt");
                } catch (JSONException e) {
                    Log.e("ada", "range", e);
                }

                final int finalMin = min;
                final int finalMax = max;
                autoCompleteTextView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                    @Override
                    public void onFocusChange(View view, boolean hasFocus) {
                        if (hasFocus)
                            return;
                        EditText editText = (EditText) view;
                        String valStr = editText.getText().toString();
                        int val = -1;
                        try {
                            val = Integer.parseInt(valStr);
                        } catch (NumberFormatException e) {
                            Log.w("ada", "parse", e);
                        }
                        if (val >= 0 && (val < finalMin || finalMax < val)) {
                            String errorMsg = context.getString(R.string.param_out_of_range, finalMin, finalMax);
                            editText.setError(errorMsg);
                        }
                    }
                });
            }

            List<FavStructure> favs = db.getFavs(name);
            ArrayList<String> favList = new ArrayList<>();
            for (int i = 0; i < favs.size(); i++)
                favList.add(favs.get(i).value);
            SeanAdapter<String> favAdapter = new SeanAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, favList);
            autoCompleteTextView.setAdapter(favAdapter);

            final String finalDesc = desc;
            autoCompleteTextView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    Snackbar.make(view, finalDesc, Snackbar.LENGTH_LONG).show();
                    return false;
                }
            });

            final int finalType = type;
            autoCompleteTextView.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                }

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                }

                @Override
                public void afterTextChanged(Editable editable) {
                    String value = editable.toString();

                    if ((finalType & TYPE_JSON) != 0 && value.length() > 0) {
                        try {
                            Gson gson = new GsonBuilder().setPrettyPrinting().create();
                            JsonParser jp = new JsonParser();
                            JsonElement je;
                            je = jp.parse(value);
                            String json = gson.toJson(je);
                            if (je.isJsonArray() || je.isJsonObject()) {
                                if (!json.equals(value)) {
                                    value = json;   // for update parameter
                                    editable.clear();
                                    editable.append(json);
                                }

                                TelegramAPI.jsonColor((SpannableStringBuilder) editable);
                            }
                        } catch (JsonSyntaxException e) {
                            e.getMessage();   // Do nothing
                        }
                    }

                    db.updateParam(name, value);
                    modified = true;
                }
            });

            textInputLayout.addView(autoCompleteTextView);
            iListView.add(autoCompleteTextView);
        }
    }

    @Override
    public int getItemCount() {
        return iList.size();
    }

    public void fitView(RecyclerView recyclerView) {
        final int maxHeightPercentage = 30;
        final int fitHeightPercentage = 25;

        final int screenHeight = recyclerView.getRootView().getHeight();
        recyclerView.measure(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);   // Measure height when wrap content
        final int recyclerHeight = recyclerView.getMeasuredHeight();
        Log.d("adapter", "screen height " + screenHeight);
        Log.d("adapter", "recycler view height " + recyclerHeight);

        ViewGroup.LayoutParams inputLayoutParams = recyclerView.getLayoutParams();
        if (recyclerHeight > (screenHeight * maxHeightPercentage / 100)) {   // If child exceed max% of screen height
            inputLayoutParams.height = (screenHeight * fitHeightPercentage / 100);   // Set height to fit% of screen
        } else {
            inputLayoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;   // Under max% of screen height, just wrap content
        }
        recyclerView.setLayoutParams(inputLayoutParams);   // Set Layout Parameter to original view
    }

    public String getName(int pos) {
        return iListName.get(pos);
    }

    public String getValue(int pos) {
        View view = iListView.get(pos);

        if (view instanceof EditText) {
            EditText editText = (EditText) view;
            CharSequence valueChar = editText.getText();
            if (null == valueChar) {
                Log.w("ada", "value char null");
                return "";
            }
            return valueChar.toString();
        } else if (view instanceof CheckBox) {
            CheckBox checkBox = (CheckBox) view;
            if (checkBox.isChecked())
                return "1";
            else
                return null;
        } else {
            Log.e("ada", "Unknown view " + view.toString());
            return null;
        }
    }

    public Bitmap getScreenshot() {
        Bitmap bigBitmap = null;
        int size = getItemCount();
        int height = 0;
        Paint paint = new Paint();
        int iHeight = 0;
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);

        // Use 1/8th of the available memory for this memory cache.
        final int cacheSize = maxMemory / 8;
        LruCache<String, Bitmap> bitmaCache = new LruCache<>(cacheSize);
        for (int i = 0; i < size; i++) {
            if (null == getValue(i) || getValue(i).length() == 0)
                continue;
            RecyclerView.ViewHolder holder = this.createViewHolder(mRecyclerView, getItemViewType(i));
            onBindViewHolder(holder, i);
            holder.itemView.measure(View.MeasureSpec.makeMeasureSpec(mRecyclerView.getWidth(), View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            holder.itemView.layout(0, 0, holder.itemView.getMeasuredWidth(), holder.itemView.getMeasuredHeight());
            holder.itemView.setDrawingCacheEnabled(true);
            holder.itemView.buildDrawingCache();
            Bitmap drawingCache = holder.itemView.getDrawingCache();
            if (drawingCache != null) {
                bitmaCache.put(String.valueOf(i), drawingCache);
            }
            height += holder.itemView.getMeasuredHeight();
        }

        if (height == 0)
            return null;

        bigBitmap = Bitmap.createBitmap(mRecyclerView.getMeasuredWidth(), height, Bitmap.Config.ARGB_8888);
        Canvas bigCanvas = new Canvas(bigBitmap);

        for (int i = 0; i < size; i++) {
            if (null == getValue(i) || getValue(i).length() == 0)
                continue;
            Bitmap bitmap = bitmaCache.get(String.valueOf(i));
            bigCanvas.drawBitmap(bitmap, 0f, iHeight, paint);
            iHeight += bitmap.getHeight();
            bitmap.recycle();
        }

        return bigBitmap;
    }

    public JSONObject getJson(String method) {
        JSONObject jsonObject = new JSONObject();
        final int inputCount = getItemCount();
        for (int i = 0; i < inputCount; i++) {
            String name = getName(i);
            String value = getValue(i);

            if (null == value)
                continue;
            if (value.equals(""))
                continue;

            if (method != null)
                db.insertFav(name, value, method);

            try {
                JSONObject valueJson = new JSONObject(value);   // if can be JSON Object
                jsonObject.put(name, valueJson);   // treat as JSON Object
            } catch (JSONException e1) {
                try {
                    JSONArray valueJson = new JSONArray(value);   // if not Object, but can be Array
                    jsonObject.put(name, valueJson);   // treat as Array
                } catch (JSONException e2) {
                    try {
                        jsonObject.put(name, value);   // not JSON, treat as string
                    } catch (JSONException e3) {
                        Log.e("ada", "put", e3);   // Can't put value to jsonObject
                    }
                }
            }
        }
        return jsonObject;
    }

    private class DummyViewHolder extends RecyclerView.ViewHolder {
        public DummyViewHolder(View itemView) {
            super(itemView);
        }
    }
}