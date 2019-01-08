package org.bonevet.ajri_communication;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.media.VolumeShaper;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import de.nitri.gauge.Gauge;

@SuppressWarnings("ALL")
public class MainActivity extends AppCompatActivity {
    public final String ACTION_USB_PERMISSION = "org.bonevet.ajri_communication.USB_PERMISSION";
    Button startButton, stopButton, btLang;
    TextView vl_intensity, vl_voltage, vl_battery, vl_temp, vl_range, vl_speed, vl_rpm;
    Integer status = 1;
    Integer i = 0;
    List<String> vlerat = new ArrayList<>();
    String[] te_dhenat, stringi_ndare;
    String data = null, stringi;
    UsbManager usbManager;
    UsbDevice device;
    UsbSerialDevice serialPort;
    UsbDeviceConnection connection;
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
                                vl_intensity.setText(te_dhenat[0] + " A");
                                vl_voltage.setText(te_dhenat[1] + " V");
                                vl_battery.setText(te_dhenat[2] + " %");
                                vl_temp.setText(te_dhenat[3] + " °C");
                                vl_range.setText(te_dhenat[4] + " km");
                                vl_speed.setText(te_dhenat[5] + " km/h");
                                vl_rpm.setText(te_dhenat[6] + " rpm");

//                                try {
//                                    speed_value = Integer.parseInt(te_dhenat[5]);
//                                    rpm_value = Integer.parseInt(te_dhenat[6]);
//                                }catch (UnknownError e){
//
//                                }
//                                gauge.moveToValue(speed_value);
//                                gauge2.moveToValue(rpm_value);
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
                        if (!keep)
                            break;
                    }
                }
            } else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_DETACHED)) {
                if (serialPort == null) {

                } else {
                    Toast.makeText(context, "Device Closed", Toast.LENGTH_SHORT).show();
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
        seek_bar = (SeekBar)findViewById(R.id.seekBar);
        btLang = findViewById(R.id.lang);
        stopButton = findViewById(R.id.buttonClose);
        vl_intensity = findViewById(R.id.vl_intensity);
        vl_voltage = findViewById(R.id.vl_voltage);
        vl_battery = findViewById(R.id.vl_battery);
        vl_temp = findViewById(R.id.vl_temp);
        vl_range = findViewById(R.id.vl_range);
        vl_speed = findViewById(R.id.vl_speed);
        vl_rpm = findViewById(R.id.vl_rpm);
        batteryPhoto = findViewById(R.id.foto_battery);
        batteryPhoto.setRotation(90);
        Typeface typeface = ResourcesCompat.getFont(this, R.font.digital);
        vl_speed.setTypeface(typeface);
        vl_rpm.setTypeface(typeface);
        seebbarr();
        seek_bar.setEnabled(false);



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

        btLang.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                showLanguageDialog();
            }
        });

    }

//    private void showLanguageDialog() {
//        final String[] listaGjuheve = {"Shqip","English"};
//        AlertDialog.Builder mBuilder = new AlertDialog.Builder(MainActivity.this);
//        mBuilder.setTitle("Choose Language...");
//        mBuilder.setSingleChoiceItems(listaGjuheve, -1, new DialogInterface.OnClickListener() {
//            @Override
//            public void onClick(DialogInterface dialog, int i) {
//                if (i==0){
//                    setLocale("sq");
//                    recreate();
//                }
//                else if (i==1){
//                    setLocale("en");
//                    recreate();
//                }
//                dialog.dismiss();
//            }
//        });
//        AlertDialog mDialog = mBuilder.create();
//        mDialog.show();
//    }

//    private void setLocale(String lang) {
//        Locale locale = new Locale(lang);
//        Locale.setDefault(Locale.forLanguageTag(lang));
//        Configuration config = new Configuration();
//        config.locale = locale;
//        getBaseContext().getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());
//        SharedPreferences.Editor editor = getSharedPreferences("Settings", MODE_PRIVATE).edit();
//        editor.putString("My_Lang", lang);
//        editor.apply();
//    }

//    public void loadLocale(){
//        SharedPreferences prefs = getSharedPreferences("Settings", Activity.MODE_PRIVATE);
//        String language = prefs.getString("My_Lang","");
//        setLocale(language);
//    }

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

    private void rotate(float degree) {
        final RotateAnimation rotateAnim = new RotateAnimation(0.0f, degree,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f,
                RotateAnimation.RELATIVE_TO_SELF, 0.5f);

        rotateAnim.setDuration(0);
        rotateAnim.setFillAfter(true);
        batteryPhoto.startAnimation(rotateAnim);
    }
    public void setFontTextView(Context c, TextView name) {
        Typeface font = Typeface.createFromAsset(c.getAssets(),"font/ralewayregular.ttf");
        name.setTypeface(font);
    }

    public void seebbarr( ){

        text_view =(TextView)findViewById(R.id.textView);
        text_view.setText("Covered : " + seek_bar.getProgress() + " / " +seek_bar.getMax());

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
                    public void onStopTrackingTouch(SeekBar seekBar) {
                        serialPort.write(Integer.toString(progress_value).getBytes());
                        text_view.setText("Covered : " + progress_value + " / " +seek_bar.getMax());
                    }
                }
        );

    }

    public void setUiEnabled(boolean bool) {
        startButton.setEnabled(!bool);
        btLang.setEnabled(!bool);
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
