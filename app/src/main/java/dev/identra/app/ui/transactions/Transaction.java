package dev.identra.app.ui.transactions;

import android.content.Context;

import androidx.annotation.NonNull;

import dev.identra.app.api.XResult;

public class Transaction {
    public Context context;
    public int id = -1;
    public int code = -1;
    public int adapterPosition = -1;
    public XResult result;

    @NonNull
    @Override
    public String toString() {
        return new StringBuilder()
                .append(" id=")
                .append(id).append("\n")
                .append(" code=")
                .append(code).append("\n")
                .append(" index=")
                .append(adapterPosition).append("\n").toString();
    }
}
