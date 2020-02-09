package com.github.exact7.xtra.ui.login

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.webkit.CookieManager
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.github.exact7.xtra.R
import com.github.exact7.xtra.di.Injectable
import com.github.exact7.xtra.model.LoggedIn
import com.github.exact7.xtra.model.NotLoggedIn
import com.github.exact7.xtra.model.User
import com.github.exact7.xtra.repository.AuthRepository
import com.github.exact7.xtra.ui.Utils
import com.github.exact7.xtra.util.C
import com.github.exact7.xtra.util.TwitchApiHelper
import com.github.exact7.xtra.util.applyTheme
import com.github.exact7.xtra.util.gone
import com.github.exact7.xtra.util.visible
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.regex.Pattern
import javax.inject.Inject

class LoginActivity : AppCompatActivity(), Injectable {

    @Inject
    lateinit var repository: AuthRepository

    //        private val authUrl = "https://id.twitch.tv/oauth2/authorize?response_type=token&client_id=${TwitchApiHelper.CLIENT_ID}&redirect_uri=http://localhost&scope=chat_login user_follows_edit user_subscriptions user_read"
    private val authUrl = "https://id.twitch.tv/oauth2/authorize?response_type=token&client_id=${TwitchApiHelper.TWITCH_CLIENT_ID}&redirect_uri=https://twitch.tv"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyTheme()
        setContentView(R.layout.activity_login)
        try { //TODO remove after updated to 1.2.0
            val oldPrefs = getSharedPreferences("authPrefs", Context.MODE_PRIVATE)
            if (oldPrefs.all.isNotEmpty()) {
                with(oldPrefs) {
                    if (getString(C.USER_ID, null) != null) {
                        User.set(this@LoginActivity, LoggedIn(getString(C.USER_ID, null)!!, getString(C.USERNAME, null)!!, getString(C.TOKEN, null)!!, true))
                    }
                }
                oldPrefs.edit { clear() }
            }
        } catch (e: Exception) {

        }
        val user = User.get(this)
        if (user is NotLoggedIn) {
            if (intent.getBooleanExtra(C.FIRST_LAUNCH, false)) {
                welcomeContainer.visible()
                login.setOnClickListener { initWebView() }
                skip.setOnClickListener { finish() }
            } else {
                initWebView()
            }
        } else {
            TwitchApiHelper.checkedValidation = false
            initWebView()
            repository.deleteAllEmotes()
            if (!user.newToken) {
                GlobalScope.launch {
                    repository.revoke(user.token)
                    User.set(this@LoginActivity, null)
                }
            } else {
                User.set(this, null)
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initWebView() {
        webViewContainer.visible()
        welcomeContainer.gone()
        toolbar.apply {
            navigationIcon = Utils.getNavigationIcon(this@LoginActivity)
            setNavigationOnClickListener { finish() }
        }
        havingTrouble.setOnClickListener {
            AlertDialog.Builder(this)
                    .setMessage(getString(R.string.login_problem_solution))
                    .setPositiveButton(R.string.log_in) { dialog, _ ->
                        val intent = Intent(Intent.ACTION_VIEW, authUrl.toUri())
                        if (intent.resolveActivity(packageManager) != null) {
                            webView.reload()
                            startActivity(intent)
                        } else {
                            Toast.makeText(this, getString(R.string.no_browser_found), Toast.LENGTH_LONG).show()
                            dialog.dismiss()
                        }
                    }
                    .setNegativeButton(android.R.string.cancel) { _, _ -> }
                    .show()
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            CookieManager.getInstance().removeAllCookies(null)
        } else {
            CookieManager.getInstance().removeAllCookie()
        }
        with(webView) {
            settings.javaScriptEnabled = true
            webViewClient = object : WebViewClient() {

                private val pattern = Pattern.compile("token=(.+?)(?=&)")

                override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                    val matcher = pattern.matcher(url)
                    if (matcher.find()) {
                        webViewContainer.gone()
                        welcomeContainer.gone()
                        progressBar.visible()
                        val token = matcher.group(1)
                        lifecycleScope.launch {
                            val response = repository.validate(token)
                            TwitchApiHelper.checkedValidation = true
                            User.set(this@LoginActivity, LoggedIn(response.userId, response.username, token, true))
                            setResult(RESULT_OK)
                            finish()
                        }
                    }
                    return super.shouldOverrideUrlLoading(view, url)
                }

                override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                    view?.apply {
                        val html = "<html><body><div align=\"center\" >No internet connection</div></body>"
                        loadUrl("about:blank")
                        loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
                    }
                }
            }
            loadUrl(authUrl)
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_BACK) {
            if (webView.canGoBack()) {
                webView.goBack()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }
}