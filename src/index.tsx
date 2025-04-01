import Rm200Sdk from './NativeRm200Sdk';

// ✅ Connect to RM200 Device
export function connectToDevice(): Promise<string> {
  return Rm200Sdk.connectToDevice();
}

// ✅ Send Hex Data to RM200 Device
export function sendHexData(hexString: string): Promise<string> {
  return Rm200Sdk.sendHexData(hexString);
}
