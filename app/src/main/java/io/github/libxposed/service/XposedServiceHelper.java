// Vendored from libxposed/service 102.0.0 (Apache-2.0, https://github.com/libxposed/service).
// Identra 0.6.1: rewritten to Java 8 syntax (record -> final class, no var/switch expressions)
// because AGP's D8 cannot desugar the published record in the dependency transform.
package io.github.libxposed.service;

import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.HashSet;
import java.util.Set;

@SuppressWarnings("unused")
public final class XposedServiceHelper {

    /**
     * Callback interface for Xposed service.
     */
    public interface OnServiceListener {
        /**
         * Callback when the service is connected.<br/>
         * This method could be called multiple times if multiple Xposed frameworks exist.
         *
         * @param service Service instance
         */
        void onServiceBind(@NonNull XposedService service);

        /**
         * Callback when the service is dead.
         */
        void onServiceDied(@NonNull XposedService service);
    }

    private static final String TAG = "XposedServiceHelper";
    private static final Set<XposedService> mCache = new HashSet<>();
    private static OnServiceListener mListener = null;

    static void onBinderReceived(IBinder binder) {
        if (binder == null) return;
        synchronized (mCache) {
            try {
                final XposedService service = new XposedService(IXposedService.Stub.asInterface(binder));
                if (mListener == null) {
                    mCache.add(service);
                } else {
                    binder.linkToDeath(() -> mListener.onServiceDied(service), 0);
                    mListener.onServiceBind(service);
                }
            } catch (Throwable t) {
                Log.e(TAG, "onBinderReceived", t);
            }
        }
    }

    /**
     * Register a ServiceListener to receive service binders from Xposed frameworks.<br/>
     * This method should only be called once.
     *
     * @param listener Listener to register
     */
    public static void registerListener(OnServiceListener listener) {
        synchronized (mCache) {
            mListener = listener;
            if (!mCache.isEmpty()) {
                for (java.util.Iterator<XposedService> it = mCache.iterator(); it.hasNext(); ) {
                    try {
                        final XposedService service = it.next();
                        service.asInterface().asBinder().linkToDeath(() -> mListener.onServiceDied(service), 0);
                        mListener.onServiceBind(service);
                    } catch (Throwable t) {
                        Log.e(TAG, "registerListener", t);
                        it.remove();
                    }
                }
                mCache.clear();
            }
        }
    }
}
