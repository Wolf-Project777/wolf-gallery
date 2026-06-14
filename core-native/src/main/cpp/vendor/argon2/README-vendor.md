# Vendored: Argon2 reference implementation

**Upstream:** https://github.com/P-H-C/phc-winner-argon2
**Pinned tag:** `20190702` (latest release as of import; spec is RFC-frozen)
**Imported on:** 2026-05-01
**License:** dual CC0-1.0 / Apache-2.0 (see `LICENSE`)

## Why vendored, not pulled as a CMake FetchContent

Reproducible offline builds. Anyone cloning this repo gets a bit-identical
crypto core without depending on GitHub being reachable at build time, and
without trusting whatever happens to be on `master` the day they build.

## How to update

If a CVE forces an update, replace `LICENSE`, `include/`, and `src/` with
the new tag's contents and bump the pinned tag in this file. Re-run the
`secureZero` + Argon2 self-tests on a real device to confirm nothing
regressed. Do NOT cherry-pick patches into vendored sources — keep them
identical to upstream so audits stay tractable.

## What we use

- `argon2id_hash_raw` (raw 32-byte tag derivation, no PHC string format)
- BLAKE2b portable C reference (`src/blake2/blake2b.c`)
- Reference G compression function (`src/ref.c`) — portable, no SSE2/AVX
  intrinsics. ARM64 chips run this just as fast on the memory-bound loop.
- pthread-based parallelism (`src/thread.c`) — bionic libc on Android
  provides pthreads; no extra link flags needed.

## What we deliberately do NOT use

- `opt.c` — x86 SSE2 path. On ARM64 it would not compile.
- PHC encoding helpers (`encoding.c::encode_string`) — we always work with
  raw bytes; the encoded string format leaks parameters.
