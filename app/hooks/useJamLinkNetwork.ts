import { useState, useEffect, useCallback } from 'react';
import { DeviceEventEmitter } from 'react-native';
import NativeJamLinkBridge from '../specs/NativeJamLinkBridge';

export interface Peer {
  address: string;
  name: string;
  status: number; // 0=CONNECTED, 1=INVITED, 2=FAILED, 3=AVAILABLE, 4=UNAVAILABLE
}

export interface NetworkState {
  state: 'DISCONNECTED' | 'CONNECTED' | 'PENDING';
  role: 'MASTER' | 'CLIENT' | 'NONE';
  masterIp?: string;
}

export interface TimeSyncState {
  status: 'NOT_SYNCED' | 'SYNCING' | 'SYNCED' | 'ERROR';
  offsetMs: number;
  rttMs: number;
  sampleCount: number;
}

export function useJamLinkNetwork() {
  const [peers, setPeers] = useState<Peer[]>([]);
  const [networkState, setNetworkState] = useState<NetworkState>({ state: 'DISCONNECTED', role: 'NONE' });
  const [lastCommand, setLastCommand] = useState<string | null>(null);
  const [timeSyncState, setTimeSyncState] = useState<TimeSyncState>({ status: 'NOT_SYNCED', offsetMs: 0, rttMs: 0, sampleCount: 0 });

  const getNetworkState = useCallback(async () => {
    const json = await NativeJamLinkBridge.getNetworkState();
    return JSON.parse(json) as NetworkState;
  }, []);

  useEffect(() => {
    // Fetch initial state
    getNetworkState().then(setNetworkState).catch(console.error);
    getTimeSyncState().then(setTimeSyncState).catch(console.error);

    const peerSub = DeviceEventEmitter.addListener('onPeersUpdated', (peersList: Peer[]) => {
      setPeers(peersList || []);
    });

    const stateSub = DeviceEventEmitter.addListener('onConnectionStateChanged', (state: NetworkState) => {
      setNetworkState(state);
    });

    const commandSub = DeviceEventEmitter.addListener('onCommandReceived', (cmd: string) => {
      setLastCommand(cmd);
    });

    const timeSyncSub = DeviceEventEmitter.addListener('onTimeSyncStateChanged', (stateJson: string) => {
      try {
        const state = JSON.parse(stateJson);
        setTimeSyncState(state);
      } catch (e) {
        console.error("Failed to parse time sync state", e);
      }
    });

    return () => {
      peerSub.remove();
      stateSub.remove();
      commandSub.remove();
      timeSyncSub.remove();
    };
  }, []);

  const requestPermissions = useCallback(async () => {
    return await NativeJamLinkBridge.requestPermissions();
  }, []);

  const startDiscovery = useCallback(async () => {
    await NativeJamLinkBridge.startDiscovery();
  }, []);

  const stopDiscovery = useCallback(async () => {
    await NativeJamLinkBridge.stopDiscovery();
  }, []);

  const createGroup = useCallback(async () => {
    await NativeJamLinkBridge.createGroup();
  }, []);

  const connectToDevice = useCallback(async (address: string) => {
    await NativeJamLinkBridge.connectToDevice(address);
  }, []);

  const disconnect = useCallback(async () => {
    await NativeJamLinkBridge.disconnect();
  }, []);

  const sendCommand = useCallback(async (cmd: string) => {
    await NativeJamLinkBridge.sendCommand(cmd);
  }, []);

  const getTimeSyncState = useCallback(async () => {
    const json = await NativeJamLinkBridge.getTimeSyncState();
    return JSON.parse(json) as TimeSyncState;
  }, []);

  const forceSyncNow = useCallback(async () => {
    await NativeJamLinkBridge.forceSyncNow();
  }, []);

  return {
    peers,
    networkState,
    lastCommand,
    requestPermissions,
    startDiscovery,
    stopDiscovery,
    createGroup,
    connectToDevice,
    disconnect,
    sendCommand,
    timeSyncState,
    getNetworkState,
    getTimeSyncState,
    forceSyncNow,
  };
}
