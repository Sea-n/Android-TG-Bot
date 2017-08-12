package taipei.sean.telegram.botplayground.adapter;

import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.HorizontalScrollView;
import android.widget.TextView;

import java.util.ArrayList;

import taipei.sean.telegram.botplayground.activity.AddFavoriteActivity;
import taipei.sean.telegram.botplayground.FavStructure;

public class FavoriteItemAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private ArrayList<FavStructure> iList;

    public FavoriteItemAdapter() {
        iList = new ArrayList<>();
    }

    public void addData(FavStructure fav) {
        iList.add(fav);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        HorizontalScrollView view = new HorizontalScrollView(parent.getContext());
        ViewGroup.LayoutParams params = new RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        view.setLayoutParams(params);

        return new DummyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        final FavStructure fav = iList.get(position);

        HorizontalScrollView horizontalScrollView = (HorizontalScrollView) holder.itemView;

        GridLayout gridLayout = new GridLayout(horizontalScrollView.getContext());
        gridLayout.setColumnCount(2);
        gridLayout.setOrientation(GridLayout.HORIZONTAL);
        gridLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent mIntent = new Intent(view.getContext(), AddFavoriteActivity.class);
                mIntent.putExtra("id", fav._id);
                view.getContext().startActivity(mIntent);
            }
        });

        GridLayout.LayoutParams valLayoutParams = new GridLayout.LayoutParams();
        valLayoutParams.width = 400;
        TextView valView = new TextView(gridLayout.getContext());
        valView.setText(fav.value);
        gridLayout.addView(valView, valLayoutParams);

        GridLayout.LayoutParams nameLayoutParams = new GridLayout.LayoutParams();
        nameLayoutParams.width = ViewGroup.LayoutParams.WRAP_CONTENT;
        TextView nameView = new TextView(gridLayout.getContext());
        nameView.setText(fav.name);
        gridLayout.addView(nameView, nameLayoutParams);

        horizontalScrollView.addView(gridLayout);
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