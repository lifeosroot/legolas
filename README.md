# Legolas

Legolas is the Android client for Arwen. It pairs by QR code, keeps the API key in Android Keystore-backed encrypted storage, and provides privacy-sensitive features as explicit runtime opt-ins.

## Architecture

```text
App shell (Compose + Hilt)
        ├── pairing → DataStore + Android Keystore
        └── :modules:location
              ├── Fused Location + Activity Recognition
              ├── Room outbox
              └── authenticated Arwen upload and route reads
```

The location module is compiled into this build but is **off by default**. It does not collect or upload anything until the user enables it and grants the required Android permissions. Existing samples can be read by date from the paired Arwen server without enabling collection. Routes have an on-device preview by default; an OpenFreeMap background map is a separate explicit opt-in. See [Location collection](docs/location-collection.md) for the state machine, data fields, map disclosure, opt-out behavior, and device test checklist.

To build Legolas without the location module, its permissions, services, and dependencies:

```bash
./gradlew assembleDebug -PlocationEnabled=false
```

Location support is included when the property is omitted.

## Design

See [Legolas Design System](docs/design-system.md). It records the shared Root colors, typography, shapes, components, source-of-truth projects, and implementation rules.

## Run

1. Start Arwen with its location module enabled and keep its terminal QR code visible.
2. Open Legolas on an Android device with Google Play services.
3. Tap **Scan QR code** and scan the Arwen QR.

The Google Code Scanner handles camera access without adding a camera permission to Legolas. Use **Forget Arwen** to remove the locally stored connection.

Real devices must pair to an HTTPS Arwen URL. Plain HTTP is accepted only for loopback and the Android emulator host (`10.0.2.2`) so API keys and location samples are not exposed on the network. Android 17 also asks for local-network access when the paired host is on the LAN.

While the Legolas process is running, it checks Arwen's authenticated health endpoint immediately and every 60 seconds. An explicit `401` or `403` stops location collection, deletes pending samples to prevent cross-owner upload, clears the pairing credential, and returns to pairing. Network timeouts and server errors keep the pairing and retry on the next check.

## Build

```bash
./gradlew test lintDebug assembleDebug
```

Run the Android Keystore and DataStore integration tests with a device or emulator connected:

```bash
./gradlew connectedDebugAndroidTest
```
