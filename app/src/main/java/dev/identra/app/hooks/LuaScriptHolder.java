package dev.identra.app.hooks;

import dev.identra.app.Identra;

public class LuaScriptHolder {
    public String script;

    public LuaScriptHolder(String script) {
        String[] lines = script.split("\\r?\\n");
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            if (!line.startsWith("--"))
                sb.append(line.trim());
            sb.append("\n");
        }
        this.script = sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof LuaScriptHolder))
            return false;
        LuaScriptHolder other = (LuaScriptHolder) obj;
        return this.script.equals(other.script);
    }

    @Override
    public int hashCode() {
        return this.script.hashCode();
    }
}
