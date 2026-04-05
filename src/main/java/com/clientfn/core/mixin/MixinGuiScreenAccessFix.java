package com.clientfn.mixin.core;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Widens GuiScreen.field_146297_k (mc) from protected to public.
 *
 * The vanilla jar shipped with ClientFN is missing the synthetic accessor
 * method that javac normally generates for anonymous inner-class access to
 * protected fields. GuiConnecting$1.run() uses a raw getfield bytecode to
 * reach this field, which triggers IllegalAccessError at runtime because
 * GuiConnecting$1 extends Thread, not GuiScreen.
 *
 * Mixin automatically widens the target field's access to match the shadow
 * declaration, so declaring a public shadow here promotes the field to public
 * and fixes the IllegalAccessError.
 */
@Mixin(targets = "net.minecraft.client.gui.GuiScreen")
public abstract class MixinGuiScreenAccessFix {

    @Shadow
    public Minecraft field_146297_k;
}
