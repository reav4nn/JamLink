import { useState, useEffect, useCallback } from 'react';
import { DeviceEventEmitter } from 'react-native';
import NativeJamLinkBridge from '../specs/NativeJamLinkBridge';

export interface Peer {
  address: string;
  name: string;
  status: number; // 0=CONNECTED, 1=INVITED, 2=FAILED, 3=AVAILABLE, 4=UNAVAILABLE
}

export interface NetworkState {
  state: 'DISCONNECTED' | 'CONNECTED';
  role: 'MASTER' | 'CLIENT' | 'NONE';
  masterIp?: string;
}

export function useJamLinkNetwork() {
  const [peers, setPeers] = useState<Peer[]>([]);
  const [networkState, setNetworkState] = useState<NetworkState>({ state: 'DISCONNECTED', role: 'NONE' });
  const [lastCommand, setLastCommand] = useState<string | null>(null);

  useEffect(() => {
    const peerSub = DeviceEventEmitter.addListener('onPeersUpdated', (peersList: Peer[]) => {
      setPeers(peersList || []);
    });

    const stateSub = DeviceEventEmitter.addListener('onConnectionStateChanged', (state: NetworkState) => {
      setNetworkState(state);
    });

    const commandSub = DeviceEventEmitter.addListener('onCommandReceived', (cmd: string) => {
      setLastCommand(cmd);
    });

    return () => {
      peerSub.remove();
      stateSub.remove();
      commandSub.remove();
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
  };
}
