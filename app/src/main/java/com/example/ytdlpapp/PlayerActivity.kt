package com.example.ytdlpapp

import android.net.Uri
import android.os.Bundle
import android.widget.MediaController
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.ytdlpapp.databinding.ActivityPlayerBinding
import java.io.File
import android.content.Intent
import androidx.core.content.FileProvider
import android.media.MediaMetadataRetriever
import android.graphics.BitmapFactory
import android.view.View

class PlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlayerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.topAppBar.setNavigationOnClickListener {
            finish()
        }

        val filePath = intent.getStringExtra("FILE_PATH")
        if (filePath != null) {
            val file = File(filePath)
            binding.topAppBar.title = file.name

            binding.topAppBar.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.action_share -> {
                        val uri = FileProvider.getUriForFile(this, "${applicationContext.packageName}.provider", file)
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "*/*"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        startActivity(Intent.createChooser(shareIntent, "Share file"))
                        true
                    }
                    else -> false
                }
            }

            val mediaController = MediaController(this)
            mediaController.setAnchorView(binding.videoView)

            binding.videoView.setMediaController(mediaController)
            binding.videoView.setVideoURI(Uri.fromFile(file))
            binding.videoView.requestFocus()

            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(file.absolutePath)
                val hasVideo = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO)
                val picture = retriever.embeddedPicture

                if (picture != null && (hasVideo == null || hasVideo == "no")) {
                    val bitmap = BitmapFactory.decodeByteArray(picture, 0, picture.size)
                    binding.ivCoverArt.setImageBitmap(bitmap)
                    binding.ivCoverArt.visibility = View.VISIBLE
                } else {
                    binding.ivCoverArt.visibility = View.GONE
                }
                retriever.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            binding.videoView.setOnPreparedListener {
                binding.videoView.start()
            }

            binding.videoView.setOnErrorListener { _, _, _ ->
                Toast.makeText(this, getString(R.string.error_playing_file), Toast.LENGTH_SHORT).show()
                true
            }
        } else {
            Toast.makeText(this, getString(R.string.error_no_file), Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
