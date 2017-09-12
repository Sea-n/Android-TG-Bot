package taipei.sean.telegram.botplayground.adapter;

import android.content.Context;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import taipei.sean.telegram.botplayground.FavStructure;
import taipei.sean.telegram.botplayground.InstantComplete;
import taipei.sean.telegram.botplayground.R;
import taipei.sean.telegram.botplayground.SeanAdapter;
import taipei.sean.telegram.botplayground.SeanDBHelper;

public class ApiCallerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    final private Context context;
    final private int _dbVer = 4;
    private SeanDBHelper db;
    private ArrayList<JSONObject> iList;
    private ArrayList<String> iListName;
    private ArrayList<View> iListView;

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
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        TextInputLayout view = new TextInputLayout(parent.getContext());
        ViewGroup.LayoutParams params = new RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        view.setLayoutParams(params);

        return new DummyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        String type = "String";
        boolean req = false;
        String desc = "";
        int maxChar = -1;

        final JSONObject data = iList.get(position);
        final String name = iListName.get(position);
        try {
            if (data.has("type"))
                type = data.getString("type");
            if (data.has("required"))
                req = data.getBoolean("required");
            if (data.has("description"))
                desc = data.getString("description");
            if (data.has("maxChar"))
                maxChar = data.getInt("maxChar");
            Log.d("ada", "add " + name);
        } catch (JSONException e) {
            Log.e("caller", "ada", e);
        }



        TextInputLayout textInputLayout = (TextInputLayout) holder.itemView;
        textInputLayout.setHint(name);
        ViewGroup.LayoutParams layoutParams = textInputLayout.getLayoutParams();
        layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
        textInputLayout.setLayoutParams(layoutParams);

        iListView.add(textInputLayout);


        InstantComplete autoCompleteTextView = new InstantComplete(textInputLayout.getContext());

        if (req)
            autoCompleteTextView.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.ic_star_border_black_24dp, 0);

        switch (type) {
            case "Float number":
                autoCompleteTextView.setRawInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                autoCompleteTextView.setSingleLine();
                break;
            case "Boolean":
            case "Bool":
            case "Integer":
            case "int":
            case "int128":
            case "long":
                autoCompleteTextView.setRawInputType(InputType.TYPE_CLASS_NUMBER);
            case "Integer or String":
            case "string":
                autoCompleteTextView.setSingleLine();
                break;
        }

        if (maxChar > 0) {
            autoCompleteTextView.setFilters(new InputFilter[] {new InputFilter.LengthFilter(maxChar)});
        }

        String text = db.getParam(name);
        autoCompleteTextView.setText(text);

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

                db.updateParam(name, value);
            }
        });

        textInputLayout.addView(autoCompleteTextView);
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

    public View getViewByPos(int pos) {
        return iListView.get(pos);
    }

    private class DummyViewHolder extends RecyclerView.ViewHolder {
        public DummyViewHolder(View itemView) {
            super(itemView);
        }
    }
}