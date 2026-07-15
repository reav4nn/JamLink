import React from 'react';
import { View, Text, Button, StyleSheet } from 'react-native';
import { useJamLinkNetwork } from '../hooks/useJamLinkNetwork';

export default function TimeSyncScreen() {
  const { networkState, timeSyncState, forceSyncNow } = useJamLinkNetwork();

  const getStatusColor = () => {
    switch (timeSyncState.status) {
      case 'SYNCED': return '#4ade80'; // green
      case 'SYNCING': return '#fbbf24'; // yellow
      case 'ERROR': return '#f87171'; // red
      default: return '#a0a0a0'; // gray
    }
  };

  return (
    <View style={styles.container}>
      <Text style={styles.title}>Time Sync (Phase 3)</Text>
      
      <View style={styles.card}>
        <Text style={styles.label}>Network Role:</Text>
        <Text style={styles.value}>{networkState.role} ({networkState.state})</Text>
      </View>

      <View style={styles.card}>
        <Text style={styles.label}>Sync Status:</Text>
        <Text style={[styles.value, { color: getStatusColor() }]}>{timeSyncState.status}</Text>
        
        <Text style={styles.label}>Clock Offset:</Text>
        <Text style={styles.value}>{timeSyncState.offsetMs > 0 ? '+' : ''}{timeSyncState.offsetMs} ms</Text>
        
        <Text style={styles.label}>Round-Trip Time (RTT):</Text>
        <Text style={styles.value}>{timeSyncState.rttMs} ms</Text>
        
        <Text style={styles.label}>Valid Samples Count:</Text>
        <Text style={styles.value}>{timeSyncState.sampleCount}</Text>
      </View>

      <View style={styles.buttonContainer}>
        <Button 
          title="Force Re-Sync" 
          onPress={() => forceSyncNow()} 
          disabled={networkState.role !== 'CLIENT' || networkState.state !== 'CONNECTED'}
        />
      </View>
      
      {networkState.role === 'MASTER' && (
        <Text style={styles.note}>
          Master device runs the UDP Time Sync Server automatically.
        </Text>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    padding: 16,
    backgroundColor: '#0f3460',
  },
  title: {
    fontSize: 24,
    fontWeight: 'bold',
    color: 'white',
    marginBottom: 24,
    textAlign: 'center',
  },
  card: {
    backgroundColor: '#16213e',
    padding: 16,
    borderRadius: 8,
    marginBottom: 16,
  },
  label: {
    color: '#a0a0a0',
    fontSize: 14,
    marginTop: 8,
  },
  value: {
    color: 'white',
    fontSize: 18,
    fontWeight: 'bold',
    marginBottom: 8,
  },
  buttonContainer: {
    marginTop: 16,
  },
  note: {
    color: '#a0a0a0',
    marginTop: 24,
    textAlign: 'center',
    fontStyle: 'italic',
  }
});
