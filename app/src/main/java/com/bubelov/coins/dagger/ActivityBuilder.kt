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
    abstract fun bindMainActivity(): MainActivity

    @ContributesAndroidInjector(modules = arrayOf(ProfileActivityModule::class))
    abstract fun bindProfileActivity(): ProfileActivity

    @ContributesAndroidInjector(modules = arrayOf(SignInActivityModule::class))
    abstract fun bindSignInActivity(): SignInActivity

    @ContributesAndroidInjector(modules = arrayOf(EditPlaceActivityModule::class))
    abstract fun bindEditPlaceActivity(): EditPlaceActivity
}