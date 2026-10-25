package com.danni.fileswiper

import android.content.Context
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.danni.fileswiper.data.model.SessionRequest
import com.danni.fileswiper.ui.theme.FileSwiperTheme
import com.danni.fileswiper.viewmodel.SetupViewModel
import com.danni.fileswiper.viewmodel.SwipeViewModel
import java.util.Locale

class MainActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        val container = (newBase.applicationContext as? FileSwiperApp)?.container
        val tag = container?.settingsRepository?.language?.value
        val wrapped = if (tag.isNullOrBlank()) newBase else wrapLocale(newBase, tag)
        super.attachBaseContext(wrapped)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as FileSwiperApp).container
        setContent {
            val themeMode by container.settingsRepository.themeMode.collectAsState()
            FileSwiperTheme(themeMode = themeMode) {
                AppRoot()
            }
        }
    }

    private fun wrapLocale(base: Context, tag: String): Context {
        return try {
            val config = android.content.res.Configuration(base.resources.configuration)
            config.setLocale(Locale.forLanguageTag(tag))
            base.createConfigurationContext(config)
        } catch (_: Exception) {
            base
        }
    }
}
