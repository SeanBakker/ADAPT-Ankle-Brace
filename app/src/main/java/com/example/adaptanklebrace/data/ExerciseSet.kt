package com.example.adaptanklebrace.data

import android.os.Parcel
import android.os.Parcelable
import java.io.Serializable

data class ExerciseSet(
    val id: Int, // Unique identifier for tables
    var reps: Int = 0,
    var hold: Double = 0.0
) : Parcelable, Serializable {

    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readInt(),
        parcel.readDouble()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeInt(reps)
        parcel.writeDouble(hold)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<ExerciseSet> {
        override fun createFromParcel(parcel: Parcel): ExerciseSet {
            return ExerciseSet(parcel)
        }

        override fun newArray(size: Int): Array<ExerciseSet?> {
            return arrayOfNulls(size)
        }
    }
}
