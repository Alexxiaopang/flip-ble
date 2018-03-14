package cn.bingerz.flipble.central;

import android.annotation.TargetApi;
import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.text.TextUtils;

import java.util.List;
import java.util.UUID;

import cn.bingerz.flipble.peripheral.MultiplePeripheralController;
import cn.bingerz.flipble.peripheral.Peripheral;
import cn.bingerz.flipble.central.callback.ScanCallback;
import cn.bingerz.flipble.exception.BLEException;
import cn.bingerz.flipble.exception.hanlder.DefaultExceptionHandler;
import cn.bingerz.flipble.utils.EasyLog;

/**
 * Created by hanson on 09/01/2018.
 */

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
public class CentralManager {

    public static final int DEFAULT_SCAN_TIME = 10000;
    private static final int DEFAULT_MAX_MULTIPLE_DEVICE = 7;
    private static final int DEFAULT_OPERATE_TIME = 5000;

    private int operateTimeout = DEFAULT_OPERATE_TIME;
    private int maxConnectCount = DEFAULT_MAX_MULTIPLE_DEVICE;

    private CentralScanner mCentralScanner;
    private ScanRuleConfig mScanRuleConfig;

    private Application mContext;
    private BluetoothAdapter mBluetoothAdapter;
    private MultiplePeripheralController mMultiPeripheralController;

    private DefaultExceptionHandler mBLEExceptionHandler;

    private CentralManager() {}

    public static CentralManager getInstance() {
        return CentralManagerHolder.sCentralManager;
    }

    private static class CentralManagerHolder {
        private static final CentralManager sCentralManager = new CentralManager();
    }

    public void init(Application application) {
        if (application == null) {
            throw new IllegalArgumentException("Init exception, application is null.");
        }
        if (mContext == null) {
            mContext = application;
            EasyLog.setExplicitTag("FlipBLE");
            BluetoothManager bluetoothManager = (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
            if (bluetoothManager != null) {
                mBluetoothAdapter = bluetoothManager.getAdapter();
            }
            mBLEExceptionHandler = new DefaultExceptionHandler();
            mMultiPeripheralController = new MultiplePeripheralController();

            mScanRuleConfig = new ScanRuleConfig();
            mCentralScanner = CentralScanner.getInstance();
        }
    }

    /**
     * Get the BleScanner
     */
    public CentralScanner getScanner() {
        return mCentralScanner;
    }

    public boolean isScanning() {
        if (mCentralScanner == null) {
            throw new IllegalStateException("Central Scanner is null.");
        }
        return mCentralScanner.getScanState() == CentralScanState.STATE_SCANNING;
    }
    /**
     * get the ScanRuleConfig
     */
    public ScanRuleConfig getScanRuleConfig() {
        return mScanRuleConfig;
    }

    /**
     * Configure scan and connection properties
     */
    public void initScanRule(ScanRuleConfig scanRuleConfig) {
        this.mScanRuleConfig = scanRuleConfig;
    }

    /**
     * scan device around
     * Caution:Above Android 6.0,
     * ensure ACCESS_FINE_LOCATION and ACCESS_COARSE_LOCATION permissions have been granted
     */
    public void scan(ScanCallback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("ScanCallback can not be null!");
        }

        UUID[] serviceUUIDs  = mScanRuleConfig.getServiceUUIDs();
        String[] deviceNames = mScanRuleConfig.getDeviceNames();
        String deviceMac     = mScanRuleConfig.getDeviceMac();
        boolean fuzzy        = mScanRuleConfig.isFuzzy();
        long timeOut         = mScanRuleConfig.getScanTimeOut();

        mCentralScanner.scan(serviceUUIDs, deviceNames, deviceMac, fuzzy, timeOut, callback);
    }

    /**
     * Cancel scan
     */
    public void cancelScan() {
        mCentralScanner.stopLeScan();
    }

    public Context getContext() {
        return mContext;
    }

    /**
     * Get the BluetoothAdapter
     */
    public BluetoothAdapter getBluetoothAdapter() {
        return mBluetoothAdapter;
    }


    /**
     * Handle Exception Information
     */
    public void handleException(BLEException exception) {
        mBLEExceptionHandler.handleException(exception);
    }

    /**
     * Get the multiple peripheral Controller
     */
    public MultiplePeripheralController getMultiplePeripheralController() {
        return mMultiPeripheralController;
    }

    /**
     * Get the maximum number of connections
     */
    public int getMaxConnectCount() {
        return maxConnectCount;
    }

    /**
     * Set the maximum number of connections
     */
    public CentralManager setMaxConnectCount(int maxCount) {
        if (maxCount > DEFAULT_MAX_MULTIPLE_DEVICE)
            maxCount = DEFAULT_MAX_MULTIPLE_DEVICE;
        this.maxConnectCount = maxCount;
        return this;
    }

    /**
     * Get operate timeout
     */
    public int getOperateTimeout() {
        return operateTimeout;
    }

    /**
     * Set operate timeout
     */
    public CentralManager setOperateTimeout(int operateTimeout) {
        this.operateTimeout = operateTimeout;
        return this;
    }


    /**
     * print log?
     */
    public CentralManager enableLog(boolean enable) {
        EasyLog.setLoggable(enable);
        return this;
    }


    /**
     * is support ble?
     */
    public boolean isSupportBle() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2
                && mContext.getApplicationContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
    }

    /**
     * Open bluetooth
     */
    public void enableBluetooth() {
        if (mBluetoothAdapter != null) {
            mBluetoothAdapter.enable();
        }
    }

    /**
     * Disable bluetooth
     */
    public void disableBluetooth() {
        if (mBluetoothAdapter != null) {
            if (mBluetoothAdapter.isEnabled())
                mBluetoothAdapter.disable();
        }
    }

    /**
     * judge Bluetooth is enable
     */
    public boolean isBluetoothEnable() {
        return mBluetoothAdapter != null && mBluetoothAdapter.isEnabled();
    }

    private BluetoothDevice retrieveDevice(String address) {
        if (mBluetoothAdapter != null) {
            return mBluetoothAdapter.getRemoteDevice(address);
        }
        return null;
    }

    private BluetoothDevice retrieveDevice(byte[] address) {
        if (mBluetoothAdapter != null) {
            return mBluetoothAdapter.getRemoteDevice(address);
        }
        return null;
    }

    public Peripheral retrievePeripheral(String address) {
        if (!TextUtils.isEmpty(address)) {
            BluetoothDevice device = retrieveDevice(address);
            return new Peripheral(device);
        }
        return null;
    }

    public Peripheral retrievePeripheral(byte[] address) {
        if (!(address == null || address.length != 6)) {
            BluetoothDevice device = retrieveDevice(address);
            return new Peripheral(device);
        }
        return null;
    }

    public List<Peripheral> getAllConnectedDevice() {
        if (mMultiPeripheralController == null)
            return null;
        return mMultiPeripheralController.getPeripheralList();
    }

    public boolean isConnected(Peripheral peripheral) {
        BluetoothManager bluetoothManager = (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothDevice device = retrieveDevice(peripheral.getAddress());
        int state = bluetoothManager.getConnectionState(device, BluetoothProfile.GATT);
        return state == BluetoothProfile.STATE_CONNECTED;
    }

    public boolean isConnected(String key) {
        return mMultiPeripheralController != null && mMultiPeripheralController.isContainDevice(key);
    }

    public Peripheral getPeripheral(String key) {
        if (mMultiPeripheralController != null) {
            return mMultiPeripheralController.getPeripheral(key);
        }
        return null;
    }

    public void disconnectAllDevice() {
        if (mMultiPeripheralController != null) {
            mMultiPeripheralController.disconnectAllDevice();
        }
    }

    public void destroy() {
        if (mMultiPeripheralController != null) {
            mMultiPeripheralController.destroy();
        }
    }
}
