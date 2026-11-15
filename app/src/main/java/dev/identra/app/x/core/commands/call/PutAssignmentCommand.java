package dev.identra.app.x.core.commands.call;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import org.json.JSONObject;

import dev.identra.app.DebugUtil;
import dev.identra.app.api.XProxyContent;
import dev.identra.app.x.Str;
import dev.identra.app.x.core.LibUtil;
import dev.identra.app.x.core.commands.CallCommandHandlerEx;
import dev.identra.app.x.core.commands.packet.CallPacket;
import dev.identra.app.x.core.database.A_CODE;
import dev.identra.app.x.core.database.DatabaseHelpEx;
import dev.identra.app.x.core.hook.AssignmentPacket;

public class PutAssignmentCommand extends CallCommandHandlerEx {
    private static final String TAG = LibUtil.generateTag(PutAssignmentCommand.class);

    public static final String COMMAND_NAME = "putAssignmentEx";
    public static final String FIELD_DATA = "raw";

    public PutAssignmentCommand() {
        name = COMMAND_NAME;
        requiresPermissionCheck = true;
    }

    @Override
    public Bundle handle(CallPacket commandData) throws Throwable {
        String json = commandData.getExtraString(FIELD_DATA);
        if(json == null || Str.isEmpty(json)) {
            Log.e(TAG, "Failed to put Assignment, JSON Data is null or empty!");
            return A_CODE.FAILED.toBundle();
        }

        try {
            //Init Cache or ?
            if(!DatabaseHelpEx.ensureTableIsReady_locked(AssignmentPacket.TABLE_INFO, commandData.getDatabase())) {
                Log.e(TAG, "Failed to Prepare Database! Table=" + AssignmentPacket.TABLE_NAME);
                return A_CODE.FAILED.toBundle();
            }

            AssignmentPacket val = new AssignmentPacket();
            val.fromJSONObject(new JSONObject(json));
            if(DebugUtil.isDebug())
                Log.d(TAG, Str.fm("Pushing (%s) Assignment: %s",
                        val.getObjectId(),
                        Str.toStringOrNull(Str.ensureNoDoubleNewLines(json))));

            if(Str.isEmpty(val.getHookId()))
                return A_CODE.FAILED.toBundle();

            boolean result = DatabaseHelpEx.insertItem(commandData.getDatabase(), AssignmentPacket.TABLE_NAME, val);
            if(DebugUtil.isDebug())
                Log.d(TAG, Str.fm("Hook [%s] was Pushed ? (%s)\nAssignment=%s",
                        val.getObjectId(),
                        result,
                        Str.ensureNoDoubleNewLines(Str.toStringOrNull(val))));

            return A_CODE.result(result).toBundle();
        }catch (Exception e) {
            Log.e(TAG, "Error Putting Assignment, Error=" + e);
            return A_CODE.FAILED.toBundle();
        }
    }

    public static A_CODE put(Context context, AssignmentPacket assignmentPacket) {
        try {
            Bundle send = new Bundle();
            String assignmentJson = assignmentPacket.toJSONString();
            if(DebugUtil.isDebug())
                Log.d(TAG, "Putting Assignment=" + Str.ensureNoDoubleNewLines(assignmentJson));

            send.putString(FIELD_DATA, assignmentJson);
            Bundle result = XProxyContent.luaCall(context, COMMAND_NAME, send);
            return A_CODE.fromBundle(result);
        }catch (Exception e) {
            Log.e(TAG, "Failed to Put Assignment, Error=" + e);
            return A_CODE.FAILED;
        }
    }
}