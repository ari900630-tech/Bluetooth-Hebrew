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
import android.widget.Toast;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class MainActivity extends Activity {
    private static final int REQ_PERMISSIONS = 210;
    private static final int REQ_ENABLE_BT = 211;
    private static final int REQ_FILE = 212;
    private BluetoothAdapter adapter;
    private LinearLayout deviceList;
    private TextView status;
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
                setStatus(adapter != null && adapter.isEnabled() ? "Bluetooth מופעל" : "Bluetooth כבוי");
            }
        }
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(246, 248, 252));
        getWindow().setNavigationBarColor(Color.WHITE);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
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
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(22), dp(20), dp(28));
        root.setBackgroundColor(Color.rgb(246, 248, 252));
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        scroll.addView(root);

        TextView title = new TextView(this);
        title.setText("Bluetooth בעברית");
        title.setTextColor(Color.rgb(24, 36, 62));
        title.setTextSize(27);
        title.setGravity(Gravity.RIGHT);
        title.setTypeface(null, 1);
        root.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("ניהול מכשירים, צימוד ושיתוף קבצים");
        subtitle.setTextColor(Color.rgb(92, 105, 128));
        subtitle.setTextSize(15);
        subtitle.setPadding(0, dp(6), 0, dp(16));
        root.addView(subtitle);

        status = new TextView(this);
        status.setText("בודק את מצב Bluetooth…");
        status.setTextColor(Color.rgb(32, 89, 155));
        status.setTextSize(14);
        status.setPadding(dp(14), dp(12), dp(14), dp(12));
        status.setBackgroundColor(Color.WHITE);
        root.addView(status, matchWrap());

        addButton(root, "הפעל Bluetooth", v -> enableBluetooth());
        addButton(root, "סרוק מכשירים בסביבה", v -> startScan());
        addButton(root, "רענן מכשירים משויכים", v -> refreshPairedDevices());
        addButton(root, "בחר קובץ לשיתוף", v -> chooseFile());
        addButton(root, "פתח הגדרות Bluetooth", v -> startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS)));

        TextView heading = new TextView(this);
        heading.setText("מכשירים");
        heading.setTextSize(19);
        heading.setTypeface(null, 1);
        heading.setTextColor(Color.rgb(24, 36, 62));
        heading.setPadding(0, dp(22), 0, dp(8));
        root.addView(heading);

        deviceList = new LinearLayout(this);
        deviceList.setOrientation(LinearLayout.VERTICAL);
        deviceList.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.addView(deviceList, matchWrap());
        setContentView(scroll);
    }

    private void addButton(LinearLayout root, String label, View.OnClickListener click) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextSize(15);
        button.setOnClickListener(click);
        LinearLayout.LayoutParams p = matchWrap();
        p.topMargin = dp(8);
        root.addView(button, p);
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
        } catch (Exception e) {
            setStatus("לא ניתן להפעיל האזנה לאירועי Bluetooth");
        }
    }

    private boolean hasPermissions() {
        if (Build.VERSION.SDK_INT >= 31) {
            return checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
                    && checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
        }
        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= 31) {
            requestPermissions(new String[]{Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT}, REQ_PERMISSIONS);
        } else {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQ_PERMISSIONS);
        }
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
            setStatus("סורק מכשירים… השאירו את המסך פתוח");
            adapter.startDiscovery();
        } catch (SecurityException e) {
            setStatus("אין הרשאה לסריקה. בדקו את הרשאות האפליקציה בהגדרות");
        } catch (Exception e) {
            setStatus("הסריקה לא התחילה: " + e.getMessage());
        }
    }

    private void refreshPairedDevices() {
        if (adapter == null) { setStatus("אין תמיכה ב-Bluetooth במכשיר"); return; }
        if (!hasPermissions()) { requestBluetoothPermissions(); return; }
        try {
            devices.clear();
            deviceList.removeAllViews();
            if (!adapter.isEnabled()) { setStatus("Bluetooth כבוי"); return; }
            Set<BluetoothDevice> paired = adapter.getBondedDevices();
            if (paired != null) for (BluetoothDevice device : paired) addDevice(device, "משויך");
            setStatus("מכשירים משויכים: " + (paired == null ? 0 : paired.size()) + ". לסריקה לחצו על הכפתור למעלה.");
        } catch (SecurityException e) {
            setStatus("אין הרשאה להציג מכשירים משויכים");
        }
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
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(14), dp(10), dp(14), dp(10));
            card.setBackgroundColor(Color.WHITE);
            LinearLayout.LayoutParams cp = matchWrap();
            cp.topMargin = dp(7);
            deviceList.addView(card, cp);

            TextView label = new TextView(this);
            label.setText(name + "\n" + address + "\n" + state);
            label.setTextColor(Color.rgb(35, 47, 69));
            label.setTextSize(14);
            card.addView(label, matchWrap());

            Button pair = new Button(this);
            pair.setAllCaps(false);
            pair.setText("בקש צימוד למכשיר");
            pair.setOnClickListener(v -> pairDevice(selected));
            card.addView(pair, matchWrap());
        } catch (SecurityException e) {
            setStatus("נדרשת הרשאת Bluetooth כדי לקרוא פרטי מכשיר");
        }
    }

    private void pairDevice(BluetoothDevice device) {
        if (!hasPermissions()) { requestBluetoothPermissions(); return; }
        try {
            if (adapter != null && adapter.isDiscovering()) adapter.cancelDiscovery();
            boolean started = device.createBond();
            setStatus(started ? "נשלחה בקשת צימוד. אשרו אותה גם במכשיר השני אם נדרש." : "לא ניתן להתחיל צימוד. נסו דרך הגדרות Bluetooth.");
        } catch (SecurityException e) {
            setStatus("אין הרשאה לצימוד Bluetooth");
        } catch (Exception e) {
            setStatus("הצימוד נכשל: " + e.getMessage());
        }
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
            share.setType(getContentResolver().getType(uri) == null ? "*/*" : getContentResolver().getType(uri));
            share.putExtra(Intent.EXTRA_STREAM, uri);
            share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            try {
                startActivity(Intent.createChooser(share, "שיתוף קובץ — בחרו Bluetooth אם זמין"));
                setStatus("נפתח תפריט השיתוף של Android. העברה בפועל תלויה ביעד שנבחר.");
            } catch (Exception e) {
                setStatus("לא ניתן לפתוח את תפריט השיתוף");
            }
        } else if (requestCode == REQ_ENABLE_BT) {
            if (resultCode == RESULT_OK) refreshPairedDevices();
            else setStatus("Bluetooth לא הופעל");
        }
    }

    private void setStatus(String message) {
        if (status != null) status.setText(message);
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
