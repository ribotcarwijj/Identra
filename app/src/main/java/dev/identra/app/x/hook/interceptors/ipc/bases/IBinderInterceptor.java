package dev.identra.app.x.hook.interceptors.ipc.bases;

import dev.identra.app.XParam;
import dev.identra.app.x.hook.interceptors.ipc.holders.InterfaceBinderData;

public interface IBinderInterceptor {
    String getInterfaceName();
    String getSettingName();
    boolean isEnabled(XParam param);
    boolean intercept(XParam param, InterfaceBinderData helper);
}
