package com.why.alarm

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.os.Bundle
import android.widget.*

class AlarmActivity : Activity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER
            setBackgroundColor(Color.BLACK); setPadding(48, 48, 48, 48)
        }
        root.addView(TextView(this).apply {
            text = "REMEMBER WHY"; setTextColor(Color.parseColor("#FFB300")); textSize = 18f; gravity = Gravity.CENTER
        })
        root.addView(TextView(this).apply {
            text = intent.getStringExtra("why"); setTextColor(Color.WHITE); textSize = 34f
            gravity = Gravity.CENTER; setPadding(0, 48, 0, 96)
        }, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        root.addView(Button(this).apply {
            text = "Dismiss"
            setOnClickListener { stopService(Intent(this@AlarmActivity, AlarmService::class.java)); finish() }
        })
        setContentView(root)
    }
}
