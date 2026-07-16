package com.swordfish.lemuroid.common.kotlin

fun <K, V> Map<K, V>.reverseLookup(): Map<V, K> = entries.associateBy({ it.value }, { it.key })
