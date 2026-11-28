package app.olaunchercf.data

import android.annotation.SuppressLint
import android.content.Context
import app.olaunchercf.R
import java.util.*

interface EnumOption {
    fun string(context: Context): String
}

object Constants {

    /*const val FLAG_LAUNCH_APP = 100
    const val FLAG_HIDDEN_APPS = 101

    const val FLAG_SET_HOME_APP = 1

    const val FLAG_SET_SWIPE_LEFT_APP = 11
    const val FLAG_SET_SWIPE_RIGHT_APP = 12

    const val FLAG_SET_CLICK_CLOCK_APP = 13
    const val FLAG_SET_CLICK_DATE_APP = 14*/

    const val REQUEST_CODE_ENABLE_ADMIN = 666

    const val TRIPLE_TAP_DELAY_MS = 300
    const val LONG_PRESS_DELAY_MS = 500

    const val MAX_HOME_APPS = 15
    const val TEXT_SIZE_MIN = 10
    const val TEXT_SIZE_MAX = 60

    const val BACKUP_WRITE = 1
    const val BACKUP_READ = 2

    enum class AppDrawerFlag {
        LaunchApp,
        HiddenApps,
        SetHomeApp,
        SetSwipeLeft,
        SetSwipeRight,
        SetSwipeUp,
        SetSwipeDown,
        SetClickClock,
        SetClickDate,
        SetDoubleTap,
    }

    enum class Language: EnumOption {
        System,
        Arabic,
        Chinese,
        Croatian,
        Dutch,
        English,
        Estonian,
        French,
        German,
        Greek,
        Indonesian,
        Italian,
        Japanese,
        Korean,
        Lithuanian,
        Nepali,
        Persian,
        Portuguese,
        Russian,
        Spanish,
        Swedish,
        Thai,
        Turkish,
        Vietnamese;

        override fun string(context: Context): String {
            return when(this) {
                System -> context.getString(R.string.lang_system)
                Arabic -> "العربية"
                Chinese -> "中文"
                Croatian -> "Hrvatski"
                Dutch -> "Nederlands"
                English -> "English"
                Estonian -> "Eesti keel"
                French -> "Français"
                German -> "Deutsch"
                Greek -> "Ελληνική"
                Indonesian -> "Bahasa Indonesia"
                Italian -> "Italiano"
                Japanese -> "日本"
                Korean -> "한국어"
                Lithuanian -> "Lietuvių"
                Nepali -> "नेपाली"
                Persian -> "فارسی"
                Portuguese -> "Português"
                Russian -> "Русский"
                Spanish -> "Español"
                Swedish -> "Svenska"
                Thai -> "ไทย"
                Turkish -> "Türkçe"
                Vietnamese -> "Tiếng Việt"
            }
        }
    }

    fun Language.value(): String {
        return when(this) {
            Language.System -> Locale.getDefault().language
            Language.Arabic -> "ar"
            Language.Chinese -> "cn"
            Language.Croatian -> "hr"
            Language.Dutch -> "nl"
            Language.English -> "en"
            Language.Estonian -> "et"
            Language.French -> "fr"
            Language.German -> "de"
            Language.Greek -> "gr"
            Language.Indonesian -> "id"
            Language.Italian -> "it"
            Language.Japanese -> "ja"
            Language.Korean -> "ko"
            Language.Lithuanian -> "lt"
            Language.Nepali -> "ne"
            Language.Persian -> "fa"
            Language.Portuguese -> "pt"
            Language.Russian -> "ru"
            Language.Spanish -> "es"
            Language.Swedish -> "se"
            Language.Thai -> "th"
            Language.Turkish -> "tr"
            Language.Vietnamese -> "vi"
        }
    }

    enum class Gravity: EnumOption {
        Left,
        Center,
        Right;

        override fun string(context: Context): String {
            return when(this) {
                Left -> context.getString(R.string.left)
                Center -> context.getString(R.string.center)
                Right -> context.getString(R.string.right)
            }
        }

        @SuppressLint("RtlHardcoded")
        fun value(): Int {
            return when(this) {
                Left -> android.view.Gravity.LEFT
                Center -> android.view.Gravity.CENTER
                Right -> android.view.Gravity.RIGHT
            }
        }
    }

    enum class Action: EnumOption {
        Disabled,
        OpenApp,
        LockScreen,
        ShowAppList,
        OpenQuickSettings,
        ShowRecents,
        ShowNotification;

        override fun string(context: Context): String {
            return when(this) {
                OpenApp -> context.getString(R.string.open_app)
                LockScreen -> context.getString(R.string.lock_screen)
                ShowNotification -> context.getString(R.string.show_notifications)
                ShowAppList -> context.getString(R.string.show_app_list)
                OpenQuickSettings -> context.getString(R.string.open_quick_settings)
                ShowRecents -> context.getString(R.string.show_recents)
                Disabled -> context.getString(R.string.disabled)
            }
        }
    }

    enum class Theme: EnumOption {
        System,
        Dark,
        Light;

        override fun string(context: Context): String {
            return when(this) {
                System -> context.getString(R.string.lang_system)
                Dark -> context.getString(R.string.dark)
                Light -> context.getString(R.string.light)
            }
        }
    }

    /** Home screen date display mode. */
    enum class DateMode: EnumOption {
        AD,
        BS,
        Off;

        override fun string(context: Context): String {
            return when(this) {
                AD -> context.getString(R.string.date_ad)
                BS -> context.getString(R.string.date_bs)
                Off -> context.getString(R.string.off)
            }
        }
    }
}
