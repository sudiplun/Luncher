package app.olaunchercf.ui

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Space
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import app.olaunchercf.BuildConfig
import app.olaunchercf.MainActivity
import app.olaunchercf.MainViewModel
import app.olaunchercf.R
import app.olaunchercf.data.Constants
import app.olaunchercf.data.Constants.Action
import app.olaunchercf.data.Constants.AppDrawerFlag
import app.olaunchercf.data.Constants.Theme.Dark
import app.olaunchercf.data.Constants.Theme.Light
import app.olaunchercf.data.Constants.Theme.System
import app.olaunchercf.data.EnumOption
import app.olaunchercf.data.Prefs
import app.olaunchercf.databinding.FragmentSettingsBinding
import app.olaunchercf.helper.hideStatusBar
import app.olaunchercf.helper.isOlauncherDefault
import app.olaunchercf.helper.loadFile
import app.olaunchercf.helper.openAppInfo
import app.olaunchercf.helper.resetDefaultLauncher
import app.olaunchercf.helper.showStatusBar
import app.olaunchercf.helper.storeFile
import app.olaunchercf.listener.DeviceAdmin
import kotlin.math.roundToInt

class SettingsFragment : Fragment() {

    private lateinit var prefs: Prefs
    private lateinit var viewModel: MainViewModel
    private lateinit var deviceManager: DevicePolicyManager
    private lateinit var componentName: ComponentName

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val offset = 5
    private var activeSelectorClose: (() -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        prefs = Prefs(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]
        viewModel.isOlauncherDefault()

        deviceManager = requireContext().getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        componentName = ComponentName(requireContext(), DeviceAdmin::class.java)
        checkAdminPermission()

        buildSettingsUi()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        activeSelectorClose = null
        _binding = null
    }

    private fun buildSettingsUi() {
        val ctx = requireContext()
        val container = binding.settingsContainer
        container.removeAllViews()

        val isDark = when (prefs.appTheme) {
            Light -> false
            Dark -> true
            System -> (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        }
        val textColor = if (isDark) Color.WHITE else Color.BLACK
        val disabledColor = 0xFF858585.toInt()
        val cardBgColor = ContextCompat.getColor(ctx, if (isDark) R.color.blackTrans50 else R.color.blackInverseTrans50)
        val selectorBgColor = ContextCompat.getColor(ctx, if (isDark) R.color.blackTrans80 else R.color.blackInverseTrans80)
        val borderColor = ContextCompat.getColor(ctx, R.color.blackInverseTrans50)
        val fontSizeSp = (prefs.textSize - offset).toFloat()
        val sansLight = Typeface.create("sans-serif-light", Typeface.NORMAL)
        val sansBold = Typeface.create("sans-serif", Typeface.BOLD)

        fun dp(value: Float): Int = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, value, resources.displayMetrics
        ).roundToInt()

        fun roundedBackground(fillColor: Int, strokeWidthPx: Int = 0): GradientDrawable {
            return GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(10f).toFloat()
                setColor(fillColor)
                if (strokeWidthPx > 0) {
                    setStroke(strokeWidthPx, borderColor)
                }
            }
        }

        fun closeOpenSelector() {
            activeSelectorClose?.invoke()
            activeSelectorClose = null
        }

        fun createCard(): LinearLayout {
            val card = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                background = roundedBackground(cardBgColor, dp(0.5f).coerceAtLeast(1))
                setPadding(dp(20f), dp(20f), dp(20f), dp(20f))
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(dp(12f), dp(12f), dp(12f), 0)
                }
            }
            container.addView(card)
            return card
        }

        fun addSectionTitle(card: LinearLayout, title: String) {
            val tv = TextView(ctx).apply {
                text = title
                setTextColor(textColor)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 32f)
                setPadding(0, 0, 0, dp(12f))
            }
            card.addView(tv)
        }

        fun createButtonText(label: String, bold: Boolean = true, color: Int = textColor, onClick: () -> Unit): TextView {
            return TextView(ctx).apply {
                text = label
                setTextColor(color)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSizeSp)
                typeface = if (bold) sansBold else sansLight
                gravity = Gravity.CENTER
                minWidth = dp(64f)
                minHeight = dp(36f)
                setPadding(dp(12f), dp(8f), dp(12f), dp(8f))
                setOnClickListener { onClick() }
            }
        }

        fun addSettingRow(
            parent: LinearLayout,
            title: String,
            buttonText: String,
            active: Boolean = true,
            onClick: () -> Unit
        ): TextView {
            val row = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
            val titleView = TextView(ctx).apply {
                text = title
                setTextColor(textColor)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSizeSp)
                typeface = sansLight
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }
            val buttonView = createButtonText(
                label = buttonText,
                bold = true,
                color = if (active) textColor else disabledColor,
                onClick = onClick
            )
            row.addView(titleView)
            row.addView(buttonView)
            parent.addView(row)
            return buttonView
        }

        fun addToggle(card: LinearLayout, title: String, initial: Boolean, onToggle: () -> Boolean) {
            var state = initial
            lateinit var btn: TextView
            btn = addSettingRow(
                parent = card,
                title = title,
                buttonText = getString(if (state) R.string.on else R.string.off),
                onClick = {
                    closeOpenSelector()
                    state = onToggle()
                    btn.text = getString(if (state) R.string.on else R.string.off)
                }
            )
        }

        fun <T : EnumOption> addEnumItem(
            card: LinearLayout,
            title: String,
            initial: T,
            values: Array<T>,
            activeCheck: (T) -> Boolean = { true },
            displayText: (T) -> String = { it.string(ctx) },
            onSelect: (T) -> Unit
        ) {
            var current = initial
            val slot = FrameLayout(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
            card.addView(slot)

            val rowHolder = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
            val selectorScroll = HorizontalScrollView(ctx).apply {
                visibility = View.GONE
                isHorizontalScrollBarEnabled = false
                background = roundedBackground(selectorBgColor)
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
            val selectorRow = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.END
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
            selectorScroll.addView(selectorRow)
            slot.addView(rowHolder)
            slot.addView(selectorScroll)

            lateinit var btn: TextView
            fun close() {
                selectorScroll.visibility = View.GONE
                rowHolder.visibility = View.VISIBLE
            }

            btn = addSettingRow(
                parent = rowHolder,
                title = title,
                buttonText = displayText(current),
                active = activeCheck(current),
                onClick = {
                    closeOpenSelector()
                    rowHolder.visibility = View.GONE
                    selectorScroll.visibility = View.VISIBLE
                    activeSelectorClose = ::close
                }
            )

            for (opt in values) {
                selectorRow.addView(createButtonText(opt.string(ctx)) {
                    closeOpenSelector()
                    current = opt
                    onSelect(opt)
                    btn.text = displayText(opt)
                    btn.setTextColor(if (activeCheck(opt)) textColor else disabledColor)
                })
            }
        }

        fun addNumberItem(
            card: LinearLayout,
            title: String,
            initial: Int,
            min: Int,
            max: Int,
            onSelect: (Int) -> Unit
        ) {
            var committed = initial
            var pending = initial
            val slot = FrameLayout(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
            card.addView(slot)

            val rowHolder = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
            val selectorRow = LinearLayout(ctx).apply {
                visibility = View.GONE
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = roundedBackground(selectorBgColor)
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
            slot.addView(rowHolder)
            slot.addView(selectorRow)

            lateinit var btn: TextView
            lateinit var valueLabel: TextView
            fun close() {
                selectorRow.visibility = View.GONE
                rowHolder.visibility = View.VISIBLE
            }

            btn = addSettingRow(
                parent = rowHolder,
                title = title,
                buttonText = committed.toString(),
                onClick = {
                    closeOpenSelector()
                    pending = committed
                    valueLabel.text = pending.toString()
                    rowHolder.visibility = View.GONE
                    selectorRow.visibility = View.VISIBLE
                    activeSelectorClose = ::close
                }
            )

            val plusBtn = createButtonText("+") {
                if (pending < max) {
                    pending += 1
                    valueLabel.text = pending.toString()
                }
            }
            valueLabel = TextView(ctx).apply {
                text = pending.toString()
                setTextColor(textColor)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSizeSp)
                typeface = sansLight
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }
            val minusBtn = createButtonText("-") {
                if (pending > min) {
                    pending -= 1
                    valueLabel.text = pending.toString()
                }
            }
            val saveBtn = createButtonText(getString(R.string.save)) {
                closeOpenSelector()
                committed = pending
                btn.text = committed.toString()
                onSelect(committed)
            }

            selectorRow.addView(plusBtn)
            selectorRow.addView(valueLabel)
            selectorRow.addView(minusBtn)
            selectorRow.addView(saveBtn)
        }

        // Top card
        val topCard = createCard()
        val topHeader = FrameLayout(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        val topTitle = TextView(ctx).apply {
            text = getString(R.string.app_name)
            setTextColor(textColor)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 32f)
            setPadding(0, 0, 0, dp(12f))
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER_VERTICAL or Gravity.START
            )
        }
        val infoIcon = ImageView(ctx).apply {
            setImageResource(R.drawable.ic_outline_info_24)
            layoutParams = FrameLayout.LayoutParams(dp(16f), dp(16f), Gravity.TOP or Gravity.END)
            setOnClickListener {
                openAppInfo(ctx, android.os.Process.myUserHandle(), BuildConfig.APPLICATION_ID)
            }
        }
        topHeader.addView(topTitle)
        topHeader.addView(infoIcon)
        topCard.addView(topHeader)

        val changeLauncherText = if (isOlauncherDefault(ctx)) {
            R.string.change_default_launcher
        } else {
            R.string.set_as_default_launcher
        }
        topCard.addView(createButtonText(getString(R.string.hidden_apps), bold = false) {
            showHiddenApps()
        }.apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        })
        topCard.addView(createButtonText(getString(changeLauncherText), bold = false) {
            resetDefaultLauncher(ctx)
        }.apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        })

        // Appearance
        val appearanceCard = createCard()
        addSectionTitle(appearanceCard, getString(R.string.appearance))
        addToggle(appearanceCard, getString(R.string.status_bar), prefs.showStatusBar) {
            toggleStatusBar()
            prefs.showStatusBar
        }
        addEnumItem(
            appearanceCard,
            getString(R.string.theme_mode),
            prefs.appTheme,
            arrayOf(System, Light, Dark)
        ) { setTheme(it) }
        addEnumItem(
            appearanceCard,
            getString(R.string.app_language),
            prefs.language,
            Constants.Language.values()
        ) { setLang(it) }
        addNumberItem(
            appearanceCard,
            getString(R.string.app_text_size),
            prefs.textSize,
            Constants.TEXT_SIZE_MIN,
            Constants.TEXT_SIZE_MAX
        ) { setTextSize(it) }

        // Behavior
        val behaviorCard = createCard()
        addSectionTitle(behaviorCard, getString(R.string.behavior))
        addToggle(behaviorCard, getString(R.string.auto_show_keyboard), prefs.autoShowKeyboard) {
            toggleKeyboardText()
            prefs.autoShowKeyboard
        }
        addToggle(behaviorCard, getString(R.string.auto_open_apps), prefs.autoOpenApp) {
            toggleAutoOpenApp()
            prefs.autoOpenApp
        }

        // Homescreen
        val homescreenCard = createCard()
        addSectionTitle(homescreenCard, getString(R.string.homescreen))
        addNumberItem(
            homescreenCard,
            getString(R.string.apps_on_home_screen),
            prefs.homeAppsNum,
            0,
            Constants.MAX_HOME_APPS
        ) { updateHomeAppsNum(it) }
        addToggle(homescreenCard, getString(R.string.show_time), prefs.showTime) {
            toggleShowTime()
            prefs.showTime
        }
        addEnumItem(
            homescreenCard,
            getString(R.string.show_date),
            prefs.dateMode,
            Constants.DateMode.values(),
            activeCheck = { it != Constants.DateMode.Off }
        ) { setDateMode(it) }
        addToggle(homescreenCard, getString(R.string.lock_home_apps), prefs.homeLocked) {
            prefs.homeLocked = !prefs.homeLocked
            prefs.homeLocked
        }
        addToggle(homescreenCard, getString(R.string.extend_home_apps_area), prefs.extendHomeAppsArea) {
            prefs.extendHomeAppsArea = !prefs.extendHomeAppsArea
            prefs.extendHomeAppsArea
        }

        // Alignment
        val alignmentCard = createCard()
        addSectionTitle(alignmentCard, getString(R.string.alignment))
        val gravities = arrayOf(Constants.Gravity.Left, Constants.Gravity.Center, Constants.Gravity.Right)
        addEnumItem(alignmentCard, getString(R.string.home_alignment), prefs.homeAlignment, gravities) {
            setHomeAlignment(it)
        }
        addToggle(alignmentCard, getString(R.string.home_alignment_bottom), prefs.homeAlignmentBottom) {
            toggleHomeAppsBottom()
            prefs.homeAlignmentBottom
        }
        addEnumItem(alignmentCard, getString(R.string.clock_alignment), prefs.clockAlignment, gravities) {
            setClockAlignment(it)
        }
        addEnumItem(alignmentCard, getString(R.string.drawer_alignment), prefs.drawerAlignment, gravities) {
            viewModel.updateDrawerAlignment(it)
        }

        // Gestures
        val gesturesCard = createCard()
        addSectionTitle(gesturesCard, getString(R.string.gestures))
        fun addGestureItem(titleRes: Int, current: Action, appLabel: String, flag: AppDrawerFlag) {
            addEnumItem(
                gesturesCard,
                getString(titleRes),
                current,
                Action.values(),
                activeCheck = { it != Action.Disabled },
                displayText = { action ->
                    if (action == Action.OpenApp) "Open $appLabel" else action.string(ctx)
                }
            ) { updateGesture(flag, it) }
        }
        addGestureItem(R.string.swipe_left_app, prefs.swipeLeftAction, prefs.appSwipeLeft.appLabel.ifEmpty { "Camera" }, AppDrawerFlag.SetSwipeLeft)
        addGestureItem(R.string.swipe_right_app, prefs.swipeRightAction, prefs.appSwipeRight.appLabel.ifEmpty { "Phone" }, AppDrawerFlag.SetSwipeRight)
        addGestureItem(R.string.swipe_up_app, prefs.swipeUpAction, prefs.appSwipeUp.appLabel, AppDrawerFlag.SetSwipeUp)
        addGestureItem(R.string.swipe_down_app, prefs.swipeDownAction, prefs.appSwipeDown.appLabel, AppDrawerFlag.SetSwipeDown)
        addGestureItem(R.string.clock_click_app, prefs.clickClockAction, prefs.appClickClock.appLabel.ifEmpty { "Clock" }, AppDrawerFlag.SetClickClock)
        addGestureItem(R.string.date_click_app, prefs.clickDateAction, prefs.appClickDate.appLabel.ifEmpty { "Calendar" }, AppDrawerFlag.SetClickDate)
        addGestureItem(R.string.double_tap, prefs.doubleTapAction, prefs.appDoubleTap.appLabel, AppDrawerFlag.SetDoubleTap)

        // Backup
        val backupCard = createCard()
        addSectionTitle(backupCard, getString(R.string.backup))
        val backupRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        backupRow.addView(Space(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(0, 0, 1f)
        })
        backupRow.addView(createButtonText(getString(R.string.load_backup)) { loadFile(requireActivity()) })
        backupRow.addView(createButtonText(getString(R.string.store_backup)) { storeFile(requireActivity()) })
        backupCard.addView(backupRow)

        // Version footer
        val versionName = ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName
        val versionView = TextView(ctx).apply {
            text = "Version: $versionName"
            setTextColor(Color.DKGRAY)
            setPadding(dp(10f), dp(5f), dp(10f), dp(5f))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.END
            }
        }
        container.addView(versionView)
    }

    private fun setHomeAlignment(gravity: Constants.Gravity) {
        prefs.homeAlignment = gravity
        viewModel.updateHomeAppsAlignment(gravity, prefs.homeAlignmentBottom)
    }

    private fun toggleHomeAppsBottom() {
        val onBottom = !prefs.homeAlignmentBottom
        prefs.homeAlignmentBottom = onBottom
        viewModel.updateHomeAppsAlignment(prefs.homeAlignment, onBottom)
    }

    private fun setClockAlignment(gravity: Constants.Gravity) {
        prefs.clockAlignment = gravity
        viewModel.updateClockAlignment(gravity)
    }

    private fun toggleStatusBar() {
        val showStatusbar = !prefs.showStatusBar
        prefs.showStatusBar = showStatusbar
        if (showStatusbar) showStatusBar(requireActivity()) else hideStatusBar(requireActivity())
    }

    private fun setDateMode(mode: Constants.DateMode) {
        prefs.dateMode = mode
        viewModel.setDateMode(mode)
    }

    private fun toggleShowTime() {
        prefs.showTime = !prefs.showTime
        viewModel.setShowTime(prefs.showTime)
    }

    private fun showHiddenApps() {
        viewModel.getHiddenApps()
        (activity as? MainActivity)?.openAppDrawer(
            bundleOf("flag" to AppDrawerFlag.HiddenApps.toString())
        )
    }

    private fun checkAdminPermission() {
        val isAdmin: Boolean = deviceManager.isAdminActive(componentName)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P)
            prefs.lockModeOn = isAdmin
    }

    private fun updateHomeAppsNum(homeAppsNum: Int) {
        prefs.homeAppsNum = homeAppsNum
        viewModel.homeAppsCount.value = homeAppsNum
    }

    private fun toggleKeyboardText() {
        prefs.autoShowKeyboard = !prefs.autoShowKeyboard
    }

    private fun toggleAutoOpenApp() {
        prefs.autoOpenApp = !prefs.autoOpenApp
    }

    private fun setTheme(appTheme: Constants.Theme) {
        prefs.appTheme = appTheme
        requireActivity().recreate()
    }

    private fun setLang(lang: Constants.Language) {
        prefs.language = lang
        requireActivity().recreate()
    }

    private fun setTextSize(size: Int) {
        prefs.textSize = size
    }

    private fun updateGesture(flag: AppDrawerFlag, action: Action) {
        when (flag) {
            AppDrawerFlag.SetSwipeLeft -> prefs.swipeLeftAction = action
            AppDrawerFlag.SetSwipeRight -> prefs.swipeRightAction = action
            AppDrawerFlag.SetSwipeUp -> prefs.swipeUpAction = action
            AppDrawerFlag.SetSwipeDown -> prefs.swipeDownAction = action
            AppDrawerFlag.SetClickClock -> prefs.clickClockAction = action
            AppDrawerFlag.SetClickDate -> prefs.clickDateAction = action
            AppDrawerFlag.SetDoubleTap -> prefs.doubleTapAction = action
            AppDrawerFlag.SetHomeApp,
            AppDrawerFlag.HiddenApps,
            AppDrawerFlag.LaunchApp -> {}
        }

        if (action == Action.OpenApp) {
            viewModel.getAppList()
            (activity as? MainActivity)?.openAppDrawer(
                bundleOf("flag" to flag.toString())
            )
        }
    }
}
