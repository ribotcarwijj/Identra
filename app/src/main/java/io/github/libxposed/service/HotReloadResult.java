package io.github.libxposed.service;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;


/**
 * Result of an asynchronous hot reload request.
 *
 * @param status  The completion status
 * @param message Optional framework-provided diagnostic message. For {@link Status#FAILED}, a
 *                null message means the old module refused reload by returning {@code false} from
 *                {@code onHotReloading}; a non-null message describes a reload exception.
 */
public final class HotReloadResult {
    @NonNull private final Status status;
    @Nullable private final String message;

    public HotReloadResult(@NonNull Status status, @Nullable String message) {
        this.status = status;
        this.message = message;
    }

    /** The completion status. */
    @NonNull public Status status() { return status; }

    /** Optional framework-provided diagnostic message. */
    @Nullable public String message() { return message; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof HotReloadResult)) return false;
        HotReloadResult r = (HotReloadResult) o;
        return status == r.status && java.util.Objects.equals(message, r.message);
    }

    @Override public int hashCode() { return java.util.Objects.hash(status, message); }

    @Override public String toString() { return "HotReloadResult[status=" + status + ", message=" + message + "]"; }

    /**
     * Hot reload completion status.
     */
    public enum Status {
        /**
         * Hot reload completed successfully.
         */
        SUCCEEDED,

        /**
         * The old module refused reload, or hot reload raised an exception.
         * <p>
         * When the old module refuses reload by returning {@code false} from
         * {@code onHotReloading}, {@link HotReloadResult#message()} is null. When reload fails
         * because of an exception, the message contains a framework-provided diagnostic string.
         * </p>
         */
        FAILED,

        /**
         * The target does not support hot reload.
         * <p>
         * For example, this can be returned for modules that do not declare exactly one Java
         * entry class or targets for which the framework cannot provide a valid new module
         * generation.
         * </p>
         */
        UNSUPPORTED,

        /**
         * The target is already being hot-reloaded.
         */
        IN_PROGRESS,

        /**
         * The target process died before hot reload could complete.
         */
        PROCESS_DIED
    }

    static HotReloadResult from(int code, @Nullable String message) {
        Status status;
        switch (code) {
            case IXposedService.HOT_RELOAD_SUCCEEDED: status = Status.SUCCEEDED; break;
            case IXposedService.HOT_RELOAD_FAILED: status = Status.FAILED; break;
            case IXposedService.HOT_RELOAD_UNSUPPORTED: status = Status.UNSUPPORTED; break;
            case IXposedService.HOT_RELOAD_IN_PROGRESS: status = Status.IN_PROGRESS; break;
            case IXposedService.HOT_RELOAD_PROCESS_DIED: status = Status.PROCESS_DIED; break;
            default: throw new XposedService.ServiceException("Invalid hot reload status code: " + code);
        }
        return new HotReloadResult(status, message);
    }
}
