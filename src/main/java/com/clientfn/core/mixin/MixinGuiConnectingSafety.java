package com.clientfn.mixin.core;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Guards the vanilla GuiConnecting NPE when exitMessage exists but netHandler is still null.
 * This only intercepts that narrow case and keeps all normal connection logic untouched.
 */
@Mixin(targets = "net.minecraft.client.multiplayer.GuiConnecting")
public abstract class MixinGuiConnectingSafety {

    @Inject(method = {"func_73876_c", "updateScreen"}, at = @At("HEAD"), cancellable = true, require = 0)
    private void clientfn$guardNullDisconnectHandler(CallbackInfo ci) {
        try {
            Object manager = clientfn$getFieldValue(this, "field_146371_g", "networkManager");
            if (manager == null) {
                return;
            }

            if (clientfn$invokeBooleanNoArg(manager, "func_150724_d", "isChannelOpen")) {
                return;
            }

            Object reason = clientfn$invokeObjectNoArg(manager, "func_150730_f", "getExitMessage");
            if (reason == null) {
                return;
            }

            Object handler = clientfn$invokeObjectNoArg(manager, "func_150729_e", "getNetHandler");
            if (handler != null) {
                return;
            }

            try { net.minecraft.launchwrapper.LogWrapper.info("[ClientFN] GuiConnecting null netHandler guard triggered, showing disconnect screen."); } catch (Throwable _ig) { System.out.println("[ClientFN] GuiConnecting null netHandler guard triggered, showing disconnect screen."); }
            clientfn$showDisconnectedScreen(reason);
            clientfn$setFieldValue(this, null, "field_146371_g", "networkManager");
            ci.cancel();
        } catch (Throwable ignored) {
            // Keep this guard non-fatal.
        }
    }

    @Unique
    private void clientfn$showDisconnectedScreen(Object reason) {
        try {
            Object minecraft = clientfn$getFieldValue(this, "field_146297_k", "mc");
            if (minecraft == null) {
                return;
            }

            Object parentScreen = clientfn$getFieldValue(this, "field_146374_i", "previousGuiScreen");
            ClassLoader loader = minecraft.getClass().getClassLoader();

            Class<?> guiScreenClass = Class.forName("net.minecraft.client.gui.GuiScreen", false, loader);
            Class<?> chatComponentClass = Class.forName("net.minecraft.util.IChatComponent", false, loader);
            Class<?> disconnectedClass = Class.forName("net.minecraft.client.gui.GuiDisconnected", false, loader);

            Object chatReason = reason;
            if (chatReason == null || !chatComponentClass.isInstance(chatReason)) {
                chatReason = clientfn$toChatComponent(String.valueOf(reason), loader);
            }
            if (chatReason == null) {
                chatReason = clientfn$toChatComponent("Connection lost", loader);
            }

            Constructor<?> ctor = disconnectedClass.getDeclaredConstructor(guiScreenClass, String.class, chatComponentClass);
            ctor.setAccessible(true);
            Object disconnectedScreen = ctor.newInstance(parentScreen, "connect.failed", chatReason);

            Method displayMethod = clientfn$resolveMethod(
                minecraft.getClass(),
                new Class<?>[]{guiScreenClass},
                "func_147108_a",
                "displayGuiScreen"
            );
            if (displayMethod != null) {
                displayMethod.invoke(minecraft, disconnectedScreen);
            }
        } catch (Throwable ignored) {
            // Best-effort only.
        }
    }

    @Unique
    private static Object clientfn$toChatComponent(String text, ClassLoader loader) {
        try {
            Class<?> chatTextClass = Class.forName("net.minecraft.util.ChatComponentText", false, loader);
            Constructor<?> ctor = chatTextClass.getDeclaredConstructor(String.class);
            ctor.setAccessible(true);
            return ctor.newInstance(text);
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Unique
    private static Object clientfn$getFieldValue(Object target, String... names) {
        Field field = clientfn$resolveField(target.getClass(), names);
        if (field == null) {
            return null;
        }
        try {
            return field.get(target);
        } catch (IllegalAccessException ignored) {
            return null;
        }
    }

    @Unique
    private static void clientfn$setFieldValue(Object target, Object value, String... names) {
        Field field = clientfn$resolveField(target.getClass(), names);
        if (field == null) {
            return;
        }
        try {
            field.set(target, value);
        } catch (IllegalAccessException ignored) {
            // Ignore write failures in best-effort path.
        }
    }

    @Unique
    private static Field clientfn$resolveField(Class<?> owner, String... names) {
        Class<?> cursor = owner;
        while (cursor != null) {
            for (String name : names) {
                try {
                    Field field = cursor.getDeclaredField(name);
                    field.setAccessible(true);
                    return field;
                } catch (NoSuchFieldException ignored) {
                    // Try next candidate.
                }
            }
            cursor = cursor.getSuperclass();
        }
        return null;
    }

    @Unique
    private static Method clientfn$resolveMethod(Class<?> owner, Class<?>[] paramTypes, String... names) {
        Class<?> cursor = owner;
        while (cursor != null) {
            for (String name : names) {
                try {
                    Method method = cursor.getDeclaredMethod(name, paramTypes);
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

    @Unique
    private static boolean clientfn$invokeBooleanNoArg(Object target, String... names) {
        try {
            Method method = clientfn$resolveMethod(target.getClass(), new Class<?>[0], names);
            if (method == null) {
                return false;
            }
            Object value = method.invoke(target);
            return value instanceof Boolean && ((Boolean) value);
        } catch (Throwable ignored) {
            return false;
        }
    }

    @Unique
    private static Object clientfn$invokeObjectNoArg(Object target, String... names) {
        try {
            Method method = clientfn$resolveMethod(target.getClass(), new Class<?>[0], names);
            if (method == null) {
                return null;
            }
            return method.invoke(target);
        } catch (Throwable ignored) {
            return null;
        }
    }
}
