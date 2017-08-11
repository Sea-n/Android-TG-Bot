package taipei.sean.telegram.botplayground;

import android.content.Context;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputLayout;
import android.support.v7.widget.RecyclerView;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

class ApiCallerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private Context context;
    final private int _dbVer = 2;
    private SeanDBHelper db;
    private ArrayList<String> iList;
    private ArrayList<String> iListType;
    private ArrayList<Boolean> iListReq;
    private ArrayList<String> iListDesc;

    ApiCallerAdapter(Context context) {
        this.context = context;

        db = new SeanDBHelper(context, "data.db", null, _dbVer);

        iList = new ArrayList<>();
        iListType = new ArrayList<>();
        iListDesc = new ArrayList<>();
        iListReq = new ArrayList<>();
    }

    public void addData(String name, JSONObject data) {
        try {
            String type = data.getString("type");
            Boolean required = data.getBoolean("required");
            String desc = data.getString("description");

            iList.add(name);
            iListType.add(type);
            iListReq.add(required);
            iListDesc.add(desc);
        } catch (JSONException e) {
            Log.e("caller", "ada", e);
        }
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
        final String name = iList.get(position);
        final String type = iListType.get(position);
        final Boolean req = iListReq.get(position);
        final String desc = iListDesc.get(position);

        TextInputLayout textInputLayout = (TextInputLayout) holder.itemView;
        textInputLayout.setHint(name);
        ViewGroup.LayoutParams layoutParams = textInputLayout.getLayoutParams();
        layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
        textInputLayout.setLayoutParams(layoutParams);


        AutoCompleteTextView autoCompleteTextView = new AutoCompleteTextView(textInputLayout.getContext());

        if (req)
            autoCompleteTextView.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.ic_star_border_black_24dp, 0);

        switch (type) {
            case "Float number":
                autoCompleteTextView.setRawInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                autoCompleteTextView.setSingleLine();
                break;
            case "Boolean":
            case "Integer":
                autoCompleteTextView.setRawInputType(InputType.TYPE_CLASS_NUMBER);
            case "Integer or String":
                autoCompleteTextView.setSingleLine();
                break;
        }

        List<FavStructure> favs = db.getFavs(name);
        ArrayList<String> favList = new ArrayList<>();
        for (int i=0; i<favs.size(); i++)
            favList.add(favs.get(i).value);
        ArrayAdapter<String> favAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, favList);
        autoCompleteTextView.setAdapter(favAdapter);

        autoCompleteTextView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                Snackbar.make(view, desc, Snackbar.LENGTH_LONG).show();
                return false;
            }
        });

        textInputLayout.addView(autoCompleteTextView);
    }

    @Override
    public int getItemCount() {
        return iList.size();
    }

    private class DummyViewHolder extends RecyclerView.ViewHolder {
        public DummyViewHolder(View itemView) {
            super(itemView);
        }
    }
}