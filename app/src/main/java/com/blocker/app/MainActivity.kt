package com.blocker.app

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsetsController
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
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

    private val BG = Color.parseColor("#0E1013")
    private val CARD = Color.parseColor("#1A1D23")
    private val TEXT = Color.parseColor("#F2F4F8")
    private val MUTED = Color.parseColor("#8B93A1")
    private val GREEN = Color.parseColor("#2ECC71")
    private val RED = Color.parseColor("#FF5A5F")
    private val ACCENT = Color.parseColor("#6C5CE7")
    private val GRAY_BTN = Color.parseColor("#2B2F38")

    private class Row(val view: View, val label: String, val sw: Switch)

    private val rows = ArrayList<Row>()
    private lateinit var counter: TextView
    private lateinit var dot: View
    private lateinit var statusTitle: TextView
    private lateinit var statusSub: TextView
    private lateinit var actionBtn: TextView

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun rounded(color: Int, radiusDp: Int): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radiusDp).toFloat()
        }

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

    private fun updateCounter() {
        val n = rows.count { it.sw.isChecked }
        counter.text = "Заблокировано: $n"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = BG
        window.navigationBarColor = BG

        val matchParent = ViewGroup.LayoutParams.MATCH_PARENT
        val wrap = ViewGroup.LayoutParams.WRAP_CONTENT

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(BG)
            setPadding(dp(20), dp(24), dp(20), 0)
        }

        // Заголовок
        root.addView(TextView(this).apply {
            text = "Блокировщик"
            textSize = 30f
            setTypeface(null, Typeface.BOLD)
            setTextColor(TEXT)
        })
        root.addView(TextView(this).apply {
            text = "Закрывает выбранные приложения при запуске"
            textSize = 14f
            setTextColor(MUTED)
            setPadding(0, dp(2), 0, dp(18))
        })

        // Карточка статуса
        val statusCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(CARD, 20)
            setPadding(dp(18), dp(16), dp(18), dp(16))
        }
        val statusRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        dot = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(12), dp(12)).apply { rightMargin = dp(10) }
        }
        statusTitle = TextView(this).apply {
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            setTextColor(TEXT)
        }
        statusRow.addView(dot)
        statusRow.addView(statusTitle)
        statusCard.addView(statusRow)

        statusSub = TextView(this).apply {
            textSize = 13f
            setTextColor(MUTED)
            setPadding(0, dp(6), 0, 0)
        }
        statusCard.addView(statusSub)

        actionBtn = TextView(this).apply {
            gravity = Gravity.CENTER
            textSize = 15f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(matchParent, dp(48)).apply { topMargin = dp(14) }
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        }
        statusCard.addView(actionBtn)
        root.addView(statusCard)

        // Счётчик
        counter = TextView(this).apply {
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
            setTextColor(MUTED)
            setPadding(dp(4), dp(22), 0, dp(8))
        }
        root.addView(counter)

        // Поиск
        val search = EditText(this).apply {
            hint = "Поиск приложения"
            setHintTextColor(MUTED)
            setTextColor(TEXT)
            textSize = 15f
            isSingleLine = true
            background = rounded(CARD, 14)
            setPadding(dp(16), dp(12), dp(16), dp(12))
            layoutParams = LinearLayout.LayoutParams(matchParent, wrap).apply { bottomMargin = dp(12) }
        }
        root.addView(search)

        // Список приложений
        val list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, dp(16))
        }
        val scroll = ScrollView(this).apply {
            isVerticalScrollBarEnabled = false
            addView(list)
        }
        root.addView(scroll, LinearLayout.LayoutParams(matchParent, 0, 1f))
        setContentView(root)

        if (Build.VERSION.SDK_INT >= 30) {
            window.insetsController?.setSystemBarsAppearance(
                0,
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
                    WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
            )
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = 0
        }

        val blocked = blockedSet()
        val pm = packageManager
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = pm.queryIntentActivities(launcher, 0)
            .filter { it.activityInfo.packageName != packageName }
            .distinctBy { it.activityInfo.packageName }
            .sortedWith(
                compareBy({ it.activityInfo.packageName !in blocked },
                    { it.loadLabel(pm).toString().lowercase() })
            )

        val states = arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf())

        for (ri in apps) {
            val pkg = ri.activityInfo.packageName
            val label = ri.loadLabel(pm).toString()

            val card = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = rounded(CARD, 16)
                setPadding(dp(14), dp(12), dp(14), dp(12))
                layoutParams = LinearLayout.LayoutParams(matchParent, wrap).apply { bottomMargin = dp(8) }
            }
            val icon = ImageView(this).apply {
                setImageDrawable(ri.loadIcon(pm))
                layoutParams = LinearLayout.LayoutParams(dp(42), dp(42))
            }
            val name = TextView(this).apply {
                text = label
                textSize = 16f
                setTextColor(TEXT)
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
                setPadding(dp(14), 0, dp(10), 0)
                layoutParams = LinearLayout.LayoutParams(0, wrap, 1f)
            }
            val sw = Switch(this).apply {
                thumbTintList = ColorStateList(states, intArrayOf(Color.WHITE, Color.parseColor("#B0B6C0")))
                trackTintList = ColorStateList(states, intArrayOf(RED, Color.parseColor("#3A3F4A")))
                isChecked = pkg in blocked
                setOnCheckedChangeListener { _, checked ->
                    val s = blockedSet()
                    if (checked) s.add(pkg) else s.remove(pkg)
                    saveBlocked(s)
                    updateCounter()
                }
            }
            card.setOnClickListener { sw.toggle() }
            card.addView(icon)
            card.addView(name)
            card.addView(sw)
            list.addView(card)
            rows.add(Row(card, label, sw))
        }
        updateCounter()

        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val q = s?.toString()?.trim()?.lowercase() ?: ""
                for (r in rows) {
                    r.view.visibility =
                        if (q.isEmpty() || r.label.lowercase().contains(q)) View.VISIBLE else View.GONE
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        val on = serviceEnabled()
        dot.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(if (on) GREEN else RED)
        }
        if (on) {
            statusTitle.text = "Защита включена"
            statusSub.text = "Отмеченные приложения будут закрываться сразу при запуске"
            actionBtn.text = "Настройки разрешения"
            actionBtn.background = rounded(GRAY_BTN, 14)
        } else {
            statusTitle.text = "Защита выключена"
            statusSub.text = "Нажмите кнопку и включите «Блокировщик приложений» в специальных возможностях"
            actionBtn.text = "Выдать разрешение"
            actionBtn.background = rounded(ACCENT, 14)
        }
    }
}
