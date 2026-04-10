package com.opendtu.app

import android.app.DownloadManager
import android.content.*
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.json.JSONObject
import java.io.File
import java.net.URL

class MainActivity : AppCompatActivity() {

    private var downloadId: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL

        val container = FrameLayout(this)
        container.id = 1
        container.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        )

        val nav = BottomNavigationView(this)
        nav.menu.add(0, 1, 0, "Home")
        nav.menu.add(0, 2, 1, "Stats")
        nav.menu.add(0, 3, 2, "Settings")

        layout.addView(container)
        layout.addView(nav)

        setContentView(layout)

        loadHome(container)

        nav.setOnItemSelectedListener {
            when (it.itemId) {
                1 -> loadHome(container)
                2 -> loadStats(container)
                3 -> loadSettings(container)
            }
            true
        }

        registerReceiver(onDownloadComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
    }

    private fun loadHome(container: FrameLayout) {
        val tv = TextView(this)
        tv.text = "Home"
        container.removeAllViews()
        container.addView(tv)
    }

    private fun loadStats(container: FrameLayout) {
        val tv = TextView(this)
        tv.text = "Stats"
        container.removeAllViews()
        container.addView(tv)
    }

    private fun loadSettings(container: FrameLayout) {
        val btn = Button(this)
        btn.text = "Check Update"

        btn.setOnClickListener {
            checkUpdate()
        }

        container.removeAllViews()
        container.addView(btn)
    }

    private fun checkUpdate() {
        Thread {
            try {
                val json = URL("https://api.github.com/repos/USERNAME/REPO/releases/latest").readText()
                val obj = JSONObject(json)
                val asset = obj.getJSONArray("assets").getJSONObject(0)
                val url = asset.getString("browser_download_url")

                runOnUiThread {
                    downloadApk(url)
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    private fun downloadApk(url: String) {
        val request = DownloadManager.Request(Uri.parse(url))
        request.setTitle("Update")
        request.setDestinationInExternalFilesDir(this, Environment.DIRECTORY_DOWNLOADS, "update.apk")

        val dm = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
        downloadId = dm.enqueue(request)
    }

    private val onDownloadComplete = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            if (id == downloadId) {
                installApk()
            }
        }
    }

    private fun installApk() {
        val file = File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "update.apk")

        val uri = FileProvider.getUriForFile(
            this,
            "$packageName.provider",
            file
        )

        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(uri, "application/vnd.android.package-archive")
        intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK

        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(onDownloadComplete)
    }
}
