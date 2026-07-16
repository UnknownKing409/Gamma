package com.swordfish.lemuroid.app.shared.input.inputclass

import com.swordfish.lemuroid.app.shared.input.InputKey

object InputClassUnknown : InputClass {
    override fun getInputKeys(): Set<InputKey> = emptySet()

    override fun getAxesMap(): Map<Int, Int> = emptyMap()
}
