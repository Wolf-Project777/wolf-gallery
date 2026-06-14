cmake_minimum_required(VERSION 3.22.1)

project("encnative" LANGUAGES C CXX)

# ─── Vendored Argon2 reference (BSD/Apache, see vendor/argon2/LICENSE) ────────
#
# Built as a private STATIC archive and linked into libencnative.so. We never
# expose the Argon2 symbols to the JVM directly — only our JNI shim in
# native-lib.cpp can call them. Static linkage means no separate libargon2.so
# in the APK and no dlsym surface for any tampering tool.
add_library(argon2_ref STATIC
    vendor/argon2/src/argon2.c
    vendor/argon2/src/core.c
    vendor/argon2/src/encoding.c
    vendor/argon2/src/thread.c
    vendor/argon2/src/ref.c
    vendor/argon2/src/blake2/blake2b.c
)
target_include_directories(argon2_ref
    PUBLIC  vendor/argon2/include
    PRIVATE vendor/argon2/src
)
# Reference Argon2 was authored against -Wall-clean GCC ~5; modern Clang flags
# its sign-compares and unused-parameters. We do NOT patch vendored crypto —
# audits stay tractable only if the source is bit-identical to upstream — so
# we silence warnings on this target alone. Our own code keeps -Wall -Wextra.
target_compile_options(argon2_ref PRIVATE -w)
# -fPIC so the static lib can be folded into our SHARED .so on arm64.
set_property(TARGET argon2_ref PROPERTY POSITION_INDEPENDENT_CODE ON)

# ─── Our JNI bridge ───────────────────────────────────────────────────────────
#
# Single shared library produced by this module. Loaded from Kotlin via
# System.loadLibrary("encnative"). Anything that crosses the JNI boundary —
# secureZero, Argon2id, future AES-GCM helpers — lives here.
add_library(encnative SHARED
    native-lib.cpp
)

find_library(log-lib log)

target_link_libraries(encnative
    PRIVATE
    argon2_ref
    ${log-lib}
)

# Hardening flags applied at the linker level. Standard defaults for any .so
# that touches secret material.
#
#   -z relro       : mark relocation sections read-only after dynamic linking
#   -z now         : resolve all symbols at load time (full RELRO)
#   -z noexecstack : never mark the stack executable
target_link_options(encnative PRIVATE
    "-Wl,-z,relro"
    "-Wl,-z,now"
    "-Wl,-z,noexecstack"
)

# Strip release builds so reverse-engineering doesn't get the symbol table
# for free. Debug builds keep symbols for ndk-stack / addr2line.
target_compile_options(encnative PRIVATE
    $<$<CONFIG:Release>:-fvisibility=hidden>
)
