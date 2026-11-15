package dev.identra.app.x.core.identity;

public interface IIdentification {
    boolean isGlobal();
    int getUid();
    int getUserId(boolean resolve);
    String getCategory();

    int getCallingUid();
    int getCallingUserId();
    String getCallingPackageName();

    UserIdentity asObject();
}
