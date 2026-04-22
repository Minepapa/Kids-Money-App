package com.minepapa.kidsmoneyapp

data class Achievement(
    val id: String,
    val emoji: String,
    val titleKo: String,
    val descriptionKo: String,
    val unlockedDate: String? = null,
    val seen: Boolean = false
) {
    val isUnlocked: Boolean get() = unlockedDate != null
}
