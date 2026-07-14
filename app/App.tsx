/**
 * JamLink — Root Application Component
 *
 * Phase 1: renders PingTestScreen for bridge verification.
 * This will be replaced with proper navigation in Phase 5.
 */

import React from 'react';
import { StatusBar } from 'react-native';
import { SafeAreaView, SafeAreaProvider } from 'react-native-safe-area-context';
import NetworkTestScreen from './screens/NetworkTestScreen';

function App(): React.JSX.Element {
  return (
    <SafeAreaProvider>
      <SafeAreaView style={{ flex: 1, backgroundColor: '#0f3460' }}>
        <StatusBar barStyle="light-content" backgroundColor="#0f3460" />
        <NetworkTestScreen />
      </SafeAreaView>
    </SafeAreaProvider>
  );
}

export default App;
