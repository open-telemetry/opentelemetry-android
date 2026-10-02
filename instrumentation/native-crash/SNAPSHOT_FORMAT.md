# Native Crash Snapshot Format

Status: version 1 format for
[#1940](https://github.com/open-telemetry/opentelemetry-android/issues/1940). Parsing,
validation, unwinding, and restart-time replay use this contract. Runtime capture remains follow-up
work.

The writer and reader rules below are the normative requirements for version 1.

The native crash implementation uses a fixed-size snapshot so the signal handler can copy bounded
state without allocation, locks, JNI, logging, or process-map parsing. Capture, parsing, unwinding,
and replay are separate changes. `native_crash_snapshot.h` is the source of truth for the binary
layout; this document explains the corresponding fields and invariants. Keep both in sync.

## Version 1

Version 1 is a 20,568-byte, little-endian record. Readers require the exact version and record size.
An incompatible record is discarded rather than interpreted using a newer layout.
It covers the four Android NDK ABIs currently produced by this module's native build. The explicit
architecture value lets readers apply the correct pointer width and register rules. Value `0`
(`OTEL_NCS_ARCH_UNKNOWN`) is an internal compile-time sentinel and is not a valid serialized
architecture.

| Offset | Size | Field |
| ---: | ---: | --- |
| 0 | 8 | Magic: `OTELNCS\0` |
| 8 | 4 | Format version (`1`) |
| 12 | 4 | Architecture: ARM (`1`), ARM64 (`2`), x86 (`3`), or x86_64 (`4`) |
| 16 | 4 | Total record size (`20568`) |
| 20 | 4 | Fatal signal number |
| 24 | 8 | Crash timestamp in Unix epoch nanoseconds |
| 32 | 32 | Program counter, stack pointer, frame pointer, and link register |
| 64 | 4 | Number of populated module entries (`1..128`) |
| 68 | 4 | Number of captured stack bytes (`0..4096`) |
| 72 | 8 | Address of the first captured stack byte; equal to the stack pointer |
| 80 | 16384 | 128 fixed-size module entries |
| 16464 | 4096 | Bounded stack bytes |
| 20560 | 4 | Reserved; must be zero |
| 20564 | 4 | 32-bit FNV-1a checksum of bytes 0 through 20563 |

Each module entry identifies one executable `PT_LOAD` segment from an ELF image loaded in the
process. Its address range and load bias let the reader turn a captured address into a
module-relative frame; its basename and build ID identify the corresponding binary for downstream
symbolication.

Each 128-byte module entry contains:

| Relative offset | Size | Field |
| ---: | ---: | --- |
| 0 | 8 | Load bias |
| 8 | 8 | Executable segment start |
| 16 | 8 | Executable segment end |
| 24 | 64 | NUL-terminated UTF-8 module basename |
| 88 | 4 | ELF build-ID length (`0..32`; zero means unavailable) |
| 92 | 32 | Raw ELF build-ID bytes followed by zero padding |
| 124 | 4 | Reserved; must be zero |

The writer zero-fills unused entries. It records app-owned modules before system modules and
preserves loader order within each group, so the table is not address-sorted. Every populated entry
satisfies `loadBias <= executableStart < executableEnd`. Names are nonblank UTF-8 without control
characters, use at most 63 bytes, and always include a NUL terminator followed by zero padding.
The 32-byte capacity accommodates build IDs up through 256 bits. Longer IDs are recorded as
unavailable rather than truncated because a partial identifier is not safe for exact artifact
matching; increasing a fixed-size field would only move that bound.

## Files And Pairing

The marker and snapshot are separate fixed internal files whose paths are supplied when the native
handler is installed. Temporary writes use the corresponding destination path with a `.tmp` suffix.
Exact destination names are implementation details rather than part of the binary format.

Recovery pairs a snapshot with a marker only when their signal number and timestamp match. A stale
or otherwise mismatched snapshot is invalid and is never attached to the marker's crash event.

## Writer Rules

Register and module addresses are unsigned. A 32-bit writer zero-extends every address into its
64-bit field. Registers come from the signal handler's `ucontext_t.uc_mcontext`, never from the
handler's own frame or alternate stack. The stack start equals the unmodified recorded stack
pointer. ARM64 code-address registers retain their raw tag and pointer-authentication bits, while
stack and frame pointers retain raw top-byte tags. Readers normalize code addresses before module
comparisons and remove only top-byte tags before captured-stack comparisons. Writers set the
link-register field to zero on x86 and x86_64.

The snapshot record lives in pre-allocated static storage. Before installing the signal handler,
the writer walks the loaded ELF program headers and prepares the module table while normal runtime
services are available. At crash time, the handler uses that table as-is; it does not inspect ELF
metadata. If `OTEL_NCS_ARCH_CURRENT` is `OTEL_NCS_ARCH_UNKNOWN`, or if module preparation fails,
marker capture remains enabled but snapshot capture is omitted. This costs 20,568 bytes of process
storage but does not consume the alternate signal stack or allocate during a crash.

The marker and snapshot use the same signal number and a timestamp from a single `clock_gettime`
call. The marker is written, synced, and atomically renamed first. The snapshot is then written and
atomically renamed without `fsync` to bound work in the fatal signal handler. A second fault or
abrupt process termination during the larger snapshot write therefore still leaves the marker as a
valid crash without native frames. A snapshot without a marker is an orphan and is removed during
startup recovery. Signal and timestamp matching prevents an older snapshot from being attached to
a newer marker.

Stack copying is fault-tolerant; a failed or short read produces a stack size from zero through
4,096 rather than reading through an invalid page. The handler loops over interrupted short writes,
writes each file through a temporary path, closes it, and atomically renames it into place.

## Reader Rules

Readers treat snapshots as corrupt or stale until validated. They check the exact size, magic,
version, checksum, architecture, bounds, reserved fields, module metadata, and matching marker
signal and timestamp. The checksum uses 32-bit FNV-1a with offset basis `0x811c9dc5` and prime
`0x01000193`, applying XOR before multiplication for each byte. It detects accidental corruption;
it is not an authenticity mechanism. Malformed module entries are skipped without discarding
structurally valid entries. A record with one or more populated module entries remains valid if
none of those entries are usable after validation, but it produces no native frames.

Readers reject zero stack pointers, a stack start that differs from the stack pointer, and a stack
start that is not aligned to the recorded architecture's pointer width. A zero program counter is
valid for crashes such as an indirect call through a null function pointer. Readers omit the
program-counter frame in that case and continue with frame-pointer and link-register recovery. A
misaligned or otherwise corrupt stack pointer produces no native frames. Readers ignore the
link-register field on x86 and x86_64.

ARM64 readers preserve captured values and try the raw address first. For code addresses that do
not resolve, they retry after clearing bits 56 through 63, then 52 through 63, 48 through 63, 47
through 63, and 39 through 63 to account for top-byte tags, pointer authentication, and Android's
supported ARM64 virtual-address widths. Captured-stack comparisons only retry after clearing bits
56 through 63 because stack and frame pointers may be tagged but are not pointer-authenticated. ARM
readers clear the Thumb bit before lookup. The first candidate inside a captured executable segment
wins.

Version 1 reports at most 64 frames. ARM64, x86, and x86_64 readers walk frame records containing
the previous frame pointer at `[fp]` and the return address at `[fp + pointerSize]`. The captured
stack bounds the number of records examined; malformed records do not consume the 64-frame output
limit. ARM32 readers do not perform a frame-pointer walk; they report the program counter and may
use the link register. When the program counter does not resolve or a frame-pointer walk yields no
caller, ARM and ARM64 readers may add a nonzero link register before frame-pointer callers. Readers
omit a link-register candidate that duplicates a recovered frame. The link register can be stale
for a non-leaf crash, so readers preserve its provenance even when a stacktrace renderer cannot
distinguish it from a frame-pointer result.

The crash-time program counter is reported exactly after architecture-specific normalization.
Frame-pointer and link-register return addresses are adjusted into the calling instruction after
normalization: one byte on x86 and x86_64, four bytes on ARM64 and ARM state, and two bytes on Thumb
state when executable bytes are unavailable to distinguish a two-byte from a four-byte call.

Confirmed frames are reported from the resolved instruction address: the normalized program
counter, or a normalized caller address after the architecture-specific return-address adjustment.
The module-relative address is `instructionAddress - module.loadBias`, with the module build ID when
available. Segment containment is checked against that instruction address.

A malformed marker is discarded together with its snapshot and recovery state without emitting an
event. A missing, malformed, or mismatched snapshot is discarded while its valid marker continues
as a crash without native frames. Transient marker or snapshot read failures are retried. If
snapshot retries are exhausted, recovery discards the snapshot and emits the valid marker-only
crash. Other exhausted read failures discard all recovery files without emitting an event.

Before emitting an event, recovery durably records that delivery has been claimed. If that state
cannot be persisted, recovery does not emit and abandons the crash with best-effort cleanup. Once
delivery is claimed, later launches perform cleanup only and never emit the event again. After the
event is handed to OpenTelemetry, recovery deletes the marker, snapshot, and recovery state.
Cleanup failures retain the remaining files for bounded cleanup-only retries.

The signal handler is not installed while a retry is pending because version 1 uses one fixed marker
and snapshot pair that a new crash would overwrite. Ordinary recovery exceptions and linkage errors
are contained at the instrumentation boundary so they cannot terminate the host application;
unrecoverable virtual-machine errors are not intercepted. Every retry path is bounded by count or
age. After the applicable limit is reached, recovery performs best-effort cleanup and allows the
signal handler to be installed again.

## Limitations

Version 1 does not interpret DWARF or compact unwind metadata. ARM32 does not use frame-pointer
walking, and optimized builds on other architectures can omit usable frame pointers, so recovery
can contain only the crashing frame and, on ARM and ARM64, a link-register candidate.

The snapshot is limited to 128 executable segments and 4 KiB of stack memory. Modules loaded after
the module table is prepared are not represented, so recovery can return a partial stack. A future
CFI-aware unwinder or Crashpad integration can replace the reader without changing the
signal-handler safety model. Symbol upload and backend symbolication remain separate work.

While recovery is pending, native crashes in that process are not captured. Supporting concurrent
pending and new crashes requires per-crash paths or renaming the pending files out of the handler's
fixed destinations and is outside version 1.
