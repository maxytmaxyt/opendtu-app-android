package com.opendtu.app

import android.app.DownloadManager
import android.content.*
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.json.JSONObject
import java.io.File
import java.net.URL

class MainActivity : AppCompatActivity() {

    private var downloadId: Long = -1
    private lateinit var container: FrameLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        container = FrameLayout(this).apply {
            id = View.generateViewId()
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
            )
        }

        val nav = BottomNavigationView(this).apply {
            menu.add(0, 1, 0, "Home")
            menu.add(0, 2, 1, "Live")
            menu.add(0, 3, 2, "Inverters")
            menu.add(0, 4, 3, "Settings")
        }

        layout.addView(container)
        layout.addView(nav)
        setContentView(layout)

        // Load Initial View
        loadHome()

        nav.setOnItemSelectedListener {
            when (it.itemId) {
                1 -> loadHome()
                2 -> LiveDataView(this).load(container)
                3 -> InverterView(this).load(container)
                4 -> SettingsView(this).load(container) { checkUpdate() }
            }
            true
        }

        // Register Update Receiver
        ContextCompat.registerReceiver(
            this,
            onDownloadComplete,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    private fun loadHome() {
        container.removeAllViews()
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 50, 50, 50)
        }

        val tv = TextView(this).apply { text = "OpenDTU Home"; textSize = 24f }
        
        val ipInput = EditText(this).apply {
            hint = "DTU IP Address"
            setText(getSharedPreferences("prefs", MODE_PRIVATE).getString("dtu_ip", ""))
        }

        val saveBtn = Button(this).apply {
            text = "Save IP"
            setOnClickListener {
                val ip = ipInput.text.toString()
                getSharedPreferences("prefs", MODE_PRIVATE).edit().putString("dtu_ip", ip).apply()
                Toast.makeText(context, "IP Saved: $ip", Toast.LENGTH_SHORT).show()
            }
        }

        layout.addView(tv)
        layout.addView(ipInput)
        layout.addView(saveBtn)
        container.addView(layout)
    }

    // --- Update Logic ---
    private fun checkUpdate() {
        Thread {
            try {
                // Replace with your real GitHub repo
                val json = URL("https://api.github.com/repos/USERNAME/REPO/releases/latest").readText()
                val obj = JSONObject(json)
                val asset = obj.getJSONArray("assets").getJSONObject(0)
                val url = asset.getString("browser_download_url")
                runOnUiThread { downloadApk(url) }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    private fun downloadApk(url: String) {
        val request = DownloadManager.Request(Uri.parse(url)).apply {
            setTitle("OpenDTU Update")
            setDestinationInExternalFilesDir(this@MainActivity, Environment.DIRECTORY_DOWNLOADS, "update.apk")
        }
        val dm = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
        downloadId = dm.enqueue(request)
        Toast.makeText(this, "Downloading update...", Toast.LENGTH_SHORT).show()
    }

    private val onDownloadComplete = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            if (id == downloadId) installApk()
        }
    }

    private fun installApk() {
        val file = File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "update.apk")
        val uri = FileProvider.getUriForFile(this, "$packageName.provider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(onDownloadComplete)
    }
}
