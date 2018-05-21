package org.bonevet.ajri_communication;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.RequiresApi;
import android.util.Log;

import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import de.nitri.gauge.Gauge;

@SuppressWarnings("ALL")
public class MainActivity extends Activity {
    public final String ACTION_USB_PERMISSION = "org.bonevet.ajri_communication.USB_PERMISSION";
    Button startButton, stopButton, btRelays;
    TextView vl_intensity, vl_voltage, vl_battery, vl_temp, vl_range;
    Integer status = 1;
    Integer i = 0;
    List<String> vlerat = new ArrayList<>();
    String[] te_dhenat;
    String data = null, stringi;
    UsbManager usbManager;
    UsbDevice device;
    UsbSerialDevice serialPort;
    UsbDeviceConnection connection;
    int speed_value, rpm_value;
    Gauge gauge, gauge2;
    private int currentApiVersion;

    UsbSerialInterface.UsbReadCallback mCallback = new UsbSerialInterface.UsbReadCallback() { //Defining a Callback which triggers whenever data is read.
        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void onReceivedData(byte[] arg0) {
            try {
                data = new String(arg0, "UTF-8");
                vlerat.add(data);
                StringBuilder sb = new StringBuilder();
                for (String s : vlerat) {
                    sb.append(s);
                }
                stringi = sb.toString();
                if (data.contains("!")) {
                    te_dhenat = stringi.split("-");
                    try {
                        Thread.sleep(0);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                vl_intensity.setText(te_dhenat[0] + " A");
                                vl_voltage.setText(te_dhenat[1] + " V");
                                vl_battery.setText(te_dhenat[2] + " %");
                                vl_temp.setText(te_dhenat[3] + " °C");
                                vl_range.setText(te_dhenat[4] + " km");
                                speed_value= Integer.parseInt(te_dhenat[5]);
                                rpm_value= Integer.parseInt(te_dhenat[6]);
                                gauge.moveToValue(speed_value);
                                gauge2.moveToValue(rpm_value);
                            }
                        });
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    vlerat.clear();
                }

            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
    };

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() { //Broadcast Receiver to automatically start and stop the Serial connection.
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction()==(ACTION_USB_PERMISSION)) {
                boolean granted = intent.getExtras().getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED);
                if (granted) {
                    connection = usbManager.openDevice(device);
                    serialPort = UsbSerialDevice.createUsbSerialDevice(device, connection);
                    if (serialPort != null) {
                        if (serialPort.open()) { //Set Serial Connection Parameters.
                            setUiEnabled(true);
                            serialPort.setBaudRate(9600);
                            serialPort.setDataBits(UsbSerialInterface.DATA_BITS_8);
                            serialPort.setStopBits(UsbSerialInterface.STOP_BITS_1);
                            serialPort.setParity(UsbSerialInterface.PARITY_NONE);
                            serialPort.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
                            btRelays.setClickable(false);
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    btRelays.setClickable(true);
                                }
                            }, 1700);
                            Toast.makeText(context, "Device Open", Toast.LENGTH_SHORT).show();
                        } else {
                            Log.d("SERIAL", "PORT NOT OPEN");
                        }
                    } else {
                        Log.d("SERIAL", "PORT IS NULL");
                    }
                } else {
                    Log.d("SERIAL", "PERM NOT GRANTED");
                }
            } else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
                HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();
                if (!usbDevices.isEmpty()) {
                    boolean keep = true;
                    for (Map.Entry<String, UsbDevice> entry : usbDevices.entrySet()) {
                        device = entry.getValue();
                        int deviceVID = device.getVendorId();
                        if (deviceVID == 0x2341 || deviceVID == 0x2A03)
                        {
                            PendingIntent pi = PendingIntent.getBroadcast(context, 0, new Intent(ACTION_USB_PERMISSION), 0);
                            usbManager.requestPermission(device, pi);
                            keep = false;
                        } else {
                            connection = null;
                            device = null;
                        }
                        if (!keep)
                            break;
                    }
                }
            } else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_DETACHED)) {
                if (serialPort == null) {

                } else {
                    onClickStop(stopButton);
                    Toast.makeText(context, "Device Closed", Toast.LENGTH_SHORT).show();
                    btRelays.setText("Ndalur");
                    btRelays.setBackgroundResource(R.drawable.round1);
                    serialPort.write("0".getBytes());
                    status = 1;
                }
            }
        }

        ;
    };

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        usbManager = (UsbManager) getSystemService(USB_SERVICE);
        startButton = findViewById(R.id.buttonOpen);
        btRelays = findViewById(R.id.btRelays);
        stopButton = findViewById(R.id.buttonClose);
        vl_intensity = findViewById(R.id.vl_intensity);
        vl_voltage = findViewById(R.id.vl_voltage);
        vl_battery = findViewById(R.id.vl_battery);
        vl_temp = findViewById(R.id.vl_temp);
        vl_range = findViewById(R.id.vl_range);

        setUiEnabled(false);

        currentApiVersion = android.os.Build.VERSION.SDK_INT;

        final int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        if(currentApiVersion >= Build.VERSION_CODES.KITKAT)
        {
            getWindow().getDecorView().setSystemUiVisibility(flags);
            final View decorView = getWindow().getDecorView();
            decorView
                    .setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener()
                    {
                        @Override
                        public void onSystemUiVisibilityChange(int visibility)
                        {
                            if((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0)
                            {
                                decorView.setSystemUiVisibility(flags);
                            }
                        }
                    });
        }

        gauge = findViewById(R.id.gauge);
        gauge2 = findViewById(R.id.gauge2);


        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(broadcastReceiver, filter);

        btRelays.setBackgroundResource(R.drawable.round1);

    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus)
    {
        super.onWindowFocusChanged(hasFocus);
        if(currentApiVersion >= Build.VERSION_CODES.KITKAT && hasFocus)
        {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    public void onClickSwitch(View v) {
        if (status == 1) {
            btRelays.setText("Kontakt");
            btRelays.setBackgroundResource(R.drawable.round2);
            serialPort.write("1".getBytes());
            status = 2;
            btRelays.setClickable(false);
            serialPort.read(mCallback);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    btRelays.setClickable(true);
                }
            }, 1700);
        } else if (status == 2) {
            btRelays.setText("Ndezur");
            btRelays.setBackgroundResource(R.drawable.round3);
            serialPort.write("2".getBytes());
            status = 3;
            btRelays.setClickable(false);
            serialPort.read(mCallback);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    btRelays.setClickable(true);
                }
            }, 1700);
        } else {
            btRelays.setText("Ndalur");
            btRelays.setBackgroundResource(R.drawable.round1);
            serialPort.write("0".getBytes());
            status = 1;
            btRelays.setClickable(false);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    btRelays.setClickable(true);
                }
            }, 1700);
        }
    }

    public void setUiEnabled(boolean bool) {
        startButton.setEnabled(!bool);
        stopButton.setEnabled(bool);
        btRelays.setEnabled(bool);
    }

    public void onClickStart(View view) {

        HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();
        if (!usbDevices.isEmpty()) {
            boolean keep = true;
            for (Map.Entry<String, UsbDevice> entry : usbDevices.entrySet()) {
                device = entry.getValue();
                int deviceVID = device.getVendorId();
                if (deviceVID == 0x2341 || deviceVID == 0x2A03)
                {
                    PendingIntent pi = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
                    usbManager.requestPermission(device, pi);
                    keep = false;
                } else {
                    connection = null;
                    device = null;
                }
                if (!keep)
                    break;
            }
        }
    }

    public void onClickStop(View view) {
        setUiEnabled(false);
        serialPort.close();
        btRelays.setText("Ndalur");
        btRelays.setBackgroundResource(R.drawable.round1);
        serialPort.write("0".getBytes());
        status = 1;
    }

}