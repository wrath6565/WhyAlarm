package com.why.alarm

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {
    private var songUri: Uri? = null
    private lateinit var songLabel: TextView
    private val dayBtns = ArrayList<ToggleButton>()
    private val dayNames = arrayOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    private val dayCodes = intArrayOf(2, 3, 4, 5, 6, 7, 1) // Calendar constants

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        if (Build.VERSION.SDK_INT >= 33) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)

        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(48, 80, 48, 48) }
        val why = EditText(this).apply { hint = "Why are you setting this alarm?"; minLines = 3 }
        val picker = TimePicker(this)
        val repeatLabel = TextView(this).apply { text = "Repeat on (none selected = one-time alarm)" }
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        dayNames.forEach { n ->
            val t = ToggleButton(this).apply { textOn = n; textOff = n; text = n; textSize = 11f; setPadding(0, 0, 0, 0) }
            dayBtns.add(t)
            row.addView(t, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
        }
        songLabel = TextView(this).apply { text = "Sound: default alarm tone" }
        val pickSong = Button(this).apply { text = "Choose song from phone" }
        val set = Button(this).apply { text = "Set alarm" }
        listOf(why, picker, repeatLabel, row, songLabel, pickSong, set)
            .forEach { root.addView(it, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)) }
        setContentView(ScrollView(this).apply { addView(root) })

        pickSong.setOnClickListener {
            startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                type = "audio/*"; addCategory(Intent.CATEGORY_OPENABLE)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            }, 42)
        }
        set.setOnClickListener {
            val days = dayBtns.indices.filter { dayBtns[it].isChecked }.map { dayCodes[it] }
            Scheduler.cancelAll(this)
            Scheduler.save(this, why.text.toString().ifBlank { "Alarm" }, songUri?.toString(), picker.hour, picker.minute, days)
            val t = Scheduler.scheduleFromPrefs(this)
            val fmt = SimpleDateFormat("EEE d MMM, h:mm a", Locale.getDefault())
            val kind = if (days.isEmpty()) "One-time alarm" else "Repeating alarm"
            Toast.makeText(this, "$kind set. Next: ${t?.let { fmt.format(Date(it)) }}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onActivityResult(req: Int, res: Int, data: Intent?) {
        if (req == 42 && res == RESULT_OK && data?.data != null) {
            songUri = data.data
            contentResolver.takePersistableUriPermission(songUri!!, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            val name = contentResolver.query(songUri!!, null, null, null, null)?.use {
                if (it.moveToFirst()) it.getString(it.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME)) else null
            }
            songLabel.text = "Sound: ${name ?: "selected song"}"
        }
    }
}
