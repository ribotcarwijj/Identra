package dev.identra.app.x.core.settings.test;

import dev.identra.app.x.runtime.reflect.DynamicType;
import dev.identra.app.x.core.LibUtil;

public class EventTrigger {
    private static final String TAG = LibUtil.generateTag(EventTrigger.class);

    public String from;
    public int code;
    public Object data;

    public void setCode(EventKind kind) { this.code = kind.getValue(); }

    public boolean isCheckEvent() { return code == EventKind.CHECK.getValue(); }
    public boolean isDataNull() { return data == null; }
    public boolean isType(EventKind kind) { return kind.getValue() == code; }
    public boolean isTypeUnknown() { return code == EventKind.UNKNOWN.getValue();  }

    public <T> T dataAs() { return data == null ? null : (T)data; }
    public <T> T tryCast() { return DynamicType.tryCast(data); }
}
