package com.example.adaptanklebrace.data

import android.os.Parcel
import android.os.Parcelable
import com.example.adaptanklebrace.utils.GeneralUtil
import java.io.Serializable
import java.time.LocalTime

data class Metric(
    val id: Int, // Unique identifier for tables
    val name: String = "",
    var timeCompleted: LocalTime = LocalTime.now(),
    var frequency: String = "",
    var difficulty: Int = 0,
    var comments: String = "",
    var isSelected: Boolean = false,
    var percentageCompleted: Double = 0.0,
    var isVisible: Boolean = true,
    var romPlantarDorsiflexionRange: Double = 0.0,
    var romInversionEversionRange: Double = 0.0
) : Parcelable, Serializable {

    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readString() ?: "",
        parcel.readString().let { LocalTime.parse(it, GeneralUtil.timeFormatter) },
        parcel.readString() ?: "",
        parcel.readInt(),
        parcel.readString() ?: "",
        parcel.readBoolean(),
        parcel.readDouble(),
        parcel.readBoolean(),
        parcel.readDouble(),
        parcel.readDouble()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeString(name)
        parcel.writeString(timeCompleted.format(GeneralUtil.timeFormatter))
        parcel.writeString(frequency)
        parcel.writeInt(difficulty)
        parcel.writeString(comments)
        parcel.writeBoolean(isSelected)
        parcel.writeDouble(percentageCompleted)
        parcel.writeBoolean(isVisible)
        parcel.writeDouble(romPlantarDorsiflexionRange)
        parcel.writeDouble(romInversionEversionRange)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Metric> {
        override fun createFromParcel(parcel: Parcel): Metric {
            return Metric(parcel)
        }

        override fun newArray(size: Int): Array<Metric?> {
            return arrayOfNulls(size)
        }
    }
}
