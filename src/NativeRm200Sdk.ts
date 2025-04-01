import type { TurboModule } from 'react-native';
import { TurboModuleRegistry } from 'react-native';

// ✅ Extend Spec with new methods
export interface Spec extends TurboModule {
  connectToDevice(): Promise<string>;
  sendHexData(hexString: string): Promise<string>;
}

export default TurboModuleRegistry.getEnforcing<Spec>('Rm200Sdk');
