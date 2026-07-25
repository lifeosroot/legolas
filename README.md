# Legolas

Legolas is the Android client for Arwen. It scans the QR code printed by Arwen, validates the `legolas://pair` URI, encrypts the shared API key with Android Keystore, and stores the pairing with DataStore.

## Architecture

```text
UI (Compose + ViewModel)
        ↓
PairingRepository
        ↓
DataStore + Android Keystore
```

Pairing is required for the app to connect to Arwen. Optional feature modules are opt-in: none are included or enabled by default.

## Design

See [Legolas Design System](docs/design-system.md). It records the shared Root colors, typography, shapes, components, source-of-truth projects, and implementation rules.

## Run

1. Start Arwen and keep its terminal QR code visible.
2. Open Legolas on an Android device with Google Play services.
3. Tap **Scan QR code** and scan the Arwen QR.

The Google Code Scanner handles camera access without adding a camera permission to Legolas. Use **Forget Arwen** to remove the locally stored connection.

## Build

```bash
./gradlew test lintDebug assembleDebug
```

Run the Android Keystore and DataStore integration tests with a device or emulator connected:

```bash
./gradlew connectedDebugAndroidTest
```
