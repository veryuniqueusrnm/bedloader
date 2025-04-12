package net.veryuniqueusrnm.bedloader.client.mixin;

import net.veryuniqueusrnm.bedloader.client.interfaces.BedrockPlayerInfo;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRendererDispatcherMixin {

    @Inject(
            method = "getRenderer",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/player/AbstractClientPlayer;getSkin()Lnet/minecraft/client/resources/PlayerSkin;"),
            cancellable = true
    )
    public void getRenderer(Entity entity, CallbackInfoReturnable<EntityRenderer<?, ?>> cir) {
        PlayerInfo playerListEntry = ((BedrockAbstractClientPlayerEntity) entity).bedrockskinutility$getPlayerListEntry();
        if (playerListEntry != null) {
            PlayerRenderer renderer = ((BedrockPlayerInfo) playerListEntry).bedrockskinutility$getModel();
            if (renderer != null) {
                cir.setReturnValue(renderer);
            }
        }
    }
}
