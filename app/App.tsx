/**
 * JamLink — Root Application Component
 *
 * Phase 1: renders PingTestScreen for bridge verification.
 * This will be replaced with proper navigation in Phase 5.
 */

import React from 'react';
import { StatusBar, SafeAreaView } from 'react-native';
import NetworkTestScreen from './screens/NetworkTestScreen';

function App(): React.JSX.Element {
  return (
    <SafeAreaView style={{ flex: 1, backgroundColor: '#1a1a2e' }}>
      <StatusBar barStyle="light-content" backgroundColor="#1a1a2e" />
      <NetworkTestScreen />
    </SafeAreaView>
  );
}

export default App;
