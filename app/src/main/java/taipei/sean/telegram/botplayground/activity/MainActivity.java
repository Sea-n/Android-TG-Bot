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
import android.view.ViewTreeObserver;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.onesignal.OneSignal;

import java.io.File;
import java.io.IOException;
import java.util.List;

import taipei.sean.telegram.botplayground.BotStructure;
import taipei.sean.telegram.botplayground.R;
import taipei.sean.telegram.botplayground.SeanDBHelper;

public class MainActivity extends AppCompatActivity {
    final private int _dbVer = 2;
    private SeanDBHelper db;
    private List<BotStructure> _bots = null;
    private boolean changeAccountMenuOpen = false;
    private BotStructure currentBot = null;
    private final int _requestCode_addBot = 1;
    private final int _requestCode_editBot = 2;
    private final int _requestCode_reqPerm = 4;
    private final int _requestCode_selectDb = 8;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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

        final NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                return navItemSelected(item);
            }
        });

        final ViewTreeObserver vto = drawer.getViewTreeObserver();
        vto.addOnGlobalLayoutListener (new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                final LinearLayout navHeader = (LinearLayout) findViewById(R.id.nav_header);
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

        initAccount();

        final TextView footer = (TextView) findViewById(R.id.main_footer);
        try {
            String appName = getString(R.string.app_name);
            PackageInfo pInfo = this.getPackageManager().getPackageInfo(getPackageName(), 0);
            String verName = pInfo.versionName;
            int verCode = pInfo.versionCode;
            String footerStr = getString(R.string.main_footer, appName, verName, verCode+"");
            footer.setText(footerStr);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
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
                    FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.main_fab);
                    Snackbar.make(fab, R.string.main_did_not_select_bot, Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                } else
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
                .setTitle("Closing Activity")
                .setMessage("Are you sure you want to delete this bot?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        db.deleteBot(id);
                        _bots = db.getBots();
                        if (_bots.size() == 0) {
                            currentBot = null;
                            askAddBot();
                        } else {
                            currentBot = null;
                        }
                        initAccount();
                    }

                })
                .setNegativeButton("No", null)
                .show();
    }

    private void askAddBot() {
        new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.btn_plus)
                .setTitle(R.string.main_ask_add_bot_title)
                .setMessage(R.string.main_ask_add_bot_msg)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        addBot();
                    }

                })
                .setNegativeButton("Not now", null)
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
                    case R.id.nav_caller:
                        if (null == currentBot) {
                            Log.w("nav", "no bots");
                            View fab = findViewById(R.id.main_fab);
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
                    case R.id.nav_import:
                        importDB();
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
                break;
            case _requestCode_editBot:
                initAccount();
                break;
            case _requestCode_reqPerm:
                break;
            case _requestCode_selectDb:
                if (null == data) {
                    break;
                }
                String backupFile = data.getData().getPath();
                final File backupDb = new File(backupFile);
                final File oldDb = this.getDatabasePath("data.db");
                final FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.main_fab);

                try {
                    db.copyDatabase(backupDb, oldDb);
                    _bots = db.getBots();
                } catch (IOException e) {
                    Log.e("main", "export", e);
                    Snackbar.make(fab, getString(R.string.main_db_import_fail)+e.getMessage(), Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                    break;
                }
                Snackbar.make(fab, R.string.main_success_import, Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                initAccount();
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

    private void importDB() {
        int permR = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
        if (permR == PackageManager.PERMISSION_DENIED) {
            Log.w("main", "permission READ_EXTERNAL_STORAGE denied");
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.READ_EXTERNAL_STORAGE}, _requestCode_reqPerm);
            return;
        }

//        int permDoc = ContextCompat.checkSelfPermission(this, Manifest.permission.MANAGE_DOCUMENTS);
//        if (permDoc == PackageManager.PERMISSION_DENIED) {
//            Log.w("main", "permission MANAGE_DOCUMENTS denied");
//            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.MANAGE_DOCUMENTS}, _requestCode_reqPerm);
//            return;
//        }

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        startActivityForResult(Intent.createChooser(intent, "Select a Database"), _requestCode_selectDb);
    }

    public void addBot() {
        Intent mIntent = new Intent(MainActivity.this, AddBotActivity.class);
        startActivityForResult(mIntent, _requestCode_addBot);
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

    public void changeAccount(@Nullable View view) {
        if (changeAccountMenuOpen) {
            restoreMenu();
            return;
        }

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
            Log.e("main", "change token", e);
            _bots = db.getBots();
            bot = db.getBot(id);
        }

        if (bot.name == null) {
            Log.w("main", "change token null bot" + id);
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
}
