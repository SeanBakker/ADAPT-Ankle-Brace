package com.example.adaptanklebrace.data

import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.RequiresApi
import java.io.Serializable

@RequiresApi(Build.VERSION_CODES.Q)
data class ExerciseInfo(
    val name: String = "",
    var description: String = "",
    var steps: String = "",
    var imageId: Int = 0,
) : Parcelable, Serializable {

    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readInt()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(name)
        parcel.writeString(description)
        parcel.writeString(steps)
        parcel.writeInt(imageId)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<ExerciseInfo> {
        const val EXERCISE_KEY = "exercise_key"

        override fun createFromParcel(parcel: Parcel): ExerciseInfo {
            return ExerciseInfo(parcel)
        }

        override fun newArray(size: Int): Array<ExerciseInfo?> {
            return arrayOfNulls(size)
        }
    }
}
