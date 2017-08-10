package taipei.sean.telegram.botplayground;

import android.content.Intent;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v7.widget.RecyclerView;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

class ApiCallerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private ArrayList<String> iList;
    private ArrayList<String> iListType;
    private ArrayList<Boolean> iListReq;
    private ArrayList<String> iListDesc;

    ApiCallerAdapter() {
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


        TextInputEditText textInputEditText = new TextInputEditText(textInputLayout.getContext());

        if (req)
            textInputEditText.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.ic_star_border_black_24dp, 0);

        switch (type) {
            case "Boolean":
            case "Integer":
                textInputEditText.setRawInputType(InputType.TYPE_CLASS_NUMBER);
                break;
            case "Float number":
                textInputEditText.setRawInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                break;
        }

        textInputLayout.addView(textInputEditText);
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