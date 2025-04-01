package com.rm200sdk

import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.util.Log
import com.facebook.react.bridge.*
import java.util.*

class Rm200SdkModule(reactContext: ReactApplicationContext) :
    NativeRm200SdkSpec(reactContext) {

    private val TAG = "Rm200Sdk"
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var bluetoothGatt: BluetoothGatt? = null
    private val DEVICE_NAME = "ABC"
    private var serviceUUID: UUID? = null
    private var characteristicUUID: UUID? = null

    override fun getName(): String {
        return NAME
    }

    @ReactMethod
    override fun connectToDevice(promise: Promise) {
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
                    Log.d(TAG, "Found device: ${device.name}, connecting...")
                    device.connectGatt(reactApplicationContext, false, object : BluetoothGattCallback() {
                        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
                            if (newState == BluetoothProfile.STATE_CONNECTED) {
                                bluetoothGatt = gatt
                                Log.d(TAG, "Connected to $DEVICE_NAME, discovering services...")
                                gatt?.discoverServices()
                            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                                Log.e(TAG, "Disconnected from device")
                                promise.reject("DISCONNECTED", "Disconnected from $DEVICE_NAME")
                            } else if (status != BluetoothGatt.GATT_SUCCESS) {
                                promise.reject("CONNECTION_FAILED", "Failed to connect to $DEVICE_NAME")
                            }
                        }

                        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
                            if (status == BluetoothGatt.GATT_SUCCESS) {
                                var foundCharacteristic = false
                                gatt?.services?.forEach { service ->
                                    Log.d(TAG, "Service discovered: ${service.uuid}")
                                    service.characteristics.forEach { characteristic ->
                                        Log.d(TAG, "Characteristic found: ${characteristic.uuid}")
                                        if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_WRITE != 0) {
                                            serviceUUID = service.uuid
                                            characteristicUUID = characteristic.uuid
                                            foundCharacteristic = true
                                        }
                                    }
                                }

                                if (foundCharacteristic) {
                                    Log.d(TAG, "Service and characteristic UUIDs stored successfully")
                                    promise.resolve("Connected and ready to send data")
                                } else {
                                    Log.e(TAG, "No writable characteristic found")
                                    promise.reject("CHARACTERISTIC_NOT_FOUND", "No writable characteristic found on device")
                                }
                            } else {
                                Log.e(TAG, "Service discovery failed with status: $status")
                                promise.reject("SERVICE_DISCOVERY_FAILED", "Failed to discover services")
                            }
                        }
                    })
                }
            }

            override fun onScanFailed(errorCode: Int) {
                super.onScanFailed(errorCode)
                promise.reject("SCAN_FAILED", "Bluetooth scan failed with error code $errorCode")
            }
        })
    }

    @ReactMethod
    override fun sendHexData(hexString: String, promise: Promise) {
        if (serviceUUID == null || characteristicUUID == null) {
            promise.reject("UUID_NOT_FOUND", "Service or Characteristic UUID not found yet")
            return
        }

        bluetoothGatt?.let { gatt ->
            val service = gatt.getService(serviceUUID)
            val characteristic = service?.getCharacteristic(characteristicUUID)

            if (characteristic != null) {
                if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_WRITE == 0) {
                    promise.reject("WRITE_NOT_SUPPORTED", "Characteristic does not support write operations")
                    return
                }

                val data = hexString.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
                characteristic.value = data
                if (gatt.writeCharacteristic(characteristic)) {
                    Log.d(TAG, "Data successfully sent: $hexString")
                    promise.resolve("Data sent successfully")
                } else {
                    promise.reject("WRITE_FAILED", "Failed to send data")
                }
            } else {
                Log.e(TAG, "Characteristic not found for sending data")
                promise.reject("CHARACTERISTIC_NOT_FOUND", "Writable characteristic not found")
            }
        } ?: promise.reject("NOT_CONNECTED", "No device connected")
    }

    companion object {
        const val NAME = "Rm200Sdk"
    }
}
