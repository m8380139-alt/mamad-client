package ir.mamadcraft.client.mixin;

import ir.mamadcraft.client.cosmetics.CapeManager;
import ir.mamadcraft.client.cosmetics.SkinManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayer.class)
public abstract class MixinAbstractClientPlayer {

    @Inject(method = "getLocationCape", at = @At("HEAD"), cancellable = true)
    private void mamadclient$getLocationCape(CallbackInfoReturnable<ResourceLocation> cir) {
        ResourceLocation cape = CapeManager.get(((AbstractClientPlayer) (Object) this).getName());
        if (cape != null) cir.setReturnValue(cape);
    }

    /** اسکین دلخواه: فقط روی بازیکن خودمان. */
    @Inject(method = "getLocationSkin()Lnet/minecraft/util/ResourceLocation;", at = @At("HEAD"), cancellable = true)
    private void mamadclient$getLocationSkin(CallbackInfoReturnable<ResourceLocation> cir) {
        if ((Object) this != Minecraft.getMinecraft().thePlayer) return;
        ResourceLocation skin = SkinManager.get();
        if (skin != null) cir.setReturnValue(skin);
    }

    /** مدل دست: default (Steve) یا slim (Alex). */
    @Inject(method = "getSkinType", at = @At("HEAD"), cancellable = true)
    private void mamadclient$getSkinType(CallbackInfoReturnable<String> cir) {
        if ((Object) this != Minecraft.getMinecraft().thePlayer) return;
        if (SkinManager.get() != null) cir.setReturnValue(SkinManager.skinType());
    }
}
