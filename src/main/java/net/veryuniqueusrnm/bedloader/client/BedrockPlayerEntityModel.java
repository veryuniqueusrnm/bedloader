package net.veryuniqueusrnm.bedloader.client;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.LivingEntity;

public class BedrockPlayerEntityModel<T extends LivingEntity> extends PlayerModel {
    public BedrockPlayerEntityModel(ModelPart root) {
        super(root, false);
    }
}
