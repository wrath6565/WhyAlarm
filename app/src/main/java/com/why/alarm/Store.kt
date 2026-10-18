package com.why.alarm

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class Alarm(
    val id: Int, var why: String, var song: String?, var h: Int, var m: Int,
    var days: List<Int>, var enabled: Boolean, var once: Long
)

object Store {
    private fun prefs(c: Context) = c.getSharedPreferences("alarms", Context.MODE_PRIVATE)

    fun load(c: Context): MutableList<Alarm> {
        val arr = JSONArray(prefs(c).getString("list", "[]"))
        return (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            Alarm(o.getInt("id"), o.getString("why"), if (o.isNull("song")) null else o.getString("song"),
                o.getInt("h"), o.getInt("m"),
                o.getString("days").split(",").filter { d -> d.isNotEmpty() }.map { d -> d.toInt() },
                o.getBoolean("enabled"), o.getLong("once"))
        }.toMutableList()
    }

    private fun write(c: Context, list: List<Alarm>) {
        val arr = JSONArray()
        list.forEach { a ->
            arr.put(JSONObject().put("id", a.id).put("why", a.why).put("song", a.song ?: JSONObject.NULL)
                .put("h", a.h).put("m", a.m).put("days", a.days.joinToString(","))
                .put("enabled", a.enabled).put("once", a.once))
        }
        prefs(c).edit().putString("list", arr.toString()).apply()
    }

    fun upsert(c: Context, a: Alarm) {
        val l = load(c); val i = l.indexOfFirst { it.id == a.id }
        if (i >= 0) l[i] = a else l.add(a)
        write(c, l)
    }

    fun delete(c: Context, id: Int) = write(c, load(c).filter { it.id != id })
    fun nextId(c: Context): Int = (load(c).maxOfOrNull { it.id } ?: 0) + 1
}
