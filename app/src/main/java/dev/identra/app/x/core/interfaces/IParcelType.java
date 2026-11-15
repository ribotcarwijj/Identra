package dev.identra.app.x.core.interfaces;

import android.os.Parcel;
import android.os.Parcelable;

public interface IParcelType extends Parcelable {
    void fromParcel(Parcel in);
}
