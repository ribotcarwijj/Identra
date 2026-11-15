package dev.identra.app.interceptors.shell.handlers;

import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import dev.identra.app.DebugUtil;
import dev.identra.app.api.xstandard.interfaces.ICommandIntercept;
import dev.identra.app.interceptors.UserContextMaps;
import dev.identra.app.interceptors.shell.CommandInterceptor;
import dev.identra.app.interceptors.shell.ShellInterception;
import dev.identra.app.x.hook.filter.PropRules;

/**
 * getprop through Runtime.exec / ProcessBuilder (0.5.0 rewrite).
 *
 * The real command runs; its output is then rewritten with the same property rules the Java
 * SystemProperties hooks use ({@link PropRules}), so the shell lens and the Java lens agree:
 *  - "getprop KEY [DEFAULT]": the spoofed value when KEY has a rule and really exists
 *    (a hidden key prints DEFAULT or an empty line, like a missing one)
 *  - "getprop" and pipelines ("getprop | grep model"): every "[key]: [value]" line is rewritten,
 *    hidden keys are dropped, everything else is passed through unchanged
 *
 * The older implementation looked the keys up in the legacy property-map table, which the
 * API 102 port no longer fills, so getprop always returned the real values.
 */
public class GetPropIntercept extends CommandInterceptor implements ICommandIntercept {
    private static final String TAG = "Identra.GetPropIntercept";

    public static final String GETPROP_INTERCEPT_SETTING = "intercept.shell.getprop.bool";
    private static final List<String> OPERATORS = Arrays.asList("|", "||", "&&", ";", ">", ">>", "<", "&");

    @SuppressWarnings("unused")
    public GetPropIntercept() { this.command = "getprop"; this.setting = GETPROP_INTERCEPT_SETTING; }

    private static boolean isGetprop(String token) {
        if (token == null) return false;
        String t = token.trim();
        return t.equals("getprop") || t.endsWith("/getprop");
    }

    @Override
    public boolean interceptCommand(ShellInterception result) {
        try {
            if (result == null || !result.isValid) return false;
            UserContextMaps maps = result.getUserMaps();
            if (maps == null || !maps.isSettingsValid()) return false;
            if (!keepGoing(maps, GETPROP_INTERCEPT_SETTING)) return false;
            Map<String, String> settings = maps.getSettings();
            String[] cl = result.commandLine;
            if (cl == null || cl.length == 0) return false;

            int at = -1;
            for (int i = 0; i < cl.length; i++) if (isGetprop(cl[i])) { at = i; break; }
            if (at < 0) return false;

            List<String> args = new ArrayList<>();
            boolean pipeline = false;
            for (int i = at + 1; i < cl.length; i++) {
                String t = cl[i] == null ? "" : cl[i].trim();
                if (t.isEmpty()) continue;
                if (OPERATORS.contains(t) || t.startsWith("|") || t.startsWith(";") || t.startsWith("&")) { pipeline = true; break; }
                args.add(stripQuotes(t));
            }
            // anything before getprop other than a shell ("sh -c") also makes it a pipeline
            for (int i = 0; i < at; i++) {
                String t = cl[i] == null ? "" : cl[i].trim();
                if (OPERATORS.contains(t)) pipeline = true;
            }

            if (!pipeline && !args.isEmpty()) {
                String key = args.get(0);
                if (key.startsWith("-")) return false;                 // -T / -Z: types and contexts
                if (!PropRules.hasRule(settings, key)) return false;  // untouched: do not even read the process
                String out = result.getCommandOutput();
                String real = out == null ? "" : out.replaceAll("[\\r\\n]+$", "");
                String spoof = PropRules.resolve(settings, key, real);
                String replaced;
                if (spoof == null) replaced = out == null ? "" : out;  // keep the real output (already read)
                else if (spoof.isEmpty()) replaced = (args.size() > 1 ? args.get(1) : "") + "\n";
                else replaced = spoof + "\n";
                if (DebugUtil.isDebug()) Log.d(TAG, "getprop " + key + " real=[" + real + "] -> [" + replaced.trim() + "]");
                result.setNewValue(replaced);
                result.setIsMalicious(true);
                return true;
            }

            // full listing or a pipeline: rewrite "[key]: [value]" lines
            String out = result.getCommandOutput();
            if (out == null) return false;
            String rewritten = PropRules.rewriteListing(settings, out);
            result.setNewValue(rewritten == null ? out : rewritten);
            result.setIsMalicious(true);   // the process output was consumed: always hand back a process
            return true;
        } catch (Throwable t) {
            Log.e(TAG, "getprop interception failed: " + t);
            return false;
        }
    }

    private static String stripQuotes(String s) {
        if (s.length() >= 2 && ((s.startsWith("'") && s.endsWith("'")) || (s.startsWith("\"") && s.endsWith("\""))))
            return s.substring(1, s.length() - 1);
        return s;
    }
}
