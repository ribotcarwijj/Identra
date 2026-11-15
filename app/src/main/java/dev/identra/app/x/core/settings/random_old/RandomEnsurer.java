package dev.identra.app.x.core.settings.random_old;

import android.util.Log;

import dev.identra.app.x.runtime.RuntimeUtils;

public class RandomEnsurer {
    private static final String TAG = "Identra.RandomEnsurer";
    private static final int DEFAULT_FAIL_TRIES = 50;
    public interface IInvokeRandom<T> {
        boolean canRandomize();
        T randomize();
        boolean isSame(T newObject);
        T getOriginal();
    }

    public static <T> T ensureRandom(IInvokeRandom<T> randomWrapper) { return ensureRandom(randomWrapper, DEFAULT_FAIL_TRIES); }
    public static <T> T ensureRandom(IInvokeRandom<T> randomWrapper, int failTries) {
        if(randomWrapper == null || !randomWrapper.canRandomize()) return null;
        T original = null;
        try {
            original = randomWrapper.getOriginal();
            int tries = 0;
            while (tries < failTries) {
                T random = randomWrapper.randomize();
                if(!randomWrapper.isSame(random)) return random;
                tries++;
            }

            Log.e(TAG, "Failed to ensure that the randomized value Generated was not the Same as last Instance, failed [" + tries + "] Times... Odd, Stack=" + RuntimeUtils.getStackTraceSafeString());
            return original;
        } catch (Exception e) {
            Log.e(TAG, "Failed to ensure that the randomized value Generated was Random not the Same as last instance. Error=" + e + " Stack=" + RuntimeUtils.getStackTraceSafeString(e));
            return original;
        }
    }
}
