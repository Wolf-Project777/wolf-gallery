package dev.encgallery.crypto

enum class CryptoMode {

    AES_GCM;

    companion object {

        val DEFAULT: CryptoMode = AES_GCM
    }
}
