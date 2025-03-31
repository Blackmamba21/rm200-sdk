package com.rm200sdk

import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.util.Log
import com.facebook.react.bridge.*
import java.util.*
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod

class Rm200SdkModule(reactContext: ReactApplicationContext) :
    NativeRm200SdkSpec(reactContext) {

    private val TAG = "Rm200Sdk"
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var bluetoothGatt: BluetoothGatt? = null
    private val DEVICE_NAME = "TGI-PTID"
    private var serviceUUID: UUID? = null
    private var characteristicUUID: UUID? = null

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
                                    
                                    serviceUUID = service.uuid // Store first service UUID found
                                    
                                    service.characteristics.forEach { characteristic ->
                                        Log.d(TAG, "Characteristic: ${characteristic.uuid}")
                                        if (characteristicUUID == null) {
                                            characteristicUUID = characteristic.uuid // Store first characteristic UUID found
                                        }
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
        if (serviceUUID == null || characteristicUUID == null) {
            promise.reject("UUID_NOT_FOUND", "Service or Characteristic UUID not found yet")
            return
        }

        bluetoothGatt?.let { gatt ->
            val service = gatt.getService(serviceUUID)
            val characteristic = service?.getCharacteristic(characteristicUUID)

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
        if (serviceUUID == null || characteristicUUID == null) {
            promise.reject("UUID_NOT_FOUND", "Service or Characteristic UUID not found yet")
            return
        }

        bluetoothGatt?.let { gatt ->
            val service = gatt.getService(serviceUUID)
            val characteristic = service?.getCharacteristic(characteristicUUID)

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
    }
}
