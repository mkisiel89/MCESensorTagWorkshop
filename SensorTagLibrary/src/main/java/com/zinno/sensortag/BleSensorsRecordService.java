package com.zinno.sensortag;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.zinno.sensortag.ble.BleDevicesScanner;
import com.zinno.sensortag.ble.BleUtils;
import com.zinno.sensortag.config.AppConfig;
import com.zinno.sensortag.sensor.TiAccelerometerSensor;
import com.zinno.sensortag.sensor.TiSensor;
import com.zinno.sensortag.sensor.TiSensors;

public class BleSensorsRecordService extends BleService {
    private static final String TAG = BleSensorsRecordService.class.getSimpleName();

    private static final String RECORD_DEVICE_NAME = "SensorTag";

    private final TiSensor<?> sensorToRead = TiSensors.getSensor(TiAccelerometerSensor.UUID_SERVICE);
    private BleDevicesScanner scanner;
    private String lastDiscoveredBleAddress;

    @Override
    public void onCreate() {
        super.onCreate();

        if (!AppConfig.ENABLE_RECORD_SERVICE) {
            stopSelf();
            return;
        }

        final int bleStatus = BleUtils.getBleStatus(getBaseContext());
        switch (bleStatus) {
            case BleUtils.STATUS_BLE_NOT_AVAILABLE:
                Toast.makeText(getApplicationContext(), R.string.dialog_error_no_ble, Toast.LENGTH_SHORT).show();
                stopSelf();
                return;
            case BleUtils.STATUS_BLUETOOTH_NOT_AVAILABLE:
                Toast.makeText(getApplicationContext(), R.string.dialog_error_no_bluetooth, Toast.LENGTH_SHORT).show();
                stopSelf();
                return;
            default:
                break;
        }

        if (!getBleManager().initialize(getBaseContext())) {
            stopSelf();
            return;
        }

        // initialize scanner
        final BluetoothAdapter bluetoothAdapter = BleUtils.getBluetoothAdapter(getBaseContext());
        scanner = new BleDevicesScanner(bluetoothAdapter, new BluetoothAdapter.LeScanCallback() {
            @Override
            public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
                Log.d(TAG, "Device discovered: " + device.getName());
                if (RECORD_DEVICE_NAME.equals(RECORD_DEVICE_NAME)) {
                    scanner.stop();
                    lastDiscoveredBleAddress = device.getAddress();
                    getBleManager().connect(getBaseContext(), lastDiscoveredBleAddress);
                }
            }
        });

        setServiceListener(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (scanner == null) {
            return super.onStartCommand(intent, flags, startId);
        }
        Log.d(TAG, "Service started");
        scanner.start();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Service stopped");
        setServiceListener(null);
        if (scanner != null) {
            scanner.stop();
        }
    }

    @Override
    public void onConnected(String deviceAddress) {
        Log.d(TAG, "Connected");
    }

    @Override
    public void onDisconnected(String deviceAddress) {
        Log.d(TAG, "Disconnected");
        scanner.start();
    }

    @Override
    public void onServiceDiscovered(String deviceAddress) {
        Log.d(TAG, "Service discovered");
        //TODO cant be field in service
        enableSensor(lastDiscoveredBleAddress, sensorToRead, true);
    }

    @Override
    public void onDataAvailable(String deviceAddress, String serviceUuid, String characteristicUUid, String text, Object data) {
        Log.d(TAG, "Data='" + text + "'");
        //TODO: put your record code here. Please note that it is not main thread.
    }
}
