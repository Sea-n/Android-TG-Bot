package taipei.sean.telegram.botplayground;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
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
        List<FavStructure> chats = db.getFavs("chat_id");
        RecyclerView chatList = (RecyclerView) findViewById(R.id.fav_chat_id_list);
        FavoriteAdapter chatAdapter = new FavoriteAdapter();
        for (FavStructure fav: chats) {
            chatAdapter.addData(fav);
        }
        chatList.setAdapter(chatAdapter);
        chatList.setLayoutManager(new StaggeredGridLayoutManager(1, StaggeredGridLayoutManager.VERTICAL));
        chatList.setItemViewCacheSize(chats.size());

        List<FavStructure> msg = db.getFavs("msg");
        RecyclerView msgList = (RecyclerView) findViewById(R.id.fav_msg_list);
        FavoriteAdapter msgAdapter = new FavoriteAdapter();
        for (FavStructure fav: msg) {
            msgAdapter.addData(fav);
        }
        msgList.setAdapter(msgAdapter);
        msgList.setLayoutManager(new StaggeredGridLayoutManager(1, StaggeredGridLayoutManager.VERTICAL));
        msgList.setItemViewCacheSize(msg.size());

    }

    private void addFav() {
        Intent addFavIntent = new Intent(FavoriteActivity.this, AddFavoriteActivity.class);
        startActivityForResult(addFavIntent, _reqCode_addFav);
    }
}