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
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class MainActivity extends Activity {
    private static final int REQ_PERMISSIONS = 210;
    private static final int REQ_ENABLE_BT = 211;
    private static final int REQ_FILE = 212;
    private static final int NAVY = Color.rgb(20, 39, 77);
    private static final int BLUE = Color.rgb(39, 103, 232);
    private static final int BG = Color.rgb(245, 247, 252);
    private static final int TEXT = Color.rgb(35, 48, 72);
    private static final int MUTED = Color.rgb(105, 119, 143);
    private BluetoothAdapter adapter;
    private LinearLayout deviceList;
    private TextView status;
    private TextView count;
    private final Map<String, BluetoothDevice> devices = new LinkedHashMap<>();
    private boolean receiverRegistered = false;

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
        BluetoothManager manager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        adapter = manager == null ? null : manager.getAdapter();
        buildUi();
        registerBluetoothReceiver();
        if (adapter == null) setStatus("המכשיר אינו תומך ב-Bluetooth");
        else if (!hasPermissions()) requestBluetoothPermissions();
        else refreshPairedDevices();
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(14), dp(18), dp(30));
        root.setBackgroundColor(BG);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        scroll.addView(root);

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setOrientation(LinearLayout.HORIZONTAL);
        TextView mark = new TextView(this);
        mark.setText("ᛒ");
        mark.setTextSize(25);
        mark.setTextColor(Color.WHITE);
        mark.setGravity(Gravity.CENTER);
        mark.setTypeface(null, Typeface.BOLD);
        mark.setBackground(round(BLUE, 18));
        top.addView(mark, new LinearLayout.LayoutParams(dp(54), dp(54)));
        LinearLayout titleBox = new LinearLayout(this);
        titleBox.setOrientation(LinearLayout.VERTICAL);
        titleBox.setPadding(dp(12), 0, 0, 0);
        TextView title = text("Bluetooth בעברית", 22, NAVY, true);
        TextView subtitle = text("חיבור פשוט למכשירים שלך", 13, MUTED, false);
        titleBox.addView(title);
        titleBox.addView(subtitle);
        top.addView(titleBox, new LinearLayout.LayoutParams(0, -2, 1));
        root.addView(top, matchWrap());

        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setPadding(dp(20), dp(19), dp(20), dp(20));
        hero.setBackground(gradient(new int[]{Color.rgb(28, 76, 160), Color.rgb(45, 116, 239)}, 24));
        LinearLayout.LayoutParams hp = matchWrap();
        hp.topMargin = dp(20);
        root.addView(hero, hp);
        TextView heroEyebrow = text("חיבור אלחוטי", 13, Color.rgb(219, 232, 255), true);
        hero.addView(heroEyebrow);
        TextView heroTitle = text("כל המכשירים שלך, במקום אחד", 21, Color.WHITE, true);
        heroTitle.setPadding(0, dp(7), 0, dp(6));
        hero.addView(heroTitle);
        TextView heroBody = text("סרוק מכשירים בסביבה, הצג מכשירים משויכים ונהל צימוד.", 14, Color.rgb(231, 239, 255), false);
        heroBody.setLineSpacing(dp(3), 1f);
        hero.addView(heroBody);
        Button scan = button("⌕   סריקת מכשירים", true);
        LinearLayout.LayoutParams sp = matchWrap();
        sp.topMargin = dp(16);
        hero.addView(scan, sp);
        scan.setOnClickListener(v -> startScan());

        LinearLayout statusCard = new LinearLayout(this);
        statusCard.setOrientation(LinearLayout.VERTICAL);
        statusCard.setPadding(dp(16), dp(13), dp(16), dp(13));
        statusCard.setBackground(round(Color.WHITE, 18));
        statusCard.setElevation(dp(2));
        LinearLayout.LayoutParams stp = matchWrap();
        stp.topMargin = dp(14);
        root.addView(statusCard, stp);
        LinearLayout statusRow = new LinearLayout(this);
        statusRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView dot = text("●", 12, Color.rgb(35, 176, 117), true);
        statusRow.addView(dot);
        TextView statusTitle = text("  מצב המכשיר", 14, TEXT, true);
        statusRow.addView(statusTitle);
        statusCard.addView(statusRow);
        status = text("בודק את מצב Bluetooth…", 13, MUTED, false);
        status.setPadding(0, dp(7), 0, 0);
        status.setLineSpacing(dp(2), 1f);
        statusCard.addView(status);

        TextView actionsHeading = text("פעולות מהירות", 18, NAVY, true);
        actionsHeading.setPadding(0, dp(23), 0, dp(10));
        root.addView(actionsHeading);
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.VERTICAL);
        root.addView(actions, matchWrap());
        addAction(actions, "◉", "הפעל Bluetooth", "הפעל או בדוק את מצב החיבור", v -> enableBluetooth());
        addAction(actions, "↻", "רענן מכשירים משויכים", "הצג מכשירים שכבר צומדו", v -> refreshPairedDevices());
        addAction(actions, "⇧", "שתף קובץ", "בחר קובץ ופתח את תפריט השיתוף של Android", v -> chooseFile());
        addAction(actions, "⚙", "הגדרות Bluetooth", "הגדרות המערכת לניהול חיבורים", v -> {
            try { startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS)); }
            catch (Exception e) { setStatus("לא ניתן לפתוח את הגדרות Bluetooth"); }
        });

        LinearLayout listHeading = new LinearLayout(this);
        listHeading.setGravity(Gravity.CENTER_VERTICAL);
        listHeading.setPadding(0, dp(25), 0, dp(10));
        TextView devicesTitle = text("המכשירים שלי", 18, NAVY, true);
        listHeading.addView(devicesTitle, new LinearLayout.LayoutParams(0, -2, 1));
        count = text("0", 13, BLUE, true);
        count.setGravity(Gravity.CENTER);
        count.setBackground(round(Color.rgb(226, 236, 255), 14));
        listHeading.addView(count, new LinearLayout.LayoutParams(dp(36), dp(28)));
        root.addView(listHeading);
        deviceList = new LinearLayout(this);
        deviceList.setOrientation(LinearLayout.VERTICAL);
        deviceList.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.addView(deviceList, matchWrap());
        showEmptyState();

        TextView foot = text("העברת קבצים ושמע תלויה בתמיכה של Android והמכשיר שאליו מתחברים.", 12, MUTED, false);
        foot.setGravity(Gravity.CENTER);
        foot.setPadding(dp(8), dp(22), dp(8), 0);
        root.addView(foot, matchWrap());
        setContentView(scroll);
    }

    private void addAction(LinearLayout parent, String icon, String title, String description, View.OnClickListener click) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(13), dp(12), dp(13), dp(12));
        card.setBackground(round(Color.WHITE, 17));
        card.setElevation(dp(1));
        LinearLayout.LayoutParams cp = matchWrap();
        cp.bottomMargin = dp(8);
        parent.addView(card, cp);
        TextView badge = text(icon, 22, BLUE, true);
        badge.setGravity(Gravity.CENTER);
        badge.setBackground(round(Color.rgb(235, 241, 255), 14));
        card.addView(badge, new LinearLayout.LayoutParams(dp(46), dp(46)));
        LinearLayout words = new LinearLayout(this);
        words.setOrientation(LinearLayout.VERTICAL);
        words.setPadding(dp(12), 0, dp(6), 0);
        words.addView(text(title, 15, TEXT, true));
        TextView desc = text(description, 12, MUTED, false);
        desc.setPadding(0, dp(4), 0, 0);
        words.addView(desc);
        card.addView(words, new LinearLayout.LayoutParams(0, -2, 1));
        TextView arrow = text("‹", 27, MUTED, false);
        card.addView(arrow);
        card.setOnClickListener(click);
        card.setClickable(true);
        card.setForeground(ripple());
    }

    private void showEmptyState() {
        if (deviceList == null || deviceList.getChildCount() > 0) return;
        LinearLayout empty = new LinearLayout(this);
        empty.setOrientation(LinearLayout.VERTICAL);
        empty.setGravity(Gravity.CENTER);
        empty.setPadding(dp(20), dp(22), dp(20), dp(22));
        empty.setBackground(round(Color.WHITE, 18));
        TextView icon = text("⌁", 34, Color.rgb(150, 169, 201), true);
        empty.addView(icon);
        TextView title = text("עדיין אין מכשירים להצגה", 15, TEXT, true);
        title.setPadding(0, dp(7), 0, dp(4));
        empty.addView(title);
        TextView desc = text("הפעל סריקה או רענן מכשירים משויכים.", 13, MUTED, false);
        empty.addView(desc);
        deviceList.addView(empty, matchWrap());
    }

    private void addDevice(BluetoothDevice device, String state) {
        if (device == null || deviceList == null) return;
        try {
            String address = device.getAddress();
            if (devices.containsKey(address)) return;
            devices.put(address, device);
            String name = device.getName();
            if (name == null || name.trim().isEmpty()) name = "מכשיר Bluetooth";
            final BluetoothDevice selected = device;
            if (deviceList.getChildCount() == 1 && devices.size() == 1) {
                View first = deviceList.getChildAt(0);
                if (first.getTag() != null && "empty".equals(first.getTag())) deviceList.removeAllViews();
                else if (first instanceof LinearLayout) deviceList.removeAllViews();
            }
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(15), dp(13), dp(15), dp(13));
            card.setBackground(round(Color.WHITE, 17));
            card.setElevation(dp(1));
            LinearLayout.LayoutParams cp = matchWrap();
            cp.bottomMargin = dp(8);
            deviceList.addView(card, cp);
            TextView label = text("◉  " + name, 16, TEXT, true);
            card.addView(label);
            TextView details = text(address + "   •   " + state, 12, MUTED, false);
            details.setPadding(0, dp(6), 0, dp(8));
            card.addView(details);
            Button pair = button("בקש צימוד", false);
            pair.setOnClickListener(v -> pairDevice(selected));
            card.addView(pair, matchWrap());
            if (count != null) count.setText(String.valueOf(devices.size()));
        } catch (SecurityException e) {
            setStatus("נדרשת הרשאת Bluetooth כדי לקרוא פרטי מכשיר");
        }
    }

    private void registerBluetoothReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        try {
            if (Build.VERSION.SDK_INT >= 33) registerReceiver(bluetoothReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
            else registerReceiver(bluetoothReceiver, filter);
            receiverRegistered = true;
        } catch (Exception e) { setStatus("לא ניתן להאזין לאירועי Bluetooth"); }
    }

    private boolean hasPermissions() {
        if (Build.VERSION.SDK_INT >= 31) {
            return checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
                    && checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
        }
        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= 31) requestPermissions(new String[]{Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT}, REQ_PERMISSIONS);
        else requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQ_PERMISSIONS);
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == REQ_PERMISSIONS) {
            if (hasPermissions()) refreshPairedDevices();
            else setStatus("נדרשות הרשאות Bluetooth כדי לסרוק ולהציג מכשירים");
        }
    }

    private void enableBluetooth() {
        if (adapter == null) { setStatus("אין תמיכה ב-Bluetooth במכשיר"); return; }
        if (!hasPermissions()) { requestBluetoothPermissions(); return; }
        try {
            if (adapter.isEnabled()) setStatus("Bluetooth כבר מופעל");
            else startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), REQ_ENABLE_BT);
        } catch (SecurityException e) { setStatus("אין הרשאה לבדוק או להפעיל Bluetooth"); }
    }

    private boolean safeEnabled() {
        try { return adapter != null && adapter.isEnabled(); }
        catch (SecurityException e) { return false; }
    }

    private void startScan() {
        if (adapter == null) { setStatus("אין תמיכה ב-Bluetooth במכשיר"); return; }
        if (!hasPermissions()) { requestBluetoothPermissions(); return; }
        try {
            if (!adapter.isEnabled()) {
                setStatus("יש להפעיל Bluetooth לפני הסריקה");
                enableBluetooth();
                return;
            }
            if (adapter.isDiscovering()) adapter.cancelDiscovery();
            devices.clear();
            deviceList.removeAllViews();
            if (count != null) count.setText("0");
            setStatus("סורק מכשירים… השאירו את המסך פתוח");
            adapter.startDiscovery();
        } catch (SecurityException e) { setStatus("אין הרשאה לסריקה. בדקו את הרשאות האפליקציה"); }
        catch (Exception e) { setStatus("הסריקה לא התחילה: " + e.getMessage()); }
    }

    private void refreshPairedDevices() {
        if (adapter == null) { setStatus("אין תמיכה ב-Bluetooth במכשיר"); return; }
        if (!hasPermissions()) { requestBluetoothPermissions(); return; }
        try {
            devices.clear();
            deviceList.removeAllViews();
            if (count != null) count.setText("0");
            if (!adapter.isEnabled()) { setStatus("Bluetooth כבוי"); showEmptyState(); return; }
            Set<BluetoothDevice> paired = adapter.getBondedDevices();
            if (paired != null) for (BluetoothDevice device : paired) addDevice(device, "משויך");
            if (devices.isEmpty()) showEmptyState();
            setStatus("מכשירים משויכים: " + (paired == null ? 0 : paired.size()) + ". לסריקה לחצו על הכפתור למעלה.");
        } catch (SecurityException e) { setStatus("אין הרשאה להציג מכשירים משויכים"); }
    }

    private void pairDevice(BluetoothDevice device) {
        if (!hasPermissions()) { requestBluetoothPermissions(); return; }
        try {
            if (adapter != null && adapter.isDiscovering()) adapter.cancelDiscovery();
            boolean started = device.createBond();
            setStatus(started ? "נשלחה בקשת צימוד. אשרו אותה גם במכשיר השני אם נדרש." : "לא ניתן להתחיל צימוד. נסו דרך הגדרות Bluetooth.");
        } catch (SecurityException e) { setStatus("אין הרשאה לצימוד Bluetooth"); }
        catch (Exception e) { setStatus("הצימוד נכשל: " + e.getMessage()); }
    }

    private void chooseFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try { startActivityForResult(Intent.createChooser(intent, "בחרו קובץ לשיתוף"), REQ_FILE); }
        catch (Exception e) { setStatus("לא נמצא מנהל קבצים במכשיר"); }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_FILE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            Intent share = new Intent(Intent.ACTION_SEND);
            String mime = getContentResolver().getType(uri);
            share.setType(mime == null ? "*/*" : mime);
            share.putExtra(Intent.EXTRA_STREAM, uri);
            share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            try {
                startActivity(Intent.createChooser(share, "שיתוף קובץ — בחרו Bluetooth אם זמין"));
                setStatus("נפתח תפריט השיתוף של Android. ההעברה תלויה ביעד שתבחרו.");
            } catch (Exception e) { setStatus("לא ניתן לפתוח את תפריט השיתוף"); }
        } else if (requestCode == REQ_ENABLE_BT) {
            if (resultCode == RESULT_OK) refreshPairedDevices();
            else setStatus("Bluetooth לא הופעל");
        }
    }

    private void setStatus(String message) { if (status != null) status.setText(message); }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        if (bold) view.setTypeface(null, Typeface.BOLD);
        view.setGravity(Gravity.RIGHT);
        view.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);
        return view;
    }

    private Button button(String label, boolean primary) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextSize(15);
        b.setTypeface(null, Typeface.BOLD);
        b.setPadding(dp(12), dp(8), dp(12), dp(8));
        b.setTextColor(primary ? BLUE : Color.WHITE);
        b.setBackground(round(primary ? Color.WHITE : BLUE, 14));
        b.setMinHeight(dp(48));
        b.setStateListAnimator(null);
        return b;
    }

    private GradientDrawable round(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        return d;
    }

    private GradientDrawable gradient(int[] colors, int radius) {
        GradientDrawable d = new GradientDrawable(GradientDrawable.Orientation.TL_BR, colors);
        d.setCornerRadius(dp(radius));
        return d;
    }

    private android.graphics.drawable.Drawable ripple() {
        android.util.TypedValue out = new android.util.TypedValue();
        getTheme().resolveAttribute(android.R.attr.selectableItemBackground, out, true);
        return getDrawable(out.resourceId);
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private int dp(int value) { return (int) (value * getResources().getDisplayMetrics().density + 0.5f); }

    @Override protected void onDestroy() {
        if (adapter != null && hasPermissions()) {
            try { if (adapter.isDiscovering()) adapter.cancelDiscovery(); } catch (Exception ignored) {}
        }
        if (receiverRegistered) {
            try { unregisterReceiver(bluetoothReceiver); } catch (Exception ignored) {}
        }
        super.onDestroy();
    }
}
