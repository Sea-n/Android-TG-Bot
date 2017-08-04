package taipei.sean.telegram.botplayground;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.TextView;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    private SeanDBHelper db;
    private List<BotStructure> _bots = null;
    private boolean changeAccountMenuOpen = false;
    private BotStructure currentBot = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = new SeanDBHelper(this, "data.db", null, 1);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addBot();
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                return navItemSelected(item);
            }
        });

        initAccount();

    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
            restoreMenu();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id) {
            case R.id.action_settings:
                Log.d("option", "Press Setting");
                break;
            default:
                Log.d("option", "Press" + id);
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    public boolean navItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        int gid = item.getGroupId();
        int id = item.getItemId();

        switch (gid) {
            case R.id.menu_accounts:
                changeToken(id);
                break;
            default:
                switch (id) {
                    case R.id.nav_sendMessage:
                        Log.d("nav", "press sendMessage");
                        if (null == currentBot) {
                            Log.w("nav", "no bots");
                            break;
                        }
                        Intent sIntent = new Intent(MainActivity.this, SendMessageActivity.class);
                        sIntent.putExtra("token", currentBot.token);
                        startActivity(sIntent);
                        break;
                    case R.id.nav_caller:
                        Log.d("nav", "press free api caller");
                        if (null == currentBot) {
                            Log.w("nav", "no bots");
                            break;
                        }
                        Intent mIntent = new Intent(MainActivity.this, ApiCallerActivity.class);
                        mIntent.putExtra("token", currentBot.token);
                        startActivity(mIntent);
                        break;
                    case R.id.nav_add_bot:
                        Log.d("nav", "press add bot");
                        addBot();
                        break;
                    case R.id.nav_join_group:
                        Log.d("nav", "press share");
                        joinGroup();
                        break;
                    default:
                        Log.w("nav", "press unknown item" + id);
                        break;
                }
                break;
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public void addBot() {
        Intent mIntent = new Intent(MainActivity.this, AddBotActivity.class);
        startActivity(mIntent);
    }

    public void initAccount() {
        NavigationView navView = (NavigationView) findViewById(R.id.nav_view);
        Menu navMenu = navView.getMenu();
        MenuItem menuItem = navMenu.findItem(R.id.menu_accounts_item);
        SubMenu subMenu = menuItem.getSubMenu();

        _bots = db.getBots();
        for (int i = 0; i < _bots.size(); i++) {
            BotStructure bot = _bots.get(i);
            Log.d("main", "append bot " + bot.name);
            subMenu.add(R.id.menu_accounts, Menu.FIRST + i, Menu.NONE, bot.name);
        }

        SharedPreferences settings = getSharedPreferences("data", MODE_PRIVATE);
        final int id = settings.getInt("currentBotId", -1);
        if (id > 0) {
            Thread thread = new Thread() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(200);   /// Wait for UI ready
                    } catch (InterruptedException e) {
                        Log.e("main", "sleep", e);
                    }
                    Log.d("main", "restore bot " + id);
                    changeToken(id);
                }
            };
            thread.start();
        }
    }

    public void changeAccount(View view) {
        if (changeAccountMenuOpen) {
            Log.d("main", "close change acount list");
            restoreMenu();
            return;
        }

        Log.d("main", "change account");

        NavigationView navView = (NavigationView) findViewById(R.id.nav_view);
        Menu menu = navView.getMenu();

        menu.setGroupVisible(R.id.menu_api, false);
        menu.setGroupVisible(R.id.menu_accounts, true);
        menu.getItem(2).setVisible(true);
        changeAccountMenuOpen = true;
    }

    private boolean changeToken(final int id) {
        Log.d("main", "Changing Token to bot" + id);

        BotStructure bot;
        try {
            bot = _bots.get(id - 1);
        } catch (RuntimeException e) {
            Log.e("main", "ct", e);
            bot = db.getBot(id);
        }

        if (bot.name == null) {
            Log.w("main", "ct null bot" + id);
            return false;
        }


        final BotStructure finalBot = bot;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    TextView title = (TextView) findViewById(R.id.nav_header_title);
                    TextView subtitle = (TextView) findViewById(R.id.nav_header_subtitle);

                    title.setText(finalBot.name);
                    subtitle.setText(finalBot.token);
                } catch (NullPointerException e) {
                    Log.e("main", "Switching Account Error" + id, e);
                }
            }
        });
        currentBot = bot;

        SharedPreferences settings = getSharedPreferences("data", MODE_PRIVATE);
        settings.edit()
                .putInt("currentBotId", id)
                .apply();

        restoreMenu();
        return true;
    }

    private void restoreMenu() {
        Log.d("main", "restore menu");
        NavigationView navView = (NavigationView) findViewById(R.id.nav_view);
        Menu menu = navView.getMenu();
        menu.setGroupVisible(R.id.menu_api, true);
        menu.setGroupVisible(R.id.menu_accounts, false);
        menu.getItem(2).setVisible(false);
        changeAccountMenuOpen = false;
    }

    private void joinGroup() {
        Uri uri = Uri.parse("https://t.me/joinchat/AAAAAA7VO5bHlMdH9MPecA");
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, uri);
        startActivity(browserIntent);
    }
}
