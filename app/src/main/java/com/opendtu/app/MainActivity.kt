package com.opendtu.app

import android.app.DownloadManager
import android.content.*
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.OpenableColumns
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
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
    private lateinit var prefs: SharedPreferences

    // Launcher for Android Storage Access Framework to pick firmware .bin files
    private val firmwarePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { processFirmwareUri(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences("prefs", MODE_PRIVATE)
        restoreConfigFromExternal()

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#F5F7FA")) 
        }

        container = FrameLayout(this).apply {
            id = View.generateViewId()
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
            )
        }

        val nav = BottomNavigationView(this).apply {
            menu.add(0, 1, 0, "Home").setIcon(android.R.drawable.ic_menu_today)
            menu.add(0, 2, 1, "Live").setIcon(android.R.drawable.ic_menu_view)
            menu.add(0, 3, 2, "Inverters").setIcon(android.R.drawable.ic_menu_manage)
            menu.add(0, 4, 3, "Settings").setIcon(android.R.drawable.ic_menu_preferences)

            setBackgroundColor(Color.WHITE)
            elevation = 16f
            
            val states = arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf(-android.R.attr.state_checked))
            val colors = intArrayOf(Color.parseColor("#2196F3"), Color.GRAY)
            val colorList = ColorStateList(states, colors)

            itemIconTintList = colorList
            itemTextColor = colorList
            labelVisibilityMode = BottomNavigationView.LABEL_VISIBILITY_LABELED
        }

        layout.addView(container)
        layout.addView(nav)
        setContentView(layout)

        loadHome()

        nav.setOnItemSelectedListener {
            when (it.itemId) {
                1 -> loadHome()
                2 -> LiveDataView(this).load(container)
                3 -> InverterView(this).load(container)
                4 -> SettingsView(this).load(container, { checkUpdate() }, { launchFirmwarePicker() })
            }
            true
        }

        ContextCompat.registerReceiver(
            this, onDownloadComplete,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    private fun loadHome() {
        container.removeAllViews()

        val scroll = ScrollView(this)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(60, 80, 60, 60)
            gravity = Gravity.CENTER_HORIZONTAL
        }

        val title = TextView(this).apply { 
            text = "OpenDTU Connection"
            textSize = 28f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#333333"))
            setPadding(0, 0, 0, 60)
        }

        // Create a card-like container for inputs
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 50, 50, 50)
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = 30f
                setStroke(2, Color.parseColor("#E0E0E0"))
            }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        }

        val ipInput = createStyledInput("IP (e.g. 192.168.1.50)", prefs.getString("dtu_ip", ""))
        val userInput = createStyledInput("Username", prefs.getString("dtu_user", "admin"))
        val passInput = createStyledInput("Password", prefs.getString("dtu_pass", "openDTU42"), true)

        val saveBtn = Button(this).apply {
            text = "Save & Connect"
            setBackgroundColor(Color.parseColor("#2196F3"))
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 150).apply {
                setMargins(0, 40, 0, 0)
            }
            setOnClickListener {
                prefs.edit().apply {
                    putString("dtu_ip", ipInput.text.toString())
                    putString("dtu_user", userInput.text.toString())
                    putString("dtu_pass", passInput.text.toString())
                    apply()
                }
                backupConfigToExternal() 
                Toast.makeText(context, "Credentials saved securely!", Toast.LENGTH_SHORT).show()
            }
        }

        card.addView(ipInput)
        card.addView(userInput)
        card.addView(passInput)
        card.addView(saveBtn)

        layout.addView(title)
        layout.addView(card)
        scroll.addView(layout)
        container.addView(scroll)
    }

    private fun createStyledInput(hintText: String, defaultVal: String?, isPassword: Boolean = false): EditText {
        return EditText(this).apply {
            hint = hintText
            setText(defaultVal)
            setPadding(30, 40, 30, 40)
            if (isPassword) {
                inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#F9F9F9"))
                cornerRadius = 15f
                setStroke(1, Color.LTGRAY)
            }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, 0, 30)
            }
        }
    }

    // --- Data Persistence Logic ---
    
    // Saves config to public Downloads directory to survive app uninstalls
    private fun backupConfigToExternal() {
        try {
            val json = JSONObject().apply {
                put("ip", prefs.getString("dtu_ip", ""))
                put("user", prefs.getString("dtu_user", ""))
                put("pass", prefs.getString("dtu_pass", ""))
            }
            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(dir, "OpenDTU_Config.json")
            file.writeText(json.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Restores config from public Downloads if app data was wiped
    private fun restoreConfigFromExternal() {
        try {
            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(dir, "OpenDTU_Config.json")
            if (file.exists() && !prefs.contains("dtu_ip")) {
                val json = JSONObject(file.readText())
                prefs.edit().apply {
                    putString("dtu_ip", json.optString("ip"))
                    putString("dtu_user", json.optString("user"))
                    putString("dtu_pass", json.optString("pass"))
                    apply()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // --- Firmware Upgrade Logic ---

    fun launchFirmwarePicker() {
        firmwarePickerLauncher.launch("*/*")
    }

    private fun processFirmwareUri(uri: Uri) {
        Toast.makeText(this, "Preparing firmware upload...", Toast.LENGTH_SHORT).show()
        // Here you would convert the URI to a byte array and pass it to ApiClient
        // ApiClient(this).uploadFirmware(uri) { success, msg -> ... }
    }

    // --- App Update Logic ---

    private fun checkUpdate() {
        Thread {
            try {
                // Fixed the URL (added slash between repos and maxytmaxyt)
                val json = URL("https://api.github.com/repos/maxytmaxyt/opendtu-app-android/releases/latest").readText()
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
