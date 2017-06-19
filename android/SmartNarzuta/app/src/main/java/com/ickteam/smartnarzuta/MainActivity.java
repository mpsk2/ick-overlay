package com.ickteam.smartnarzuta;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.os.ParcelUuid;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.ColorUtils;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    private String status = "disconnected";
    TextView connectionStatusConnected;
    TextView connectionStatusDisconnected;
    TextView connectionStatusConnecting;

    private void hideAllIcons() {
        connectionStatusConnected.setVisibility(View.INVISIBLE);
        connectionStatusDisconnected.setVisibility(View.INVISIBLE);
        connectionStatusConnecting.setVisibility(View.INVISIBLE);
    }

    BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    private int REQUEST_ENABLE_BT = 123;

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i("MLEKO", "wróciło " + String.valueOf(requestCode) + " " +
                String.valueOf(resultCode));
        if (resultCode == RESULT_OK) {
            connectToNarzuta();
        }
    }

    private int connectToNarzuta() {
        if (mBluetoothAdapter == null) {
            // Device does not support Bluetooth
            return 1;
        }
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        return mBluetoothAdapter.startDiscovery() ? 0 : 2;
    }

    private void workWithConnectedSocket(BluetoothSocket socket) throws IOException {
        InputStream stream = socket.getInputStream();
        final BufferedReader br = new BufferedReader(new InputStreamReader(stream));
        (new Thread(
                new Runnable() {
                    @Override
                    public void run() {
                        while (true) {
                            try {
                                Log.i("JOGURT", br.readLine());
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
        )).start();
    }

    // Create a BroadcastReceiver for ACTION_FOUND.
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
                Log.i("SER", "discovered " + deviceName + " " +
                        deviceHardwareAddress);
                try {
                    if (deviceHardwareAddress.equalsIgnoreCase("20:17:01:10:24:80")) {
                        BluetoothSocket socket = device.createRfcommSocketToServiceRecord(
                                UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"));
                        socket.connect();
                        workWithConnectedSocket(socket);
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    private int getColorFromProgress(float normalProgress){
        int R = Math.round(255 * clipColor(2 * normalProgress));
        int G = Math.round(255 * clipColor((1 - normalProgress) * 2));
        int B = 0;

        R = (R << 16) & 0x00FF0000;
        G = (G << 8) & 0x0000FF00;
        B = B & 0x000000FF;


        return 0xFF000000 | R | G | B;
    }

    private float clipColor(float color) {
        return (color < 0 ? 0 : (color > 1 ? 1 : color));
    }

    private class ButtState {
        int lf;
        int rf;
        int lb;
        int rb;
        public ButtState(int a, int b, int c, int d) {
            this.lf = a;
            this.rf = b;
            this.lb = c;
            this.rb = d;
        }
    }

    private ButtState buttState;

    final Pattern parsePattern = Pattern.compile("p=(\\d+),s=(\\d+),c=(\\d+),min=(\\d+),max=(\\d+),t=(\\d+)");

    void considerNextInput(String str) {
        Matcher m = parsePattern.matcher(str);
        String p = m.group(1);
        String s = m.group(2);
        String c = m.group(3);
        float progress = ((float)Integer.valueOf(s)/(float)Integer.valueOf(c))/1000.f;
        switch (Integer.valueOf(p)) {
            case 0:
                buttState.lf = getColorFromProgress(progress);
                break;
            case 1:
                buttState.rf = getColorFromProgress(progress);
                break;
            case 2:
                buttState.lb = getColorFromProgress(progress);
                break;
            case 3:
                buttState.rb = getColorFromProgress(progress);
                break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Button button = (Button) findViewById(R.id.connect_button);

        connectionStatusConnected = (FontAwesome) findViewById(R.id.connection_status_connected);
        connectionStatusConnecting = (FontAwesome) findViewById(R.id.connection_status_connecting);
        connectionStatusDisconnected = (FontAwesome) findViewById(R.id.connection_status_disconnected);


        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter);

        final ImageView leftFront = (ImageView) findViewById(R.id.left_front_gradient);
        final ImageView rightFront = (ImageView) findViewById(R.id.right_front_gradient);
        final ImageView leftBack = (ImageView) findViewById(R.id.left_back_gradient);
        final ImageView rightBack = (ImageView) findViewById(R.id.right_back_gradient);

        for (ImageView iv : new ImageView[]{leftFront, rightFront, leftBack, rightBack}) {
            DrawableCompat.setTint(iv.getDrawable(), Color.parseColor("#ffff0000"));
        }

        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.i("MLEKO", "you just pressed connect button");
                hideAllIcons();
                if (status.equalsIgnoreCase("disconnected")) {
                    status = "connecting";
                    connectionStatusConnecting.setVisibility(View.VISIBLE);
                    /*int ret = connectToNarzuta();
                    if (ret != 0) {
                        Log.i("MLEKO", "coś nie działa " + String.valueOf(ret));
                    }*/
                    /*Drawable mDrawable = ContextCompat.getDrawable(getApplicationContext(), R.drawable.green);
                    mDrawable.setColorFilter(new
                            PorterDuffColorFilter(0xffff00, PorterDuff.Mode.SRC_IN));*/
                    Log.i("MLEKO", "bardziej działać nie będzie");
                } else if (status.equalsIgnoreCase("connected")) {
                    status = "disconnected";
                    connectionStatusDisconnected.setVisibility(View.VISIBLE);
                } else if (status.equalsIgnoreCase("connecting")) {
                    status = "connected";
                    connectionStatusConnected.setVisibility(View.VISIBLE);
                }
            }
        });
    }
}
