package taipei.sean.telegram.botplayground.adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Objects;


public class AboutAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    final private Context context;
    private ArrayList<JSONObject> list;

    public AboutAdapter(Context context) {
        this.context = context;

        list = new ArrayList<>();
    }

    public void addData(JSONObject json) {
        list.add(json);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LinearLayout view = new LinearLayout(parent.getContext());
        ViewGroup.LayoutParams params = new RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        view.setLayoutParams(params);

        return new DummyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        final JSONObject json = list.get(position);

        LinearLayout linearLayout = (LinearLayout) holder.itemView;
        linearLayout.setOrientation(LinearLayout.VERTICAL);

        try {
            if (json.has("header")) {
                String title = json.getString("title");
                newHeader(linearLayout, title);
            } else {
                String title = json.getString("title");
                String desc = json.getString("desc");
                String url = json.getString("url");
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                newItem(linearLayout, title, desc, intent);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void newItem(final LinearLayout linearLayout, final String title, final String desc, final Intent intent) {
        TextView titleView = new TextView(context);
        titleView.setText(title);
        titleView.setTextSize(16);
        titleView.setTextColor(Color.BLACK);
        titleView.setPaddingRelative(40, 20, 40, 3);
        linearLayout.addView(titleView);

        if (!Objects.equals(desc, "")) {
            TextView descView = new TextView(context);
            descView.setText(desc);
            descView.setTextSize(12);
            descView.setTextColor(Color.GRAY);
            linearLayout.addView(descView);


            titleView.setPaddingRelative(40, 30, 40, 3);
            descView.setPaddingRelative(40, 3, 40, 20);
        } else {
            titleView.setPaddingRelative(40, 40, 40, 40);
        }

        View dividerView = new View(context);
        dividerView.setBackgroundColor(Color.GRAY);
        dividerView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
        linearLayout.addView(dividerView);

        linearLayout.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                context.startActivity(intent);
                return true;
            }
        });
    }

    private void newHeader(final LinearLayout linearLayout, final String title) {
        TextView titleView = new TextView(context);
        titleView.setText(title);
        titleView.setTextSize(15);
        titleView.setTextColor(Color.rgb(0x10, 0x30, 0xc0));
        titleView.setPaddingRelative(40, 60, 40, 10);
        linearLayout.addView(titleView);
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    private class DummyViewHolder extends RecyclerView.ViewHolder {
        public DummyViewHolder(View itemView) {
            super(itemView);
        }
    }
}
