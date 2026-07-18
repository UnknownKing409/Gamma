package com.swordfish.lemuroid.lib.library.skin

import com.swordfish.lemuroid.lib.library.SystemID

/**
 * Maps between Delta `gameTypeIdentifier` strings and Lemuroid [SystemID]s. Only the systems that
 * Delta defines skins for are represented; these are the systems that expose a controller-skin slot.
 */
object DeltaSkinSystemMapping {
    // Delta gameTypeIdentifier -> Lemuroid SystemID.
    private val GAME_TYPE_TO_SYSTEM: Map<String, SystemID> =
        mapOf(
            "com.rileytestut.delta.game.gbc" to SystemID.GBC,
            "com.rileytestut.delta.game.gba" to SystemID.GBA,
            "com.rileytestut.delta.game.ds" to SystemID.NDS,
            "com.rileytestut.delta.game.nes" to SystemID.NES,
            "com.rileytestut.delta.game.snes" to SystemID.SNES,
            "com.rileytestut.delta.game.n64" to SystemID.N64,
            "com.rileytestut.delta.game.genesis" to SystemID.GENESIS,
        )

    /**
     * Systems (in display order) that can have a controller skin assigned. Game Boy and Game Boy
     * Color share a single merged "Game Boy" slot (represented by [SystemID.GB]), so GBC does not
     * appear separately here.
     */
    val SUPPORTED_SYSTEMS: List<SystemID> =
        listOf(
            SystemID.NES,
            SystemID.SNES,
            SystemID.N64,
            SystemID.GB,
            SystemID.GBA,
            SystemID.GENESIS,
            SystemID.NDS,
        )

    fun systemForGameType(gameTypeIdentifier: String): SystemID? = GAME_TYPE_TO_SYSTEM[gameTypeIdentifier]

    /**
     * The system whose skin slot [systemID] shares. Game Boy and Game Boy Color use a single merged
     * "Game Boy" slot, so GBC canonicalises to GB; every other system maps to itself. Use this
     * whenever a system needs to be resolved to its skin slot (preference key, compatibility).
     */
    fun canonicalSkinSystem(systemID: SystemID): SystemID =
        when (systemID) {
            SystemID.GBC -> SystemID.GB
            else -> systemID
        }

    /**
     * True if a skin declaring [gameTypeIdentifier] is compatible with [systemID]. Comparison is
     * done on the canonical skin slot, so Delta's `gbc` skins are usable for the original Game Boy
     * and vice versa.
     */
    fun isCompatible(
        gameTypeIdentifier: String,
        systemID: SystemID,
    ): Boolean {
        val mapped = systemForGameType(gameTypeIdentifier) ?: return false
        return canonicalSkinSystem(mapped) == canonicalSkinSystem(systemID)
    }
}
