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
     * Systems (in display order) that can have a controller skin assigned. GB is included because
     * Delta's `gbc` skins are also usable for the original Game Boy.
     */
    val SUPPORTED_SYSTEMS: List<SystemID> =
        listOf(
            SystemID.NES,
            SystemID.SNES,
            SystemID.N64,
            SystemID.GB,
            SystemID.GBC,
            SystemID.GBA,
            SystemID.GENESIS,
            SystemID.NDS,
        )

    fun systemForGameType(gameTypeIdentifier: String): SystemID? = GAME_TYPE_TO_SYSTEM[gameTypeIdentifier]

    /**
     * True if a skin declaring [gameTypeIdentifier] is compatible with [systemID]. Delta's `gbc`
     * skins are also usable for the original Game Boy.
     */
    fun isCompatible(
        gameTypeIdentifier: String,
        systemID: SystemID,
    ): Boolean {
        val mapped = systemForGameType(gameTypeIdentifier) ?: return false
        if (mapped == systemID) return true
        return mapped == SystemID.GBC && systemID == SystemID.GB
    }
}
