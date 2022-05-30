package jp.plum.pidspoofing

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder.FIRST_CALL_TRANSACTION
import android.os.Parcel
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.system.exitProcess

abstract class TrampolineService : Service() {
    companion object {
        const val ReportPid = FIRST_CALL_TRANSACTION
        const val Suicide = ReportPid + 1
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) = START_NOT_STICKY

    override fun onBind(intent: Intent?) = object : Binder() {
        override fun onTransact(code: Int, data: Parcel?, reply: Parcel, flags: Int) =
            if (code == ReportPid) {
                reply.writeInt(android.os.Process.myPid())
                GlobalScope.launch(Dispatchers.IO) {
                    Log.e(TAG, "pid:${android.os.Process.myPid()} suiciding")
                    delay(300L)
                    stopSelf()
                    exitProcess(0)
                }
                true
            } else {
                super.onTransact(code, data, reply, flags)
            }
    }
}

class A1 : TrampolineService()
class A2 : TrampolineService()
class A3 : TrampolineService()
