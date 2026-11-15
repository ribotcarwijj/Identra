package dev.identra.app;

public class XUiGroup {
    public String name;
    public String title;

    @Override
    public String toString() {
        return title;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof XUiGroup))
            return false;
        XUiGroup other = (XUiGroup) obj;
        return this.name.equals(other.name);
    }
}
