package dev.identra.app;

import dev.identra.IdentraNs;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;

import dev.identra.xposed.XC_MethodHook;
import dev.identra.app.api.core.call.CleanHooksCommand;
import dev.identra.app.api.core.call.ClearSettingsCommand;
import dev.identra.app.api.xmock.call.ClearAppDataCommand;
import dev.identra.app.api.xstandard.CommanderService_old;
import dev.identra.app.api.xstandard.command.CallPacket_old;
import dev.identra.app.api.xstandard.command.QueryPacket_old;
import dev.identra.app.api.core.IdentraDatabase;
import dev.identra.app.api.core.call.AssignHooksCommand;
import dev.identra.app.api.core.call.ClearAppCommand;
import dev.identra.app.api.core.call.ClearDataCommand;
import dev.identra.app.api.core.call.GetAppCommand;
import dev.identra.app.api.core.call.GetGroupsCommand;
import dev.identra.app.api.core.call.GetSettingCommand;
import dev.identra.app.api.core.call.GetVersionCommand;
import dev.identra.app.api.core.call.InitAppCommand;
import dev.identra.app.api.core.call.PutHookCommand;
import dev.identra.app.api.core.call.PutSettingCommand;
import dev.identra.app.api.core.call.ReportCommand;
import dev.identra.app.api.core.query.GetAppsCommand;
import dev.identra.app.api.core.query.GetHooksCommand;
import dev.identra.app.api.core.query.GetLogCommand;
import dev.identra.app.api.core.query.GetSettingsCommand;
import dev.identra.app.api.xmock.XMockDatabase;
import dev.identra.app.api.xmock.call.GetMockCpuCommand;
import dev.identra.app.api.xmock.call.GetMockCpusCommand;
import dev.identra.app.api.xmock.call.GetRandomUserAgentCommand;
import dev.identra.app.api.xmock.call.KillAppCommand;
import dev.identra.app.api.xmock.call.PutGroupStateCommand;
import dev.identra.app.api.xmock.call.PutMockConfigCommand;
import dev.identra.app.api.xmock.call.PutMockCpuCommand;
import dev.identra.app.api.xmock.call.PutMockPropCommand;
import dev.identra.app.api.xmock.call.PutMockSettingCommand;
import dev.identra.app.api.xmock.query.GetMockAgentsCommand;
import dev.identra.app.api.xmock.query.GetMockConfigsCommand;
import dev.identra.app.api.xmock.query.GetMockPropMapsCommand;
import dev.identra.app.api.xmock.query.GetMockPropertiesCommand;
import dev.identra.app.api.xmock.query.GetMockSettingsCommand;
import dev.identra.app.utilities.BundleUtil;
import dev.identra.app.x.core.commands.call.PutSettingExCommand;
import dev.identra.app.x.core.commands.query.GetAssignedHooksExCommand;
import dev.identra.app.x.core.commands.query.GetAssignedHooksLegacyCommand;
import dev.identra.app.x.core.commands.query.GetSettingsExCommand;

public class XCommandBridgeStatic {
    public static final String PRO_PACKAGE = "dev.identra.app.pro";
    public static final String X_LUA_PACKAGE = "dev.identra.app";//pretty sure this is a string somewhere already

    private static final String TAG = "Identra.XSettingBridgeStatic";

    public static final CommanderService_old luaCommandService = new CommanderService_old(IdentraNs.CMD_LUA, IdentraDatabase.createEmpty(), 100);
    private static final CommanderService_old mockCommandService = new CommanderService_old(IdentraNs.CMD_MOCK, XMockDatabase.createEmpty(), 100);

    static {
        luaCommandService
                //.registerCall(PutSettingExCommand.class)

                .registerCall(AssignHooksCommand.class)
                .registerCall(ClearAppCommand.class)
                .registerCall(ClearDataCommand.class)
                .registerCall(GetGroupsCommand.class)
                .registerCall(GetSettingCommand.class)
                .registerCall(GetVersionCommand.class)
                .registerCall(InitAppCommand.class)
                .registerCall(PutHookCommand.class)
                .registerCall(PutSettingCommand.class)
                .registerCall(ReportCommand.class)
                .registerQuery(GetAppsCommand.class, true)
                //.registerQuery(GetAssignedHooksCommand.class, true)

                //New Query
                //.registerQuery(GetAssignedHooksExCommand.class, true)
                //.registerQuery(GetAssignedHooksLegacyCommand.class, true)

                //End of New

                .registerQuery(GetHooksCommand.class, true)
                .registerQuery(GetSettingsCommand.class)

                //.registerQuery(GetSettingsExCommand.class, true)

                //didnt register log
                .registerQuery(GetLogCommand.class)
                //Mock Settings that link to the Identra DB
                .registerQuery(GetMockSettingsCommand.class, true)
                .registerCall(PutMockSettingCommand.class)
                .registerCall(GetAppCommand.class)
                .registerCall(CleanHooksCommand.class)
                .registerCall(ClearSettingsCommand.class);

        mockCommandService
                .registerCall(GetMockCpuCommand.class)
                .registerCall(GetMockCpusCommand.class)
                .registerCall(KillAppCommand.class)
                .registerCall(PutGroupStateCommand.class)
                .registerCall(PutMockConfigCommand.class)
                .registerCall(PutMockCpuCommand.class)
                .registerCall(PutMockPropCommand.class)
                .registerQuery(GetMockConfigsCommand.class, true)
                .registerQuery(GetMockPropertiesCommand.class, true)
                .registerQuery(GetMockPropMapsCommand.class, true)
                .registerQuery(GetMockAgentsCommand.class, true)
                .registerCall(GetRandomUserAgentCommand.class)
                .registerCall(ClearAppDataCommand.class);
    }

    public static Bundle vxpCall(Context context, String arg, Bundle extras, String method)  {
        CommanderService_old cService = null;
        if(luaCommandService.isCommandPrefix(method))
            cService = luaCommandService;
        else if(mockCommandService.isCommandPrefix(method))
            cService = mockCommandService;

        assert cService != null;
        CallPacket_old packet = new CallPacket_old(context, method, arg, extras, cService.getDatabase(context));
        return cService.handleCall(packet);
    }

    public static Cursor vxpQuery(Context context, String arg, String[] selection, String method)  {
        CommanderService_old cService = null;
        if(luaCommandService.isCommandPrefix(method))
            cService = luaCommandService;
        else if(mockCommandService.isCommandPrefix(method))
            cService = mockCommandService;

        assert cService != null;
        QueryPacket_old packet = new QueryPacket_old(context, method, arg, selection, cService.getDatabase(context));
        return cService.handleQuery(packet);
    }

    public static void handleQuery(XC_MethodHook.MethodHookParam param, String packageName) {
        QueryPacket_old packet = luaCommandService.tryCreateQueryPacket(param, packageName);
        if (packet != null) {
            //Lua Command (query)
            param.setResult(luaCommandService.handleQuery(packet));
        }
        else {
            //Mock Commands (query)
            packet = mockCommandService.tryCreateQueryPacket(param, packageName);
            if(packet != null) param.setResult(mockCommandService.handleQuery(packet));
        }
    }

    public static void handeCall(XC_MethodHook.MethodHookParam param, String packageName)  {
        CallPacket_old packet = luaCommandService.tryCreateCallPacket(param, packageName);
        if(packet != null) {
            //Lua commands (call)
            if(packet.getMethod().equalsIgnoreCase("getVersion"))
                param.setResult(BundleUtil.createSingleInt("version", Identra.version));
            else
                param.setResult(luaCommandService.handleCall(packet));
        }
        else {
            //Mock Commands (call)
            packet = mockCommandService.tryCreateCallPacket(param, packageName);
            if(packet != null) param.setResult(mockCommandService.handleCall(packet));
        }
    }
}
