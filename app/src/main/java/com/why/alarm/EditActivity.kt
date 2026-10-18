package com.why.alarm

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.*

class EditActivity : Activity() {
    private var songUri: Uri? = null
    private lateinit var songLabel: TextView
    private val dayBtns = ArrayList<ToggleButton>()
    private val dayNames = arrayOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    private val dayCodes = intArrayOf(2, 3, 4, 5, 6, 7, 1)

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val existing = Store.load(this).find { it.id == intent.getIntExtra("id", -1) }
        songUri = existing?.song?.let { Uri.parse(it) }

        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(48, 80, 48, 48) }
        val why = EditText(this).apply { hint = "Why are you setting this alarm?"; minLines = 3; setText(existing?.why ?: "") }
        val picker = TimePicker(this).apply { existing?.let { hour = it.h; minute = it.m } }
        val repeatLabel = TextView(this).apply { text = "Repeat on (none selected = one-time alarm)" }
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        dayNames.forEachIndexed { idx, n ->
            val t = ToggleButton(this).apply {
                textOn = n; textOff = n; text = n; textSize = 11f; setPadding(0, 0, 0, 0)
                isChecked = existing?.days?.contains(dayCodes[idx]) == true
            }
            dayBtns.add(t); row.addView(t, LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
        }
        songLabel = TextView(this).apply { text = if (songUri == null) "Sound: default alarm tone" else "Sound: your chosen song" }
        val pickSong = Button(this).apply { text = "Choose song from phone" }
        val save = Button(this).apply { text = "Save alarm" }
        listOf(why, picker, repeatLabel, row, songLabel, pickSong, save)
            .forEach { root.addView(it, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)) }
        setContentView(ScrollView(this).apply { addView(root) })

        pickSong.setOnClickListener {
            startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                type = "audio/*"; addCategory(Intent.CATEGORY_OPENABLE)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            }, 42)
        }
        save.setOnClickListener {
            val days = dayBtns.indices.filter { dayBtns[it].isChecked }.map { dayCodes[it] }
            val id = existing?.id ?: Store.nextId(this)
            val a = Alarm(id, why.text.toString().ifBlank { "Alarm" }, songUri?.toString(), picker.hour, picker.minute,
                days, true, if (days.isEmpty()) Scheduler.next(0, picker.hour, picker.minute) else 0L)
            Scheduler.cancel(this, id); Store.upsert(this, a); Scheduler.schedule(this, a)
            Toast.makeText(this, "Alarm saved", Toast.LENGTH_SHORT).show()
            finish()
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
