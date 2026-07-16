package com.swordfish.lemuroid.common.kotlin

fun <T> lazySequenceOf(vararg producers: () -> T): Sequence<T> =
    producers
        .asSequence()
        .map { it() }
