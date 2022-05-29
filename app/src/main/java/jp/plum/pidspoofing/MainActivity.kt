package jp.plum.pidspoofing

import android.os.*
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import org.jetbrains.anko.button
import org.jetbrains.anko.intentFor
import org.jetbrains.anko.sdk27.coroutines.onClick
import org.jetbrains.anko.verticalLayout
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.locks.ReentrantLock

class MainActivity : AppCompatActivity() {
    companion object {
        const val Key = "Key"
        const val RequestRemain = IBinder.FIRST_CALL_TRANSACTION
        const val RollingTo = IBinder.LAST_CALL_TRANSACTION
    }

    val queue = LinkedBlockingQueue<Int>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        verticalLayout {
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
                                        // TODO:  here you can spawn process and rolling system pid to target pid (:hijack)
                                        Thread.sleep(5000)
                                        (0 until HijackActivity.source.size - HijackActivity.limit).forEach {
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