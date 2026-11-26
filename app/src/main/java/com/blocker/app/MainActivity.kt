package com.blocker.app

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {

    companion object {
        const val PREFS = "blocker_prefs"
        const val KEY_BLOCKED = "blocked"
        private val DEFAULT_BLOCKED = setOf(
            "com.zhiliaoapp.musically",
            "com.ss.android.ugc.trill"
        )
    }

    private lateinit var status: TextView
    private lateinit var permButton: Button

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun blockedSet(): MutableSet<String> {
        val p = getSharedPreferences(PREFS, MODE_PRIVATE)
        if (!p.contains(KEY_BLOCKED)) p.edit().putStringSet(KEY_BLOCKED, DEFAULT_BLOCKED).apply()
        return HashSet(p.getStringSet(KEY_BLOCKED, emptySet()) ?: emptySet())
    }

    private fun saveBlocked(set: Set<String>) {
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putStringSet(KEY_BLOCKED, set).apply()
    }

    private fun serviceEnabled(): Boolean {
        val enabled = Settings.Secure.getString(
            contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabled.contains("$packageName/")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(32), dp(16), dp(8))
        }

        root.addView(TextView(this).apply {
            text = "Блокировщик приложений"
            textSize = 22f
            setTextColor(Color.BLACK)
        })

        status = TextView(this).apply {
            textSize = 15f
            setPadding(0, dp(8), 0, dp(8))
        }
        root.addView(status)

        permButton = Button(this).apply {
            text = "Выдать разрешение"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        }
        root.addView(permButton)

        root.addView(TextView(this).apply {
            text = "Отметьте приложения, которые нужно блокировать:"
            textSize = 15f
            setPadding(0, dp(16), 0, dp(8))
        })

        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val scroll = ScrollView(this).apply { addView(list) }
        root.addView(
            scroll,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        )
        setContentView(root)

        val blocked = blockedSet()
        val pm = packageManager
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = pm.queryIntentActivities(launcher, 0)
            .filter { it.activityInfo.packageName != packageName }
            .distinctBy { it.activityInfo.packageName }
            .sortedBy { it.loadLabel(pm).toString().lowercase() }

        for (ri in apps) {
            val pkg = ri.activityInfo.packageName
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(6), 0, dp(6))
            }
            row.addView(ImageView(this).apply {
                setImageDrawable(ri.loadIcon(pm))
                layoutParams = LinearLayout.LayoutParams(dp(40), dp(40))
            })
            row.addView(TextView(this).apply {
                text = ri.loadLabel(pm).toString()
                textSize = 16f
                setTextColor(Color.BLACK)
                setPadding(dp(12), 0, dp(8), 0)
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            })
            row.addView(CheckBox(this).apply {
                isChecked = pkg in blocked
                setOnCheckedChangeListener { _, checked ->
                    val s = blockedSet()
                    if (checked) s.add(pkg) else s.remove(pkg)
                    saveBlocked(s)
                }
            })
            list.addView(row)
        }
    }

    override fun onResume() {
        super.onResume()
        if (serviceEnabled()) {
            status.text = "Защита включена ✅"
            status.setTextColor(Color.rgb(0, 128, 0))
            permButton.text = "Настройки разрешения"
        } else {
            status.text = "Защита выключена. Нажмите кнопку, найдите «Блокировщик приложений» и включите."
            status.setTextColor(Color.rgb(200, 0, 0))
            permButton.text = "Выдать разрешение"
        }
    }
}
