import type { TurboModule } from 'react-native';
import { TurboModuleRegistry } from 'react-native';

export interface Spec extends TurboModule {
  ping(input: string): Promise<string>;
  requestPermissions(): Promise<boolean>;
  startDiscovery(): Promise<void>;
  stopDiscovery(): Promise<void>;
  createGroup(): Promise<void>;
  connectToDevice(deviceAddress: string): Promise<void>;
  disconnect(): Promise<void>;
  sendCommand(commandJson: string): Promise<void>;
  getNetworkState(): Promise<string>;
  
  // Phase 3 — Time Sync
  getTimeSyncState(): Promise<string>;
  forceSyncNow(): Promise<void>;
}

export default TurboModuleRegistry.getEnforcing<Spec>('JamLinkBridge');
