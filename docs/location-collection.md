# Location collection

Location collection is an explicit opt-in module. Pairing alone does not enable it.

## What runs

The foreground service combines Google Play services Activity Recognition with fused location updates. Its state machine and thresholds were ported from the Root Android reference implementation without changing the decision rules. The states are `Unknown`, `Still`, `MovingCandidate`, `Moving`, and `MovingDegraded`; GPS evidence can promote or heal activity-recognition decisions. Departure, arrival, and motion-transition samples have dedicated boundary and deduplication rules.

The 11 pure policy files and the original 56 policy tests were moved together. The Android service adds only lifecycle, permission, local outbox, and Arwen upload integration around those rules.

## Permissions and visible state

Collection requires:

- precise foreground location;
- **Allow all the time** background location on Android 10 and later;
- physical activity access on Android 10 and later;
- notifications on Android 13 and later; and
- the device-wide location switch to be on.

Approximate location is shown but is not accepted as ready because the collection policy rejects readings with insufficient accuracy. A persistent Android foreground-service notification remains visible while collection is active and includes a **Stop collection** action.

On Android 17 and later, Legolas requests local-network access only when the paired Arwen host is a private IP or local hostname. Denying that permission does not stop collection: samples remain in the local outbox until the server is reachable.

## Data and opt-out behavior

Each queued and uploaded sample contains only:

- client-generated sample ID;
- collection time;
- latitude and longitude;
- horizontal accuracy;
- collection source;
- activity type; and
- the boundary/save reason, when present.

Turning the module off stops the service, cancels scheduled uploads, and prevents both new inserts and uploads. Pending samples remain locally so collection can resume without silent data loss. **Forget Arwen** turns the module off, deletes pending samples, and removes the local pairing secret. It does not delete data already received by Arwen.

Uploads are idempotent by owner and client sample ID. Network failures, HTTP 408/429, and server errors are retried; permanent client or authentication errors are surfaced without an endless background retry.

## Route reading

The Route tab reads owner-scoped samples for a selected Seoul date from the paired Arwen server. It supports previous, today, and next date controls, quality filtering, refresh, and explicit loading, empty, permission, and failure states. This read-only view works even when collection is off.

The default route preview is drawn locally and makes no map-tile request. The optional background map uses [MapLibre Compose](https://maplibre.org/maplibre-compose/) with [OpenFreeMap](https://openfreemap.org/), requires no API key, and stays off until the user accepts a separate disclosure. Once enabled, OpenFreeMap receives tile requests that reveal the approximate area being viewed; Legolas does not add location samples, the Arwen API key, or account data to those requests. The setting is persisted and can be turned off from either the Route or Settings tab. If the provider cannot load, Legolas falls back to the local preview.

The provider is represented by a single style URL in the location module so a self-hosted MapLibre-compatible style can replace it later without changing route data or UI state.

## Transport

Use HTTPS for internet-facing Arwen deployments. Plain HTTP is accepted for `localhost`, `127.0.0.1`, `::1`, and RFC 1918 private IPv4 addresses (`10/8`, `172.16/12`, and `192.168/16`), including `10.0.2.2`. Use cleartext pairing only on a trusted LAN because the API key and location samples are visible in transit.

## Verification

Run the deterministic checks with:

```bash
./gradlew test lintDebug assembleDebug
```

Before a release, also test on the supported Android versions: API 23, 29, 30/31, 33, 34, and 37. Cover precise versus approximate location, permission revocation, device location off, immediate enable/disable, a delayed activity update, process death and sticky restart, reboot recovery, offline queue recovery, local-network denial, and forgetting an empty and non-empty outbox.

OEM battery restrictions can still delay callbacks. Legolas deliberately does not request a blanket battery-optimization exemption; users can allow unrestricted background use in Android system settings when their device vendor requires it.
