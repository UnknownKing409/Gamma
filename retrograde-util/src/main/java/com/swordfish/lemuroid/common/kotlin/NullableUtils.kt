package com.swordfish.lemuroid.common.kotlin

inline fun <T> T?.filterNullable(predicate: (T) -> Boolean): T? =
    if (this != null && predicate(this)) {
        this
    } else {
        null
    }
