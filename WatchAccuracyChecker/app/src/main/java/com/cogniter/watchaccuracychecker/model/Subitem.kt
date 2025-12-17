package com.cogniter.watchaccuracychecker.model

import android.graphics.Bitmap
import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class Subitem(val subitemId: Long,val name: String, val image: String,val date: String) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readLong()!!,
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readString()!!
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(subitemId)
        parcel.writeString(name)
        parcel.writeString(image)
        parcel.writeString(date)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Subitem> {
        override fun createFromParcel(parcel: Parcel): Subitem {
            return Subitem(parcel)
        }

        override fun newArray(size: Int): Array<Subitem?> {
            return arrayOfNulls(size)
        }
    }
}

