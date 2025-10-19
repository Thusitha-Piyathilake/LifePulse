package com.example.lifepulse.data

import android.os.Parcel
import android.os.Parcelable

data class Habit(
    val name: String,      // immutable (cannot change)
    var time: String,      // mutable (can change in TodayHabitsFragment)
    var duration: Int      // mutable (can change in TodayHabitsFragment)
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readInt()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(name)
        parcel.writeString(time)
        parcel.writeInt(duration)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<Habit> {
        override fun createFromParcel(parcel: Parcel): Habit = Habit(parcel)
        override fun newArray(size: Int): Array<Habit?> = arrayOfNulls(size)
    }
}
