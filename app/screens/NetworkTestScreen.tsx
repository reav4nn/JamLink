import React, { useState } from 'react';
import { View, Text, Button, FlatList, StyleSheet, TextInput, Alert, PermissionsAndroid, Platform } from 'react-native';
import { useJamLinkNetwork } from '../hooks/useJamLinkNetwork';

export default function NetworkTestScreen() {
  const {
    peers,
    networkState,
    lastCommand,
    startDiscovery,
    stopDiscovery,
    createGroup,
    connectToDevice,
    disconnect,
    sendCommand,
  } = useJamLinkNetwork();

  const [commandInput, setCommandInput] = useState('{"type":"PLAY","targetTimestamp":1721054320000}');

  const handleRequestPermissions = async () => {
    try {
      if (Platform.OS === 'android') {
        const perms = [
          PermissionsAndroid.PERMISSIONS.ACCESS_FINE_LOCATION,
        ];
        if (Platform.Version >= 33) {
          perms.push(PermissionsAndroid.PERMISSIONS.NEARBY_WIFI_DEVICES);
        }
        
        const granted = await PermissionsAndroid.requestMultiple(perms);
        const allGranted = Object.values(granted).every(
          status => status === PermissionsAndroid.RESULTS.GRANTED
        );
        Alert.alert('Permissions', allGranted ? 'Granted' : 'Denied');
      }
    } catch (e: any) {
      Alert.alert('Error', e.message);
    }
  };

  const handleSend = () => {
    sendCommand(commandInput);
  };

  const handleStartDiscovery = async () => {
    try {
      await startDiscovery();
    } catch (e: any) {
      Alert.alert('Discovery Error', e.message);
    }
  };

  const handleStopDiscovery = async () => {
    try {
      await stopDiscovery();
    } catch (e: any) {
      Alert.alert('Stop Error', e.message);
    }
  };

  const handleCreateGroup = async () => {
    try {
      await createGroup();
    } catch (e: any) {
      Alert.alert('Create Group Error', e.message);
    }
  };

  const handleDisconnect = async () => {
    try {
      await disconnect();
    } catch (e: any) {
      Alert.alert('Disconnect Error', e.message);
    }
  };

  const handleConnectToDevice = async (address: string) => {
    try {
      await connectToDevice(address);
    } catch (e: any) {
      Alert.alert('Connect Error', e.message);
    }
  };

  return (
    <View style={styles.container}>
      <Text style={styles.title}>Network Test (Phase 2)</Text>
      
      <View style={styles.stateContainer}>
        <Text style={styles.stateText}>State: {networkState.state}</Text>
        <Text style={styles.stateText}>Role: {networkState.role}</Text>
        {networkState.masterIp && (
          <Text style={styles.stateText}>Master IP: {networkState.masterIp}</Text>
        )}
      </View>

      <View style={styles.buttonRow}>
        <Button title="Perms" onPress={handleRequestPermissions} />
        <Button title="Master (Group)" onPress={handleCreateGroup} />
        <Button title="Disconnect" onPress={handleDisconnect} />
      </View>

      <View style={styles.buttonRow}>
        <Button title="Start Discover" onPress={handleStartDiscovery} />
        <Button title="Stop Discover" onPress={handleStopDiscovery} />
      </View>

      <Text style={styles.subtitle}>Peers Found:</Text>
      <FlatList
        data={peers}
        keyExtractor={(item) => item.address}
        renderItem={({ item }) => (
          <View style={styles.peerItem}>
            <View>
              <Text style={styles.peerName}>{item.name}</Text>
              <Text style={styles.peerAddress}>{item.address} (Status: {item.status})</Text>
            </View>
            <Button title="Connect" onPress={() => handleConnectToDevice(item.address)} disabled={networkState.state === 'CONNECTED'} />
          </View>
        )}
      />

      <View style={styles.commandContainer}>
        <Text style={styles.subtitle}>TCP Command Test</Text>
        <TextInput
          style={styles.input}
          value={commandInput}
          onChangeText={setCommandInput}
        />
        <Button title="Send Command" onPress={handleSend} disabled={networkState.state !== 'CONNECTED'} />
        <View style={styles.lastCommandContainer}>
          <Text style={styles.subtitle}>Last Received Command:</Text>
          <Text style={styles.commandText}>{lastCommand || 'None'}</Text>
        </View>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    padding: 16,
    backgroundColor: '#0f3460', // was #1a1a2e, let's use a lighter blue/green to ensure it's rendering
  },
  title: {
    fontSize: 24,
    fontWeight: 'bold',
    color: 'white',
    marginBottom: 16,
    textAlign: 'center',
  },
  subtitle: {
    fontSize: 18,
    fontWeight: 'bold',
    color: '#e0e0e0',
    marginTop: 16,
    marginBottom: 8,
  },
  stateContainer: {
    backgroundColor: '#16213e',
    padding: 12,
    borderRadius: 8,
    marginBottom: 16,
  },
  stateText: {
    color: '#4ade80',
    fontSize: 16,
    marginBottom: 4,
  },
  buttonRow: {
    flexDirection: 'row',
    justifyContent: 'space-around',
    marginBottom: 12,
  },
  peerItem: {
    backgroundColor: '#0f3460',
    padding: 12,
    borderRadius: 8,
    marginBottom: 8,
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  peerName: {
    color: 'white',
    fontSize: 16,
    fontWeight: 'bold',
  },
  peerAddress: {
    color: '#a0a0a0',
    fontSize: 12,
  },
  commandContainer: {
    marginTop: 16,
    borderTopWidth: 1,
    borderTopColor: '#333',
    paddingTop: 16,
  },
  input: {
    backgroundColor: 'white',
    padding: 8,
    borderRadius: 4,
    marginBottom: 8,
    color: 'black',
  },
  lastCommandContainer: {
    marginTop: 12,
    backgroundColor: '#333',
    padding: 12,
    borderRadius: 4,
  },
  commandText: {
    color: '#00ffcc',
    fontFamily: 'monospace',
  },
});
