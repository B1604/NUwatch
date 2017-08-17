package org.pdinda.nuwatch_android;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Base64;
import android.util.Log;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

// Discrete SeekBar


public class MainActivity extends Activity implements BluetoothAdapter.LeScanCallback  {
    public static String shortUuidFormat = "0000%04X-0000-1000-8000-00805F9B34FB";

    public final static UUID UUID_SERVICE = UUID.fromString(String.format(shortUuidFormat,0x2220));
    public final static UUID UUID_RECEIVE = UUID.fromString(String.format(shortUuidFormat,0x2221));
    public final static UUID UUID_SEND = UUID.fromString(String.format(shortUuidFormat,0x2222));
    public final static UUID UUID_DISCONNECT = UUID.fromString(String.format(shortUuidFormat,0x2223));
    public final static UUID UUID_CLIENT_CONFIGURATION = UUID.fromString(String.format(shortUuidFormat,0x2902));


    private final static String TAG = "MainActivity";
    private boolean inDialog = false;
    private Timer timer = new Timer();

    private nuwatch_protocol protocol = new nuwatch_protocol();

    // UI Stuff
    private Plot plot;
    private TextView status;
    private SeekBar green;
    private SeekBar rate;



    // BLE Stuff
    private BluetoothAdapter adapter;
    private BluetoothDevice  watch;
    private BluetoothGatt watchGatt;
    private BluetoothGattService watchService;
    private BluetoothGattCharacteristic watchRead;
    private BluetoothGattCharacteristic watchWrite;
    private BluetoothGattCharacteristic watchReset;
    private String boundWatchAddr = "";

    // Write to file
    /*private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            //Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };*/

    private File myoutput;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        plot = (Plot) findViewById(R.id.plot);
        status = (TextView) findViewById(R.id.status);
        green = (SeekBar) findViewById(R.id.green);
        rate = (SeekBar) findViewById(R.id.rate);


        if (status==null) {
            Log.e(TAG, "Strange... status cannot be looked up\n");
        }

        adapter = BluetoothAdapter.getDefaultAdapter();

        if (adapter==null) {
            if (status!=null) {status.setText("No Bluetooth!");}
            return;
        }

        ActivityCompat.requestPermissions(this,new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1001);

        protocol.reset();

        green.setProgress(128);
        rate.setProgress(0);


        green.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                updateWatch();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                //nothing
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                //nothing
            }
        });

        rate.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                updateWatch();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                //nothing
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Toast.makeText(getBaseContext(),"sampling rate=" + String.valueOf(rate.getProgress()*10+1)+"Hz",Toast.LENGTH_SHORT).show();//nothing
            }
        });

        // write to file
        /*int permission = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_EXTERNAL_STORAGE
            );
        }*/

        String fileName = "wenting.txt";
        //String filename = "/storage/self/primary/nuWatch.csv";
        myoutput = new File("/storage/self/primary/"+fileName);
        //myoutput = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+ fileName);
        //System.out.println(Environment.getExternalStorageDirectory().getAbsolutePath());
        if (myoutput.exists()) {
            System.out.println("TAMAM");}
        else{
            try {
                myoutput.createNewFile();
                System.out.println("Create file successfully");
            } catch (Exception e) {
                System.out.println("Cannot create file: " + e);
            }
        }
        try {
            FileOutputStream output = new FileOutputStream(myoutput, true);
            System.out.println("asd");
            StringBuilder sb = new StringBuilder("Writing output to this file\n");
//            sb.append("TimePhone,TimeInMilSecPhone,TimeStampBoard,GSR,AccelX,AccelY,AccelZ,MagX,MagY,MagZ\n");
            sb.append("TimePhone,TimeInMilSecPhone,GSR,AccelX,AccelY,AccelZ,MagX,MagY,MagZ\n");

            output.write(sb.toString().getBytes());
            output.close();

        }catch (Exception e){
            System.out.println("File does not exist");
            e.printStackTrace();
        }

        //start periodic updates to any connected watch since we can lose writes
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                //Log.i(TAG,"Timer Task");
                updateWatch();
            }
        },500,1000);
    }




    private final BluetoothGattCallback  gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int state) {
            //Log.i(TAG, "Connection State Change");

            if (state == BluetoothProfile.STATE_CONNECTED) {
                //Log.i(TAG, "Connected, Scanning Services");
                gatt.discoverServices();
            } else if (state == BluetoothProfile.STATE_DISCONNECTED) {
                //Log.i(TAG, "Disconnected");
                watchService=null;
                watchRead=null;
                watchWrite=null;
                watchReset=null;
                MainActivity.this.runOnUiThread(new Runnable() { public void run() { MainActivity.this.status.setText("Disconnected");}});
                searchForWatch();
            } else {
                //Log.i(TAG, "Ignoring State Change To " + state);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            watchGatt = gatt;

            List<BluetoothGattService> services = gatt.getServices();

            //Log.i(TAG, "Setting up service/characteristics");

            watchService = null;
            for (BluetoothGattService s : services) {
                //Log.i(TAG,"Service " + s.getUuid());
                //Log.i(TAG,"Looking for " + UUID_SERVICE);
                if (s.getUuid().equals(UUID_SERVICE)) {
                    //Log.i(TAG, "Found Service");
                    watchService = s;
                    break;
                }
            }

            if (watchService == null) {
                Log.e(TAG, "Odd - does not have expected service...");
                MainActivity.this.runOnUiThread(new Runnable() { public void run() { connectWatch(); } });
                return;
            }


            watchRead = watchService.getCharacteristic(UUID_RECEIVE);
            watchWrite = watchService.getCharacteristic(UUID_SEND);
            watchReset = watchService.getCharacteristic(UUID_DISCONNECT);

            // start the data stream
            gatt.setCharacteristicNotification(watchRead, true);
            //gatt.readCharacteristic(watchRead);

            BluetoothGattDescriptor d = watchRead.getDescriptor(UUID_CLIENT_CONFIGURATION);
            d.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            gatt.writeDescriptor(d);

            MainActivity.this.runOnUiThread(new Runnable() { public void run() {
                MainActivity.this.status.setText("Operational");
            } });

        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic c)
        {
            byte[] seg;

            //Log.i(TAG,"Changed: "+c);

            gatt.readCharacteristic(watchRead);
            seg = watchRead.getValue();

            //Log.i(TAG,"Received "+seg.length+" bytes of data\n");
            //Log.i(TAG,"Data: " + hexify(seg));

            if (protocol.pump_input(seg)) {
                byte[] msg;
                msg = protocol.get_message();
                updateDisplay(msg);
                //updateServer(msg);
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt,
                                          BluetoothGattCharacteristic ch,
                                          int status)
        {

            if (status != BluetoothGatt.GATT_SUCCESS){
                Log.e(TAG,"Write to Characteristic FAILURE - timer task will retry");
            } else {
                //Log.i(TAG,"Write to Characteristic SUCCESS");
            }

        }
    };

    public void updateWatch()
    {
//        byte[] msg = {(byte) green.getProgress(),(byte) (rate.getProgress()+1)};
        byte[] msg = {(byte) green.getProgress(),(byte) (5)};
        boolean s;

        if (watchWrite == null) {
            // skip update if we are not connected
            return;
        }

//        Log.i(TAG,"UpdateWatch rate="+rate.getProgress()+1+" green="+green.getProgress());
        Log.i(TAG,"UpdateWatch rate="+"5Hz"+" green="+green.getProgress());

        s = watchWrite.setValue(msg);

        if (!s) {
            Log.e(TAG, "BLE setvalue failed - timer task will retry");
            return;
        }

        watchWrite.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);

        s = watchGatt.writeCharacteristic(watchWrite);

        if (!s) {
            Log.e(TAG, "BLE Write failed - timer task will retry");
            return;
        }
    }

    public void updateDisplay(byte [] msg)
    {
        // new data available

        //Log.i(TAG,"Message Received: "+msg.length+" bytes long");

        sensor_data s = new sensor_data(msg);

        //Log.i(TAG,"Sensor data: light_cds = "+s.light_cds_light);
        //Log.i(TAG,"ObjTemp = "+s.temp_mlx90614_temp_object_f);

        plot.pushData(new int[] {
                s.accel_gyro_mpu6050_accel_x,
                s.accel_gyro_mpu6050_accel_y,
                s.accel_gyro_mpu6050_accel_z,
                s.mag_hmc5883_mag_x,
                s.mag_hmc5883_mag_y,
                s.mag_hmc5883_mag_z,
                s.light_cds_light,
                s.gsr_div_gsr,
                s.temp_mlx90614_temp_ambient_f,
                s.temp_mlx90614_temp_object_f
        });

        plot.setText(new String[] {"AccX", "AccY", "AccZ", "MagX", "MagY", "MagZ", "Light", "GSR", "TempA", "TempO", "TempR"});

//        int permission = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
//        if (permission != PackageManager.PERMISSION_GRANTED) {
//
//            ActivityCompat.requestPermissions(
//                    this,
//                    PERMISSIONS_STORAGE,
//                    REQUEST_EXTERNAL_STORAGE
//            );
//        }
//
//        String fileName = "wenting.txt";
//
//        myoutput = new File(Environment.getExternalStorageDirectory().getAbsoluteFile(), fileName);
//        if (!myoutput.exists()) {
//            try {
//                myoutput.createNewFile();
//                System.out.println("Create file successfully");
//            } catch (Exception e) {
//                 System.out.println("Cannot create file: " + e);
//            }
//        }
        try {
            Date date = Calendar.getInstance().getTime();
            DateFormat formatter = new SimpleDateFormat("dd MMMM yyyy, hh:mm:ss.SSS a");
            String today = formatter.format(date);
            long time= System.currentTimeMillis();

            FileOutputStream output = new FileOutputStream(myoutput, true);

            StringBuilder sb = new StringBuilder();
            sb.append(today + ",");
            sb.append(time + ",");
//            sb.append(s.timestamp + ",");
            sb.append(s.gsr_div_gsr + ",");
            sb.append(s.accel_gyro_mpu6050_accel_x + ",");
            sb.append(s.accel_gyro_mpu6050_accel_y + ",");
            sb.append(s.accel_gyro_mpu6050_accel_z + ",");
            sb.append(s.mag_hmc5883_mag_x + ",");
            sb.append(s.mag_hmc5883_mag_y + ",");
            sb.append(s.mag_hmc5883_mag_z + "\n");

//            sb.append("TempA : " +s.temp_mlx90614_temp_ambient_f);
//            sb.append(" ");
//            sb.append("TempO : " + s.temp_mlx90614_temp_object_f);
            output.write(sb.toString().getBytes());
            output.close();

        }catch (Exception e){
            System.out.println("File does not exist");
        }
        MainActivity.this.runOnUiThread(new Runnable() { public void run() { plot.invalidate();} });

    }

    public void updateServer(byte [] msg) {
        String url = "http://empathicsystems.org/nuwatch/nuwatch.cgi";
        String charset = "UTF-8";
        String t = "req=record-data&data="+Base64.encodeToString(msg,Base64.DEFAULT);
        String req = t.trim();

        try {
            String line;
            URLConnection c = new URL(url).openConnection();

            c.setDoOutput(true);
            c.setRequestProperty("Accept-Charset", charset);
            c.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset="+charset);

            OutputStream os = c.getOutputStream();
            os.write(req.getBytes(charset));
            InputStream is = c.getInputStream();
            InputStreamReader ir = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(ir);
            /*
            while ((line = br.readLine())!=null) {
                Log.i(TAG,"Response line: "+line);
            }
            */
            // ignore for now
        } catch (Exception e) {
            Log.e(TAG, "Failed to send request - exception: " + e);
        }


    }

    public void connectWatch()
    {
        //Log.i(TAG, "Connecting to Watch");
        status.setText("Connecting to NUWatch");
        watch.connectGatt(this,false,gattCallback);
    }


    public void bindWatch(BluetoothDevice dev, final byte[] scanRec)
    {
        boundWatchAddr = dev.getAddress();
        watch = dev;
        status.setText("Bound to " + dev.getAddress());
        adapter.stopLeScan(this);
        connectWatch();
    }


    @Override
    public void onLeScan(final BluetoothDevice device, final int rssi, final byte[] scanRecord) {

        if (inDialog) {
            return;
        }

        //Log.i(TAG, "Found:" + device.getName());

        if (device.getAddress().equals(boundWatchAddr) && device.getName().equals("NU Watch 4")) {
            //Log.i(TAG, "Restoring bound watch");
            watch = device;
            adapter.stopLeScan(this);
            connectWatch();
            return;
        }

        if (device.getName().equals("NU Watch 4")) {
            //Log.i(TAG, "Asking user about " + device.getName());
            inDialog=true;

            AlertDialog.Builder q = new AlertDialog.Builder(this);
            q.setMessage("Bind to NU Watch 4 with address " + device.getAddress() + "?");
            q.setTitle("NU Watch");
            q.setPositiveButton("YES", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface d, int which) { bindWatch(device,scanRecord); }
            });
            q.setNegativeButton("NO",new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface d, int w) { }
            });
            q.create();
            q.show();
        } else {
            //Log.i(TAG,"Skipping\n");
        }
    }

    public void searchForWatch()
    {
        if (status!=null) { status.setText("Searching for NUWatches...");}

        adapter.startLeScan(new UUID[] { UUID_SERVICE }, this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1001: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED
                        && (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED)
                        && ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    //Log.i(TAG, "Have Permissions!");

                    searchForWatch();

                } else {
                    Log.e(TAG, "NO PERMISSION GRANTED");
                }
            }
        }
    }

    public static String hexify(byte[] data)
    {
        String nyb[] = { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "e", "f" };

        StringBuilder hex = new StringBuilder();
        hex.append("");

        for (int i = 0; i < data.length; i++) {
            hex.append(nyb[(data[i]>>4)&0xf]);
            hex.append(nyb[(data[i]>>0)&0xf]);
        }
        return hex.toString();
    }
}
