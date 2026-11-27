package app.olaunchercf

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModelProvider
import app.olaunchercf.data.Constants
import app.olaunchercf.data.Constants.value
import app.olaunchercf.data.Prefs
import app.olaunchercf.databinding.ActivityMainBinding
import app.olaunchercf.helper.isTablet
import app.olaunchercf.helper.showToastLong
import app.olaunchercf.ui.AppDrawerFragment
import app.olaunchercf.ui.HomeFragment
import app.olaunchercf.ui.SettingsFragment
import java.io.BufferedReader
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.util.*

class MainActivity : FragmentActivity() {

    private lateinit var prefs: Prefs
    private lateinit var viewModel: MainViewModel
    private lateinit var binding: ActivityMainBinding

    override fun attachBaseContext(newBase: Context) {
        val theme = Prefs(newBase).appTheme
        if (theme != Constants.Theme.System) {
            val config = Configuration(newBase.resources.configuration)
            val nightBit = if (theme == Constants.Theme.Dark) {
                Configuration.UI_MODE_NIGHT_YES
            } else {
                Configuration.UI_MODE_NIGHT_NO
            }
            config.uiMode = (config.uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or nightBit
            super.attachBaseContext(newBase.createConfigurationContext(config))
        } else {
            super.attachBaseContext(newBase)
        }
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount > 0)
            super.onBackPressed()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        prefs = Prefs(this)
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        setLanguage()

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment, HomeFragment())
                .commit()
        }

        viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        initClickListeners()
        initObservers(viewModel)
        viewModel.getAppList()
        setupOrientation()

        window.addFlags(FLAG_LAYOUT_NO_LIMITS)
    }

    @Suppress("DEPRECATION")
    fun setLanguage() {
        val locale = Locale(prefs.language.value())
        val config = resources.configuration
        config.locale = locale
        resources.updateConfiguration(config, resources.displayMetrics)
    }

    override fun onStop() {
        backToHomeScreen()
        super.onStop()
    }

    override fun onUserLeaveHint() {
        backToHomeScreen()
        super.onUserLeaveHint()
    }

    override fun onNewIntent(intent: Intent?) {
        backToHomeScreen()
        super.onNewIntent(intent)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        recreate()
    }

    private fun initClickListeners() {
        binding.okay.setOnClickListener {
            binding.messageLayout.visibility = View.GONE
            viewModel.showMessageDialog("")
        }
    }

    private fun initObservers(viewModel: MainViewModel) {
        viewModel.launcherResetFailed.observe(this) {
            openLauncherChooser(it)
        }
        viewModel.showMessageDialog.observe(this) {
            showMessage(it)
        }
    }

    @SuppressLint("SourceLockedOrientationActivity")
    private fun setupOrientation() {
        if (isTablet(this)) return
        // In Android 8.0, windowIsTranslucent cannot be used with screenOrientation=portrait
        if (Build.VERSION.SDK_INT != Build.VERSION_CODES.O)
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }

    fun openAppDrawer(args: Bundle? = null) {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                android.R.anim.fade_in,
                android.R.anim.fade_out,
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
            .replace(R.id.nav_host_fragment, AppDrawerFragment().apply { arguments = args })
            .addToBackStack(null)
            .commit()
    }

    fun openSettings() {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(
                android.R.anim.fade_in,
                android.R.anim.fade_out,
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
            .replace(R.id.nav_host_fragment, SettingsFragment())
            .addToBackStack(null)
            .commit()
    }

    fun backToHomeScreen() {
        // Whenever home button is pressed or user leaves the launcher,
        // pop all the fragments except main
        if (supportFragmentManager.backStackEntryCount > 0)
            supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
    }

    private fun openLauncherChooser(resetFailed: Boolean) {
        if (resetFailed) {
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS) else {
                showToastLong(
                    this,
                    "Search for launcher or home app"
                )
                Intent(Settings.ACTION_SETTINGS)
            }
            startActivity(intent)
        }
    }

    private fun showMessage(message: String) {
        if (message.isEmpty()) return
        binding.messageTextView.text = message
        binding.messageLayout.visibility = View.VISIBLE
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode != Activity.RESULT_OK) {
            // showToastLong(applicationContext, "Intent Error")
            return
        }

        when (requestCode) {
            Constants.REQUEST_CODE_ENABLE_ADMIN -> {
                prefs.lockModeOn = true
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P)
                    showMessage(getString(R.string.double_tap_lock_is_enabled_message))
                else
                    showMessage(getString(R.string.double_tap_lock_uninstall_message))
            }
            Constants.BACKUP_READ -> {
                data?.data?.also { uri ->
                    applicationContext.contentResolver.openInputStream(uri).use { inputStream ->
                        val stringBuilder = StringBuilder()
                        BufferedReader(InputStreamReader(inputStream)).use { reader ->
                            var line: String? = reader.readLine()
                            while (line != null) {
                                stringBuilder.append(line)
                                line = reader.readLine()
                            }
                        }

                        val string = stringBuilder.toString()
                        val prefs = Prefs(applicationContext)
                        prefs.clear()
                        prefs.loadFromString(string)
                    }
                }
                startActivity(Intent.makeRestartActivityTask(this.intent?.component))
            }
            Constants.BACKUP_WRITE -> {
                data?.data?.also { uri ->
                    applicationContext.contentResolver.openFileDescriptor(uri, "w")?.use { file ->
                        FileOutputStream(file.fileDescriptor).use { stream ->
                            val text = Prefs(applicationContext).saveToString()
                            stream.channel.truncate(0)
                            stream.write( text.toByteArray() )
                        }
                    }
                }
            }
        }
    }
}
