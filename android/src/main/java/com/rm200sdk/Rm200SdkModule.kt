package com.rm200sdk

import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.util.Log
import com.facebook.react.bridge.*
import java.util.*

@ReactModule(name = Rm200SdkModule.NAME)
class Rm200SdkModule(reactContext: ReactApplicationContext) :
    NativeRm200SdkSpec(reactContext) {

    private val TAG = "Rm200Sdk"
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var bluetoothGatt: BluetoothGatt? = null
    private val DEVICE_NAME = "TGI-PTID"

    override fun getName(): String {
        return NAME
    }

    @ReactMethod
    fun connectToDevice(promise: Promise) {
        if (bluetoothAdapter == null) {
            promise.reject("BLUETOOTH_NOT_AVAILABLE", "Bluetooth is not supported on this device")
            return
        }

        val bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner
        bluetoothLeScanner.startScan(object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                super.onScanResult(callbackType, result)
                val device = result?.device
                if (device != null && device.name == DEVICE_NAME) {
                    bluetoothLeScanner.stopScan(this)
                    device.connectGatt(reactApplicationContext, false, object : BluetoothGattCallback() {
                        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
                            if (newState == BluetoothProfile.STATE_CONNECTED) {
                                bluetoothGatt = gatt
                                gatt?.discoverServices()
                                promise.resolve("Connected to $DEVICE_NAME")
                            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                                promise.reject("DISCONNECTED", "Disconnected from $DEVICE_NAME")
                            }
                        }

                        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
                            if (status == BluetoothGatt.GATT_SUCCESS) {
                                gatt?.services?.forEach { service ->
                                    Log.d(TAG, "Service discovered: ${service.uuid}")
                                    service.characteristics.forEach { characteristic ->
                                        Log.d(TAG, "Characteristic: ${characteristic.uuid}")
                                    }
                                }
                            } else {
                                Log.e(TAG, "Service discovery failed")
                            }
                        }
                    })
                }
            }
        })
    }

    @ReactMethod
    fun sendHexData(hexString: String, promise: Promise) {
        bluetoothGatt?.let { gatt ->
            val service = gatt.services.find { it.uuid == UUID.fromString(YOUR_SERVICE_UUID) }
            val characteristic = service?.characteristics?.find { it.uuid == UUID.fromString(YOUR_CHARACTERISTIC_UUID) }

            if (characteristic != null) {
                val data = hexString.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
                characteristic.value = data
                gatt.writeCharacteristic(characteristic)
                promise.resolve("Data sent: $hexString")
            } else {
                promise.reject("WRITE_FAILED", "Failed to send data")
            }
        } ?: promise.reject("NOT_CONNECTED", "No device connected")
    }

    @ReactMethod
    fun subscribeToNotifications(promise: Promise) {
        bluetoothGatt?.let { gatt ->
            val service = gatt.services.find { it.uuid == UUID.fromString(YOUR_SERVICE_UUID) }
            val characteristic = service?.characteristics?.find { it.uuid == UUID.fromString(YOUR_CHARACTERISTIC_UUID) }

            if (characteristic != null && characteristic.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0) {
                gatt.setCharacteristicNotification(characteristic, true)
                promise.resolve("Subscribed to notifications")
            } else {
                promise.reject("NOTIFY_FAILED", "Failed to subscribe")
            }
        } ?: promise.reject("NOT_CONNECTED", "No device connected")
    }

    companion object {
        const val NAME = "Rm200Sdk"
        private const val YOUR_SERVICE_UUID = "0000180F-0000-1000-8000-00805f9b34fb" // Update dynamically
        private const val YOUR_CHARACTERISTIC_UUID = "00002A19-0000-1000-8000-00805f9b34fb" // Update dynamically
    }
}
