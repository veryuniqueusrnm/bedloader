package net.veryuniqueusrnm.bedloader.client.mixin;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LivingEntityRenderer.class)
public interface PlayerEntityRendererChangeModel {
    @Accessor("model")
    void bedloader$setModel(EntityModel<?> model);
}
