package com.example.adaptanklebrace.data

import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.RequiresApi
import java.io.Serializable

data class Exercise(
    val name: String = "",
    val description: String = "",
    val steps: String = "",
    val imageId: Int = 0,
    var sets: Int = 0,
    var reps: Int = 0,
    var hold: Int = 0,
    var tension: Int = 0,
    var frequency: String = "",
    var difficulty: Int = 0,
    var comments: String = "",
    var isStarted: Boolean = false
) : Parcelable, Serializable {

    @RequiresApi(Build.VERSION_CODES.Q)
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readString() ?: "",
        parcel.readInt(),
        parcel.readString() ?: "",
        parcel.readBoolean()
    )

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(name)
        parcel.writeString(description)
        parcel.writeString(steps)
        parcel.writeInt(imageId)
        parcel.writeInt(sets)
        parcel.writeInt(reps)
        parcel.writeInt(hold)
        parcel.writeInt(tension)
        parcel.writeString(frequency)
        parcel.writeInt(difficulty)
        parcel.writeString(comments)
        parcel.writeBoolean(isStarted)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Exercise> {
        @RequiresApi(Build.VERSION_CODES.Q)
        override fun createFromParcel(parcel: Parcel): Exercise {
            return Exercise(parcel)
        }

        override fun newArray(size: Int): Array<Exercise?> {
            return arrayOfNulls(size)
        }
    }
}
