package dev.encgallery.crypto

enum class LockMethod {

    PIN,

    PASSWORD;

    companion object {

        val DEFAULT: LockMethod = PASSWORD
    }
}
