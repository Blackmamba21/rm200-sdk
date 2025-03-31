package com.rm200sdk

import com.facebook.react.BaseReactPackage
import com.facebook.react.bridge.NativeModule
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.module.model.ReactModuleInfo
import com.facebook.react.module.model.ReactModuleInfoProvider

class Rm200SdkPackage : BaseReactPackage() {

  override fun getModule(name: String, reactContext: ReactApplicationContext): NativeModule? {
    return when (name) {
      Rm200SdkModule.NAME -> Rm200SdkModule(reactContext)
      else -> null
    }
  }

  override fun getReactModuleInfoProvider(): ReactModuleInfoProvider {
    return ReactModuleInfoProvider {
      mapOf(
        Rm200SdkModule.NAME to ReactModuleInfo(
          Rm200SdkModule.NAME,
          Rm200SdkModule.NAME,
          canOverrideExistingModule = false,
          needsEagerInit = false,
          isCxxModule = false,
          isTurboModule = true
        )
      )
    }
  }
}
