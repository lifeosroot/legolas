# Legolas

Legolas is the Android client for Arwen. It pairs by QR code, keeps the API key in Android Keystore-backed encrypted storage, and provides privacy-sensitive features as explicit runtime opt-ins.

## Architecture

```text
App shell (Compose + Hilt)
        ├── pairing → DataStore + Android Keystore
        └── :modules:location
              ├── Fused Location + Activity Recognition
              ├── Room outbox
              └── authenticated Arwen upload, route reads, and place management
```

The location module is compiled into this build but is **off by default**. It does not collect or upload anything until the user enables it and grants the required Android permissions. Existing samples, saved places, and the derived timeline can be read or managed on the paired Arwen server without enabling collection. Routes have an on-device preview by default; an OpenFreeMap background map is a separate explicit opt-in. See [Location collection](docs/location-collection.md) for the state machine, data fields, map disclosure, opt-out behavior, and device test checklist.

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

Plain HTTP is accepted for loopback and RFC 1918 private IPv4 addresses (`10/8`, `172.16/12`, and `192.168/16`), including the Android emulator host (`10.0.2.2`). Use it only on a trusted LAN because the API key and location samples are not encrypted in transit. Public addresses require HTTPS. Android 17 also asks for local-network access when the paired host is on the LAN.

While the Legolas process is running, it checks Arwen's authenticated health endpoint immediately and every 60 seconds. After five consecutive network or server failures, Legolas asks whether to sign out; choosing to stay signed in suppresses the warning until a successful check. An explicit `401` or `403` stops location collection, deletes pending samples to prevent cross-owner upload, clears the pairing credential, and returns to pairing.

## Build

```bash
./gradlew test lintDebug assembleDebug
```

Run the Android Keystore and DataStore integration tests with a device or emulator connected:

```bash
./gradlew connectedDebugAndroidTest
```
