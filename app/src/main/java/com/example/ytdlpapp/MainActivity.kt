package com.example.ytdlpapp

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
import android.widget.ArrayAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val TAG = "MainActivity"

    private val videoQualities = arrayOf(getString(R.string.quality_best), "1080p", "720p", "480p", "360p")
    private val audioQualities = arrayOf(getString(R.string.quality_best), "320kbps", "256kbps", "128kbps")

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission granted
            Toast.makeText(this, getString(R.string.permission_granted), Toast.LENGTH_SHORT).show()
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
        updateQualityDropdown(false)
    }

    private fun updateQualityDropdown(isAudio: Boolean) {
        val options = if (isAudio) audioQualities else videoQualities
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, options)
        binding.actvQuality.setAdapter(adapter)
        binding.actvQuality.setText(options[0], false)
    }

    private fun requestPermissionsOnLaunch() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissions = arrayOf(
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO
            )
            val notGranted = permissions.filter {
                ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }
            if (notGranted.isNotEmpty()) {
                requestPermissionLauncher.launch(notGranted.first())
            }
        } else {
            // Include Android 10-12 so users always get prompted if they haven't granted it
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
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

        binding.rgFormat.setOnCheckedChangeListener { _, checkedId ->
            updateQualityDropdown(checkedId == R.id.rbAudio)
        }
    }

    private fun updateYoutubeDL() {
        binding.btnUpdate.isEnabled = false
        binding.tvStatus.text = getString(R.string.status_updating)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val status = YoutubeDL.getInstance().updateYoutubeDL(this@MainActivity, YoutubeDL.UpdateChannel.STABLE)
                withContext(Dispatchers.Main) {
                    binding.tvStatus.text = getString(R.string.status_update_status) + status?.name
                    binding.btnUpdate.isEnabled = true
                }
            } catch (e: Exception) {
                Log.e(TAG, "Update failed", e)
                withContext(Dispatchers.Main) {
                    binding.tvStatus.text = getString(R.string.status_update_failed, e.message)
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
        binding.tvStatus.text = getString(R.string.status_updating_before_download)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                try {
                    YoutubeDL.getInstance().updateYoutubeDL(this@MainActivity, YoutubeDL.UpdateChannel.STABLE)
                    withContext(Dispatchers.Main) {
                        binding.tvStatus.text = getString(R.string.status_update_complete)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Update failed, continuing with download anyway", e)
                }

                val youtubeDLDir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "YTDlpApp"
                )
                if (!youtubeDLDir.exists()) {
                    youtubeDLDir.mkdir()
                }

                val request = YoutubeDLRequest(url)
                request.addOption("-o", youtubeDLDir.absolutePath + "/%(title)s.%(ext)s")

                val selectedQuality = binding.actvQuality.text.toString()

                if (isAudio) {
                    request.addOption("--extract-audio")
                    request.addOption("--audio-format", "mp3")
                    request.addOption("--embed-thumbnail")

                    val audioQualityArg = when (selectedQuality) {
                        "320kbps" -> "320K"
                        "256kbps" -> "256K"
                        "128kbps" -> "128K"
                        else -> "0" // best
                    }
                    request.addOption("--audio-quality", audioQualityArg)
                    request.addOption("-f", "ba/best")
                } else {
                    val formatSelection = when (selectedQuality) {
                        "1080p" -> "bv*[height<=1080][ext=mp4]+ba[ext=m4a]/b[height<=1080]/b"
                        "720p" -> "bv*[height<=720][ext=mp4]+ba[ext=m4a]/b[height<=720]/b"
                        "480p" -> "bv*[height<=480][ext=mp4]+ba[ext=m4a]/b[height<=480]/b"
                        "360p" -> "bv*[height<=360][ext=mp4]+ba[ext=m4a]/b[height<=360]/b"
                        else -> "bv*[ext=mp4]+ba[ext=m4a]/b"
                    }
                    request.addOption("-f", formatSelection)
                    request.addOption("--embed-thumbnail")
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
                    Toast.makeText(this@MainActivity, getString(R.string.status_completed), Toast.LENGTH_LONG).show()
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

                // Auto-update yt-dlp on launch
                withContext(Dispatchers.Main) {
                    binding.tvStatus.text = getString(R.string.status_checking_update)
                }
                val status = YoutubeDL.getInstance().updateYoutubeDL(this@MainActivity, YoutubeDL.UpdateChannel.STABLE)
                withContext(Dispatchers.Main) {
                    binding.tvStatus.text = getString(R.string.status_update_status) + status?.name
                }
            } catch (e: Exception) {
                Log.e(TAG, "Initialization or update failed", e)
                withContext(Dispatchers.Main) {
                    binding.tvStatus.text = getString(R.string.status_update_failed, e.message)
                }
            }
        }
    }
}
