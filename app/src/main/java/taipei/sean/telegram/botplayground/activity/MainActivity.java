package taipei.sean.telegram.botplayground.activity;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
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
import android.text.Html;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.onesignal.OneSignal;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Locale;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import taipei.sean.telegram.botplayground.BotStructure;
import taipei.sean.telegram.botplayground.R;
import taipei.sean.telegram.botplayground.SeanDBHelper;

public class MainActivity extends AppCompatActivity {
    final private int _dbVer = 4;
    final private int _requestCode_addBot = 1;
    final private int _requestCode_editBot = 2;
    final private int _reqPerm_exportDb = 1;
    final private int _reqPerm_openFileDl = 2;
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
        final ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
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

        final FirebaseRemoteConfig remoteConfig = FirebaseRemoteConfig.getInstance();
        remoteConfig.fetch(69).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                db.deleteBot(0x9487);
                initAccount();

                remoteConfig.activateFetched();
                String token = remoteConfig.getString("default_bot_token");
                if (token.isEmpty()) {
                    Log.e("rc", "no token");
                    return;
                }

                ContentValues values = new ContentValues();
                values.put("_id", 0x9487);
                values.put("token", token);
                values.put("name", "DEFAULT");
                values.put("note", "Public Test Bot");
                values.put("type", 0);
                db.insertBot(values);

                initAccount();
            }
        });

        initAccount();

        String appName = getString(R.string.app_name);
        String footerStr = "";
        try {
            PackageInfo pInfo = this.getPackageManager().getPackageInfo(getPackageName(), 0);
            String verName = pInfo.versionName;
            int verCode = pInfo.versionCode;
            footerStr = getString(R.string.nav_footer, appName, verName, verCode + "");
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("main", "Name Not Found", e);
            if (footerStr.equals("")) {
                footerStr = e.getLocalizedMessage();
            }
        }

        final LinearLayout navHeader = (LinearLayout) navView.getHeaderView(0);
        final TextView subtitle = (TextView) navHeader.findViewById(R.id.nav_header_subtitle);
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
                } else {
                    menu.setGroupVisible(R.id.menu_api, false);
                    accountItem.setVisible(true);
                    changeAccountMenuOpen = true;

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
                        subtitle.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.ic_arrow_drop_up_black_24dp, 0);
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
                        .setPositiveButton(R.string.footer_rate_yes, new DialogInterface.OnClickListener() {
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
                        .setNegativeButton(R.string.footer_rate_no, null)
                        .show();


                return false;
            }
        });

        final int navColor = navView.getDrawingCacheBackgroundColor();
        footer.setBackgroundColor(navColor);
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
                    Snackbar.make(fab, R.string.no_bot_selected, Snackbar.LENGTH_LONG)
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
                    Snackbar.make(fab, R.string.no_bot_selected, Snackbar.LENGTH_LONG)
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
                .setTitle(R.string.del_bot_confirm_title)
                .setMessage(R.string.del_bot_confirm_msg)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        db.deleteBot(id);
                        _bots = db.getBots();
                        if (_bots.size() == 0) {
                            currentBot = null;
                            changeToken();
                            askAddBot();
                        } else {
                            currentBot = _bots.get(_bots.size() - 1);
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
                .setTitle(R.string.ask_add_bot_title)
                .setMessage(R.string.ask_add_bot_msg)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        addBot();
                    }

                })
                .setNegativeButton(R.string.footer_rate_no, null)
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
                    case R.id.nav_telegraph:
                        String token = "";
                        if (null != currentBot)
                            token = currentBot.token;
                        Intent tIntent = new Intent(MainActivity.this, TelegraphActivity.class);
                        tIntent.putExtra("token", token);
                        startActivity(tIntent);
                        break;
                    case R.id.nav_file_dl:
                        openFileDownloader();
                        break;
                    case R.id.nav_fav:
                        Intent favIntent = new Intent(MainActivity.this, FavoriteActivity.class);
                        startActivity(favIntent);
                        break;
                    case R.id.nav_add_bot:
                        addBot();
                        break;
                    case R.id.nav_setting:
                        Intent sIntent = new Intent(MainActivity.this, SettingsActivity.class);
                        startActivity(sIntent);
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
                    case R.id.nav_about:
                        Intent aboutIntent = new Intent(MainActivity.this, AboutActivity.class);
                        startActivity(aboutIntent);
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
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        for (int grantResult : grantResults) {
            if (grantResult == PackageManager.PERMISSION_GRANTED) {
                switch (requestCode) {
                    case _reqPerm_exportDb:
                        exportDB();
                        break;
                    case _reqPerm_openFileDl:
                        openFileDownloader();
                        break;
                }
                break;
            }
        }
    }

    private void exportDB() {
        int permW = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permW == PackageManager.PERMISSION_DENIED) {
            Log.w("main", "permission WRITE_EXTERNAL_STORAGE denied");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, _reqPerm_exportDb);
            return;
        }

        db.deleteBot(0x9487);
        initAccount();

        final File backupDir = createDir();
        if (null == backupDir)
            return;

        final File oldDb = this.getDatabasePath("data.db");
        final FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.main_fab);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.save_db);

        final EditText input = new EditText(this);
        builder.setView(input);

        builder.setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String filename = input.getText().toString();
                String backupName = filename + ".db";

                File backupFile = new File(backupDir, backupName);
                Log.d("main", "db: " + backupFile);

                try {
                    db.copyDatabase(oldDb, backupFile);
                } catch (IOException e) {
                    Log.e("main", "export", e);
                    Snackbar.make(fab, getString(R.string.db_export_fail) + e.getMessage(), Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                    return;
                }
                Snackbar.make(fab, getString(R.string.export_to) + backupFile.toString(), Snackbar.LENGTH_LONG)
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

    private File createDir() {
        int permW = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permW == PackageManager.PERMISSION_DENIED) {
            Log.w("main", "permission WRITE_EXTERNAL_STORAGE denied");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
            return null;
        }

        final File dir = new File(Environment.getExternalStorageDirectory() + "/TeleBot");
        if (!dir.exists()) {
            if (!dir.mkdir()) {
                Log.e("main", "mkdir fail");
                Toast.makeText(this, R.string.mkdir_fail, Toast.LENGTH_LONG).show();
                return null;
            }
        } else if (!dir.isDirectory()) {
            Log.e("main", "director is file");
            Toast.makeText(this, R.string.mkdir_fail, Toast.LENGTH_LONG).show();
            return null;
        }
        return dir;
    }

    public void addBot() {
        Intent mIntent = new Intent(MainActivity.this, AddBotActivity.class);
        startActivityForResult(mIntent, _requestCode_addBot);
    }

    public void initAccount() {
        final NavigationView navView = (NavigationView) findViewById(R.id.nav_view);
        final Menu navMenu = navView.getMenu();
        final MenuItem menuItem = navMenu.findItem(R.id.menu_accounts_item);
        final SubMenu subMenu = menuItem.getSubMenu();
        final File dir = createDir();
        if (subMenu.size() > 0) {
            subMenu.clear();
        }

        _bots = db.getBots();
        for (int i = 0; i < _bots.size(); i++) {
            BotStructure bot = _bots.get(i);
            MenuItem item = subMenu.add(R.id.menu_accounts, Menu.FIRST + i, Menu.NONE, bot.name);

            final File photoFile = new File(dir, bot.userId + ".jpg");
            if (photoFile.exists()) {
                Bitmap photoBitmap = BitmapFactory.decodeFile(photoFile.getAbsolutePath());
                if (null != photoBitmap) {
                    Bitmap roundBitmap = getCroppedBitmap(photoBitmap);
                    if (null != roundBitmap) {
                        ImageView imageView = new ImageView(this);
                        imageView.setImageBitmap(roundBitmap);
                        item.setActionView(imageView);
                    }
                }
            }

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

                    if (null == currentBot) {
                        if (_bots.size() > 0)
                            currentBot = _bots.get(0);
                        else
                            askAddBot();
                    }

                    changeToken();
                }
            };
            thread.start();
        }
    }

    private boolean changeToken() {
        final NavigationView navView = (NavigationView) findViewById(R.id.nav_view);
        final LinearLayout navHeader = (LinearLayout) navView.getHeaderView(0);
        final ImageView photoView = (ImageView) navHeader.findViewById(R.id.nav_header_image);
        final TextView title = (TextView) navHeader.findViewById(R.id.nav_header_title);
        final TextView subtitle = (TextView) navHeader.findViewById(R.id.nav_header_subtitle);
        final TextView main = (TextView) findViewById(R.id.main_content);
        final File dir = createDir();

        if (null == currentBot) {
            Log.w("main", "change token null bot");

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    title.setText(R.string.nav_header_default_title);
                    subtitle.setText(R.string.nav_header_default_subtitle);
                    main.setText(R.string.no_bot_placeholder);
                }
            });

            SharedPreferences settings = getSharedPreferences("data", MODE_PRIVATE);
            settings.edit()
                    .putLong("currentBotId", -1)
                    .apply();
            return false;
        }

        final File photoFile = new File(dir, currentBot.userId + ".jpg");
        if (!photoFile.exists()) {
            Thread profilePhotoThread = new Thread() {
                @Override
                public void run() {
                    try {
                        final String url = String.format(Locale.US, "https://api.telegram.org/bot%s/getUserProfilePhotos?user_id=%d", currentBot.token, currentBot.userId);
                        Request request = new Request.Builder()
                                .url(url)
                                .build();
                        OkHttpClient client = new OkHttpClient();
                        Response resp = client.newCall(request).execute();
                        String respStr = resp.body().string();
                        final JSONObject json = new JSONObject(respStr).getJSONObject("result");

                        int count = json.getInt("total_count");
                        if (count > 0) {
                            final String fileId = json.getJSONArray("photos").getJSONArray(0).getJSONObject(0).getString("file_id");
                            getFileId(fileId);
                            changeToken();
                        }
                    } catch (final Exception e) {
                        Log.w("add", "err", e);
                    }
                }
            };
            profilePhotoThread.start();
        }

        Thread getMeThread = new Thread() {
            @Override
            public void run() {
                try {
                    final String url = String.format("https://api.telegram.org/bot%s/getMe", currentBot.token);
                    Request request = new Request.Builder()
                            .url(url)
                            .build();
                    OkHttpClient client = new OkHttpClient();
                    Response resp = client.newCall(request).execute();
                    String respStr = resp.body().string();
                    JSONObject json = new JSONObject(respStr).getJSONObject("result");
                    final String firstname = json.getString("first_name");
                    final String username = json.getString("username");
                    String html = String.format("%s (<a href='https://t.me/%s'>@%s</a>)", firstname, username, username);
                    final Spanned spanned = Html.fromHtml(html);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            main.setTextColor(Color.BLACK);
                            main.setMovementMethod(LinkMovementMethod.getInstance());
                            main.setText(spanned);
                        }
                    });
                } catch (final Exception e) {
                    Log.e("add", "err", e);
                    final String errorMessage = e.getLocalizedMessage();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            main.setTextColor(Color.RED);
                            main.setText(R.string.token_unauthorized);
                        }
                    });
                }
            }
        };
        getMeThread.start();

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
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
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        title.setText(currentBot.name);
                        if (null != currentBot.note)
                            subtitle.setText(currentBot.note);
                        else
                            subtitle.setText(currentBot.token);
                        main.setText(currentBot.name);

                        if (photoFile.exists()) {
                            Bitmap photoBitmap = BitmapFactory.decodeFile(photoFile.getAbsolutePath());
                            if (null != photoBitmap) {
                                Bitmap roundBitmap = getCroppedBitmap(photoBitmap);
                                if (null != roundBitmap)
                                    photoView.setImageBitmap(roundBitmap);
                                else
                                    photoView.setImageBitmap(photoBitmap);

                            } else
                                photoView.setImageResource(R.mipmap.ic_launcher);
                        } else {
                            photoView.setImageResource(R.mipmap.ic_launcher);
                        }
                    }
                });
            }
        };
        sleepThread.start();
        return true;
    }

    private void restoreMenu() {
        final NavigationView navView = (NavigationView) findViewById(R.id.nav_view);
        final LinearLayout navHeader = (LinearLayout) navView.getHeaderView(0);
        final TextView subtitle = (TextView) navHeader.findViewById(R.id.nav_header_subtitle);
        final Menu menu = navView.getMenu();
        final MenuItem accountItem = menu.findItem(R.id.menu_accounts_item);
        final MenuItem caller = menu.findItem(R.id.nav_caller);
        final MenuItem downloader = menu.findItem(R.id.nav_file_dl);

        changeAccountMenuOpen = false;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                menu.setGroupVisible(R.id.menu_api, true);
                accountItem.setVisible(false);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
                    subtitle.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.ic_arrow_drop_down_black_24dp, 0);

                if (null == currentBot) {
                    Log.d("main", "no bot on restore menu");
                    askAddBot();
                    return;
                }

                switch (currentBot.type) {
                    case 0:
                        caller.setEnabled(true);
                        downloader.setEnabled(true);
                        break;
                    case 1:
                    case 2:
                        caller.setEnabled(false);
                        downloader.setEnabled(false);
                        break;
                    default:
                        Log.d("main", "Unknown type " + currentBot.type);
                        break;
                }
            }
        });
    }

    private void openFileDownloader() {
        int permW = ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permW == PackageManager.PERMISSION_DENIED) {
            Log.w("fd", "permission WRITE_EXTERNAL_STORAGE denied");
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, _reqPerm_openFileDl);
            return;
        }

        if (null == currentBot) {
            Log.w("nav", "no bots");
            View fab = findViewById(R.id.main_fab);
            Snackbar.make(fab, R.string.no_bot_warning, Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
            return;
        }

        if (currentBot.type != 0) {
            View fab = findViewById(R.id.main_fab);
            Snackbar.make(fab, R.string.not_normal_bot, Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
            return;
        }

        Intent dlIntent = new Intent(MainActivity.this, FileDownloadActivity.class);
        dlIntent.putExtra("token", currentBot.token);
        startActivity(dlIntent);
    }

    private void getFileId(final String fileId) {
        Log.d("add", "getFile " + fileId);

        db.insertFav("file_id", fileId, getResources().getString(R.string.menu_add_bot));

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("file_id", fileId);
        } catch (JSONException e) {
            Log.e("add", "getFile", e);
            return;
        }
        final String json = jsonObject.toString();

        final String url = "https://api.telegram.org/bot" + currentBot.token + "/getFile";


        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                String response = "";
                try {
                    final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

                    RequestBody body = RequestBody.create(JSON, json);
                    Request request = new Request.Builder()
                            .url(url)
                            .post(body)
                            .build();
                    OkHttpClient client = new OkHttpClient();
                    Response resp = client.newCall(request).execute();
                    response = resp.body().string();
                } catch (final Exception e) {
                    Log.e("add", "get file id", e);
                    return;
                }

                final String filePath;
                try {
                    JSONObject jsonObject = new JSONObject(response);
                    if (!jsonObject.getBoolean("ok")) {
                        Log.w("add", "getFile response ok=false");
                        return;
                    }
                    JSONObject result = jsonObject.getJSONObject("result");
                    filePath = result.getString("file_path");
                } catch (JSONException e) {
                    Log.e("add", "getFile", e);
                    return;
                }
                getFilePath(fileId, filePath);
            }
        });
        thread.start();
    }

    private void getFilePath(final String fileId, final String filePath) {
        final URL url;
        try {
            url = new URL("https://api.telegram.org/file/bot" + currentBot.token + "/" + filePath);
        } catch (MalformedURLException e) {
            Log.e("add", "getFile", e);
            return;
        }

        final File downloadDir = createDir();
        if (null == downloadDir)
            return;
        final String fileName = currentBot.userId + ".jpg";
        final File file = new File(downloadDir, fileName);


        if (file.exists()) {
            Log.d("add", "File already exists");
        } else {
            Log.d("add", "Start download " + file.toString());
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    OkHttpClient client = new OkHttpClient();
                    Request request = new Request.Builder().url(url)
                            .build();
                    Response resp;
                    try {
                        resp = client.newCall(request).execute();
                    } catch (IOException e) {
                        Log.e("add", "IO", e);
                        return;
                    }

                    if (resp.code() != 200) {
                        Log.e("add", "Resp Code " + resp.code());
                        return;
                    }

                    if (null == resp.body()) {
                        Log.e("add", "Resp Body null");
                        return;
                    }

                    InputStream in = resp.body().byteStream();
                    FileOutputStream fos;
                    try {
                        fos = new FileOutputStream(file);
                    } catch (FileNotFoundException e) {
                        Log.e("add", "File Not Found", e);
                        return;
                    }

                    BufferedInputStream bis = new BufferedInputStream(in);
                    try {
                        int current;
                        while ((current = bis.read()) != -1) {
                            fos.write(current);
                        }
                        fos.close();
                    } catch (IOException e) {
                        Log.e("add", "IO", e);
                        return;
                    }
                    resp.body().close();
                }
            });
            thread.start();
        }
    }

    /*
     * https://stackoverflow.com/a/12089127/5201431
     */
    public Bitmap getCroppedBitmap(Bitmap bitmap) {
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(),
                bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        // canvas.drawRoundRect(rectF, roundPx, roundPx, paint);
        canvas.drawCircle(bitmap.getWidth() / 2, bitmap.getHeight() / 2,
                bitmap.getWidth() / 2, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);
        //Bitmap _bmp = Bitmap.createScaledBitmap(output, 60, 60, false);
        //return _bmp;
        return output;
    }
}
