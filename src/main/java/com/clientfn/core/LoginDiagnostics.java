package com.clientfn.core;

import net.minecraft.launchwrapper.LogWrapper;

import java.lang.reflect.Method;

/**
 * Centralized login trace helpers so multiplayer issues can be diagnosed from latest.log alone.
 */
public final class LoginDiagnostics {
    private static volatile Method chatToPlainMethod;
    private static volatile Method chatToKeyMethod;

    private LoginDiagnostics() {
    }

    public static void log(String template, Object... args) {
        String message;
        try {
            message = String.format(template, args);
        } catch (Throwable ignored) {
            message = template;
        }
        try {
            LogWrapper.info("[ClientFN][LoginDiag] " + message);
        } catch (Throwable ignored) {
            System.out.println("[ClientFN][LoginDiag] " + message);
        }
    }

    public static String safeChat(Object component) {
        if (component == null) {
            return "<null>";
        }
        try {
            Method method = chatToPlainMethod;
            if (method == null) {
                method = resolveMethod(component.getClass(), "func_150260_c", "getUnformattedText");
                chatToPlainMethod = method;
            }
            if (method != null) {
                Object value = method.invoke(component);
                if (value != null) {
                    return value.toString();
                }
            }
        } catch (Throwable ignored) {
            // Try fallback path below.
        }

        try {
            Method method = chatToKeyMethod;
            if (method == null) {
                method = resolveMethod(component.getClass(), "func_150261_e", "getUnformattedTextForChat");
                chatToKeyMethod = method;
            }
            if (method != null) {
                Object value = method.invoke(component);
                if (value != null) {
                    return value.toString();
                }
            }
        } catch (Throwable ignored) {
            // Fallback to toString below.
        }

        return component.toString();
    }

    public static String safeAddress(Object address) {
        return address == null ? "<null>" : address.toString();
    }

    public static String safeThrowable(Throwable throwable) {
        if (throwable == null) {
            return "<null>";
        }
        String message = throwable.getMessage();
        if (message == null || message.isEmpty()) {
            return throwable.getClass().getName();
        }
        return throwable.getClass().getName() + ": " + message;
    }

    private static Method resolveMethod(Class<?> owner, String... names) {
        Class<?> cursor = owner;
        while (cursor != null) {
            for (String name : names) {
                try {
                    Method method = cursor.getDeclaredMethod(name);
                    method.setAccessible(true);
                    return method;
                } catch (NoSuchMethodException ignored) {
                    // Try next candidate.
                }
            }
            cursor = cursor.getSuperclass();
        }
        return null;
    }
}
