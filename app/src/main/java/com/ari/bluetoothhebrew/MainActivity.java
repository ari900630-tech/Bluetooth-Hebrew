package com.ari.bluetoothhebrew;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class MainActivity extends Activity {
    private static final int REQ_PERMISSIONS = 210, REQ_ENABLE_BT = 211, REQ_FILE = 212;
    private static final int NAVY = Color.rgb(20,39,77), BLUE = Color.rgb(39,103,232);
    private static final int BG = Color.rgb(245,247,252), TEXT = Color.rgb(35,48,72), MUTED = Color.rgb(105,119,143);
    private BluetoothAdapter adapter;
    private LinearLayout deviceList;
    private TextView statusHome, statusDevices, count, incomingShareText;
    private FrameLayout pageHost;
    private View homePage, devicesPage, sharePage, settingsPage;
    private final Map<String,BluetoothDevice> devices = new LinkedHashMap<>();
    private boolean receiverRegistered = false;
    private String currentPage = "home";
    private final Map<String,LinearLayout> navItems = new LinkedHashMap<>();
    private final Map<String,TextView> navSymbols = new LinkedHashMap<>();
    private final Map<String,TextView> navCaptions = new LinkedHashMap<>();

    private final BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device != null) addDevice(device, "נמצא בסריקה");
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                setStatus("הסריקה הסתיימה");
            } else if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                setStatus(adapter != null && safeEnabled() ? "Bluetooth מופעל" : "Bluetooth כבוי");
            }
        }
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(Color.WHITE);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        BluetoothManager manager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
        adapter = manager == null ? null : manager.getAdapter();
        buildUi();
        handleIncomingShare(getIntent());
        registerBluetoothReceiver();
        if (adapter == null) setStatus("המכשיר אינו תומך ב-Bluetooth");
        else if (!hasPermissions()) requestBluetoothPermissions();
        else refreshPairedDevices();
    }

    private void buildUi() {
        LinearLayout shell = new LinearLayout(this);
        shell.setOrientation(LinearLayout.VERTICAL);
        shell.setBackgroundColor(BG);
        shell.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        pageHost = new FrameLayout(this);
        shell.addView(pageHost, new LinearLayout.LayoutParams(-1,0,1));
        homePage = makeHomePage();
        devicesPage = makeDevicesPage();
        sharePage = makeSharePage();
        settingsPage = makeSettingsPage();
        pageHost.addView(homePage);
        pageHost.addView(devicesPage);
        pageHost.addView(sharePage);
        pageHost.addView(settingsPage);
        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setGravity(Gravity.CENTER);
        nav.setPadding(dp(4),dp(5),dp(4),dp(5));
        nav.setBackgroundColor(Color.WHITE);
        nav.setElevation(dp(8));
        addNavItem(nav,"⌂","ראשי","home");
        addNavItem(nav,"⌁","מכשירים","devices");
        addNavItem(nav,"⇧","שיתוף","share");
        addNavItem(nav,"⚙","הגדרות","settings");
        shell.addView(nav,new LinearLayout.LayoutParams(-1,dp(66)));
        setContentView(shell);
        showPage("home");
    }

    private View makeHomePage() {
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true); scroll.setBackgroundColor(BG);
        LinearLayout root = pageColumn(); root.addView(makeHeader(),matchWrap());
        LinearLayout hero = new LinearLayout(this); hero.setOrientation(LinearLayout.VERTICAL);
        hero.setPadding(dp(20),dp(19),dp(20),dp(20));
        hero.setBackground(gradient(new int[]{Color.rgb(28,76,160),Color.rgb(45,116,239)},24));
        LinearLayout.LayoutParams hp=matchWrap(); hp.topMargin=dp(18); root.addView(hero,hp);
        hero.addView(text("חיבור אלחוטי",13,Color.rgb(219,232,255),true));
        TextView heroTitle=text("כל המכשירים שלך, במקום אחד",21,Color.WHITE,true);
        heroTitle.setPadding(0,dp(7),0,dp(6)); hero.addView(heroTitle);
        TextView body=text("סרוק מכשירים בסביבה, נהל צימוד ושתף קבצים מהמכשיר שלך.",14,Color.rgb(231,239,255),false);
        body.setLineSpacing(dp(3),1f); hero.addView(body);
        Button scan=button("⌕   סריקת מכשירים",true);
        LinearLayout.LayoutParams sp=matchWrap(); sp.topMargin=dp(16); hero.addView(scan,sp);
        scan.setOnClickListener(v->{showPage("devices");startScan();});
        LinearLayout statusCard=cardColumn(); LinearLayout.LayoutParams stp=matchWrap(); stp.topMargin=dp(14); root.addView(statusCard,stp);
        statusCard.addView(text("●  מצב החיבור",14,TEXT,true));
        statusHome=text("בודק את מצב Bluetooth…",13,MUTED,false); statusHome.setPadding(0,dp(7),0,0); statusCard.addView(statusHome);
        TextView heading=text("קיצורי דרך",18,NAVY,true); heading.setPadding(0,dp(22),0,dp(10)); root.addView(heading);
        addAction(root,"⌁","המכשירים שלי","מכשירים משויכים וסריקה חדשה",v->showPage("devices"));
        addAction(root,"⇧","שיתוף קבצים","בחר קובץ ופתח את תפריט השיתוף",v->showPage("share"));
        addAction(root,"⚙","הגדרות","הפעלת Bluetooth והגדרות המערכת",v->showPage("settings"));
        TextView note=text("העברת קבצים ושמע תלויה בתמיכת Android והמכשיר השני.",12,MUTED,false);
        note.setGravity(Gravity.CENTER); note.setPadding(dp(8),dp(18),dp(8),0); root.addView(note,matchWrap());
        scroll.addView(root); return scroll;
    }

    private View makeDevicesPage() {
        ScrollView scroll=new ScrollView(this); scroll.setFillViewport(true); scroll.setBackgroundColor(BG);
        LinearLayout root=pageColumn(); root.addView(pageTitle("המכשירים שלי","סריקה, צפייה במכשירים וצימוד"));
        LinearLayout statusCard=cardColumn(); LinearLayout.LayoutParams stp=matchWrap(); stp.topMargin=dp(15); root.addView(statusCard,stp);
        statusCard.addView(text("מצב Bluetooth",14,TEXT,true));
        statusDevices=text("בודק את מצב Bluetooth…",13,MUTED,false); statusDevices.setPadding(0,dp(6),0,0); statusCard.addView(statusDevices);
        Button scan=button("⌕   סריקה חדשה",false); LinearLayout.LayoutParams bp=matchWrap(); bp.topMargin=dp(12); root.addView(scan,bp); scan.setOnClickListener(v->startScan());
        LinearLayout heading=new LinearLayout(this); heading.setGravity(Gravity.CENTER_VERTICAL); heading.setPadding(0,dp(22),0,dp(10));
        heading.addView(text("מכשירים שנמצאו או שויכו",16,NAVY,true),new LinearLayout.LayoutParams(0,-2,1));
        count=text("0",13,BLUE,true); count.setGravity(Gravity.CENTER); count.setBackground(round(Color.rgb(226,236,255),14));
        heading.addView(count,new LinearLayout.LayoutParams(dp(36),dp(28))); root.addView(heading);
        deviceList=new LinearLayout(this); deviceList.setOrientation(LinearLayout.VERTICAL); deviceList.setLayoutDirection(View.LAYOUT_DIRECTION_RTL); root.addView(deviceList,matchWrap());
        showEmptyState(); scroll.addView(root); return scroll;
    }

    private View makeSharePage() {
        ScrollView scroll=new ScrollView(this); scroll.setFillViewport(true); scroll.setBackgroundColor(BG);
        LinearLayout root=pageColumn(); root.addView(pageTitle("שיתוף קבצים","בחר קובץ ושלח אותו דרך אפשרויות Android"));
        LinearLayout card=cardColumn(); LinearLayout.LayoutParams cp=matchWrap(); cp.topMargin=dp(20); root.addView(card,cp);
        TextView icon=text("⇧",42,BLUE,true); icon.setGravity(Gravity.CENTER); card.addView(icon);
        TextView title=text("בחירת קובץ לשיתוף",18,NAVY,true); title.setGravity(Gravity.CENTER); title.setPadding(0,dp(10),0,dp(8)); card.addView(title);
        TextView desc=text("אפשר לפתוח את האפליקציה מכאן, או לבחור בה כיעד שיתוף באפליקציה אחרת. Android יעביר אליה את הפריט שנבחר.",14,MUTED,false);
        desc.setGravity(Gravity.CENTER); desc.setLineSpacing(dp(4),1f); card.addView(desc);
        incomingShareText=text("עדיין לא התקבל פריט לשיתוף.",13,MUTED,false);
        incomingShareText.setGravity(Gravity.CENTER); incomingShareText.setPadding(0,dp(14),0,0); card.addView(incomingShareText);
        Button choose=button("בחר קובץ",false); LinearLayout.LayoutParams bp=matchWrap(); bp.topMargin=dp(18); card.addView(choose,bp); choose.setOnClickListener(v->chooseFile());
        TextView note=text("ההעברה מתבצעת דרך אפליקציה נתמכת במכשיר.",12,MUTED,false); note.setPadding(dp(3),dp(18),dp(3),0); root.addView(note,matchWrap());
        scroll.addView(root); return scroll;
    }

    private View makeSettingsPage() {
        ScrollView scroll=new ScrollView(this); scroll.setFillViewport(true); scroll.setBackgroundColor(BG);
        LinearLayout root=pageColumn(); root.addView(pageTitle("הגדרות","שליטה בהגדרות החיבור"));
        addAction(root,"◉","הפעל Bluetooth","הפעל או בדוק את מצב החיבור",v->enableBluetooth());
        addAction(root,"⚙","הגדרות Bluetooth של Android","ניהול צימוד, שמע ומכשירים במערכת",v->{try{startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS));}catch(Exception e){setStatus("לא ניתן לפתוח את הגדרות Bluetooth");}});
        addAction(root,"↻","רענון מכשירים משויכים","טען מחדש את רשימת המכשירים",v->{showPage("devices");refreshPairedDevices();});
        LinearLayout info=cardColumn(); LinearLayout.LayoutParams ip=matchWrap(); ip.topMargin=dp(14); root.addView(info,ip);
        info.addView(text("Bluetooth",16,NAVY,true)); TextView version=text("גרסה 1.0 • ממשק בעברית",13,MUTED,false); version.setPadding(0,dp(5),0,0); info.addView(version);
        scroll.addView(root); return scroll;
    }

    private void addNavItem(LinearLayout nav,String icon,String label,String page) {
        LinearLayout item=new LinearLayout(this); item.setOrientation(LinearLayout.VERTICAL); item.setGravity(Gravity.CENTER);
        item.setPadding(dp(3),dp(3),dp(3),dp(3)); item.setBackground(round(Color.WHITE,16));
        TextView symbol=text(icon,21,TEXT,true); symbol.setGravity(Gravity.CENTER);
        TextView caption=text(label,11,TEXT,true); caption.setGravity(Gravity.CENTER);
        item.addView(symbol); item.addView(caption);
        nav.addView(item,new LinearLayout.LayoutParams(0,-1,1));
        navItems.put(page,item); navSymbols.put(page,symbol); navCaptions.put(page,caption);
        item.setOnClickListener(v->showPage(page)); item.setClickable(true); item.setForeground(ripple());
    }

    private void showPage(String page) {
        currentPage=page;
        homePage.setVisibility("home".equals(page)?View.VISIBLE:View.GONE);
        devicesPage.setVisibility("devices".equals(page)?View.VISIBLE:View.GONE);
        sharePage.setVisibility("share".equals(page)?View.VISIBLE:View.GONE);
        settingsPage.setVisibility("settings".equals(page)?View.VISIBLE:View.GONE);
        for (String key : navItems.keySet()) {
            boolean selected = key.equals(page);
            navItems.get(key).setBackground(round(selected ? Color.rgb(231,239,255) : Color.WHITE,16));
            navSymbols.get(key).setTextColor(selected ? BLUE : TEXT);
            navCaptions.get(key).setTextColor(selected ? BLUE : MUTED);
        }
        if ("devices".equals(page) && deviceList!=null && devices.isEmpty()) refreshPairedDevices();
    }

    private LinearLayout pageColumn() {
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18),dp(16),dp(18),dp(26)); root.setBackgroundColor(BG);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL); return root;
    }
    private View makeHeader() {
        LinearLayout top=new LinearLayout(this); top.setGravity(Gravity.CENTER_VERTICAL); top.setOrientation(LinearLayout.HORIZONTAL);
        TextView mark=text("ᛒ",25,Color.WHITE,true); mark.setGravity(Gravity.CENTER); mark.setBackground(round(BLUE,18));
        top.addView(mark,new LinearLayout.LayoutParams(dp(54),dp(54)));
        LinearLayout box=new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(dp(12),0,0,0);
        box.addView(text("Bluetooth",22,NAVY,true)); box.addView(text("חיבור פשוט למכשירים שלך",13,MUTED,false));
        top.addView(box,new LinearLayout.LayoutParams(0,-2,1)); return top;
    }
    private View pageTitle(String title,String subtitle) {
        LinearLayout box=new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL);
        box.addView(text(title,25,NAVY,true)); TextView sub=text(subtitle,13,MUTED,false); sub.setPadding(0,dp(5),0,0); box.addView(sub); return box;
    }
    private LinearLayout cardColumn() {
        LinearLayout card=new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16),dp(14),dp(16),dp(14)); card.setBackground(round(Color.WHITE,18)); card.setElevation(dp(2)); return card;
    }
    private void addAction(LinearLayout parent,String icon,String title,String description,View.OnClickListener click) {
        LinearLayout card=new LinearLayout(this); card.setOrientation(LinearLayout.HORIZONTAL); card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(13),dp(12),dp(13),dp(12)); card.setBackground(round(Color.WHITE,17)); card.setElevation(dp(1));
        LinearLayout.LayoutParams cp=matchWrap(); cp.bottomMargin=dp(8); parent.addView(card,cp);
        TextView badge=text(icon,22,BLUE,true); badge.setGravity(Gravity.CENTER); badge.setBackground(round(Color.rgb(235,241,255),14));
        card.addView(badge,new LinearLayout.LayoutParams(dp(46),dp(46)));
        LinearLayout words=new LinearLayout(this); words.setOrientation(LinearLayout.VERTICAL); words.setPadding(dp(12),0,dp(6),0);
        words.addView(text(title,15,TEXT,true)); TextView desc=text(description,12,MUTED,false); desc.setPadding(0,dp(4),0,0); words.addView(desc);
        card.addView(words,new LinearLayout.LayoutParams(0,-2,1)); card.addView(text("‹",27,MUTED,false));
        card.setOnClickListener(click); card.setClickable(true); card.setForeground(ripple());
    }
    private void showEmptyState() {
        if(deviceList==null||deviceList.getChildCount()>0)return;
        LinearLayout empty=cardColumn(); empty.setGravity(Gravity.CENTER);
        empty.addView(text("⌁",34,Color.rgb(150,169,201),true));
        TextView title=text("עדיין אין מכשירים להצגה",15,TEXT,true); title.setPadding(0,dp(7),0,dp(4)); empty.addView(title);
        empty.addView(text("הפעל סריקה או רענן מכשירים משויכים.",13,MUTED,false)); deviceList.addView(empty,matchWrap());
    }
    private void addDevice(BluetoothDevice device,String state) {
        if(device==null||deviceList==null)return;
        try {
            String address=device.getAddress(); if(devices.containsKey(address))return; devices.put(address,device);
            String name=device.getName(); if(name==null||name.trim().isEmpty())name="מכשיר Bluetooth";
            if(devices.size()==1)deviceList.removeAllViews();
            LinearLayout card=cardColumn(); LinearLayout.LayoutParams cp=matchWrap(); cp.bottomMargin=dp(8); deviceList.addView(card,cp);
            card.addView(text("◉  "+name,16,TEXT,true));
            TextView details=text(address+"   •   "+state,12,MUTED,false); details.setPadding(0,dp(6),0,dp(8)); card.addView(details);
            Button pair=button("בקש צימוד",false); pair.setOnClickListener(v->pairDevice(device)); card.addView(pair,matchWrap());
            if(count!=null)count.setText(String.valueOf(devices.size()));
        } catch(SecurityException e){setStatus("נדרשת הרשאת Bluetooth כדי לקרוא פרטי מכשיר");}
    }
    private void registerBluetoothReceiver() {
        IntentFilter filter=new IntentFilter(); filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED); filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        try { if(Build.VERSION.SDK_INT>=33)registerReceiver(bluetoothReceiver,filter,Context.RECEIVER_NOT_EXPORTED); else registerReceiver(bluetoothReceiver,filter); receiverRegistered=true; }
        catch(Exception e){setStatus("לא ניתן להאזין לאירועי Bluetooth");}
    }
    private boolean hasPermissions() {
        if(Build.VERSION.SDK_INT>=31)return checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN)==PackageManager.PERMISSION_GRANTED&&checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)==PackageManager.PERMISSION_GRANTED;
        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED;
    }
    private void requestBluetoothPermissions() {
        if(Build.VERSION.SDK_INT>=31)requestPermissions(new String[]{Manifest.permission.BLUETOOTH_SCAN,Manifest.permission.BLUETOOTH_CONNECT},REQ_PERMISSIONS);
        else requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},REQ_PERMISSIONS);
    }
    @Override public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] results) {
        super.onRequestPermissionsResult(requestCode,permissions,results);
        if(requestCode==REQ_PERMISSIONS){if(hasPermissions())refreshPairedDevices();else setStatus("נדרשות הרשאות Bluetooth כדי לסרוק ולהציג מכשירים");}
    }
    private void enableBluetooth() {
        if(adapter==null){setStatus("אין תמיכה ב-Bluetooth במכשיר");return;}
        if(!hasPermissions()){requestBluetoothPermissions();return;}
        try{if(adapter.isEnabled())setStatus("Bluetooth כבר מופעל");else startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE),REQ_ENABLE_BT);}
        catch(SecurityException e){setStatus("אין הרשאה לבדוק או להפעיל Bluetooth");}
    }
    private boolean safeEnabled(){try{return adapter!=null&&adapter.isEnabled();}catch(SecurityException e){return false;}}
    private void startScan() {
        if(adapter==null){setStatus("אין תמיכה ב-Bluetooth במכשיר");return;}
        if(!hasPermissions()){requestBluetoothPermissions();return;}
        try{
            if(!adapter.isEnabled()){setStatus("יש להפעיל Bluetooth לפני הסריקה");enableBluetooth();return;}
            if(adapter.isDiscovering())adapter.cancelDiscovery();
            devices.clear(); deviceList.removeAllViews(); if(count!=null)count.setText("0");
            setStatus("סורק מכשירים… השאירו את המסך פתוח"); adapter.startDiscovery();
        }catch(SecurityException e){setStatus("אין הרשאה לסריקה. בדקו את הרשאות האפליקציה");}
        catch(Exception e){setStatus("הסריקה לא התחילה: "+e.getMessage());}
    }
    private void refreshPairedDevices() {
        if(adapter==null){setStatus("אין תמיכה ב-Bluetooth במכשיר");return;}
        if(!hasPermissions()){requestBluetoothPermissions();return;}
        try{
            devices.clear(); deviceList.removeAllViews(); if(count!=null)count.setText("0");
            if(!adapter.isEnabled()){setStatus("Bluetooth כבוי");showEmptyState();return;}
            Set<BluetoothDevice> paired=adapter.getBondedDevices();
            if(paired!=null)for(BluetoothDevice device:paired)addDevice(device,"משויך");
            if(devices.isEmpty())showEmptyState();
            setStatus("מכשירים משויכים: "+(paired==null?0:paired.size())+". לסריקה לחצו על הכפתור.");
        }catch(SecurityException e){setStatus("אין הרשאה להציג מכשירים משויכים");}
    }
    private void pairDevice(BluetoothDevice device) {
        if(!hasPermissions()){requestBluetoothPermissions();return;}
        try{if(adapter!=null&&adapter.isDiscovering())adapter.cancelDiscovery();
            boolean started=device.createBond();setStatus(started?"נשלחה בקשת צימוד. אשרו אותה במכשיר השני אם נדרש.":"לא ניתן להתחיל צימוד. נסו דרך הגדרות Bluetooth.");
        }catch(SecurityException e){setStatus("אין הרשאה לצימוד Bluetooth");}
        catch(Exception e){setStatus("הצימוד נכשל: "+e.getMessage());}
    }
    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIncomingShare(intent);
    }

    private void handleIncomingShare(Intent intent) {
        if (intent == null || incomingShareText == null) return;
        String action = intent.getAction();
        if (Intent.ACTION_SEND.equals(action)) {
            Uri uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            CharSequence sharedText = intent.getCharSequenceExtra(Intent.EXTRA_TEXT);
            if (uri != null) {
                showPage("share");
                setStatus("מעביר את הקובץ לשירות השיתוף של Android…");
                launchBluetoothTransfer(uri, intent.getType(), null);
            } else if (sharedText != null) {
                showPage("share");
                incomingShareText.setText("התקבל טקסט לשיתוף:\n" + sharedText);
                setStatus("התקבל טקסט לשיתוף");
                launchBluetoothTransfer(null, intent.getType(), sharedText);
            }
        } else if (Intent.ACTION_SEND_MULTIPLE.equals(action)) {
            java.util.ArrayList<Uri> items = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
            int total = items == null ? 0 : items.size();
            incomingShareText.setText("התקבלו " + total + " פריטים לשיתוף.\nהעברה בפועל תלויה באפשרויות Android ובתמיכת המכשיר.");
            showPage("share");
            setStatus("התקבלו פריטים לשיתוף: " + total);
        }
    }

    private void chooseFile() {
        Intent intent=new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(Intent.createChooser(intent,"בחרו קובץ לשליחה"),REQ_FILE);
        } catch(Exception e) {
            setStatus("לא נמצא מנהל קבצים במכשיר");
        }
    }

    private void launchBluetoothTransfer(Uri uri, String mime, CharSequence sharedText) {
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType(mime == null || mime.trim().isEmpty() ? "*/*" : mime);
        if (uri != null) {
            send.putExtra(Intent.EXTRA_STREAM, uri);
            send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
        if (sharedText != null) send.putExtra(Intent.EXTRA_TEXT, sharedText);

        // Prefer Android's built-in Bluetooth file-transfer component.
        Intent bluetoothSend = new Intent(send);
        bluetoothSend.setPackage("com.android.bluetooth");
        try {
            if (getPackageManager().resolveActivity(bluetoothSend, 0) != null) {
                startActivity(bluetoothSend);
                if (incomingShareText != null) incomingShareText.setText("הקובץ נבחר. המשיכו במסך Bluetooth של Android ובחרו את המכשיר המקבל.");
                setStatus("נפתח מסך שליחת Bluetooth");
                return;
            }
        } catch (Exception ignored) { }

        // Fallback for devices using a different Bluetooth sharing component.
        try {
            startActivity(Intent.createChooser(send, "שליחה דרך Bluetooth או אפליקציה תומכת"));
            if (incomingShareText != null) incomingShareText.setText("הקובץ נבחר. בתפריט שנפתח בחרו Bluetooth או אפליקציית שיתוף תומכת.");
            setStatus("נפתח תפריט שליחת הקובץ");
        } catch (Exception e) {
            if (incomingShareText != null) incomingShareText.setText("הקובץ נבחר, אך לא נמצאה אפליקציה לשיתוף. בדקו ש-Bluetooth מופעל.");
            setStatus("לא נמצאה אפליקציה להעברת קבצים. נסו דרך הגדרות Bluetooth של Android.");
        }
    }

    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data) {
        super.onActivityResult(requestCode,resultCode,data);
        if(requestCode==REQ_FILE&&resultCode==RESULT_OK&&data!=null&&data.getData()!=null){
            Uri uri=data.getData();
            String mime=getContentResolver().getType(uri);
            String displayName="הקובץ נבחר בהצלחה.";
            try {
                android.database.Cursor cursor=getContentResolver().query(uri,new String[]{android.provider.OpenableColumns.DISPLAY_NAME},null,null,null);
                if(cursor!=null) {
                    if(cursor.moveToFirst()) {
                        int index=cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                        if(index>=0) displayName="נבחר: "+cursor.getString(index);
                    }
                    cursor.close();
                }
            } catch(Exception ignored) { }
            if(incomingShareText!=null) incomingShareText.setText(displayName+"\\nפותח כעת את אפשרות השליחה.");
            launchBluetoothTransfer(uri,mime,null);
        } else if(requestCode==REQ_ENABLE_BT){if(resultCode==RESULT_OK)refreshPairedDevices();else setStatus("Bluetooth לא הופעל");}
    }
    private void setStatus(String message) {
        if(statusHome!=null)statusHome.setText(message);
        if(statusDevices!=null)statusDevices.setText(message);
    }
    private TextView text(String value,int size,int color,boolean bold) {
        TextView view=new TextView(this); view.setText(value); view.setTextSize(size); view.setTextColor(color);
        if(bold)view.setTypeface(null,Typeface.BOLD); view.setGravity(Gravity.RIGHT); view.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG); return view;
    }
    private Button button(String label,boolean primary) {
        Button b=new Button(this); b.setText(label); b.setAllCaps(false); b.setTextSize(15); b.setTypeface(null,Typeface.BOLD);
        b.setPadding(dp(12),dp(8),dp(12),dp(8));
        int normalBg = primary ? Color.WHITE : BLUE;
        int pressedBg = primary ? BLUE : Color.WHITE;
        int normalText = primary ? BLUE : Color.WHITE;
        int pressedText = primary ? Color.WHITE : BLUE;
        android.content.res.ColorStateList textStates = new android.content.res.ColorStateList(
            new int[][] { new int[] { android.R.attr.state_pressed }, new int[] {} },
            new int[] { pressedText, normalText });
        android.graphics.drawable.StateListDrawable backgrounds = new android.graphics.drawable.StateListDrawable();
        backgrounds.addState(new int[] { android.R.attr.state_pressed }, round(pressedBg,14));
        backgrounds.addState(new int[] {}, round(normalBg,14));
        b.setTextColor(textStates); b.setBackground(backgrounds); b.setMinHeight(dp(48)); b.setStateListAnimator(null); return b;
    }
    private GradientDrawable round(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    private GradientDrawable gradient(int[] colors,int radius){GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TL_BR,colors);d.setCornerRadius(dp(radius));return d;}
    private android.graphics.drawable.Drawable ripple(){android.util.TypedValue out=new android.util.TypedValue();getTheme().resolveAttribute(android.R.attr.selectableItemBackground,out,true);return getDrawable(out.resourceId);}
    private LinearLayout.LayoutParams matchWrap(){return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT);}
    private int dp(int value){return (int)(value*getResources().getDisplayMetrics().density+0.5f);}
    @Override public void onBackPressed(){if(!"home".equals(currentPage))showPage("home");else super.onBackPressed();}
    @Override protected void onDestroy(){
        if(adapter!=null&&hasPermissions()){try{if(adapter.isDiscovering())adapter.cancelDiscovery();}catch(Exception ignored){}}
        if(receiverRegistered){try{unregisterReceiver(bluetoothReceiver);}catch(Exception ignored){}}
        super.onDestroy();
    }
}