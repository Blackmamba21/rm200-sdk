package com.rm200sdk

import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.module.annotations.ReactModule

@ReactModule(name = Rm200SdkModule.NAME)
class Rm200SdkModule(reactContext: ReactApplicationContext) :
  NativeRm200SdkSpec(reactContext) {

  override fun getName(): String {
    return NAME
  }

  // Example method
  // See https://reactnative.dev/docs/native-modules-android
  override fun multiply(a: Double, b: Double): Double {
    return a * b
  }

  companion object {
    const val NAME = "Rm200Sdk"
  }
}
