package com.example.ytdlpapp

import android.net.Uri
import android.os.Bundle
import android.widget.MediaController
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.ytdlpapp.databinding.ActivityPlayerBinding
import java.io.File

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

            val mediaController = MediaController(this)
            mediaController.setAnchorView(binding.videoView)

            binding.videoView.setMediaController(mediaController)
            binding.videoView.setVideoURI(Uri.fromFile(file))
            binding.videoView.requestFocus()

            binding.videoView.setOnPreparedListener {
                binding.videoView.start()
            }

            binding.videoView.setOnErrorListener { _, _, _ ->
                Toast.makeText(this, "Error playing file", Toast.LENGTH_SHORT).show()
                true
            }
        } else {
            Toast.makeText(this, "No file provided", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
