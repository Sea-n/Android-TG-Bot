package taipei.sean.telegram.botplayground;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
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
import android.widget.LinearLayout;
import android.widget.TextView;
import com.onesignal.OneSignal;
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

        OneSignal.startInit(this)
                .inFocusDisplaying(OneSignal.OSInFocusDisplayOption.Notification)
                .unsubscribeWhenNotificationsAreDisabled(true)
                .init();

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


        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    Thread.sleep(200);   /// Wait for UI ready
                } catch (InterruptedException e) {
                    Log.e("main", "sleep", e);
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        LinearLayout navHeader = (LinearLayout) findViewById(R.id.nav_header);
                        navHeader.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                changeAccount(view);
                            }
                        });
                        navHeader.setOnLongClickListener(new View.OnLongClickListener() {
                            @Override
                            public boolean onLongClick(View view) {
                                initAccount();
                                return false;
                            }
                        });
                    }
                });

            }
        };
        thread.start();
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
            case R.id.action_edit:
                Intent mIntent = new Intent(MainActivity.this, AddBotActivity.class);
                if (null == currentBot) {
                    FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
                    Snackbar.make(fab, R.string.main_did_not_select_bot, Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                } else
                    mIntent.putExtra("id", currentBot._id);
                startActivity(mIntent);
                break;
            case R.id.action_remove:
                if (null == currentBot) {
                    FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
                    Snackbar.make(fab, R.string.main_did_not_select_bot, Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                    break;
                }
                deleteBot(currentBot._id);
                break;
            default:
                if (null != currentBot)
                Log.w("option", "Press unknow " + id);
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void deleteBot(final long id) {
        new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle("Closing Activity")
                .setMessage("Are you sure you want to delete this bot?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        db.deleteBot(id);
                    }

                })
                .setNegativeButton("No", null)
                .show();
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
                        if (null == currentBot) {
                            Log.w("nav", "no bots");
                            View fab = findViewById(R.id.fab);
                            Snackbar.make(fab, R.string.no_bot_warning, Snackbar.LENGTH_LONG)
                                    .setAction("Action", null).show();
                            break;
                        }
                        Intent sIntent = new Intent(MainActivity.this, SendMessageActivity.class);
                        sIntent.putExtra("token", currentBot.token);
                        startActivity(sIntent);
                        break;
                    case R.id.nav_caller:
                        if (null == currentBot) {
                            Log.w("nav", "no bots");
                            View fab = findViewById(R.id.fab);
                            Snackbar.make(fab, R.string.no_bot_warning, Snackbar.LENGTH_LONG)
                                    .setAction("Action", null).show();
                            break;
                        }
                        Intent mIntent = new Intent(MainActivity.this, ApiCallerActivity.class);
                        mIntent.putExtra("token", currentBot.token);
                        startActivity(mIntent);
                        break;
                    case R.id.nav_add_bot:
                        addBot();
                        initAccount();
                        break;
                    case R.id.nav_join_group:
                        joinGroup();
                        break;
                    default:
                        Log.w("nav", "press unknown item " + id);
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
        subMenu.clear();

        _bots = db.getBots();
        for (int i = 0; i < _bots.size(); i++) {
            BotStructure bot = _bots.get(i);
            Log.d("main", "append bot " + bot.name);
            subMenu.add(R.id.menu_accounts, Menu.FIRST + i, Menu.NONE, bot.name);
        }

        long id = -1;
        try {
            SharedPreferences settings = getSharedPreferences("data", MODE_PRIVATE);
            id = settings.getLong("currentBotId", -1);
        } catch (RuntimeException e) {
            Log.e("main", "fetch shared preference", e);
        }
        if (id > 0) {
            final long finalId = id;
            Thread thread = new Thread() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(200);   /// Wait for UI ready
                    } catch (InterruptedException e) {
                        Log.e("main", "sleep", e);
                    }
                    Log.d("main", "restore bot " + finalId);
                    changeToken(finalId);
                }
            };
            thread.start();
        }
    }

    public void changeAccount(View view) {
        if (changeAccountMenuOpen && null != view) {
            Log.d("main", "close change acount list");
            if (null != view)
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

    private boolean changeToken(final long id) {
        Log.d("main", "Changing Token to bot" + id);

        BotStructure bot;
        try {
            bot = _bots.get((int) id - 1);
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
                    TextView main = (TextView) findViewById(R.id.main_content);

                    title.setText(finalBot.name);
                    subtitle.setText(finalBot.token);
                    main.setText(finalBot.name);
                } catch (NullPointerException e) {
                    Log.e("main", "Switching Account Error" + id, e);
                }
            }
        });
        currentBot = bot;

        SharedPreferences settings = getSharedPreferences("data", MODE_PRIVATE);
        settings.edit()
                .putLong("currentBotId", id)
                .apply();

        restoreMenu();
        return true;
    }

    private void restoreMenu() {
        Log.d("main", "restore menu");
        final NavigationView navView = (NavigationView) findViewById(R.id.nav_view);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Menu menu = navView.getMenu();
                menu.setGroupVisible(R.id.menu_api, true);
                menu.setGroupVisible(R.id.menu_accounts, false);
                menu.getItem(2).setVisible(false);
            }
        });
        changeAccountMenuOpen = false;
    }

    private void joinGroup() {
        Uri uri = Uri.parse("https://t.me/joinchat/AAAAAA7VO5bHlMdH9MPecA");
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, uri);
        startActivity(browserIntent);
    }
}
