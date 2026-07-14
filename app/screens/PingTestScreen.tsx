/**
 * PingTestScreen — Phase 1 bridge verification.
 *
 * Tapping "Ping" calls NativeJamLinkBridge.ping("hello") which traverses:
 * RN (TS) → TurboModule → Kotlin → JNI → C++ → back.
 * Expected result displayed: "hello pong from C++"
 */

import React, { useState } from 'react';
import {
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
  ActivityIndicator,
} from 'react-native';
import NativeJamLinkBridge from '../specs/NativeJamLinkBridge';

function PingTestScreen(): React.JSX.Element {
  const [result, setResult] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const handlePing = async () => {
    setLoading(true);
    setError(null);
    setResult(null);
    try {
      const response = await NativeJamLinkBridge.ping('hello');
      setResult(response);
    } catch (e: unknown) {
      const message = e instanceof Error ? e.message : String(e);
      setError(message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <View style={styles.container}>
      <Text style={styles.title}>JamLink — Phase 1 Bridge Test</Text>
      <Text style={styles.subtitle}>
        RN → TurboModule → Kotlin → JNI → C++ → back
      </Text>

      <TouchableOpacity
        style={styles.button}
        onPress={handlePing}
        disabled={loading}
        activeOpacity={0.7}
      >
        {loading ? (
          <ActivityIndicator color="#fff" />
        ) : (
          <Text style={styles.buttonText}>Ping</Text>
        )}
      </TouchableOpacity>

      {result !== null && (
        <View style={styles.resultBox}>
          <Text style={styles.resultLabel}>Response:</Text>
          <Text style={styles.resultText}>{result}</Text>
        </View>
      )}

      {error !== null && (
        <View style={[styles.resultBox, styles.errorBox]}>
          <Text style={styles.resultLabel}>Error:</Text>
          <Text style={styles.errorText}>{error}</Text>
        </View>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#1a1a2e',
    padding: 24,
  },
  title: {
    fontSize: 22,
    fontWeight: '700',
    color: '#e0e0e0',
    marginBottom: 8,
  },
  subtitle: {
    fontSize: 13,
    color: '#888',
    marginBottom: 40,
    textAlign: 'center',
  },
  button: {
    backgroundColor: '#6c63ff',
    paddingHorizontal: 48,
    paddingVertical: 16,
    borderRadius: 12,
    elevation: 4,
    minWidth: 160,
    alignItems: 'center',
  },
  buttonText: {
    color: '#fff',
    fontSize: 18,
    fontWeight: '600',
  },
  resultBox: {
    marginTop: 32,
    backgroundColor: '#16213e',
    padding: 20,
    borderRadius: 12,
    borderWidth: 1,
    borderColor: '#0f3460',
    width: '100%',
    maxWidth: 360,
  },
  errorBox: {
    borderColor: '#e94560',
  },
  resultLabel: {
    fontSize: 12,
    color: '#888',
    marginBottom: 6,
    textTransform: 'uppercase',
    letterSpacing: 1,
  },
  resultText: {
    fontSize: 18,
    color: '#53cf6d',
    fontFamily: 'monospace',
  },
  errorText: {
    fontSize: 16,
    color: '#e94560',
    fontFamily: 'monospace',
  },
});

export default PingTestScreen;
