package dev.identra.app.x.ui.core;

import android.content.Context;

import dev.identra.app.R;
import dev.identra.app.XUtil;

public class CoreUiColors {
    public static int getSwipeRefreshColor(Context context) { return XUtil.resolveColor(context, R.attr.colorAccent); }
}
