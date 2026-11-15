package dev.identra.app.x.core.hook;

import android.content.Context;

import dev.identra.app.AdapterApp;

public interface IAssignListener {
    void setAssigned(Context context, String group, boolean assign);
   /* void updateAssigned(
            Context context,
            String groupName,
            boolean assign,
            boolean notifyChangeMain,
            AdapterApp.IOnAssignmentFinished onFinish);*/
}
