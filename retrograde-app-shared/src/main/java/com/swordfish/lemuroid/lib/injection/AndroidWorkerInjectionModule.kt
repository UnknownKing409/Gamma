package com.swordfish.lemuroid.lib.injection

import dagger.Module
import dagger.android.AndroidInjector
import dagger.multibindings.Multibinds

private typealias WorkerFactory = AndroidInjector.Factory<*>

@Module
abstract class AndroidWorkerInjectionModule {
    @Multibinds
    abstract fun workerInjectorFactories(): Map<Class<*>, WorkerFactory>

    @Multibinds
    abstract fun workerInjectorFactoriesWithStringKeys(): Map<String, WorkerFactory>
}
