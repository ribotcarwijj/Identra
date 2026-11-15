package dev.identra.app.api.xmock;

import android.content.Context;

import java.util.Collection;

import dev.identra.app.api.XResult;
import dev.identra.app.api.configs.MockConfigPacket;
import dev.identra.app.api.cpu.MockCpu;
import dev.identra.app.api.cpu.MockCpuConversions;
import dev.identra.app.api.properties.MockPropPacket;
import dev.identra.app.api.useragent.MockUserAgent;
import dev.identra.app.api.xmock.call.GetMockCpuCommand;
import dev.identra.app.api.xmock.call.GetMockCpusCommand;
import dev.identra.app.api.xmock.call.GetRandomUserAgentCommand;
import dev.identra.app.api.xmock.call.PutMockConfigCommand;
import dev.identra.app.api.xmock.call.PutMockCpuCommand;
import dev.identra.app.api.xmock.call.PutMockPropCommand;

public class XMockCall {
    private static final String TAG = "Identra.XMockCall";

    /*public static String getPropertyValue(Context context, String propertyName) { return getPropertyValue(context, propertyName, "Global"); }
    public static String getPropertyValue(Context context, String propertyName, String packageName) { return getPropertyValue(context, propertyName, packageName, 0); }
    public static String getPropertyValue(Context context, String propertyName, String packageName, int uid) {
        XMockProp packet = new XMockProp(propertyName, uid, packageName);
        Bundle bundle = GetMockPropValueCommand.invoke(context, packet);
        if(bundle == null) {
            if(DebugUtil.isDebug())
                Log.e(TAG, "Bundle from [getPropertyValue] is null: " + packet);

            return MockUtils.NOT_BLACKLISTED;
        }

        return new XMockProp(bundle).getValue();
    }*/

    public static XResult putMockConfig(Context context, MockConfigPacket packet) { return XResult.from(PutMockConfigCommand.invoke(context, packet)); }
    public static XResult putMockProp(Context context, MockPropPacket packet) { return XResult.from(PutMockPropCommand.invoke(context, packet)); }

    public static Collection<MockCpu> getCpuMaps(Context context) { return MockCpuConversions.fromBundleArray(GetMockCpusCommand.invoke(context)); }
    public static MockCpu getSelectedMockCpu(Context context) { return MockCpuConversions.fromBundle(GetMockCpuCommand.invoke(context)); }
    public static XResult putMockCpu(Context context, MockCpu mockCpu) { return XResult.from(PutMockCpuCommand.invoke(context, mockCpu)); }

    public static MockUserAgent getRandomUserAgent(Context context) { return new MockUserAgent(GetRandomUserAgentCommand.invoke(context, MockUserAgent.GET_UA_ALL)); }
    public static MockUserAgent getRandomUserAgent(Context context, String device) { return new MockUserAgent(GetRandomUserAgentCommand.invoke(context, device)); }
}
