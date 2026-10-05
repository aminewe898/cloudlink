# Performance Notes

No startup, macrobenchmark, battery, or end-to-end terminal throughput measurements were available in this audit. The statements below are implementation properties, not benchmark claims.

## Improvements in this pass

- Terminal writes now use a bounded 64-entry queue. Backpressure is reported instead of allowing unbounded memory growth.
- Only the active terminal generation can publish state, write, or reconnect; stale transport work is cancelled.
- Terminal render runs reuse a `StringBuilder` and cached typefaces, reducing hot-path allocations.
- Parser parameter and OSC payload sizes are capped to constrain hostile/malformed terminal output.
- Telemetry polling stops when the server-detail screen leaves composition; missing metrics no longer discard all other telemetry.
- Interactive SFTP operations are serialized, and stale directory work is cancelled.
- Compose flow collection is lifecycle aware on primary screens.

## Remaining costs

- Each terminal publication copies screen cell arrays, and Canvas redraws the visible terminal. A 10,000-line scrollback retains several MiB depending on content and runtime overhead.
- Resize does not reflow and may copy a large rectangular region.
- Telemetry opens short-lived exec channels on its interval.
- Directory listings are mapped, formatted, and sorted as a batch.
- The editor holds saved/current strings and encoded output for files up to 2 MiB.
- Compose Material Icons Extended and debug tooling materially increase the debug APK. The minified unsigned release is much smaller.

## Measurements required before performance claims

- Macrobenchmark cold/warm startup and navigation on low/mid/high devices
- Terminal fixture throughput, frame timing, allocations, scrollback memory, and resize cost
- Long-running high-output SSH session with reconnect and backpressure
- SFTP listing and transfer throughput for varied directory/file sizes
- Telemetry battery/network cost in foreground and after navigation
- Baseline profile coverage and minified release startup

Record device, Android version, thermal state, server/network, build type, sample size, percentile, and raw result artifacts. Do not compare debug and release builds as equivalent.
