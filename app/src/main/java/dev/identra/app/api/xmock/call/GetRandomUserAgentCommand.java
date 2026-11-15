package dev.identra.app.api.xmock.call;

import android.content.Context;
import android.os.Bundle;

import dev.identra.app.api.XProxyContent;
import dev.identra.app.api.xstandard.CallCommandHandler;
import dev.identra.app.api.xstandard.command.CallPacket_old;
import dev.identra.app.api.useragent.MockUserAgent;
import dev.identra.app.api.xmock.database.MockUserAgentManager;
import dev.identra.app.utilities.BundleUtil;

public class GetRandomUserAgentCommand extends CallCommandHandler {
    @SuppressWarnings("unused")
    public GetRandomUserAgentCommand() {
        name = "getRandomUserAgent";
        requiresPermissionCheck = false;
    }

    @Override
    public Bundle handle(CallPacket_old commandData) throws Throwable {
        return MockUserAgentManager.getRandomUserAgent(
                commandData.getContext(),
                commandData.getDatabase(),
                commandData.getExtraString(MockUserAgent.Table.FIELD_DEVICE, MockUserAgent.GET_UA_ANDROID)).toBundle();
    }

    public static Bundle invoke(Context context, String name) {
        return XProxyContent.mockCall(
                context,
                "getRandomUserAgent",
                BundleUtil.createSingleString(MockUserAgent.Table.FIELD_DEVICE, name));
    }
}
