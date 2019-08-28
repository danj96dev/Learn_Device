package com.example.learn_device;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;


import com.mbientlab.metawear.Data;
import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.Route;
import com.mbientlab.metawear.Subscriber;
import com.mbientlab.metawear.android.BtleService;
import com.mbientlab.metawear.builder.RouteBuilder;
import com.mbientlab.metawear.builder.RouteComponent;
import com.mbientlab.metawear.data.Acceleration;
import com.mbientlab.metawear.data.AngularVelocity;
import com.mbientlab.metawear.module.Accelerometer;
import com.mbientlab.metawear.module.GyroBmi160;

import java.util.List;
import java.util.Set;

import bolts.Continuation;
import bolts.Task;

import static android.bluetooth.BluetoothProfile.GATT;


public class MainActivity extends AppCompatActivity implements ServiceConnection {


    private BtleService.LocalBinder serviceBinder;
    private final String MW_MAC_ADDRESS= "E4:01:96:A9:C3:20";
    private MetaWearBoard board;
    private Accelerometer accelerometer;
    private GyroBmi160 gyroBmi160;
    private TextView textView;
    private TextView gyroView;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView = findViewById(R.id.text);
        gyroView = findViewById(R.id.gyro);

        // Bind the service when the activity is created
        getApplicationContext().bindService(new Intent(this, BtleService.class),
                this, Context.BIND_AUTO_CREATE);


    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public void retrieveBoard(){
        // Get the MetaWear device via MAC address
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        final BluetoothDevice remoteDevice = bluetoothManager.getAdapter().getRemoteDevice(MW_MAC_ADDRESS);
        // Create a MetaWear board object for the Bluetooth Device
        board = serviceBinder.getMetaWearBoard(remoteDevice);
    }



    // ---------------------------------------------------------------------------------------------
    // ---------------------------------------------------------------------------------------------

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    public void onServiceConnected(ComponentName componentName, IBinder service) {
        // Typecast the binder to the service's LocalBinder class
        serviceBinder = (BtleService.LocalBinder) service;
        retrieveBoard();

        // Accelerometer
        // ---------------------------------------------------------------------------------------------
        board.connectAsync().onSuccessTask(new Continuation<Void, Task<Route>>() {
            @Override
            public Task<Route> then(Task<Void> task) throws Exception {

                accelerometer = board.getModule(Accelerometer.class);
                accelerometer.configure()
                        .odr(25f)       // Set sampling frequency to 25Hz, or closest valid ODR
                        .commit();
                return accelerometer.acceleration().addRouteAsync(new RouteBuilder() {
                    @Override
                    public void configure(RouteComponent source) {
                        source.stream(new Subscriber() {
                            @Override
                            public void apply(Data data, Object... env) {
                                // do something
                                textView.setText(data.value(Acceleration.class).toString());
                            }
                        });
                    }
                });
            }
        }).continueWith(new Continuation<Route, Void>() {
            @Override
            public Void then(Task<Route> task) throws Exception {
                if(task.isFaulted()){
                    // didn't work
                } else {
                    // worked
                }
                return null;
            }
        });


        // Gyro
        // ---------------------------------------------------------------------------------------------
        board.connectAsync().onSuccessTask(new Continuation<Void, Task<Void>>() {
            @Override
            public Task<Void> then(Task<Void> task) throws Exception {
                gyroBmi160 = board.getModule(GyroBmi160.class);
                gyroBmi160.configure()
                        .odr(GyroBmi160.OutputDataRate.ODR_50_HZ)
                        .range(GyroBmi160.Range.FSR_2000)
                        .commit();

                return gyroBmi160.angularVelocity().addRouteAsync(new RouteBuilder() {
                    @Override
                    public void configure(RouteComponent source) {
                        source.stream(new Subscriber() {
                            @Override
                            public void apply(Data data, Object ... env) {
                                gyroView.setText(data.value(AngularVelocity.class).toString());
                            }
                        });
                    }
                }).continueWith(new Continuation<Route, Void>() {
                    @Override
                    public Void then(Task<Route> task) throws Exception {

                        setButtonListeners();
                        return null;
                    }
                });
            }
        });


    }

    // ---------------------------------------------------------------------------------------------
    // ---------------------------------------------------------------------------------------------

    @Override
    public void onServiceDisconnected(ComponentName componentName) {

    }


    // Add button onClick to start and stop both modules
    public void setButtonListeners(){
        Button start = findViewById(R.id.start);
        Button stop = findViewById(R.id.stop);


        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                accelerometer.acceleration().start();
                accelerometer.start();
                gyroBmi160.angularVelocity().start();
                gyroBmi160.start();
            }
        });

        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                accelerometer.acceleration().stop();
                accelerometer.stop();
                gyroBmi160.angularVelocity().stop();
                gyroBmi160.stop();
            }
        });

    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Unbind the service when the activity is destroyed
        getApplicationContext().unbindService(this);
        board.tearDown();
    }
}
