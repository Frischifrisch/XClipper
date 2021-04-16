package com.kpstv.xclipper.ui.fragments

import android.content.Context
import android.graphics.Rect
import android.os.Bundle
import android.os.Parcelable
import android.view.View
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.kpstv.navigation.*
import com.kpstv.xclipper.R
import com.kpstv.xclipper.databinding.ActivitySettingsBinding
import com.kpstv.xclipper.extensions.applyTopInsets
import com.kpstv.xclipper.extensions.drawableFrom
import com.kpstv.xclipper.extensions.utils.ThemeUtils.Companion.registerForThemeChange
import com.kpstv.xclipper.extensions.viewBinding
import com.kpstv.xclipper.ui.activities.NavViewModel
import com.kpstv.xclipper.ui.activities.Start
import com.kpstv.xclipper.ui.fragments.settings.*
import com.kpstv.xclipper.ui.viewmodels.SettingNavViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.parcel.Parcelize
import kotlin.reflect.KClass

@AndroidEntryPoint
class Settings : ValueFragment(R.layout.activity_settings), NavigatorTransmitter, LookFeelPreference.ThemeChangeCallbacks {
    private val binding by viewBinding(ActivitySettingsBinding::bind)
    private val viewModel by viewModels<SettingNavViewModel>()
    private val navViewModel by activityViewModels<NavViewModel>()
    private lateinit var navigator: Navigator

    override fun getNavigator(): Navigator = navigator

    override fun onAttach(context: Context) {
        super.onAttach(context)
        registerForThemeChange()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navigator = Navigator(childFragmentManager, binding.settingsContainer)

        setToolbar()
        viewModel.navigation.observe(viewLifecycleOwner, navigationObserver)

        if (savedInstanceState == null) {
            navigator.navigateTo(Navigator.NavOptions(
                clazz = Screen.MAIN.clazz,
                animation = AnimationDefinition.SlideInRight()
            ))
        }

        if (hasKeyArgs()) {
            manageArguments()
        }
    }

    private val navigationObserver = Observer { options: Navigator.NavOptions? ->
        options?.let { opt ->
            navigator.navigateTo(opt)
            binding.toolbar.title = getString(Screen.getTitle(opt.clazz))
        }
    }

    private fun setToolbar() {
        binding.toolbar.applyTopInsets()
        binding.toolbar.navigationIcon = drawableFrom(R.drawable.ic_arrow_back)
        binding.toolbar.setNavigationOnClickListener { goBack() }
    }

    private fun manageArguments() {
        val keys = getKeyArgs<Args>()
        if (keys.openLookFeel) {
            viewModel.navigateTo(Screen.LOOK_FEEL, animation = AnimationDefinition.None)
        }
    }

    override fun onThemeChanged(viewRect: Rect) {
        navViewModel.navigateTo(
            screen = Start.Screen.SETTING,
            args = Args(openLookFeel = true),
            animation = AnimationDefinition.CircularReveal(
                forFragment = LookFeelPreference::class,
                fromTarget = viewRect
            )
        )
    }

    override fun onBackPressed(): Boolean {
        binding.toolbar.title = getString(Screen.MAIN.title)
        if (navigator.canFinish()) {
            return super.onBackPressed()
        }
        return true
    }

    enum class Screen(val clazz: KClass<out Fragment>, @StringRes val title: Int) {
        MAIN(SettingsFragment::class, R.string.settings),
        GENERAL(GeneralPreference::class, R.string.service),
        ACCOUNT(AccountPreference::class, R.string.account),
        LOOK_FEEL(LookFeelPreference::class, R.string.look_feel),
        BACKUP(BackupPreference::class, R.string.backup),
        UPGRADE(Upgrade::class, R.string.upgrade),
        ABOUT(AboutPreference::class, R.string.about);

        companion object {
            fun getTitle(clazz: KClass<out Fragment>): Int {
                return values().firstOrNull { it.clazz == clazz }?.title ?: MAIN.title
            }
        }
    }

    @Parcelize
    data class Args(val openLookFeel: Boolean = false): BaseArgs(), Parcelable
}