package com.bubelov.coins.dagger

import com.bubelov.coins.ui.activity.*

import dagger.Module
import dagger.android.ContributesAndroidInjector

/**
 * @author Igor Bubelov
 */

@Module
abstract class ActivityBuilder {
    @ContributesAndroidInjector(modules = arrayOf(MainActivityModule::class))
    abstract fun contributeMainActivityInjector(): MainActivity

    @ContributesAndroidInjector(modules = arrayOf(ProfileActivityModule::class))
    abstract fun contributeProfileActivityInjector(): ProfileActivity

    @ContributesAndroidInjector(modules = arrayOf(SignInActivityModule::class))
    abstract fun conributeSignInActivityInjector(): SignInActivity

    @ContributesAndroidInjector(modules = arrayOf(EditPlaceActivityModule::class))
    abstract fun contributeEditPlaceActivityInjector(): EditPlaceActivity

    @ContributesAndroidInjector(modules = arrayOf(SettingsActivityModule::class))
    abstract fun contributeSettingsActivityInjector(): SettingsActivity

    @ContributesAndroidInjector(modules = arrayOf(EmailSignInActivityModule::class))
    abstract fun contributeEmailSignInActivityInjector(): EmailSignInActivity
}