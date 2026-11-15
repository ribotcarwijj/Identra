package dev.identra.app.x.core;

import dev.identra.app.x.core.database.ActionFlag;
import dev.identra.app.x.core.database.ActionPacket;
import dev.identra.app.x.core.identity.UserIdentity;

public interface IActionObject {
    boolean hasAction();
    boolean hasIdentification();

    int getActionExtra();
    ActionFlag getActionFlags();
    boolean shouldKill();

    boolean isAction(ActionFlag flag);

    int getUid();
    int getUserId(boolean resolveUid);
    String getCategory();

    UserIdentity getUserIdentity();
    ActionPacket getActionPacket();
}
