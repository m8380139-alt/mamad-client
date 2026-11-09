package ir.mamadcraft.client.mixin;

import ir.mamadcraft.client.cosmetics.CapeManager;
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
}
