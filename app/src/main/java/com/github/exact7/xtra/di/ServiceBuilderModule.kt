package com.github.exact7.xtra.di

import com.github.exact7.xtra.service.ClipDownloadService
import com.github.exact7.xtra.service.DownloadService
import dagger.Module
import dagger.android.ContributesAndroidInjector

@Module
abstract class ServiceBuilderModule {

    @ContributesAndroidInjector
    abstract fun contributeVideoDownloadService(): DownloadService

    @ContributesAndroidInjector
    abstract fun contributeClipDownloadService(): ClipDownloadService
}
