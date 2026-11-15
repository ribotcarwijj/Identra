package dev.identra.app.x.core.commands;

import android.database.Cursor;
import android.util.Log;

import java.util.concurrent.Callable;

import dev.identra.app.XPolicy;
import dev.identra.app.x.Str;
import dev.identra.app.x.runtime.RuntimeUtils;
import dev.identra.app.x.core.commands.packet.QueryPacket;

public class QueryInvoker implements Callable<Cursor> {
    private static final String TAG = "Identra.QueryInvoker";

    public static QueryInvoker create(QueryPacket packet, QueryCommandHandlerEx handler) { return new QueryInvoker(packet, handler); }

    private final QueryPacket packet;
    private final QueryCommandHandlerEx handler;

    private Throwable exception;
    private boolean isRunning = false;

    public boolean isRunning() { return this.isRunning; }
    public Throwable getException() { return this.exception; }

    public QueryInvoker(QueryPacket packet, QueryCommandHandlerEx handler) {
        this.packet = packet;
        this.handler = handler;
    }

    @Override
    public Cursor call() throws Exception {
        XPolicy policy = XPolicy.policyAllowRW();
        try {
            isRunning = true;
            return handler.handle(packet);
        }catch (Throwable e) {
            exception = e;
            Log.e(TAG, Str.fm("Query Invoker Error! Command:%s  Error:%s  Packet:%s  Stack:\n%s", handler.name, e, Str.noNL(packet), RuntimeUtils.getStackTraceSafeString(e)));
            return null;
        } finally {
            policy.revert();
            isRunning = false;
        }
    }
}
