package taipei.sean.telegram.botplayground;

import android.content.Intent;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

class MyAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private ArrayList<FavStructure> iList;

    MyAdapter() {
        iList = new ArrayList<>();
    }

    public void addData(FavStructure fav) {
        iList.add(fav);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        GridLayout view = new GridLayout(parent.getContext());
        ViewGroup.LayoutParams params = new RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        view.setLayoutParams(params);

        return new DummyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        final FavStructure fav = iList.get(position);

        GridLayout gridLayout = (GridLayout) holder.itemView;
        ViewGroup.LayoutParams gridLayoutParams = gridLayout.getLayoutParams();
        gridLayoutParams.width = 1000;
        gridLayout.setLayoutParams(gridLayoutParams);
        gridLayout.setColumnCount(2);
        gridLayout.setOrientation(GridLayout.HORIZONTAL);
        gridLayout.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                Intent mIntent = new Intent(view.getContext(), AddFavoriteActivity.class);
                mIntent.putExtra("id", fav._id);
                view.getContext().startActivity(mIntent);

                return false;
            }
        });
        gridLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Long Click to Edit", Toast.LENGTH_SHORT).show();
            }
        });

        GridLayout.LayoutParams valLayoutParams = new GridLayout.LayoutParams();
        valLayoutParams.columnSpec = GridLayout.spec(0, 1, 1f);
        valLayoutParams.width = 400;
        TextView valView = new TextView(gridLayout.getContext());
        valView.setText(fav.value);
        gridLayout.addView(valView, valLayoutParams);

        GridLayout.LayoutParams nameLayoutParams = new GridLayout.LayoutParams();
        nameLayoutParams.columnSpec = GridLayout.spec(1, 1, 1f);
        TextView nameView = new TextView(gridLayout.getContext());
        nameView.setText(fav.name);
        gridLayout.addView(nameView, nameLayoutParams);
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