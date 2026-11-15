package dev.identra.app.api.hook;

import android.os.Parcel;

public class IdentraHookAssets extends IdentraHook {
    public IdentraHookAssets() { }
    public IdentraHookAssets(Parcel in) { fromParcel(in); }
    public IdentraHookAssets(HookDatabaseEntry hookDb) { fromBundle(hookDb.toBundle()); }

    public void setIsBuiltIn(Boolean isBuiltin) { if(isBuiltin != null) this.builtin = isBuiltin; }
    public void setLuaScript(String luaScript) { if(luaScript != null) this.luaScript = luaScript; }

}
