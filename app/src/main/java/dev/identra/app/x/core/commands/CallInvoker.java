package dev.identra.app.x.core.commands;

import android.os.Bundle;
import android.util.Log;

import java.util.concurrent.Callable;

import dev.identra.app.XPolicy;
import dev.identra.app.x.Str;
import dev.identra.app.x.runtime.RuntimeUtils;
import dev.identra.app.x.core.commands.packet.CallPacket;
import dev.identra.app.x.core.commands.packet.QueryPacket;

public class CallInvoker implements Callable<Bundle> {
    private static final String TAG = "Identra.CallInvoker";
    public static CallInvoker create(CallPacket packet, CallCommandHandlerEx handler) { return new CallInvoker(packet, handler); }

    private final CallPacket packet;
    private final CallCommandHandlerEx handler;

    private Throwable exception;
    private boolean isRunning = false;

    public boolean isRunning() { return this.isRunning; }
    public Throwable getException() { return this.exception; }

    public CallInvoker(CallPacket packet, CallCommandHandlerEx handler) {
        this.packet = packet;
        this.handler = handler;
    }

    @Override
    public Bundle call() {
        XPolicy policy = XPolicy.policyAllowRW();
        try {
            isRunning = true;
            return handler.handle(packet);
        }catch (Throwable e) {
            exception = e;
            Log.e(TAG, Str.fm("Call Invoker Error! Command:%s  Error:%s  Packet:%s  Stack:\n%s", handler.name, e, Str.noNL(packet), RuntimeUtils.getStackTraceSafeString(e)));
            return null;
        } finally {
            policy.revert();
            isRunning = false;
        }
    }
}
