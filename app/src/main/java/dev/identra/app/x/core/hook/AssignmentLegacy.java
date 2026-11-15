package dev.identra.app.x.core.hook;

import android.os.Parcel;

public class AssignmentLegacy extends AssignmentPacket {
    public AssignmentLegacy() { this.isLegacy = true; }
    public AssignmentLegacy(Parcel in) {
        this.isLegacy = true;
        fromParcel(in);
    }
}
