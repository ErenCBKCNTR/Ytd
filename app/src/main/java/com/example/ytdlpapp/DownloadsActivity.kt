package com.example.ytdlpapp

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ytdlpapp.databinding.ActivityDownloadsBinding
import java.io.File

class DownloadsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDownloadsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDownloadsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.topAppBar.setNavigationOnClickListener {
            finish()
        }

        binding.rvDownloads.layoutManager = LinearLayoutManager(this)
        loadDownloads()
    }

    private fun loadDownloads() {
        val downloadDir = File(
            getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
            "YTDlpApp"
        )
        val files = downloadDir.listFiles()?.filter { it.isFile } ?: emptyList()

        if (files.isEmpty()) {
            binding.tvEmpty.visibility = View.VISIBLE
            binding.rvDownloads.visibility = View.GONE
        } else {
            binding.tvEmpty.visibility = View.GONE
            binding.rvDownloads.visibility = View.VISIBLE
            binding.rvDownloads.adapter = DownloadsAdapter(files) { file ->
                val intent = Intent(this, PlayerActivity::class.java).apply {
                    putExtra("FILE_PATH", file.absolutePath)
                }
                startActivity(intent)
            }
        }
    }

    private class DownloadsAdapter(
        private val files: List<File>,
        private val onClick: (File) -> Unit
    ) : RecyclerView.Adapter<DownloadsAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvFileName: TextView = view.findViewById(android.R.id.text1)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(android.R.layout.simple_list_item_1, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val file = files[position]
            holder.tvFileName.text = file.name
            holder.itemView.setOnClickListener {
                onClick(file)
            }
        }

        override fun getItemCount() = files.size
    }
}
