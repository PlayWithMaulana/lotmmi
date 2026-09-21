package com.Maul.lotmmi.client;

import com.mojang.blaze3d.vertex.PoseStack;
import de.jakob.lotm.entity.client.beyonder_npc.BeyonderNPCRenderer;
import de.jakob.lotm.entity.custom.BeyonderNPCEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;

public class QilangosRenderer extends BeyonderNPCRenderer {

    private final PlayerModel<BeyonderNPCEntity> qilangosModel;

    public QilangosRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.qilangosModel = new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM), true);
        this.model = this.qilangosModel;
        this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
    }

    @Override
    public void render(BeyonderNPCEntity entity, float entityYaw, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        if (entity.getTargetPlayerUUID().isEmpty()) {
            this.model = this.qilangosModel;
        }
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }
}
