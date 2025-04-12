package net.veryuniqueusrnm.bedloader.client.mixin;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.layers.CapeLayer;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(CapeLayer.class)
public class CapeFeatureRendererMixin {
    @Redirect(
            method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/renderer/entity/state/PlayerRenderState;FF)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/RenderType;entitySolid(Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/client/renderer/RenderType;"),
            require = 0 // Fail safely if other mods overwrite this
    )
    public RenderType solidToTranslucent(ResourceLocation texture) {
        if (texture.getNamespace().equals("geyserskinmanager")) {
            // Capes can be translucent in Bedrock
            return RenderType.entityTranslucent(texture, true);
        }
        return RenderType.entitySolid(texture);
    }
}
