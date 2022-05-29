package jp.plum.spoofpid

import android.os.Binder
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Parcel
import android.util.Log
import org.jetbrains.anko.button
import org.jetbrains.anko.intentFor
import org.jetbrains.anko.sdk27.coroutines.onClick
import org.jetbrains.anko.verticalLayout

class MainActivity : AppCompatActivity() {
    companion object {
        const val BinderKey = "BinderKey"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        verticalLayout {
            button("bring my binder to Bob") {
                onClick {
                    val binder = object : Binder() {
                        override fun onTransact(
                            code: Int,
                            data: Parcel,
                            reply: Parcel?,
                            flags: Int
                        ): Boolean {
                            return when (code) {
                                FIRST_CALL_TRANSACTION -> {
                                    Log.e(
                                        "natsuki",
                                        "request for position: ${data.readInt()} block begin"
                                    )
                                    // TODO:  here you can spawn process and rolling system pid to target pid(:hijacking)
                                    Thread.sleep(5000)
                                    (0 until 5).forEach {
                                        reply?.writeInt(1)
                                        reply?.writeString("hello $it")
                                    }
                                    Log.e("natsuki", "blocking end")
                                    true
                                }
                                else -> super.onTransact(code, data, reply, flags)
                            }
                        }
                    }
                    startActivity(
                        intentFor<HijackingActivity>().putExtra(BinderKey, binder)
                    )
                }
            }
        }
    }
}