/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package diordna.content.pm;

import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.List;

public class StringParceledListSlice extends diordna.content.pm.BaseParceledListSlice<String> {
    public StringParceledListSlice(List<String> list, IBinder locker) {
        super(list, locker);
    }

    private StringParceledListSlice(Parcel in, ClassLoader loader) {
        super(in, loader);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    protected void writeElement(String parcelable, Parcel reply, int callFlags) {
        reply.writeString(parcelable);
    }

    @Override
    protected void writeParcelableCreator(String parcelable, Parcel dest) {
        return;
    }

    @Override
    protected Parcelable.Creator<?> readParcelableCreator(Parcel from, ClassLoader loader) {
        return Parcel.STRING_CREATOR;
    }

    @SuppressWarnings("unchecked")
    public static final Parcelable.ClassLoaderCreator<StringParceledListSlice> CREATOR =
            new Parcelable.ClassLoaderCreator<StringParceledListSlice>() {
                public StringParceledListSlice createFromParcel(Parcel in) {
                    return new StringParceledListSlice(in, null);
                }

                @Override
                public StringParceledListSlice createFromParcel(Parcel in, ClassLoader loader) {
                    return new StringParceledListSlice(in, loader);
                }

                @Override
                public StringParceledListSlice[] newArray(int size) {
                    return new StringParceledListSlice[size];
                }
            };
}
