package com.github.andreyasadchy.xtra.ui.login

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import com.github.andreyasadchy.xtra.R
import com.github.andreyasadchy.xtra.di.Injectable
import com.github.andreyasadchy.xtra.model.LoggedIn
import com.github.andreyasadchy.xtra.model.NotLoggedIn
import com.github.andreyasadchy.xtra.model.User
import com.github.andreyasadchy.xtra.repository.AuthRepository
import com.github.andreyasadchy.xtra.util.*
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.regex.Pattern
import javax.inject.Inject

class LoginActivity : AppCompatActivity(), Injectable {

    @Inject
    lateinit var repository: AuthRepository
    private val tokenPattern = Pattern.compile("token=(.+?)(?=&)")
    private var tokens = 0
    private var userId: String? = null
    private var userLogin: String? = null
    private var helixToken: String? = null
    private var gqlToken: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyTheme()
        setContentView(R.layout.activity_login)
        val helixClientId = prefs().getString(C.HELIX_CLIENT_ID, "")
        val gqlClientId = prefs().getString(C.GQL_CLIENT_ID, "")
        val user = User.get(this)
        if (user !is NotLoggedIn) {
            TwitchApiHelper.checkedValidation = false
            User.set(this, null)
            GlobalScope.launch {
                try {
                    if (!helixClientId.isNullOrBlank() && !user.helixToken.isNullOrBlank()) {
                        repository.revoke(helixClientId, user.helixToken)
                    }
                    if (!gqlClientId.isNullOrBlank() && !user.gqlToken.isNullOrBlank()) {
                        repository.revoke(gqlClientId, user.gqlToken)
                    }
                } catch (e: Exception) {

                }
            }
        }
        initWebView(helixClientId, gqlClientId)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initWebView(helixClientId: String?, gqlClientId: String?) {
        webViewContainer.visible()
        val apiSetting = prefs().getString(C.API_LOGIN, "0")?.toInt() ?: 0
        val helixRedirect = prefs().getString(C.HELIX_REDIRECT, "https://localhost")
        val helixScopes = "chat:read chat:edit channel:moderate channel_editor whispers:edit user:read:follows"
        val helixAuthUrl = "https://id.twitch.tv/oauth2/authorize?response_type=token&client_id=${helixClientId}&redirect_uri=${helixRedirect}&scope=${helixScopes}"
        val gqlRedirect = prefs().getString(C.GQL_REDIRECT, "https://www.twitch.tv/")
        val gqlAuthUrl = "https://id.twitch.tv/oauth2/authorize?response_type=token&client_id=${gqlClientId}&redirect_uri=${gqlRedirect}&scope="
        havingTrouble.setOnClickListener {
            AlertDialog.Builder(this)
                    .setMessage(getString(R.string.login_problem_solution))
                    .setPositiveButton(R.string.log_in) { _, _ ->
                        val intent = Intent(Intent.ACTION_VIEW, helixAuthUrl.toUri())
                        if (intent.resolveActivity(packageManager) != null) {
                            webView.reload()
                            startActivity(intent)
                        } else {
                            toast(R.string.no_browser_found)
                        }
                    }
                    .setNeutralButton(R.string.to_enter_url) { _, _ ->
                        val editText = EditText(this).apply {
                            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                                val margin = convertDpToPixels(10f)
                                setMargins(margin, 0, margin, 0)
                            }
                        }
                        val dialog = AlertDialog.Builder(this)
                                .setTitle(R.string.enter_url)
                                .setView(editText)
                                .setPositiveButton(R.string.log_in) { _, _ ->
                                    val text = editText.text
                                    if (text.isNotEmpty()) {
                                        if (!loginIfValidUrl(text.toString(), gqlAuthUrl, helixClientId, gqlClientId, 2)) {
                                            shortToast(R.string.invalid_url)
                                        }
                                    }
                                }
                                .setNegativeButton(android.R.string.cancel, null)
                                .show()
                        dialog.window?.setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
        }
        clearCookies()
        val theme = if (prefs().getBoolean(C.UI_THEME_FOLLOW_SYSTEM, false)) {
            when (resources.configuration.uiMode.and(Configuration.UI_MODE_NIGHT_MASK)) {
                Configuration.UI_MODE_NIGHT_YES -> prefs().getString(C.UI_THEME_DARK_ON, "0")!!
                else -> prefs().getString(C.UI_THEME_DARK_OFF, "2")!!
            }
        } else {
            prefs().getString(C.THEME, "0")!!
        }
        with(webView) {
            if (theme != "2") {
                if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
                    WebSettingsCompat.setForceDark(this.settings, WebSettingsCompat.FORCE_DARK_ON)
                }
                if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK_STRATEGY)) {
                    WebSettingsCompat.setForceDarkStrategy(this.settings, WebSettingsCompat.DARK_STRATEGY_WEB_THEME_DARKENING_ONLY)
                }
            }
            settings.javaScriptEnabled = true
            webViewClient = object : WebViewClient() {

                @Deprecated("Deprecated in Java")
                override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                    loginIfValidUrl(url, gqlAuthUrl, helixClientId, gqlClientId, apiSetting)
                    return false
                }

                @Deprecated("Deprecated in Java")
                override fun onReceivedError(view: WebView, errorCode: Int, description: String, failingUrl: String) {
                    val errorMessage = if (errorCode == -11) {
                        getString(R.string.browser_workaround)
                    } else {
                        getString(R.string.error, "$errorCode $description")
                    }
                    val html = "<html><body><div align=\"center\">$errorMessage</div></body>"
                    loadUrl("about:blank")
                    loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
                }
            }
            loadUrl(if (apiSetting == 1) gqlAuthUrl else helixAuthUrl)
        }
    }

/*    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_BACK) {
            if (webView.canGoBack()) {
                webView.goBack()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }*/

    private fun loginIfValidUrl(url: String, gqlAuthUrl: String, helixClientId: String?, gqlClientId: String?, apiSetting: Int): Boolean {
        val matcher = tokenPattern.matcher(url)
        return if (matcher.find() && tokens < 2) {
            webViewContainer.gone()
            progressBar.visible()
            val token = matcher.group(1)!!
            if (apiSetting == 0 && tokens == 0 || apiSetting == 2) {
                lifecycleScope.launch {
                    try {
                        val response = repository.validate(TwitchApiHelper.addTokenPrefixHelix(token))
                        if (!response?.clientId.isNullOrBlank() && response?.clientId == helixClientId) {
                            userId = response?.userId
                            userLogin = response?.login
                            helixToken = token
                            if (apiSetting == 0 && !gqlToken.isNullOrBlank() || apiSetting > 0) {
                                TwitchApiHelper.checkedValidation = true
                                User.set(this@LoginActivity, LoggedIn(userId, userLogin, helixToken, gqlToken))
                                setResult(RESULT_OK)
                                finish()
                            }
                        } else {
                            throw IOException()
                        }
                    } catch (e: Exception) {
                        toast(R.string.connection_error)
                    }
                }
            } else {
                lifecycleScope.launch {
                    try {
                        val response = repository.validate(TwitchApiHelper.addTokenPrefixGQL(token))
                        if (!response?.clientId.isNullOrBlank() && response?.clientId == gqlClientId) {
                            userId = response?.userId
                            userLogin = response?.login
                            gqlToken = token
                            if (apiSetting == 0 && !helixToken.isNullOrBlank() || apiSetting > 0) {
                                TwitchApiHelper.checkedValidation = true
                                User.set(this@LoginActivity, LoggedIn(userId, userLogin, helixToken, gqlToken))
                                setResult(RESULT_OK)
                                finish()
                            }
                        } else {
                            throw IOException()
                        }
                    } catch (e: Exception) {
                        toast(R.string.connection_error)
                    }
                }
            }
            if (apiSetting == 0 && tokens == 0) {
                webView.loadUrl(gqlAuthUrl)
            }
            tokens++
            true
        } else {
            false
        }
    }

    private fun clearCookies() {
        CookieManager.getInstance().removeAllCookies(null)
    }
}