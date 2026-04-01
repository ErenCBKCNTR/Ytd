package com.example.ytdlpapp

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class YoutubeLoginActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var btnSaveCookies: Button

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_youtube_login)

        webView = findViewById(R.id.webView)
        btnSaveCookies = findViewById(R.id.btnSaveCookies)

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.cacheMode = WebSettings.LOAD_DEFAULT
        webView.settings.userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/117.0.0.0 Safari/537.36"

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                // We can check cookies here as well
            }
        }

        webView.loadUrl("https://accounts.google.com/ServiceLogin?service=youtube&continue=https://www.youtube.com/signin?action_handle_signin=true&app=desktop&hl=en")

        btnSaveCookies.setOnClickListener {
            saveCookies()
        }
    }

    private fun saveCookies() {
        val cookieManager = CookieManager.getInstance()
        val cookies = cookieManager.getCookie("https://youtube.com") ?: ""
        val googleCookies = cookieManager.getCookie("https://google.com") ?: ""

        if (cookies.isEmpty() && googleCookies.isEmpty()) {
            Toast.makeText(this, "Çerez bulunamadı. Önce giriş yapın.", Toast.LENGTH_SHORT).show()
            return
        }

        // yt-dlp requires Netscape HTTP Cookie File format
        val file = File(filesDir, "cookies.txt")
        file.bufferedWriter().use { out ->
            out.write("# Netscape HTTP Cookie File\n")
            out.write("# https://curl.haxx.se/rfc/cookie_spec.html\n")
            out.write("# This is a generated file!  Do not edit.\n\n")

            // Parse and write youtube.com cookies
            parseAndWriteCookies(cookies, ".youtube.com", out)
            // Parse and write google.com cookies
            parseAndWriteCookies(googleCookies, ".google.com", out)
        }

        Toast.makeText(this, "Giriş çerezleri kaydedildi! Artık indirebilirsiniz.", Toast.LENGTH_LONG).show()
        finish()
    }

    private fun parseAndWriteCookies(cookieStr: String, domain: String, out: java.io.BufferedWriter) {
        val cookiePairs = cookieStr.split(";")
        val expiration = (System.currentTimeMillis() / 1000) + (365 * 24 * 60 * 60) // 1 year from now
        for (pair in cookiePairs) {
            val parts = pair.trim().split("=", limit = 2)
            if (parts.size == 2) {
                val name = parts[0]
                val value = parts[1]
                val flag = "TRUE" // domain specific
                val path = "/"
                val secure = "TRUE"
                // domain flag path secure expiration name value
                out.write("$domain\t$flag\t$path\t$secure\t$expiration\t$name\t$value\n")
            }
        }
    }
}
