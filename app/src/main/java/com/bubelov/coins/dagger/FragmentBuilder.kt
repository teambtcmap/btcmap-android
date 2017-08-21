package com.bubelov.coins.dagger

import com.bubelov.coins.ui.fragment.*
import dagger.Module
import dagger.android.ContributesAndroidInjector

/**
 * @author Igor Bubelov
 */

@Module
abstract class FragmentBuilder {
    @ContributesAndroidInjector(modules = arrayOf(SignInFragmentModule::class))
    abstract fun contributeSignInFragmentInjector(): SignInFragment

    @ContributesAndroidInjector(modules = arrayOf(SignUpFragmentModule::class))
    abstract fun contributeSignUpFragmentInjector(): SignUpFragment

    @ContributesAndroidInjector(modules = arrayOf(SettingsFragmentModule::class))
    abstract fun contributeSettingsFragmentInjector(): SettingsFragment
}