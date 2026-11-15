package dev.identra.app.x.core.commands;

import dev.identra.IdentraNs;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;

import dev.identra.xposed.XC_MethodHook;
import dev.identra.app.Identra;

import dev.identra.app.utilities.BundleUtil;
import dev.identra.app.x.core.commands.call.AssignHooksCommand;
import dev.identra.app.x.core.commands.call.ClearAppDataCommand;
import dev.identra.app.x.core.commands.call.DropTableCommand;
import dev.identra.app.x.core.commands.call.ForceStopAppCommand;
import dev.identra.app.x.core.commands.call.GetAppDirectoriesCommand;
import dev.identra.app.x.core.commands.call.GetAppInfoCommand;
import dev.identra.app.x.core.commands.call.GetBridgeVersionCommand;
import dev.identra.app.x.core.commands.call.GetDatabasePathCommand;
import dev.identra.app.x.core.commands.call.GetDatabaseStatusCommand;
import dev.identra.app.x.core.commands.call.GetGroupsCommand;
import dev.identra.app.x.core.commands.call.GetHookCommand;
import dev.identra.app.x.core.commands.call.GetProfileCommand;
import dev.identra.app.x.core.commands.call.GetProfileListCommand;
import dev.identra.app.x.core.commands.call.GetSettingExCommand;
import dev.identra.app.x.core.commands.call.GetVersionExCommand;
import dev.identra.app.x.core.commands.call.InitAssignments;
import dev.identra.app.x.core.commands.call.PutAppProfileCommand;
import dev.identra.app.x.core.commands.call.PutAssignmentCommand;
import dev.identra.app.x.core.commands.call.PutConfigCommand;
import dev.identra.app.x.core.commands.call.PutHookExCommand;
import dev.identra.app.x.core.commands.call.PutSettingExCommand;
import dev.identra.app.x.core.commands.call.ReportCommand;
import dev.identra.app.x.core.commands.call.FoundReportCommand;
import dev.identra.app.x.core.commands.call.GetBuildCommand;
import dev.identra.app.x.core.commands.call.SetAppProfileCommand;
import dev.identra.app.x.core.commands.packet.CallPacket;
import dev.identra.app.x.core.commands.packet.QueryPacket;
import dev.identra.app.x.core.commands.query.GetAppsCommand;
import dev.identra.app.x.core.commands.query.GetAssignedHooksExCommand;
import dev.identra.app.x.core.commands.query.GetAssignedHooksLegacyCommand;
import dev.identra.app.x.core.commands.query.GetAssignmentsCommand;
import dev.identra.app.x.core.commands.query.GetConfigsCommand;
import dev.identra.app.x.core.commands.query.GetHooksCommand;
import dev.identra.app.x.core.commands.query.GetSettingsExCommand;
import dev.identra.app.x.core.database.wrappers.IdentraDatabaseManager;
import dev.identra.app.x.core.database.wrappers.XMocDatabaseManager;

public class GlobalCommandBridge {
    public static final CommandService luaCommandService = new CommandService(IdentraNs.CMD_LUA, IdentraDatabaseManager.create(), 100);
    public static final CommandService mocCommandService = new CommandService(IdentraNs.CMD_MOCK, XMocDatabaseManager.create(), 100);

    static {
        luaCommandService
                .registerCall(PutSettingExCommand.class)
                .registerCall(GetSettingExCommand.class)
                .registerCall(GetGroupsCommand.class)
                .registerCall(GetVersionExCommand.class)
                .registerCall(AssignHooksCommand.class)
                .registerCall(GetAppInfoCommand.class)

                .registerCall(GetDatabaseStatusCommand.class)
                .registerCall(GetBridgeVersionCommand.class)

                .registerCall(ReportCommand.class)
                .registerCall(FoundReportCommand.class)
                .registerCall(GetBuildCommand.class)

                .registerCall(DropTableCommand.class)
                .registerCall(PutHookExCommand.class)
                .registerCall(PutAssignmentCommand.class)

                .registerCall(GetHookCommand.class)
                .registerCall(InitAssignments.class)

                .registerCall(PutConfigCommand.class)

                .registerCall(ForceStopAppCommand.class)
                .registerCall(ClearAppDataCommand.class)

                .registerCall(GetDatabasePathCommand.class)
                .registerCall(PutAppProfileCommand.class)
                .registerCall(SetAppProfileCommand.class)
                .registerCall(GetProfileCommand.class)
                .registerCall(GetProfileListCommand.class)

                .registerCall(GetAppDirectoriesCommand.class)

                .registerQuery(GetAppsCommand.class, true)
                .registerQuery(GetHooksCommand.class, true)

                .registerQuery(GetAssignmentsCommand.class, true)

                .registerQuery(GetConfigsCommand.class, true)

                .registerQuery(GetAssignedHooksExCommand.class, true)
                .registerQuery(GetAssignedHooksLegacyCommand.class, true)
                .registerQuery(GetSettingsExCommand.class, true);

        //No Mock ones rn
    }

    public static Bundle vxpCall(Context context, String arg, Bundle extras, String method)  {
        CommandService cService = null;
        if(luaCommandService.isCommandPrefix(method)) cService = luaCommandService;
        else if(mocCommandService.isCommandPrefix(method)) cService = mocCommandService;
        else return null;

        CallPacket packet = new CallPacket(context, method, arg, extras, cService.databaseManager.getDatabase(context));
        return cService.handleCall(packet);
    }

    public static Cursor vxpQuery(Context context, String arg, String[] selection, String method)  {
        CommandService cService = null;
        if(luaCommandService.isCommandPrefix(method)) cService = luaCommandService;
        else if(mocCommandService.isCommandPrefix(method)) cService = mocCommandService;
        else return null;

        QueryPacket packet = new QueryPacket(context, method, arg, selection, cService.databaseManager.getDatabase(context));
        return cService.handleQuery(packet);
    }

    public static void handleQuery(XC_MethodHook.MethodHookParam param, String packageName) {
        QueryPacket packet = luaCommandService.tryCreateQueryPacket(param, packageName);
        if (packet != null) param.setResult(luaCommandService.handleQuery(packet));
        else {
            packet = mocCommandService.tryCreateQueryPacket(param, packageName);
            if(packet != null) param.setResult(mocCommandService.handleQuery(packet));
        }
    }

    public static void handeCall(XC_MethodHook.MethodHookParam param, String packageName)  {
        CallPacket packet = luaCommandService.tryCreateCallPacket(param, packageName);
        if(packet != null) {
            if(packet.getMethod().equalsIgnoreCase("getVersion")) param.setResult(BundleUtil.createSingleInt("version", Identra.version));
            else {
                // 0.6.0: observe reports may come from any app; the caller uid comes from the binder, never the client
                if(FoundReportCommand.COMMAND_NAME.equals(packet.getMethod()) && packet.extras != null)
                    packet.extras.putInt(FoundReportCommand.EXTRA_CALLER_UID, android.os.Binder.getCallingUid());
                param.setResult(luaCommandService.handleCall(packet));
            }
        }
        else {
            packet = mocCommandService.tryCreateCallPacket(param, packageName);
            if(packet != null) param.setResult(mocCommandService.handleCall(packet));
        }
    }

    /** 0.6.1: open the database in the background (CollectionMigration + hook definitions from the installed APK). */
    public static void warmUp(final android.content.Context context) {
        Thread t = new Thread(() -> {
            try { luaCommandService.databaseManager.getDatabase(context); }
            catch (Throwable e) { android.util.Log.w("Identra.Bridge", "warm up failed: " + e); }
        }, "identra-warmup");
        t.setDaemon(true);
        t.start();
    }

    /** 0.6.1: before a hot reload: stop worker threads and close the databases of this generation. */
    public static void shutdown() {
        try { luaCommandService.executor.shutdown(); } catch (Throwable ignored) { }
        try { mocCommandService.executor.shutdown(); } catch (Throwable ignored) { }
        try { luaCommandService.databaseManager.reset(true); } catch (Throwable ignored) { }
        try { mocCommandService.databaseManager.reset(true); } catch (Throwable ignored) { }
    }
}
