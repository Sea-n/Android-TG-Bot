package taipei.sean.telegram.botplayground.adapter;

import android.content.Intent;
import android.os.Build;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.TextView;

import java.util.ArrayList;

import taipei.sean.telegram.botplayground.FavStructure;
import taipei.sean.telegram.botplayground.activity.AddFavoriteActivity;

public class FavoriteItemAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private ArrayList<FavStructure> iList;
    private int parentWidth;

    public FavoriteItemAdapter(int width) {
        iList = new ArrayList<>();
        parentWidth = width;
    }

    public void addData(FavStructure fav) {
        iList.add(fav);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        GridLayout view = new GridLayout(parent.getContext());
        ViewGroup.LayoutParams params = new RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        view.setLayoutParams(params);

        view.setColumnCount(2);
        view.setOrientation(GridLayout.HORIZONTAL);

        return new DummyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        final FavStructure fav = iList.get(position);

        GridLayout gridLayout = (GridLayout) holder.itemView;

        gridLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent mIntent = new Intent(view.getContext(), AddFavoriteActivity.class);
                mIntent.putExtra("id", fav._id);
                view.getContext().startActivity(mIntent);
            }
        });

        final int valWidth = parentWidth * 2 / 3;
        final int nameWidth = parentWidth - valWidth;

        TextView valView = new TextView(gridLayout.getContext());
        valView.setText(fav.value);
        valView.setWidth(valWidth);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
            valView.setPaddingRelative(0, 0, 20, 0);
        gridLayout.addView(valView);

        TextView nameView = new TextView(gridLayout.getContext());
        nameView.setText(fav.name);
        nameView.setWidth(nameWidth);
        gridLayout.addView(nameView);
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