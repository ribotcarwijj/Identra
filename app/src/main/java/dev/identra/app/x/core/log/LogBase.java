package dev.identra.app.x.core.log;

import dev.identra.app.x.ui.core.UserClientAppContext;
import dev.identra.app.x.core.hook.AppXpPacket;
import dev.identra.app.x.core.identity.UserIdentity;

/*
    Lets make interchange able core, do note we need a "service"
 */
public abstract class LogBase {
    //public static RootManager MANAGER = RootUtils.MANAGER;

    public abstract void usage(UserIdentity app);
    public abstract void usage(AppXpPacket app);
    public abstract void usage(UserClientAppContext app);
    public abstract void usage(int userId, String packageName);

    public abstract void error(UserIdentity app);
    public abstract void error(AppXpPacket app);
    public abstract void error(UserClientAppContext app);
    public abstract void error(int userId, String packageName);

    public abstract void deploy(UserIdentity app);
    public abstract void deploy(AppXpPacket app);
    public abstract void deploy(UserClientAppContext app);
    public abstract void deploy(int userId, String packageName);

}
