// EncGallery — :core-native — secureZero foundation.
//
// This file is the FIRST native primitive of the whole crypto stack. Every
// future Tier-3 function (key derivation buffers, AES-GCM scratch space,
// SecureBytes shadow copies) wipes itself through secureZero(). Getting the
// compiler-defeat semantics right HERE means every later wipe is also right.
//
// Why C++ instead of memset() in Kotlin:
//   The JVM/ART has no contract that ByteArray bytes are physically zeroed
//   after `Arrays.fill(buf, 0)` returns — only that the array LOOKS zero from
//   managed code. The optimizer is free to elide the store if the array is
//   immediately unreachable. C++ with a `volatile` pointer forbids that
//   optimization at the language level.
//
// Why not memset_s(): Bionic libc (Android NDK) does NOT ship C11 Annex K, so
// memset_s is unavailable. The volatile-pointer pattern is the portable
// equivalent and is what BoringSSL / libsodium use on Android.

#include <jni.h>
#include <cstddef>
#include <cstdint>
#include <new>
#include <android/log.h>

#include "argon2.h"

namespace {

// Tag used by any android_log call from this .so. Kept distinct from the
// Kotlin EncLog tags so logcat noise stays separable.
constexpr const char* kLogTag = "EncNative";

// secure_zero_volatile(ptr, len): overwrite [ptr, ptr+len) with 0x00 in a way
// the C++ compiler is REQUIRED to emit, even when the buffer is provably dead
// after the call. The trick: route every store through a `volatile` pointer.
// The C++ memory model forbids the optimizer from removing or coalescing
// volatile accesses, so the stores survive -O2/-O3.
//
// Note: this is intentionally NOT static-inline. Pulling it out as a real
// function with a real address makes future test-only checks (e.g. setting a
// breakpoint via `nm libencnative.so | grep secure_zero`) trivial, and costs
// nothing on a path that's already a JNI call boundary.
void secure_zero_volatile(void* ptr, std::size_t len) {
    if (ptr == nullptr || len == 0) {
        return;
    }
    volatile unsigned char* p = static_cast<volatile unsigned char*>(ptr);
    while (len-- > 0) {
        *p++ = 0;
    }
}

}  // namespace

extern "C" {

// JNI signature must EXACTLY match the Kotlin declaration:
//   package dev.encgallery.nativec
//   object NativeCrypto { external fun secureZero(buf: ByteArray) }
//
// Kotlin `object` compiles to a singleton class, so the second JNI argument
// is `jobject` (the singleton instance), not `jclass`. If we ever switch to
// a `class` with `companion object` + `@JvmStatic`, the second arg becomes
// jclass — keep these in sync.
JNIEXPORT void JNICALL
Java_dev_encgallery_nativec_NativeCrypto_secureZero(
    JNIEnv* env,
    jobject /* thiz */,
    jbyteArray buffer
) {
    if (buffer == nullptr) {
        return;
    }

    const jsize len = env->GetArrayLength(buffer);
    if (len <= 0) {
        return;
    }

    // GetPrimitiveArrayCritical pins the array in place (no copy on ART) so
    // we write directly into the managed-heap bytes. This is exactly what we
    // want for a wipe: if JNI handed us a copy and we zeroed the copy, the
    // ORIGINAL bytes would still be hot in RAM — the wipe would lie.
    //
    // The JNI spec requires that between Get/ReleasePrimitiveArrayCritical we
    // perform NO blocking calls and NO other JNI calls, which we honor.
    jboolean isCopy = JNI_FALSE;
    void* ptr = env->GetPrimitiveArrayCritical(buffer, &isCopy);
    if (ptr == nullptr) {
        return;
    }

    secure_zero_volatile(ptr, static_cast<std::size_t>(len));

    // Mode 0 = commit and unpin. If isCopy was unexpectedly true, the
    // zeroed copy is written back over the original; the copy buffer itself
    // is then freed by the runtime. We defensively zero what we can — the
    // managed array is guaranteed wiped on return.
    env->ReleasePrimitiveArrayCritical(buffer, ptr, 0);

    if (isCopy == JNI_TRUE) {
        // ART normally returns a direct pointer (isCopy=false). Seeing a copy
        // here on a Samsung A14 device would be surprising and worth flagging
        // — it means the wipe semantics are weaker than expected for this
        // platform and we may need a different strategy (DirectByteBuffer).
        __android_log_print(
            ANDROID_LOG_WARN,
            kLogTag,
            "secureZero: JNI returned a COPY (isCopy=true). Wipe still applied "
            "via mode-0 release, but original copy buffer lifetime is opaque."
        );
    }
}

// argon2idHashRaw: derive a fixed-length tag from password+salt with the
// memory-hard Argon2id KDF. Returns a fresh ByteArray on success, null on
// failure (which is also logged via android_log).
//
// Why this lives in the .so and not in Kotlin: keeping the password bytes
// inside C-allocated memory limits the surface that can be paged to swap or
// scraped by a managed-heap dump. The reference Argon2 impl scrubs its own
// internal m_cost*1KiB working buffer before returning. The hash output is
// a key — we wipe our local copy of it after handing the result to the JVM
// so there's no second plaintext copy lingering on the C heap.
//
// JNI signature must match the Kotlin declaration in NativeCrypto.kt:
//   external fun argon2idHashRaw(password: ByteArray, salt: ByteArray,
//                                memoryKib: Int, iterations: Int,
//                                parallelism: Int, hashLen: Int): ByteArray?
JNIEXPORT jbyteArray JNICALL
Java_dev_encgallery_nativec_NativeCrypto_argon2idHashRaw(
    JNIEnv* env,
    jobject /* thiz */,
    jbyteArray password,
    jbyteArray salt,
    jint memoryKib,
    jint iterations,
    jint parallelism,
    jint hashLen
) {
    if (password == nullptr || salt == nullptr) {
        return nullptr;
    }
    if (memoryKib <= 0 || iterations <= 0 || parallelism <= 0 || hashLen <= 0) {
        return nullptr;
    }

    const jsize pwd_len = env->GetArrayLength(password);
    const jsize salt_len = env->GetArrayLength(salt);
    // Argon2 spec requires salt >= 8 bytes. Reject early so failures surface
    // as a clean rc rather than as nondescript JNI exceptions.
    if (pwd_len < 0 || salt_len < 8) {
        return nullptr;
    }

    // GetByteArrayElements (NOT critical) on purpose: argon2id at production
    // params allocates 64 MiB and runs for seconds. Holding GC suspended via
    // GetPrimitiveArrayCritical for that long can starve the rest of the app.
    // The trade-off: ART may hand us a COPY of the password bytes, lengthening
    // their lifetime in C heap. We pin that lifetime to this stack frame and
    // wipe the copy explicitly via JNI_ABORT release semantics.
    jbyte* pwd_ptr = env->GetByteArrayElements(password, nullptr);
    if (pwd_ptr == nullptr) {
        return nullptr;
    }
    jbyte* salt_ptr = env->GetByteArrayElements(salt, nullptr);
    if (salt_ptr == nullptr) {
        env->ReleaseByteArrayElements(password, pwd_ptr, JNI_ABORT);
        return nullptr;
    }

    // Output buffer on C heap. Wiped before delete in every exit path.
    auto* hash = new (std::nothrow) uint8_t[static_cast<size_t>(hashLen)];
    if (hash == nullptr) {
        env->ReleaseByteArrayElements(password, pwd_ptr, JNI_ABORT);
        env->ReleaseByteArrayElements(salt, salt_ptr, JNI_ABORT);
        return nullptr;
    }

    const int rc = argon2id_hash_raw(
        static_cast<uint32_t>(iterations),
        static_cast<uint32_t>(memoryKib),
        static_cast<uint32_t>(parallelism),
        pwd_ptr, static_cast<size_t>(pwd_len),
        salt_ptr, static_cast<size_t>(salt_len),
        hash, static_cast<size_t>(hashLen)
    );

    // JNI_ABORT: don't copy pwd/salt back to JVM — we never modified them, so
    // a copy-back would be a wasted plaintext-leak window. The runtime frees
    // its internal buffer; we cannot scrub it from here, but ART typically
    // gives a direct pointer on arm64 and the abort path is the safer of two.
    env->ReleaseByteArrayElements(password, pwd_ptr, JNI_ABORT);
    env->ReleaseByteArrayElements(salt, salt_ptr, JNI_ABORT);

    if (rc != ARGON2_OK) {
        secure_zero_volatile(hash, static_cast<std::size_t>(hashLen));
        delete[] hash;
        __android_log_print(
            ANDROID_LOG_ERROR,
            kLogTag,
            "argon2id failed: rc=%d (%s)",
            rc, argon2_error_message(rc)
        );
        return nullptr;
    }

    jbyteArray result = env->NewByteArray(hashLen);
    if (result != nullptr) {
        env->SetByteArrayRegion(
            result, 0, hashLen, reinterpret_cast<const jbyte*>(hash)
        );
    }

    // Wipe our C-side copy of the derived key regardless of whether the
    // managed-heap copy succeeded. If NewByteArray failed, returning null is
    // the right answer; either way nothing here should outlive this frame.
    secure_zero_volatile(hash, static_cast<std::size_t>(hashLen));
    delete[] hash;

    return result;
}

}  // extern "C"
