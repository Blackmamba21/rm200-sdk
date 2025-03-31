package com.rm200sdk

import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.util.Log
import com.facebook.react.bridge.*

@ReactModule(name = Rm200SdkModule.NAME)
class Rm200SdkModule(reactContext: ReactApplicationContext) :
  NativeRm200SdkSpec(reactContext) {

  private val TAG = "Rm200Sdk"
  private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
  private var bluetoothGatt: BluetoothGatt? = null
  private val DEVICE_NAME = "RM200"

  override fun getName(): String {
    return NAME
  }

  // ✅ Connect to RM200 Device
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
                promise.resolve("Connected to RM200")
              } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                promise.reject("DISCONNECTED", "Disconnected from RM200")
              }
            }
          })
        }
      }
    })
  }

  // ✅ Send Hex Data to RM200
  @ReactMethod
  fun sendHexData(hexString: String, promise: Promise) {
    bluetoothGatt?.let { gatt ->
      val service = gatt.getService(YOUR_SERVICE_UUID) 
      val characteristic = service?.getCharacteristic(YOUR_CHARACTERISTIC_UUID)

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

  companion object {
    const val NAME = "Rm200Sdk"
  }
}
