/**
 * JamLink — Root Application Component
 *
 * Phase 1: renders PingTestScreen for bridge verification.
 * This will be replaced with proper navigation in Phase 5.
 */

import React from 'react';
import { StatusBar } from 'react-native';
import PingTestScreen from './screens/PingTestScreen';

function App(): React.JSX.Element {
  return (
    <>
      <StatusBar barStyle="light-content" backgroundColor="#1a1a2e" />
      <PingTestScreen />
    </>
  );
}

export default App;
