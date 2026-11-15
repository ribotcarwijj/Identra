package dev.identra.app.api.core.call;

import android.content.Context;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.List;

import dev.identra.app.XDatabaseOld;
import dev.identra.app.api.XProxyContent;
import dev.identra.app.api.XResult;
import dev.identra.app.api.hook.IdentraHook;
import dev.identra.app.api.xstandard.CallCommandHandler;
import dev.identra.app.api.xstandard.command.CallPacket_old;
import dev.identra.app.api.xstandard.database.DatabaseHelp;
import dev.identra.app.api.xstandard.database.SqlQuerySnake;

public class CleanHooksCommand extends CallCommandHandler {
    public CleanHooksCommand() {
        name = "clearHooksCommand";
        requiresPermissionCheck = true;
    }

    @Override
    public Bundle handle(CallPacket_old commandData) throws Throwable {
        int failed = 0;
        int succeeded = 0;
        int found = 0;
        XResult res = XResult.create().setMethodName("clearHooksCommand");
        try {
            XDatabaseOld db = commandData.getDatabase();
            List<IdentraHook> hooks = new ArrayList<>(DatabaseHelp.getFromDatabase(db, "hook", IdentraHook.class));
            for (IdentraHook hook : hooks) {
                if (hook.getObjectId().startsWith(dev.identra.IdentraNs.COLLECTION + ".")) {
                    found++;
                    SqlQuerySnake snake = SqlQuerySnake.create(db, "hook")
                            .whereColumn("id", hook.getObjectId());

                    if(DatabaseHelp.deleteItem(snake)) succeeded++;
                    else failed++;
                }
            }
        }catch (Exception e) {
            return res.setFailed("\nFailed: e=" + e + "\nFound=" + found + "\nFailed=" + failed + "\nSucceeded=" + succeeded).toBundle();
        }

        return res.setSucceeded("\nFound=" + found + "\nFailed=" + failed + "\nSucceeded=" + succeeded).toBundle();
    }

    public static XResult invokeEx(Context context) { return new XResult(invoke(context)); }
    public static Bundle invoke(Context context) {
        return XProxyContent.luaCall(
                context,
                "clearHooksCommand",
                new Bundle());
    }
}
