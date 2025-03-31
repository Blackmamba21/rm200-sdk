import Rm200Sdk from './NativeRm200Sdk';

export function multiply(a: number, b: number): number {
  return Rm200Sdk.multiply(a, b);
}

// ✅ Connect to RM200 Device
export function connectToDevice(): Promise<string> {
  return Rm200Sdk.connectToDevice();
}

// ✅ Send Hex Data to RM200 Device
export function sendHexData(hexString: string): Promise<string> {
  return Rm200Sdk.sendHexData(hexString);
}
