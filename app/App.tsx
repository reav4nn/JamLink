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
import TimeSyncScreen from './screens/TimeSyncScreen';
import { View, Button } from 'react-native';

function App(): React.JSX.Element {
  const [activeTab, setActiveTab] = React.useState<'network' | 'sync'>('network');

  return (
    <SafeAreaProvider>
      <SafeAreaView style={{ flex: 1, backgroundColor: '#0f3460' }}>
        <StatusBar barStyle="light-content" backgroundColor="#0f3460" />
        <View style={{ flex: 1 }}>
          {activeTab === 'network' ? <NetworkTestScreen /> : <TimeSyncScreen />}
        </View>
        <View style={{ flexDirection: 'row', backgroundColor: '#1a1a2e', padding: 8 }}>
          <View style={{ flex: 1, marginRight: 4 }}>
            <Button 
              title="Network" 
              color={activeTab === 'network' ? '#4ade80' : '#333'} 
              onPress={() => setActiveTab('network')} 
            />
          </View>
          <View style={{ flex: 1, marginLeft: 4 }}>
            <Button 
              title="Time Sync" 
              color={activeTab === 'sync' ? '#4ade80' : '#333'} 
              onPress={() => setActiveTab('sync')} 
            />
          </View>
        </View>
      </SafeAreaView>
    </SafeAreaProvider>
  );
}

export default App;
