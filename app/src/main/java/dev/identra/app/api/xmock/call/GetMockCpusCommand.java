package dev.identra.app.api.xmock.call;

import android.content.Context;
import android.os.Bundle;

import dev.identra.app.api.XProxyContent;
import dev.identra.app.api.xstandard.CallCommandHandler;
import dev.identra.app.api.xstandard.command.CallPacket_old;
import dev.identra.app.api.cpu.MockCpuConversions;
import dev.identra.app.api.xmock.database.MockCpuManager;

public class GetMockCpusCommand extends CallCommandHandler {
    public static GetMockCpusCommand create() { return new GetMockCpusCommand(); };

    public GetMockCpusCommand() {
        name = "getMockCpus";
        requiresPermissionCheck = false;
    }

    @Override
    public Bundle handle(CallPacket_old commandData) throws Throwable {
        return MockCpuConversions.toBundleArray(
                MockCpuManager.getCpuMaps(
                        commandData.getContext(),
                        commandData.getDatabase()));
    }

    public static Bundle invoke(Context context) {
        return XProxyContent.mockCall(
                context,
                "getMockCpus");
    }
}
