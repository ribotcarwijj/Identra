package dev.identra.app.x.core.commands.call;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dev.identra.app.api.XProxyContent;
import dev.identra.app.utilities.BundleUtil;
import dev.identra.app.x.Str;
import dev.identra.app.x.data.utils.ListUtil;
import dev.identra.app.x.data.utils.ObjectUtils;
import dev.identra.app.x.core.LibUtil;
import dev.identra.app.x.core.commands.CallCommandHandlerEx;
import dev.identra.app.x.core.commands.packet.CallPacket;
import dev.identra.app.x.core.hook.AppProviderUtils;
import dev.identra.app.x.core.hook.AssignmentApi;
import dev.identra.app.x.core.hook.AssignmentPacket;

public class InitAssignments extends CallCommandHandlerEx {
    private static final String TAG = LibUtil.generateTag(InitAssignments.class);

    public static final String FIELD = "assignments";
    public static final String COMMAND_NAME = "initAssignments";

    public InitAssignments() {
        name = COMMAND_NAME;
        requiresPermissionCheck = true;
        this.requiresSingleThread = true;
    }

    @Override
    public Bundle handle(CallPacket commandData) throws Throwable {
        List<String> hookIds = commandData.getExtraStringList(FIELD);
        Bundle result = new Bundle();
        if(!ListUtil.isValid(hookIds))
            return result;

        List<AssignmentPacket> assignments =
                AppProviderUtils.filterAssignments(
                        AssignmentApi.getAssignments(commandData.getDatabase(), commandData.getUserId(), commandData.getCategory()),
                        false,
                        false);

        List<String> assignmentNames = new ArrayList<>(assignments.size());
        for(AssignmentPacket assignment : assignments) {
            String hookId = assignment.getHookId();
            if(!assignmentNames.contains(hookId))
                assignmentNames.add(hookId);
        }


        for(String hookId : hookIds) {
            if(!Str.isEmpty(hookId)) {
                boolean isAssigned = assignmentNames.contains(hookId);
                result.putBoolean(hookId, isAssigned);
            }
        }

        return result;
    }


    public static Map<String, Boolean> get(Context context, List<String> hookIds, int uid, String packageName) {
        Map<String, Boolean> map = new HashMap<>();
        if(ObjectUtils.anyNull(context, hookIds, packageName)) {
            Log.e(TAG, "Invalid Input...");
            return map;
        }

        Bundle res = XProxyContent.luaCall(context, COMMAND_NAME,
                BundleUtil.writeIdentityUid(BundleUtil.createFromStringList(FIELD, hookIds), uid, packageName));
        if(res == null)
            return map;

        for(String id : hookIds) {
            if(!Str.isEmpty(id)) {
                map.put(id, res.getBoolean(id, false));
            }
        }

        return map;
    }
}