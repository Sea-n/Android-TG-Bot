package taipei.sean.telegram.botplayground;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.util.ArrayMap;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.v7.widget.Toolbar;
import android.view.View;

import java.util.List;

public class FavoriteActivity extends AppCompatActivity {
    final private int _dbVer = 2;
    final private int _reqCode_addFav = 1;
    private SeanDBHelper db = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorite);

        db = new SeanDBHelper(this, "data.db", null, _dbVer);

        Toolbar toolbar = (Toolbar) findViewById(R.id.fav_toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fav_fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addFav();
            }
        });
        ActionBar actionBar = getSupportActionBar();
        if (null != actionBar)
            actionBar.setDisplayHomeAsUpEnabled(true);

        initView();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        switch (requestCode) {
            case _reqCode_addFav:
                initView();
                break;
        }
    }

    private void initView() {
        final RecyclerView favRecyclerView = (RecyclerView) findViewById(R.id.fav_list);

        ArrayMap<String, FavoriteItemAdapter> favAdas = new ArrayMap<>();
        List<FavStructure> favs = db.getFavs(null);
        for (FavStructure fav: favs) {
            String kind = fav.kind;
            if (favAdas.containsKey(kind)) {
                FavoriteItemAdapter favAda = favAdas.get(kind);
                favAda.addData(fav);
            } else {
                FavoriteItemAdapter favAda = new FavoriteItemAdapter();
                favAda.addData(fav);
                favAdas.put(kind, favAda);
            }
        }

        FavoriteListAdapter favListAda = new FavoriteListAdapter();
        for (int i=0; i<favAdas.size(); i++) {
            String name = favAdas.keyAt(i);
            FavoriteItemAdapter favAda = favAdas.get(name);
            favListAda.addData(name, favAda);
        }
        favRecyclerView.setAdapter(favListAda);
        favRecyclerView.setLayoutManager(new StaggeredGridLayoutManager(1, StaggeredGridLayoutManager.VERTICAL));
        favRecyclerView.setItemViewCacheSize(favAdas.size());

    }

    private void addFav() {
        Intent addFavIntent = new Intent(FavoriteActivity.this, AddFavoriteActivity.class);
        startActivityForResult(addFavIntent, _reqCode_addFav);
    }
}