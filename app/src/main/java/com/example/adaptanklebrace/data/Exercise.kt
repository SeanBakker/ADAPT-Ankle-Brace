package com.example.adaptanklebrace.data

import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.RequiresApi
import java.io.Serializable
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@RequiresApi(Build.VERSION_CODES.Q)
data class Exercise(
    val name: String = "",
    val description: String = "",
    val steps: String = "",
    val imageId: Int = 0,
    var sets: Int = 0,
    var reps: Int = 0,
    var hold: Int = 0,
    var tension: Int = 0,
    var time: LocalTime = LocalTime.now(),
    var frequency: String = "",
    var difficulty: Int = 0,
    var comments: String = "",
    var isSelected: Boolean = false,
    var percentageCompleted: Double = 0.0
) : Parcelable, Serializable {

    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readString().let { LocalTime.parse(it, formatter) },
        parcel.readString() ?: "",
        parcel.readInt(),
        parcel.readString() ?: "",
        parcel.readBoolean(),
        parcel.readDouble()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(name)
        parcel.writeString(description)
        parcel.writeString(steps)
        parcel.writeInt(imageId)
        parcel.writeInt(sets)
        parcel.writeInt(reps)
        parcel.writeInt(hold)
        parcel.writeInt(tension)
        parcel.writeString(time.format(formatter))
        parcel.writeString(frequency)
        parcel.writeInt(difficulty)
        parcel.writeString(comments)
        parcel.writeBoolean(isSelected)
        parcel.writeDouble(percentageCompleted)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Exercise> {
        // Define a formatter for LocalTime
        val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

        override fun createFromParcel(parcel: Parcel): Exercise {
            return Exercise(parcel)
        }

        override fun newArray(size: Int): Array<Exercise?> {
            return arrayOfNulls(size)
        }
    }
}
