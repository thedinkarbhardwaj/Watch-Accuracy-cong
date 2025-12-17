package com.cogniter.watchaccuracychecker.model

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

import android.os.Parcel
import android.os.Parcelable

data class ListItem(val id: Long, val title: String,val watchimage: String,  val addedWatchTime: String,val isrunning: Boolean,val subItems: List<Subitem>) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readLong(),
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readBoolean()!!,
        parcel.createTypedArrayList(Subitem.CREATOR)!!
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(id)
        parcel.writeString(title)
        parcel.writeString(watchimage)
        parcel.writeString(addedWatchTime)
        parcel.writeBoolean(isrunning)
        parcel.writeTypedList(subItems)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<ListItem> {
        override fun createFromParcel(parcel: Parcel): ListItem {
            return ListItem(parcel)
        }

        override fun newArray(size: Int): Array<ListItem?> {
            return arrayOfNulls(size)
        }
    }
}

