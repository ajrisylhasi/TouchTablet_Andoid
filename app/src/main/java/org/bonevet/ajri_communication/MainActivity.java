package org.bonevet.ajri_communication;


import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Typeface;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.nitri.gauge.Gauge;

@SuppressWarnings("ALL")
public class MainActivity extends AppCompatActivity {
    public final String ACTION_USB_PERMISSION = "org.bonevet.ajri_communication.USB_PERMISSION";
    Button startButton, stopButton, btLang;
    TextView vl_intensity, vl_voltage, vl_battery, vl_temp, vl_range, vl_speed, vl_rpm;
    ProgressBar progressBar;
    ImageView image;
    Integer status = 1, bat=0;
    Handler handler;
    Integer i = 0;
    List<String> vlerat = new ArrayList<>();
    String[] te_dhenat, stringi_ndare;
    String data = null, stringi;
    UsbManager usbManager;
    UsbDevice device;
    UsbSerialDevice serialPort;
    UsbDeviceConnection connection;
    Runnable runnable;
    int speed_value, rpm_value;
    Gauge gauge, gauge2;
    private int currentApiVersion;
    ImageView batteryPhoto;
    private static SeekBar seek_bar;
    private static TextView text_view;

    UsbSerialInterface.UsbReadCallback mCallback = new UsbSerialInterface.UsbReadCallback() { //Defining a Callback which triggers whenever data is read.
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
                    te_dhenat = stringi.split("=");
                    try {
                        Thread.sleep(0);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                vl_battery.setText(te_dhenat[1] + " %");
                                vl_temp.setText(te_dhenat[2] + " °C");
                                vl_speed.setText(te_dhenat[3] + " km/h");
                                bat = Integer.parseInt(te_dhenat[1]);

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
                            serialPort.read(mCallback);
                            seek_bar.setEnabled(true);
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
                        if (!keep) {
                            break;
                        }
                    }
                }
            } else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_DETACHED)) {
                if (serialPort == null) {

                } else {
                    Toast.makeText(context, "Device Closed", Toast.LENGTH_SHORT).show()  ;
                    serialPort.write("0".getBytes());
                    seek_bar.setEnabled(false);
                    seek_bar.setProgress(0);
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
        image = (ImageView) findViewById(R.id.image);
        seek_bar = (SeekBar)findViewById(R.id.seekBar);
        stopButton = findViewById(R.id.buttonClose);
        vl_battery = findViewById(R.id.vl_battery);
        vl_temp = findViewById(R.id.vl_temp);
        vl_speed = findViewById(R.id.vl_speed);
        Typeface typeface = ResourcesCompat.getFont(this, R.font.digital);
        vl_speed.setTypeface(typeface);
        seebbarr();
        seek_bar.setEnabled(false);

        runnable = new Runnable() {
            @Override
            public void run() {
                if (bat < 25) {
                    image.setImageResource(R.drawable.battery_10);
                }
                if (bat < 50 && bat >= 25) {
                    image.setImageResource(R.drawable.battery_45);
                }
                if (bat < 75 && bat >= 50) {
                    image.setImageResource(R.drawable.battery_75);
                }
                if (bat >= 75) {
                    image.setImageResource(R.drawable.battery_100);
                }
                handler.postDelayed(runnable, 500);
            }

            ;
        };
        handler = new Handler();
        handler.postDelayed(runnable,0);

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


        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(broadcastReceiver, filter);


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

    public void setFontTextView(Context c, TextView name) {
        Typeface font = Typeface.createFromAsset(c.getAssets(),"font/ralewayregular.ttf");
        name.setTypeface(font);
    }

    public void seebbarr( ){

        seek_bar.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {

                    int progress_value;
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        progress_value = progress;
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(final SeekBar seekBar) {
                        serialPort.write(Integer.toString(progress_value).getBytes());
                        seekBar.setEnabled(false);
                        Handler h = new Handler();
                        h.postDelayed(new Runnable() {

                            @Override
                            public void run() {
                                seekBar.setEnabled(true);
                            }

                        }, 1500);


                    }
                }
        );

    }

    public void setUiEnabled(boolean bool) {
        startButton.setEnabled(!bool);



        stopButton.setEnabled(bool);
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
        serialPort.write("0".getBytes());
        serialPort.close();
    }

}
