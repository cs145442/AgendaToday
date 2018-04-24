package cs145442.silicon.khetan.com.agendatoday;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {


    //Global Variables
    Button devScan, devStop;
    BluetoothManager btManager;
    BluetoothAdapter btAdapter;
    BluetoothLeScanner btScanner;
    Handler btHandler;
    Boolean mScanning;
    BLEAdapter bleAdapter;

    //Temp butoons
    TextView peripheralTextView;
    private final static int REQUEST_ENABLE_BT = 1;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    private static final long SCAN_PERIOD = 40 * 1000;

    //ArrayList<BLE> bleArrayList;
    ListView listView;

    //Variables to handle data flow
    String uuid = "";
    String minor = "";
    String major = "";
    String namespaceid = "";
    String instanceid = "";
    String eddystoneUrl = "";
    String rawData = "";
    int j = -1;
    int k = -1;
    String scanrecord = "";
    int temp_flag;
    int tcz_flag1 = 0;
    int tcz_flag2 = 0;
    int tcz_flag3 = 0;


    //Define the Arrays to handle array of data flow
    private ArrayList<Byte> subUrlarrayList;
    private ArrayList<Byte> urlarrayList;
    private ArrayList<Byte> suffUrlarrayList;
    ArrayList<BLE> bleArrayList = new ArrayList<>();

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Set the Activity Layout
        setContentView(R.layout.activity_main);

        //Location Permission
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            // Android M Permission checkU+2028
            if (this.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                final android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
                builder.setTitle("This app needs location access");
                builder.setMessage("Please grant location access so this app can detect beacons.");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new android.content.DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(android.content.DialogInterface dialog) {
                        requestPermissions(new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
                    }
                });
                builder.show();
            }
        }

        //Toast to Verify Locaiton grant
        Toast.makeText(getApplicationContext(), "Access of Location Granted", Toast.LENGTH_SHORT).show();


        //Bluetooth Permission
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "BLE Not Supported", Toast.LENGTH_SHORT).show();
            finish();
        }
        else {
            Toast.makeText(this, "BLE Supported", Toast.LENGTH_SHORT).show();
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        btAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (btAdapter == null) {
            Toast.makeText(this, "BLE Not Supported", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        //Defining Start Button
        devScan = (Button) findViewById(R.id.deviceScanBtn);
        devScan.setOnClickListener(this);

        //Defining Stop Button
        devStop = (Button) findViewById(R.id.deviceStopBtn);

        //Defining Text View
        peripheralTextView = (TextView) findViewById(R.id.PeripheralTextView);
        peripheralTextView.setMovementMethod(new ScrollingMovementMethod());

        //Setting the Visibility of STOP button
        devStop.setVisibility(View.INVISIBLE);
/*
        btManager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
        btAdapter = btManager.getAdapter();
        btScanner = btAdapter.getBluetoothLeScanner();
*/
        // Make sure we have access coarse location enabled, if not, prompt the user to enable it
       /* if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("This app needs location access");
            builder.setMessage("Please grant location access so this app can detect peripherals.");
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
                }
            });
            builder.show();
        }*/



    }
    @Override
    protected void onStart() {
        super.onStart();
        Log.d("tag", "In OnStart");
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!btAdapter.isEnabled()) {
            if (!btAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        } else {
            scanLeDevice(true);
        }

        // Initializes list view adapter.
        Log.d("tag", "In OnResume");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d("tag", "In OnPause");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d("tag", "In OnStop");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("tag", "In OnDestroy");
    }

    @Override
    public void onClick(View view){
        switch (view.getId()){
            case R.id.deviceScanBtn:
                Log.d("tag", "Somebody wants to scan");
                //New codes
                if (bleAdapter != null)
                    bleAdapter.clear();

                bleArrayList.clear();
                subUrlarrayList.clear();
                urlarrayList.clear();
                suffUrlarrayList.clear();
                scanLeDevice(true);
                //startScanning();
                Toast.makeText(getApplicationContext(), "Scanning Started.",
                        Toast.LENGTH_SHORT).show();
                break;
            case R.id.deviceStopBtn:
                Log.d("tag", "Somebody wants to stop the scan");
                stopScanning();
                break;
        }
    }


    //User defined Functions

    public void startScanning() {
        System.out.println("start scanning");
        peripheralTextView.setText("");
        devScan.setVisibility(View.INVISIBLE);
        devStop.setVisibility(View.VISIBLE);
        AsyncTask.execute(new Runnable() {
            @TargetApi(Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void run() {
                //Toast.makeText(getApplicationContext(), "LE ScanCallBack Started.",
                  //      Toast.LENGTH_SHORT).show();
                btScanner.startScan(leScanCallback);
            }
        });
    }

    public void stopScanning() {
        System.out.println("stopping scanning");
        peripheralTextView.append("Stopped Scanning");
        devScan.setVisibility(View.VISIBLE);
        devScan.setVisibility(View.INVISIBLE);
        AsyncTask.execute(new Runnable() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void run() {
                btScanner.stopScan(leScanCallback);
            }
        });
    }


    public String IntToHex2(int i) {
        char hex_2[] = {Character.forDigit((i >> 4) & 0x0f, 16), Character.forDigit(i & 0x0f, 16)};
        String hex_2_str = new String(hex_2);
        return hex_2_str.toUpperCase();

    }

    // Device scan callback.
    private ScanCallback leScanCallback = new ScanCallback() {
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            if(result.getDevice().getName() != null) {
                peripheralTextView.append("Device Name: " + result.getDevice().getName() + "    Rssi: " + result.getRssi() + "\n");
                peripheralTextView.append("Address : " + result.getDevice().getAddress() + "\n");
                //ParcelUuid [] p = result.getDevice().getUuids();
                byte[] scanRecord = result.getScanRecord().getBytes();
                String uuid2 = IntToHex2(scanRecord[9] & 0xff) + IntToHex2(scanRecord[10] & 0xff) + IntToHex2(scanRecord[11] & 0xff) + IntToHex2(scanRecord[12] & 0xff)
                        + "-" + IntToHex2(scanRecord[13] & 0xff) + IntToHex2(scanRecord[14] & 0xff)
                        + "-" + IntToHex2(scanRecord[15] & 0xff) + IntToHex2(scanRecord[16] & 0xff)
                        + "-" + IntToHex2(scanRecord[17] & 0xff) + IntToHex2(scanRecord[18] & 0xff)
                        + "-" + IntToHex2(scanRecord[19] & 0xff) + IntToHex2(scanRecord[20] & 0xff) + IntToHex2(scanRecord[21] & 0xff) + IntToHex2(scanRecord[22] & 0xff) + IntToHex2(scanRecord[23] & 0xff) + IntToHex2(scanRecord[24] & 0xff )
                        + "\nMajor :" + IntToHex2(scanRecord[25] & 0xff)  + IntToHex2(scanRecord[26] & 0xff)
                        + "\nMinor :" + IntToHex2(scanRecord[27] & 0xff)  + IntToHex2(scanRecord[28] & 0xff);
                peripheralTextView.append("UUID "+uuid2);
                peripheralTextView.append("\n\n\n");
            }

            // auto scroll for text view
            final int scrollAmount = peripheralTextView.getLayout().getLineTop(peripheralTextView.getLineCount()) - peripheralTextView.getHeight();
            // if there is no need to scroll, scrollAmount will be <=0
            if (scrollAmount > 0)
                peripheralTextView.scrollTo(0, scrollAmount);
        }
    };


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    System.out.println("coarse location permission granted");
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Functionality limited");
                    builder.setMessage("Since location access has not been granted, this app will not be able to discover beacons when in the background.");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                        @Override
                        public void onDismiss(DialogInterface dialog) {
                        }

                    });
                    builder.show();
                }
                return;
            }
        }
    }

    public void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            btHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    btAdapter.stopLeScan(mLeScanCallback);
                    invalidateOptionsMenu();
                }
            }, SCAN_PERIOD);

            mScanning = true;
            btAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            btAdapter.stopLeScan(mLeScanCallback);
        }
        invalidateOptionsMenu();
    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

                @Override
                public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {


                            String lRawdata = "";
                            for (int i = 0; i < scanRecord.length; i++) {


                                if ((Integer.toHexString(scanRecord[i] & 0xff)).length() == 1) {
                                    lRawdata = lRawdata + "0" + Integer.toHexString(scanRecord[i] & 0xff);
                                } else {
                                    lRawdata = lRawdata + Integer.toHexString(scanRecord[i] & 0xff);
                                }
                            }
                            scanrecord = lRawdata;

                            BLE ble = new BLE();
                            temp_flag = 0;
                            int url_flag = 0;


                            String ibyte10 = "", ibyte11 = "", ibyte12 = "", ibyte13 = "";
                            String strPrefix = "", strSuffix = "";


                            String byte1 = "", byte2 = "", byte3 = "", byte4 = "", byte5 = "", byte6 = "", byte7 = "", byte8 = "", byte9 = "", byte10 = "", byte11 = "", byte12 = "", byte13 = "";
                            String byte14 = "", byte15 = "", byte16 = "", byte17 = "", byte18 = "", byte19 = "", byte20 = "", byte21 = "", byte22 = "", byte23 = "", byte24 = "", byte25 = "";
                            String byte26 = "", byte27 = "", byte28 = "", byte29 = "", byte31 = "", byte32 = "", byte33 = "", byte34 = "", byte35 = "", byte36 = "", byte37 = "", byte38 = "";
                            String bytesuffix = "", byteurl = "", bytesuburl = "";

                            byte1 = Integer.toHexString(scanRecord[0] & 0xff);
                            byte2 = Integer.toHexString(scanRecord[1] & 0xff);
                            byte3 = Integer.toHexString(scanRecord[2] & 0xff);
                            byte4 = Integer.toHexString(scanRecord[3] & 0xff);
                            byte5 = Integer.toHexString(scanRecord[4] & 0xff);
                            byte6 = Integer.toHexString(scanRecord[5] & 0xff);
                            byte7 = Integer.toHexString(scanRecord[6] & 0xff);
                            byte8 = Integer.toHexString(scanRecord[7] & 0xff);
                            byte9 = Integer.toHexString(scanRecord[8] & 0xff);
                            byte10 = Integer.toHexString(scanRecord[9] & 0xff);
                            byte11 = Integer.toHexString(scanRecord[10] & 0xff);
                            byte12 = Integer.toHexString(scanRecord[11] & 0xff);
                            byte13 = Integer.toHexString(scanRecord[12] & 0xff);
                            byte31 = Integer.toHexString(scanRecord[30] & 0xff);
                            byte32 = Integer.toHexString(scanRecord[31] & 0xff);
                            byte33 = Integer.toHexString(scanRecord[32] & 0xff);
                            byte34 = Integer.toHexString(scanRecord[33] & 0xff);
                            byte35 = Integer.toHexString(scanRecord[34] & 0xff);
                            byte36 = Integer.toHexString(scanRecord[35] & 0xff);
                            try {


                                if ((byte1.equals("2")) && (byte2.equals("1")) && (byte3.equals("6"))) {

                                    // This detects beacon with iBeacon format
                                    if (byte4.equals("1a")) {

                                        temp_flag = 1;

                                        tcz_flag1 = 0;
                                        tcz_flag2 = 0;
                                        tcz_flag3 = 0;

                                        if ((byte5.equals("ff")) && (byte6.equals("4c")) && (byte7.equals("0")) && (byte8.equals("2")) && (byte9.equals("15"))) {

                                            String lenbyte10 = "", lenbyte11 = "", lenbyte12 = "", lenbyte13 = "", lenbyte14 = "", lenbyte15 = "", lenbyte16 = "";
                                            String lenbyte17 = "", lenbyte18 = "", lenbyte19 = "", lenbyte20 = "", lenbyte21 = "", lenbyte22 = "", lenbyte23 = "";
                                            String lenbyte24 = "", lenbyte25 = "", lenbyte26 = "", lenbyte27 = "", lenbyte28 = "", lenbyte29 = "";

                                            lenbyte10 = Integer.toHexString(scanRecord[9] & 0xff);
                                            lenbyte11 = Integer.toHexString(scanRecord[10] & 0xff);
                                            lenbyte12 = Integer.toHexString(scanRecord[11] & 0xff);
                                            lenbyte13 = Integer.toHexString(scanRecord[12] & 0xff);
                                            lenbyte14 = Integer.toHexString(scanRecord[13] & 0xff);
                                            lenbyte15 = Integer.toHexString(scanRecord[14] & 0xff);
                                            lenbyte16 = Integer.toHexString(scanRecord[15] & 0xff);
                                            lenbyte17 = Integer.toHexString(scanRecord[16] & 0xff);
                                            lenbyte18 = Integer.toHexString(scanRecord[17] & 0xff);
                                            lenbyte19 = Integer.toHexString(scanRecord[18] & 0xff);
                                            lenbyte20 = Integer.toHexString(scanRecord[19] & 0xff);
                                            lenbyte21 = Integer.toHexString(scanRecord[20] & 0xff);
                                            lenbyte22 = Integer.toHexString(scanRecord[21] & 0xff);
                                            lenbyte23 = Integer.toHexString(scanRecord[22] & 0xff);
                                            lenbyte24 = Integer.toHexString(scanRecord[23] & 0xff);
                                            lenbyte25 = Integer.toHexString(scanRecord[24] & 0xff);
                                            lenbyte26 = Integer.toHexString(scanRecord[25] & 0xff);
                                            lenbyte27 = Integer.toHexString(scanRecord[26] & 0xff);
                                            lenbyte28 = Integer.toHexString(scanRecord[27] & 0xff);
                                            lenbyte29 = Integer.toHexString(scanRecord[28] & 0xff);

                                            if (lenbyte10.length() == 1) {
                                                ibyte10 = "0" + lenbyte10;
                                            } else {
                                                ibyte10 = lenbyte10;
                                            }


                                            if (lenbyte11.length() == 1) {
                                                ibyte11 = "0" + lenbyte11;
                                            } else {
                                                ibyte11 = lenbyte11;
                                            }

                                            if (lenbyte12.length() == 1) {
                                                ibyte12 = "0" + lenbyte12;
                                            } else {
                                                ibyte12 = lenbyte12;
                                            }
                                            if (lenbyte13.length() == 1) {
                                                ibyte13 = "0" + lenbyte13;
                                            } else {
                                                ibyte13 = lenbyte13;
                                            }
                                            if (lenbyte14.length() == 1) {
                                                byte14 = "0" + lenbyte14;
                                            } else {
                                                byte14 = lenbyte14;
                                            }
                                            if (lenbyte15.length() == 1) {
                                                byte15 = "0" + lenbyte15;
                                            } else {
                                                byte15 = lenbyte15;
                                            }
                                            if (lenbyte16.length() == 1) {
                                                byte16 = "0" + lenbyte16;
                                            } else {
                                                byte16 = lenbyte16;
                                            }
                                            if (lenbyte17.length() == 1) {
                                                byte17 = "0" + lenbyte17;
                                            } else {
                                                byte17 = lenbyte17;
                                            }
                                            if (lenbyte18.length() == 1) {
                                                byte18 = "0" + lenbyte18;
                                            } else {
                                                byte18 = lenbyte18;
                                            }
                                            if (lenbyte19.length() == 1) {
                                                byte19 = "0" + lenbyte19;
                                            } else {
                                                byte19 = lenbyte19;
                                            }
                                            if (lenbyte20.length() == 1) {
                                                byte20 = "0" + lenbyte20;
                                            } else {
                                                byte20 = lenbyte20;
                                            }
                                            if (lenbyte21.length() == 1) {
                                                byte21 = "0" + lenbyte21;
                                            } else {
                                                byte21 = lenbyte21;
                                            }
                                            if (lenbyte22.length() == 1) {
                                                byte22 = "0" + lenbyte22;
                                            } else {
                                                byte22 = lenbyte22;
                                            }

                                            if (lenbyte23.length() == 1) {
                                                byte23 = "0" + lenbyte23;
                                            } else {
                                                byte23 = lenbyte23;
                                            }
                                            if (lenbyte24.length() == 1) {
                                                byte24 = "0" + lenbyte24;
                                            } else {
                                                byte24 = lenbyte24;
                                            }
                                            if (lenbyte25.length() == 1) {
                                                byte25 = "0" + lenbyte25;
                                            } else {
                                                byte25 = lenbyte25;
                                            }
                                            if (lenbyte26.length() == 1) {
                                                byte26 = "0" + lenbyte26;
                                            } else {
                                                byte26 = lenbyte26;
                                            }
                                            if (lenbyte27.length() == 1) {
                                                byte27 = "0" + lenbyte27;
                                            } else {
                                                byte27 = lenbyte27;
                                            }
                                            if (lenbyte28.length() == 1) {
                                                byte28 = "0" + lenbyte28;
                                            } else {
                                                byte28 = lenbyte28;
                                            }
                                            if (lenbyte29.length() == 1) {
                                                byte29 = "0" + lenbyte29;
                                            } else {
                                                byte29 = lenbyte29;
                                            }


//                                            if (byte31.equals("4")) {
//                                                if ((byte32.equals("9")) && (byte33.equals("54")) && (byte34.equals("43")) && (byte35.equals("5a"))) {
//                                                    tcz_flag1 = 1;
                                            uuid = ibyte10 + ibyte11 + ibyte12 + ibyte13 + byte14 + byte15 + byte16 + byte17 + byte18 + byte19 + byte20 + byte21 + byte22 + byte23 + byte24 + byte25;

                                            major = byte26 + byte27;

                                            minor = byte28 + byte29;

                                            // }
                                            //}
                                        }

                                        // This detects beacon with Eddystone format
                                    } else if (byte4.equals("3")) {


                                        if ((byte5.equals("3")) && (byte6.equals("aa")) && (byte7.equals("fe"))) {


                                            byte8 = Integer.toHexString(scanRecord[7]);

                                            if ((byte9.equals("16")) && (byte10.equals("aa")) && (byte11.equals("fe"))) {

                                                // This Detects beacon with Eddystone UID format
                                                if ((byte12.equals("0"))) {

                                                    temp_flag = 2;

                                                    String lenbyte14 = "", lenbyte15 = "", lenbyte16 = "", lenbyte17 = "", lenbyte18 = "", lenbyte19 = "", lenbyte20 = "";
                                                    String lenbyte21 = "", lenbyte22 = "", lenbyte23 = "", lenbyte24 = "", lenbyte25 = "", lenbyte26 = "", lenbyte27 = "", lenbyte28 = "", lenbyte29 = "";

                                                    lenbyte14 = Integer.toHexString(scanRecord[13] & 0xff);
                                                    lenbyte15 = Integer.toHexString(scanRecord[14] & 0xff);
                                                    lenbyte16 = Integer.toHexString(scanRecord[15] & 0xff);
                                                    lenbyte17 = Integer.toHexString(scanRecord[16] & 0xff);
                                                    lenbyte18 = Integer.toHexString(scanRecord[17] & 0xff);
                                                    lenbyte19 = Integer.toHexString(scanRecord[18] & 0xff);
                                                    lenbyte20 = Integer.toHexString(scanRecord[19] & 0xff);
                                                    lenbyte21 = Integer.toHexString(scanRecord[20] & 0xff);
                                                    lenbyte22 = Integer.toHexString(scanRecord[21] & 0xff);
                                                    lenbyte23 = Integer.toHexString(scanRecord[22] & 0xff);
                                                    lenbyte24 = Integer.toHexString(scanRecord[23] & 0xff);
                                                    lenbyte25 = Integer.toHexString(scanRecord[24] & 0xff);
                                                    lenbyte26 = Integer.toHexString(scanRecord[25] & 0xff);
                                                    lenbyte27 = Integer.toHexString(scanRecord[26] & 0xff);
                                                    lenbyte28 = Integer.toHexString(scanRecord[27] & 0xff);
                                                    lenbyte29 = Integer.toHexString(scanRecord[28] & 0xff);


                                                    if (lenbyte14.length() == 1) {
                                                        byte14 = "0" + lenbyte14;
                                                    } else {
                                                        byte14 = lenbyte14;
                                                    }


                                                    if (lenbyte15.length() == 1) {
                                                        byte15 = "0" + lenbyte15;
                                                    } else {
                                                        byte15 = lenbyte15;
                                                    }

                                                    if (lenbyte16.length() == 1) {
                                                        byte16 = "0" + lenbyte16;
                                                    } else {
                                                        byte16 = lenbyte16;
                                                    }
                                                    if (lenbyte17.length() == 1) {
                                                        byte17 = "0" + lenbyte17;
                                                    } else {
                                                        byte17 = lenbyte17;
                                                    }
                                                    if (lenbyte18.length() == 1) {
                                                        byte18 = "0" + lenbyte18;
                                                    } else {
                                                        byte18 = lenbyte18;
                                                    }
                                                    if (lenbyte19.length() == 1) {
                                                        byte19 = "0" + lenbyte19;
                                                    } else {
                                                        byte19 = lenbyte19;
                                                    }
                                                    if (lenbyte20.length() == 1) {
                                                        byte20 = "0" + lenbyte20;
                                                    } else {
                                                        byte20 = lenbyte20;
                                                    }
                                                    if (lenbyte21.length() == 1) {
                                                        byte21 = "0" + lenbyte21;
                                                    } else {
                                                        byte21 = lenbyte21;
                                                    }
                                                    if (lenbyte22.length() == 1) {
                                                        byte22 = "0" + lenbyte22;
                                                    } else {
                                                        byte22 = lenbyte22;
                                                    }
                                                    if (lenbyte23.length() == 1) {
                                                        byte23 = "0" + lenbyte23;
                                                    } else {
                                                        byte23 = lenbyte23;
                                                    }
                                                    if (lenbyte24.length() == 1) {
                                                        byte24 = "0" + lenbyte24;
                                                    } else {
                                                        byte24 = lenbyte24;
                                                    }
                                                    if (lenbyte25.length() == 1) {
                                                        byte25 = "0" + lenbyte25;
                                                    } else {
                                                        byte25 = lenbyte25;
                                                    }
                                                    if (lenbyte26.length() == 1) {
                                                        byte26 = "0" + lenbyte26;
                                                    } else {
                                                        byte26 = lenbyte26;
                                                    }

                                                    if (lenbyte27.length() == 1) {
                                                        byte27 = "0" + lenbyte27;
                                                    } else {
                                                        byte27 = lenbyte27;
                                                    }
                                                    if (lenbyte28.length() == 1) {
                                                        byte28 = "0" + lenbyte28;
                                                    } else {
                                                        byte28 = lenbyte28;
                                                    }
                                                    if (lenbyte29.length() == 1) {
                                                        byte29 = "0" + lenbyte29;
                                                    } else {
                                                        byte29 = lenbyte29;
                                                    }


                                                   /* if (byte32.equals("4")) {
                                                        if ((byte33.equals("9")) && (byte34.equals("54")) && (byte35.equals("43")) && (byte36.equals("5a"))) {

                                                            tcz_flag2 = 1;*/

                                                    namespaceid = byte14 + byte15 + byte16 + byte17 + byte18 + byte19 + byte20 + byte21 + byte22 + byte23;

                                                    instanceid = byte24 + byte25 + byte26 + byte27 + byte28 + byte29;

                                                       /* }
                                                    }*/

                                                    // This detects beacon with Eddystone URL format
                                                } else if (byte12.equals("10")) {

                                                    int lenUrl = 0;
                                                    String tcz1 = "", tcz2 = "", tcz3 = "", tcz4 = "", tcz5 = "";

                                                    temp_flag = 3;

                                                    String lenbyte14 = "", lenSuffix = "";


                                                    lenbyte14 = Integer.toHexString(scanRecord[13] & 0xff);
                                                    if (lenbyte14.length() == 1) {
                                                        byte14 = 0 + lenbyte14;
                                                    } else {
                                                        byte14 = lenbyte14;
                                                    }

                                                    for (int l = 14; l < (8 + Integer.parseInt(byte8, 16)); l++) {
                                                        byte15 = Integer.toHexString(scanRecord[l]);
                                                        if (byte15.equals("0")) {
                                                            url_flag = 1;
                                                        } else if (byte15.equals("1")) {
                                                            url_flag = 1;
                                                        } else if (byte15.equals("2")) {
                                                            url_flag = 1;
                                                        } else if (byte15.equals("3")) {
                                                            url_flag = 1;
                                                        } else if (byte15.equals("4")) {
                                                            url_flag = 1;
                                                        } else if (byte15.equals("5")) {
                                                            url_flag = 1;
                                                        } else if (byte15.equals("6")) {
                                                            url_flag = 1;
                                                        } else if (byte15.equals("7")) {
                                                            url_flag = 1;
                                                        } else if (byte15.equals("8")) {
                                                            url_flag = 1;
                                                        } else if (byte15.equals("9")) {
                                                            url_flag = 1;
                                                        } else if (byte15.equals("a")) {
                                                            url_flag = 1;
                                                        } else if (byte15.equals("b")) {
                                                            url_flag = 1;
                                                        } else if (byte15.equals("c")) {
                                                            url_flag = 1;
                                                        } else if (byte15.equals("d")) {
                                                            url_flag = 1;
                                                        }

                                                        if (url_flag == 0) {
                                                            j++;
                                                            urlarrayList.add(j, scanRecord[l]);
                                                            byteurl = byteurl + Integer.toHexString(urlarrayList.get(j));


                                                        }
                                                        if (url_flag == 2) {
                                                            k++;
                                                            subUrlarrayList.add(k, scanRecord[l]);
                                                            bytesuburl = bytesuburl + Integer.toHexString(subUrlarrayList.get(k));

                                                        }

                                                        if (url_flag == 1) {
                                                            suffUrlarrayList.add(0, scanRecord[l]);
                                                            lenSuffix = Integer.toHexString(suffUrlarrayList.get(0));
                                                            url_flag = 2;
                                                        }

                                                        if (lenSuffix.length() == 1) {
                                                            bytesuffix = "0" + lenSuffix;
                                                        } else {
                                                            bytesuffix = lenSuffix;
                                                        }


                                                        if (byte14.equals("00")) {
                                                            strPrefix = "http://www.";
                                                        } else if (byte14.equals("01")) {
                                                            strPrefix = "https://www.";
                                                        } else if (byte14.equals("02")) {
                                                            strPrefix = "http://";
                                                        } else if (byte14.equals("03")) {
                                                            strPrefix = "https://";
                                                        }


                                                        if (bytesuffix.equals("00")) {
                                                            strSuffix = ".com/";
                                                        } else if (bytesuffix.equals("01")) {
                                                            strSuffix = ".org/";
                                                        } else if (bytesuffix.equals("02")) {
                                                            strSuffix = ".edu/";
                                                        } else if (bytesuffix.equals("03")) {
                                                            strSuffix = ".net/";
                                                        } else if (bytesuffix.equals("04")) {
                                                            strSuffix = ".info/";
                                                        } else if (bytesuffix.equals("05")) {
                                                            strSuffix = ".biz/";
                                                        } else if (bytesuffix.equals("06")) {
                                                            strSuffix = ".gov/";
                                                        } else if (bytesuffix.equals("07")) {
                                                            strSuffix = ".com";
                                                        } else if (bytesuffix.equals("08")) {
                                                            strSuffix = ".org";
                                                        } else if (bytesuffix.equals("09")) {
                                                            strSuffix = ".edu";
                                                        } else if (bytesuffix.equals("0a")) {
                                                            strSuffix = ".net";
                                                        } else if (bytesuffix.equals("0b")) {
                                                            strSuffix = ".info";
                                                        } else if (bytesuffix.equals("0c")) {
                                                            strSuffix = ".biz";
                                                        } else if (bytesuffix.equals("0d")) {
                                                            strSuffix = ".gov";
                                                        }


                                                        lenUrl = l;


                                                    }


                                                    tcz1 = Integer.toHexString(scanRecord[lenUrl + 1]);
                                                    tcz2 = Integer.toHexString(scanRecord[lenUrl + 2]);
                                                    tcz3 = Integer.toHexString(scanRecord[lenUrl + 3]);
                                                    tcz4 = Integer.toHexString(scanRecord[lenUrl + 4]);
                                                    tcz5 = Integer.toHexString(scanRecord[lenUrl + 5]);


//                                                    if (tcz1.equals("4")) {
//                                                        if ((tcz2.equals("9")) && (tcz3.equals("54")) && (tcz4.equals("43")) && (tcz5.equals("5a"))) {

                                                    //tcz_flag3 = 1;

                                                    StringBuilder outputUrl = new StringBuilder();
                                                    for (int p = 0; p < byteurl.length(); p += 2) {
                                                        String str = byteurl.substring(p, p + 2);
                                                        outputUrl.append((char) Integer.parseInt(str, 16));
                                                    }
                                                    StringBuilder outputSuburl = new StringBuilder();
                                                    for (int q = 0; q < bytesuburl.length(); q += 2) {
                                                        String str = bytesuburl.substring(q, q + 2);
                                                        outputSuburl.append((char) Integer.parseInt(str, 16));
                                                    }


                                                    eddystoneUrl = strPrefix + outputUrl + strSuffix + outputSuburl;
                                                    //}
                                                    // }
                                                }
                                            }

                                        }
                                    }
                                } else {
                                    temp_flag = 4;
                                    String lenRawdata = "";

                                    for (int i = 0; i < scanRecord.length; i++) {


                                        if ((Integer.toHexString(scanRecord[i] & 0xff)).length() == 1) {
                                            lenRawdata = lenRawdata + "0" + Integer.toHexString(scanRecord[i] & 0xff);
                                        } else {
                                            lenRawdata = lenRawdata + Integer.toHexString(scanRecord[i] & 0xff);
                                        }
                                    }
                                    rawData = lenRawdata;
                                }

                            } catch (Exception e) {
                            }


                            String deviceName = device.getName();
                            String deviceAddress = device.getAddress();


                            // if (tcz_flag1 == 1 || tcz_flag2 == 1 || tcz_flag3 == 1) {
                            ble.setDeviceName(deviceName);
                            ble.setDeviceAddress(deviceAddress);
                            ble.setRssi(String.valueOf(rssi));


//                                if ((temp_flag == 1)&& (tcz_flag1 == 1)) {
                            if ((temp_flag == 1)) {

                                ble.setUuid(uuid);
                                ble.setMajor(major);
                                ble.setMinor(minor);

                            }


//                                if ((temp_flag == 2)&& (tcz_flag2 == 1)) {
                            if ((temp_flag == 2)) {
                                ble.setNamespaceid(namespaceid);
                                ble.setInstanceid(instanceid);
                            }

//                            if ((temp_flag == 3) && (tcz_flag3 == 1)) {
                            if ((temp_flag == 3)) {
                                ble.setUrl(eddystoneUrl);
                            }

                            if (temp_flag == 4) {
                                ble.setRawData(rawData);
                            }


                            int flag = 0;
                            int index = 0;

                            if (bleArrayList.size() > 0) {

                                for (BLE b : bleArrayList) {
                                    if (deviceName != "TCZ") {
                                        if (b.getDeviceAddress().equals(ble.deviceAddress)) {
                                            flag = 1;
                                            bleArrayList.set(index, ble);

                                        }
                                        index++;

                                    }
                                }

                            }


                            if (flag == 0) {

                                bleArrayList.add(ble);

                            }


                            if (bleAdapter == null) {
                                bleAdapter = new BLEAdapter(getApplicationContext(), bleArrayList);
                                listView.setAdapter(bleAdapter);
                            }
                            Comparator<BLE> bleArraylistComparator = new Comparator<BLE>() {
                                @Override
                                public int compare(BLE lhs, BLE rhs) {

                                    String strRssi = lhs.rssi;
                                    String strRssi2 = rhs.rssi;
                                    return strRssi.compareToIgnoreCase(strRssi2);
                                }
                            };

                            Collections.sort(bleArrayList, bleArraylistComparator);
                            bleAdapter.notifyDataSetChanged();
                            //}

                        }
                    });
                }
            };



}
