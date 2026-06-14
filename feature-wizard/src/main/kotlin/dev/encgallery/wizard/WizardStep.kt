package dev.encgallery.wizard

enum class WizardStep {
    WELCOME,
    CRYPTO_CHOICE,
    LOCK_METHOD,
    SET_AND_CONFIRM,
    DONE;

    val number: Int get() = ordinal + 1

    val total: Int get() = entries.size

    fun next(): WizardStep? = entries.getOrNull(ordinal + 1)
    fun prev(): WizardStep? = entries.getOrNull(ordinal - 1)
}
