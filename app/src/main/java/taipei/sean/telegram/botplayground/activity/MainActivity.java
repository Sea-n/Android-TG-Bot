package taipei.sean.telegram.botplayground.activity;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
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
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.onesignal.OneSignal;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

import taipei.sean.telegram.botplayground.BotStructure;
import taipei.sean.telegram.botplayground.R;
import taipei.sean.telegram.botplayground.SeanDBHelper;

public class MainActivity extends AppCompatActivity {
    final private int _dbVer = 3;
    private SeanDBHelper db;
    private List<BotStructure> _bots = null;
    private boolean changeAccountMenuOpen = false;
    private BotStructure currentBot = null;
    private final int _requestCode_addBot = 1;
    private final int _requestCode_editBot = 2;
    private final int _requestCode_reqPerm = 4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = getIntent();
        Uri intentData = intent.getData();
        if (null != intentData)
            Log.d("main", "url data   " + intentData);


        OneSignal.startInit(this)
                .inFocusDisplaying(OneSignal.OSInFocusDisplayOption.Notification)
                .unsubscribeWhenNotificationsAreDisabled(true)
                .init();

        db = new SeanDBHelper(this, "data.db", null, _dbVer);

        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.main_fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addBot();
            }
        });

        final DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        final NavigationView navView = (NavigationView) findViewById(R.id.nav_view);
        navView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                return navItemSelected(item);
            }
        });

        initAccount();

        String appName = getString(R.string.app_name);
        String footerStr = "";
        try {
            PackageInfo pInfo = this.getPackageManager().getPackageInfo(getPackageName(), 0);
            String verName = pInfo.versionName;
            int verCode = pInfo.versionCode;
            footerStr = getString(R.string.main_footer, appName, verName, verCode+"");
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("main", "Name Not Found", e);
            if (Objects.equals(footerStr, "")) {
                footerStr = e.getLocalizedMessage();
            }
        }

        final LinearLayout navHeader = (LinearLayout) navView.getHeaderView(0);
        final Menu menu = navView.getMenu();
        final MenuItem accountItem = menu.findItem(R.id.menu_accounts_item);
        navHeader.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (null == _bots)
                    _bots = db.getBots();

                if (_bots.size() == 0) {
                    Log.d("main", "no bot");
                    askAddBot();
                    return;
                }

                if (changeAccountMenuOpen) {
                    restoreMenu();
                    changeAccountMenuOpen = false;
                } else {
                    menu.setGroupVisible(R.id.menu_api, false);
                    accountItem.setVisible(true);
                    changeAccountMenuOpen = true;
                }
            }
        });

        final TextView footer = (TextView) findViewById(R.id.main_footer);
        footer.setText(footerStr);
        footer.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(final View view) {
                final Context context = view.getContext();

                new AlertDialog.Builder(context)
                        .setTitle(R.string.footer_rate_title)
                        .setMessage(R.string.footer_rate_msg)
                        .setPositiveButton(R.string.footer_rate_yes, new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                final String packName = context.getPackageName();
                                Uri uri = Uri.parse("market://details?id=" + packName);
                                Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
                                // To count with Play market backstack, After pressing back button,
                                // to taken back to our application, we need to add following flags to intent.
                                goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY |
                                        Intent.FLAG_ACTIVITY_NEW_DOCUMENT |
                                        Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                                try {
                                    startActivity(goToMarket);
                                } catch (ActivityNotFoundException e) {
                                    startActivity(new Intent(Intent.ACTION_VIEW,
                                            Uri.parse("http://play.google.com/store/apps/details?id=" + packName)));
                                }
                            }

                        })
                        .setNegativeButton(R.string.not_now, null)
                        .show();


                return false;
            }
        });
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
            Thread sleepThread = new Thread() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        Log.e("restore", "sleep", e);
                    }
                    restoreMenu();
                }
            };
            sleepThread.start();
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
                if (null == currentBot) {
                    FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.main_fab);
                    Snackbar.make(fab, R.string.main_did_not_select_bot, Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                    askAddBot();
                    break;
                }

                Intent mIntent = new Intent(MainActivity.this, AddBotActivity.class);
                mIntent.putExtra("id", currentBot._id);
                startActivityForResult(mIntent, _requestCode_editBot);
                break;
            case R.id.action_remove:
                if (null == currentBot) {
                    FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.main_fab);
                    Snackbar.make(fab, R.string.main_did_not_select_bot, Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                    break;
                }

                deleteBot(currentBot._id);
                break;
            default:
                if (null != currentBot)
                    Log.w("option", "Press unknown " + id);
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void deleteBot(final long id) {
        new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(R.string.main_del_bot_confirm_title)
                .setMessage(R.string.main_del_bot_confirm_msg)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        db.deleteBot(id);
                        _bots = db.getBots();
                        if (_bots.size() == 0) {
                            currentBot = null;
                            changeToken();
                            askAddBot();
                        } else {
                            currentBot = _bots.get(0);
                            changeToken();
                        }
                        initAccount();
                    }

                })
                .setNegativeButton(R.string.no, null)
                .show();
    }

    private void askAddBot() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.main_ask_add_bot_title)
                .setMessage(R.string.main_ask_add_bot_msg)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        addBot();
                    }

                })
                .setNegativeButton(R.string.not_now, null)
                .show();
    }

    public boolean navItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        int gid = item.getGroupId();
        int id = item.getItemId();

        switch (gid) {
            case R.id.menu_accounts:
                _bots = db.getBots();
                currentBot = _bots.get(id - 1);
                changeToken();
                break;
            default:
                switch (id) {
                    case R.id.nav_caller:
                        if (null == currentBot) {
                            Log.w("nav", "no bots");
                            View fab = findViewById(R.id.main_fab);
                            Snackbar.make(fab, R.string.no_bot_warning, Snackbar.LENGTH_LONG)
                                    .setAction("Action", null).show();
                            break;
                        }

                        if (currentBot.type != 0) {
                            View fab = findViewById(R.id.main_fab);
                            Snackbar.make(fab, R.string.not_normal_bot, Snackbar.LENGTH_LONG)
                                    .setAction("Action", null).show();
                            break;
                        }

                        Intent mIntent = new Intent(MainActivity.this, ApiCallerActivity.class);
                        mIntent.putExtra("token", currentBot.token);
                        startActivity(mIntent);
                        break;
                    case R.id.nav_pwrtelegram:
                        if (null == currentBot) {
                            Log.w("nav", "no bots");
                            View fab = findViewById(R.id.main_fab);
                            Snackbar.make(fab, R.string.no_bot_warning, Snackbar.LENGTH_LONG)
                                    .setAction("Action", null).show();
                            break;
                        }
                        Intent pIntent = new Intent(MainActivity.this, PWRTelegramActivity.class);
                        pIntent.putExtra("token", currentBot.token);
                        pIntent.putExtra("type", currentBot.type);
                        startActivity(pIntent);
                        break;
                    case R.id.nav_file_dl:
                        if (null == currentBot) {
                            Log.w("nav", "no bots");
                            View fab = findViewById(R.id.main_fab);
                            Snackbar.make(fab, R.string.no_bot_warning, Snackbar.LENGTH_LONG)
                                    .setAction("Action", null).show();
                            break;
                        }

                        if (currentBot.type != 0) {
                            View fab = findViewById(R.id.main_fab);
                            Snackbar.make(fab, R.string.not_normal_bot, Snackbar.LENGTH_LONG)
                                    .setAction("Action", null).show();
                            break;
                        }

                        Intent dlIntent = new Intent(MainActivity.this, FileDownloadActivity.class);
                        dlIntent.putExtra("token", currentBot.token);
                        startActivity(dlIntent);
                        break;
                    case R.id.nav_add_bot:
                        addBot();
                        break;
                    case R.id.nav_fav:
                        Intent favIntent = new Intent(MainActivity.this, FavoriteActivity.class);
                        startActivity(favIntent);
                        break;
                    case R.id.nav_join_group:
                        Uri tgUri = Uri.parse(getString(R.string.telegram_group_join_link));
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, tgUri);
                        startActivity(browserIntent);
                        break;
                    case R.id.nav_github:
                        Uri githubUri = Uri.parse(getString(R.string.github_url));
                        Intent githubIntent = new Intent(Intent.ACTION_VIEW, githubUri);
                        startActivity(githubIntent);
                        break;
                    case R.id.nav_export:
                        exportDB();
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        switch (requestCode) {
            case _requestCode_addBot:
                initAccount();
                if (null == _bots)
                    _bots = db.getBots();
                if (_bots.size() == 0)
                    askAddBot();
                else {
                    if (null == currentBot)
                        currentBot = _bots.get(0);
                    changeToken();
                }
                break;
            case _requestCode_editBot:
                initAccount();
                break;
            case _requestCode_reqPerm:
                break;
        }
    }

    private void exportDB() {
        int permW = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permW == PackageManager.PERMISSION_DENIED) {
            Log.w("main", "permission WRITE_EXTERNAL_STORAGE denied");
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, _requestCode_reqPerm);
            return;
        }

        final File oldDb = this.getDatabasePath("data.db");
        final FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.main_fab);

        final File backupDir = new File(Environment.getExternalStorageDirectory() + "/Sean");
        if (!backupDir.exists()) {
            if (!backupDir.mkdir()) {
                Log.e("main", "export mkdir fail");
                Snackbar.make(fab, R.string.main_mkdir_fail, Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                return;
            }
        } else if (!backupDir.isDirectory()) {
            Log.e("main", "export director is file");
            Snackbar.make(fab, R.string.main_backup_dir_is_file, Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.main_save_db_title);

        final EditText input = new EditText(this);
        builder.setView(input);

        builder.setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String filename = input.getText().toString();
                String backupName = filename + ".db";

                File backupFile = new File(backupDir, backupName);
                Log.d("main", "db: "+backupFile);

                try {
                    db.copyDatabase(oldDb, backupFile);
                } catch (IOException e) {
                    Log.e("main", "export", e);
                    Snackbar.make(fab, getString(R.string.main_db_export_fail)+e.getMessage(), Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                    return;
                }
                Snackbar.make(fab, getString(R.string.main_export_to)+backupFile.toString(), Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    public void addBot() {
        Intent mIntent = new Intent(MainActivity.this, AddBotActivity.class);
        startActivityForResult(mIntent, _requestCode_addBot);
    }

    public void initAccount() {
        final NavigationView navView = (NavigationView) findViewById(R.id.nav_view);
        final Menu navMenu = navView.getMenu();
        final MenuItem menuItem = navMenu.findItem(R.id.menu_accounts_item);
        SubMenu subMenu = menuItem.getSubMenu();
        if (subMenu.size() > 0) {
            subMenu.clear();
        }

        _bots = db.getBots();
        for (int i = 0; i < _bots.size(); i++) {
            BotStructure bot = _bots.get(i);
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
                    currentBot = db.getBot(finalId);
                }
            };
            thread.start();
        }

        if (null == currentBot) {
            if (_bots.size() == 0) {
                askAddBot();
            } else {
                currentBot = _bots.get(0);
                changeToken();
            }
        }
    }

    private boolean changeToken() {
        final NavigationView navView = (NavigationView) findViewById(R.id.nav_view);
        final LinearLayout navHeader = (LinearLayout) navView.getHeaderView(0);

        if (null == currentBot) {
            Log.w("main", "change token null bot");
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    TextView title = (TextView) navHeader.findViewById(R.id.nav_header_title);
                    TextView subtitle = (TextView) navHeader.findViewById(R.id.nav_header_subtitle);
                    TextView main = (TextView) findViewById(R.id.main_content);

                    title.setText(R.string.nav_header_default_title);
                    subtitle.setText(R.string.nav_header_default_subtitle);
                    main.setText(R.string.main_no_bot);
                }
            });
            SharedPreferences settings = getSharedPreferences("data", MODE_PRIVATE);
            settings.edit()
                    .putLong("currentBotId", -1)
                    .apply();
            return false;
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView title = (TextView) navHeader.findViewById(R.id.nav_header_title);
                TextView subtitle = (TextView) navHeader.findViewById(R.id.nav_header_subtitle);
                TextView main = (TextView) findViewById(R.id.main_content);

                title.setText(currentBot.name);
                subtitle.setText(currentBot.token);
                main.setText(currentBot.name);


                final Menu menu = navView.getMenu();
                final MenuItem caller = menu.findItem(R.id.nav_caller);
                if (currentBot.type == 0) {
                    caller.setVisible(true);
                } else {
                    caller.setVisible(false);
                }
            }
        });

        SharedPreferences settings = getSharedPreferences("data", MODE_PRIVATE);
        settings.edit()
                .putLong("currentBotId", currentBot._id)
                .apply();


        Thread sleepThread = new Thread() {
            @Override
            public void run() {
                try {
                    Thread.sleep(500);
                    // Sleep for menu close
                } catch (InterruptedException e) {
                    Log.e("restore", "sleep", e);
                }
                restoreMenu();
            }
        };
        sleepThread.start();
        return true;
    }

    private void restoreMenu() {
        final NavigationView navView = (NavigationView) findViewById(R.id.nav_view);
        final Menu menu = navView.getMenu();
        final MenuItem accountItem = menu.findItem(R.id.menu_accounts_item);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                menu.setGroupVisible(R.id.menu_api, true);
                accountItem.setVisible(false);
            }
        });
    }
}
