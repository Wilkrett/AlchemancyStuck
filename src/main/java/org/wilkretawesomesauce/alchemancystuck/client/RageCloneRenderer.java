package org.wilkretawesomesauce.alchemancystuck.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.resources.ResourceLocation;
import org.wilkretawesomesauce.alchemancystuck.entity.RageCloneEntity;

import java.util.UUID;

/**
 * Renders the owner's real skin, looked up client-side via {@code ClientPacketListener#getPlayerInfo}
 * from the UUID synced in {@link RageCloneEntity#getSyncedOwnerUUID}. Falls back to vanilla's own
 * "steve" texture/wide model if that lookup ever comes up empty (e.g. the owner's info hasn't
 * arrived yet).
 *
 * <p>Vanilla itself handles slim vs. wide skins for real players by registering two entirely separate
 * {@code PlayerRenderer} instances and having the render dispatcher pick between them per-player -
 * that path only exists for the player entity type, not for an arbitrary {@code Mob} like this one
 * (one renderer factory per {@link net.minecraft.world.entity.EntityType}). So instead, this renderer
 * builds both variants once (wide/slim body model, wide/slim armor layer pair) and picks per-entity
 * at render time: {@link #render} swaps {@link #model} before delegating, and the two
 * {@link SkinAwareArmorLayer}s each skip rendering when they don't match that entity's skin.
 */
public class RageCloneRenderer extends HumanoidMobRenderer<RageCloneEntity, HumanoidModel<RageCloneEntity>> {

    private static final ResourceLocation FALLBACK_TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/player/wide/steve.png");

    private final HumanoidModel<RageCloneEntity> wideModel;
    private final HumanoidModel<RageCloneEntity> slimModel;

    public RageCloneRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), 0.5F);

        this.wideModel = this.getModel();
        this.slimModel = new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM));

        this.addLayer(new SkinAwareArmorLayer(false,
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
                context.getModelManager()));
        this.addLayer(new SkinAwareArmorLayer(true,
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM_INNER_ARMOR)),
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM_OUTER_ARMOR)),
                context.getModelManager()));
    }

    @Override
    public void render(RageCloneEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        this.model = isSlim(entity) ? this.slimModel : this.wideModel;
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(RageCloneEntity entity) {
        PlayerSkin skin = getOwnerSkin(entity);
        return skin != null ? skin.texture() : FALLBACK_TEXTURE;
    }

    private static boolean isSlim(RageCloneEntity entity) {
        PlayerSkin skin = getOwnerSkin(entity);
        return skin != null && skin.model() == PlayerSkin.Model.SLIM;
    }

    private static PlayerSkin getOwnerSkin(RageCloneEntity entity) {
        UUID ownerUUID = entity.getSyncedOwnerUUID().orElse(null);
        if (ownerUUID == null)
            return null;

        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection == null)
            return null;

        PlayerInfo info = connection.getPlayerInfo(ownerUUID);
        return info != null ? info.getSkin() : null;
    }

    /**
     * Registered once per body-model variant; each instance quietly does nothing for entities that
     * don't match its own {@link #slim} flag, since {@link HumanoidArmorLayer}'s inner/outer armor
     * models are set once in its constructor with no way to swap them afterward the way
     * {@link #render} swaps the base body model.
     */
    private class SkinAwareArmorLayer extends HumanoidArmorLayer<RageCloneEntity, HumanoidModel<RageCloneEntity>, HumanoidModel<RageCloneEntity>> {

        private final boolean slim;

        SkinAwareArmorLayer(boolean slim, HumanoidModel<RageCloneEntity> innerModel, HumanoidModel<RageCloneEntity> outerModel, ModelManager modelManager) {
            super(RageCloneRenderer.this, innerModel, outerModel, modelManager);
            this.slim = slim;
        }

        @Override
        public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, RageCloneEntity entity, float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
            if (isSlim(entity) == this.slim)
                super.render(poseStack, buffer, packedLight, entity, limbSwing, limbSwingAmount, partialTick, ageInTicks, netHeadYaw, headPitch);
        }
    }
}
