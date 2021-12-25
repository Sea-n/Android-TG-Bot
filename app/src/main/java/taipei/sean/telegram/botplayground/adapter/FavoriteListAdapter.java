package taipei.sean.telegram.botplayground.adapter;

import android.graphics.Color;
import android.os.Build;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

public class FavoriteListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private ArrayList<String> iList;
    private ArrayList<FavoriteItemAdapter> iListAda;

    public FavoriteListAdapter() {
        iList = new ArrayList<>();
        iListAda = new ArrayList<>();
    }

    public void addData(String kind, FavoriteItemAdapter favAda) {
        iList.add(kind);
        iListAda.add(favAda);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LinearLayout view = new LinearLayout(parent.getContext());
        ViewGroup.LayoutParams params = new RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        view.setLayoutParams(params);

        return new DummyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        final String kind = iList.get(position);
        final FavoriteItemAdapter favAda = iListAda.get(position);

        LinearLayout linearLayout = (LinearLayout) holder.itemView;
        ViewGroup.LayoutParams linearLayoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        linearLayout.setLayoutParams(linearLayoutParams);
        linearLayout.setOrientation(LinearLayout.VERTICAL);


        TextView titleTextView = new TextView(linearLayout.getContext());
        titleTextView.setText(kind);
        titleTextView.setTextSize(20f);
        titleTextView.setTextColor(Color.rgb(0x20, 0x40, 0x80));
        titleTextView.setGravity(Gravity.CENTER);
        ViewGroup.LayoutParams titleLayoutParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        titleTextView.setLayoutParams(titleLayoutParams);

        RecyclerView recyclerView = new RecyclerView(linearLayout.getContext());
        recyclerView.setItemViewCacheSize(favAda.getItemCount());
        recyclerView.setLayoutManager(new LinearLayoutManager(linearLayout.getContext()));
//        ViewGroup.LayoutParams layoutParams = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
//        recyclerView.setLayoutParams(layoutParams);
//        LinearLayoutManager layoutManager
//                = new LinearLayoutManager(linearLayout.getContext(), LinearLayoutManager.VERTICAL, false);
//        recyclerView.setLayoutManager(layoutManager);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
            recyclerView.setPaddingRelative(0, 0, 0, 50);
        recyclerView.setAdapter(favAda);

        linearLayout.addView(titleTextView);
        linearLayout.addView(recyclerView);
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