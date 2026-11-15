package dev.identra.app.api.xmock.call;

import android.content.Context;
import android.os.Bundle;

import dev.identra.app.api.XProxyContent;
import dev.identra.app.api.xstandard.CallCommandHandler;
import dev.identra.app.api.xstandard.command.CallPacket_old;
import dev.identra.app.api.cpu.MockCpu;
import dev.identra.app.api.xmock.provider.MockCpuProvider;
import dev.identra.app.utilities.BundleUtil;

public class GetMockCpuCommand extends CallCommandHandler {
    @SuppressWarnings("unused")
    public GetMockCpuCommand() {
        name = "getMockCpu";
        requiresPermissionCheck = false;
    }

    @Override
    public Bundle handle(CallPacket_old commandData) throws Throwable {
        MockCpu packet = commandData.readExtrasAs(MockCpu.class);
        if(packet.getName() == null) {
            return BundleUtil.createFromISerial(
                    MockCpuProvider.getSelectedCpuMap(
                            commandData.getContext(),
                            commandData.getDatabase()), true);
        }
        return null;
    }

    public static Bundle invoke(Context context, String name) {
        return XProxyContent.mockCall(
                context,
                "getMockCpu",
                BundleUtil.createSingleString("name", name));
    }

    public static Bundle invoke(Context context) {
        return XProxyContent.mockCall(
                context,
                "getMockCpu",
                new Bundle());
    }
}
