package com.example.ytdlpapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.ytdlpapp.databinding.ActivityMainBinding
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import android.content.Intent
import androidx.core.view.GravityCompat
import com.yausername.youtubedl_android.YoutubeDLException
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val TAG = "MainActivity"

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission granted
            Toast.makeText(this, "Storage permission granted", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, getString(R.string.permission_required), Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initLibraries()
        setupListeners()
        requestPermissionsOnLaunch()
    }

    private fun requestPermissionsOnLaunch() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
             // For Android 13+, if we want to read media files we'd need READ_MEDIA_VIDEO/AUDIO.
             // But for writing to Downloads, no permission is strictly required.
             // We can request notifications permission if needed, but not storage.
             Log.d(TAG, "Android 10+: Scoped storage applies, no write permission needed for Downloads.")
        }
    }

    private fun setupListeners() {
        binding.topAppBar.setNavigationOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }

        binding.navigationView.setNavigationItemSelectedListener { menuItem ->
            menuItem.isChecked = true
            binding.drawerLayout.closeDrawer(GravityCompat.START)

            when (menuItem.itemId) {
                R.id.nav_downloads -> {
                    startActivity(Intent(this, DownloadsActivity::class.java))
                }
            }
            true
        }

        binding.btnDownload.setOnClickListener {
            startDownload()
        }
        binding.btnUpdate.setOnClickListener {
            updateYoutubeDL()
        }
    }

    private fun updateYoutubeDL() {
        binding.btnUpdate.isEnabled = false
        binding.tvStatus.text = "Updating yt-dlp..."
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val status = YoutubeDL.getInstance().updateYoutubeDL(this@MainActivity, YoutubeDL.UpdateChannel.STABLE)
                withContext(Dispatchers.Main) {
                    binding.tvStatus.text = "Update Status: " + status?.name
                    binding.btnUpdate.isEnabled = true
                }
            } catch (e: Exception) {
                Log.e(TAG, "Update failed", e)
                withContext(Dispatchers.Main) {
                    binding.tvStatus.text = "Update failed: ${e.message}"
                    binding.btnUpdate.isEnabled = true
                }
            }
        }
    }

    private fun startDownload() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, getString(R.string.permission_required), Toast.LENGTH_LONG).show()
            requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            return
        }

        val url = binding.etUrl.text.toString().trim()
        if (url.isEmpty()) {
            Toast.makeText(this, getString(R.string.error_empty_url), Toast.LENGTH_SHORT).show()
            return
        }

        val isAudio = binding.rbAudio.isChecked

        binding.progressBar.visibility = View.VISIBLE
        binding.progressBar.progress = 0
        binding.btnDownload.isEnabled = false
        binding.tvStatus.text = "Starting download..."

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val youtubeDLDir = File(
                    getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                    "YTDlpApp"
                )
                if (!youtubeDLDir.exists()) {
                    youtubeDLDir.mkdir()
                }

                val request = YoutubeDLRequest(url)
                request.addOption("-o", youtubeDLDir.absolutePath + "/%(title)s.%(ext)s")

                if (isAudio) {
                    request.addOption("-f", "bestaudio")
                    request.addOption("--extract-audio")
                    request.addOption("--audio-format", "mp3")
                } else {
                    request.addOption("-f", "bestvideo[ext=mp4]+bestaudio[ext=m4a]/best[ext=mp4]/best")
                }

                val processId = "DownloadProcess_${System.currentTimeMillis()}"

                YoutubeDL.getInstance().execute(request, processId) { progress, etaInSeconds, _ ->
                    runOnUiThread {
                        binding.progressBar.progress = progress.toInt()
                        binding.tvStatus.text = getString(R.string.status_downloading, progress.toInt())
                    }
                }

                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    binding.tvStatus.text = getString(R.string.status_completed)
                    binding.btnDownload.isEnabled = true
                    Toast.makeText(this@MainActivity, "Download Complete", Toast.LENGTH_LONG).show()
                }

            } catch (e: Exception) {
                Log.e(TAG, "Download failed", e)
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    binding.tvStatus.text = getString(R.string.status_failed, e.message)
                    binding.btnDownload.isEnabled = true
                }
            }
        }
    }

    private fun initLibraries() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                YoutubeDL.getInstance().init(this@MainActivity)
                FFmpeg.getInstance().init(this@MainActivity)
            } catch (e: YoutubeDLException) {
                Log.e(TAG, "failed to initialize youtubedl-android", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Failed to initialize downloader", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
