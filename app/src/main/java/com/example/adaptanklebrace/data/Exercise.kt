package com.example.adaptanklebrace.data

import android.os.Parcel
import android.os.Parcelable
import com.example.adaptanklebrace.utils.ExerciseUtil
import java.io.Serializable
import java.time.LocalTime

data class Exercise(
    val id: Int, // Unique identifier for tables
    val name: String = "",
    var sets: Int = 0,
    var reps: Int = 0,
    var hold: Int = 0,
    var tension: Int = 0,
    var timeCompleted: LocalTime = LocalTime.now(),
    var frequency: String = "",
    var difficulty: Int = 0,
    var comments: String = "",
    var isSelected: Boolean = false,
    var percentageCompleted: Double = 0.0
) : Parcelable, Serializable {

    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readString() ?: "",
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readString().let { LocalTime.parse(it, ExerciseUtil.timeFormatter) },
        parcel.readString() ?: "",
        parcel.readInt(),
        parcel.readString() ?: "",
        parcel.readBoolean(),
        parcel.readDouble()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeString(name)
        parcel.writeInt(sets)
        parcel.writeInt(reps)
        parcel.writeInt(hold)
        parcel.writeInt(tension)
        parcel.writeString(timeCompleted.format(ExerciseUtil.timeFormatter))
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
        override fun createFromParcel(parcel: Parcel): Exercise {
            return Exercise(parcel)
        }

        override fun newArray(size: Int): Array<Exercise?> {
            return arrayOfNulls(size)
        }
    }
}
