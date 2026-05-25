package com.vikingposter.renderer;

import com.mojang.blaze3d.vertex.*;
import com.vikingposter.blockentity.PosterBlockEntity;
import com.vikingposter.blocks.PosterBlock;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
public class PosterBlockEntityRenderer implements BlockEntityRenderer<PosterBlockEntity> {

    // Wood frame margin around the image (in blocks). 1 px = 1/16 block → ~2px each side.
    private static final float FRAME_MARGIN = 2f / 16f;

    // Single grunge texture provided by the user. Effect uses 3 layered tricks:
    //  1. Stack N grunge passes (1..5), each with a distinct UV rotation/flip → pattern
    //     accumulates visibly with degradation (not just stronger opacity).
    //  2. Darker, more saturated brown tint so each pass has weight even alone.
    //  3. A pure-black darken pass scaled by level → image looks faded/aged underneath the grunge.
    private static final ResourceLocation DEGRADED_TEXTURE =
        ResourceLocation.fromNamespaceAndPath("vikingposter", "textures/block/degraded_overlay.png");

    // Alpha per individual grunge layer (constant). Stacked alpha grows as 1-(1-a)^N:
    // L1=~35%, L2=~58%, L3=~73%, L4=~83%, L5=~89%.
    private static final int GRUNGE_LAYER_ALPHA = 90;
    private static final int GRUNGE_TINT_R = 60, GRUNGE_TINT_G = 40, GRUNGE_TINT_B = 15;

    // Darken pass alpha per level (~6% → ~40%).
    private static final int[] DARKEN_ALPHA = { 0, 15, 35, 60, 85, 105 };

    // 5 distinct UV permutations applied per layer index (1..5). Vertex order: BL, TL, TR, BR.
    private static final float[][][] LAYER_UVS = {
        // unused slot 0
        {{0,1}, {0,0}, {1,0}, {1,1}},
        // layer 1: 0°
        {{0,1}, {0,0}, {1,0}, {1,1}},
        // layer 2: 90° CW
        {{0,0}, {1,0}, {1,1}, {0,1}},
        // layer 3: 180°
        {{1,0}, {1,1}, {0,1}, {0,0}},
        // layer 4: 270° CW
        {{1,1}, {0,1}, {0,0}, {1,0}},
        // layer 5: flip H
        {{1,1}, {1,0}, {0,0}, {0,1}},
    };

    // Direct vanilla PNG path — atlas binding fails in BER pass; standalone texture
    // is auto-loaded by TextureManager and binds reliably with entity* render types.
    private static final ResourceLocation WOOD_TEXTURE =
        ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/spruce_planks.png");

    public PosterBlockEntityRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(PosterBlockEntity be, float partialTick, PoseStack ps,
                       MultiBufferSource bufferSource, int packedLight, int overlay) {

        BlockState state = be.getBlockState();
        if (!(state.getBlock() instanceof PosterBlock)) return;

        Direction facing = state.getValue(PosterBlock.FACING);
        int light = LightTexture.FULL_BRIGHT;

        String url = be.getImageUrl();
        boolean hasImage = url != null && !url.isBlank();

        float w  = be.getDisplayWidth();
        float h  = be.getDisplayHeight();
        float ox = be.getDisplayOffsetX();
        float oy = be.getDisplayOffsetY();

        ps.pushPose();
        ps.translate(0.5, 0.5, 0.5);
        float yRot = switch (facing) {
            case NORTH -> 0f;
            case SOUTH -> 180f;
            case EAST  -> 270f;
            case WEST  -> 90f;
            default    -> 0f;
        };
        ps.mulPose(com.mojang.math.Axis.YP.rotationDegrees(yRot));
        ps.translate(0, 0, 0.4375f - 0.002f);

        if (hasImage) {
            float ix0 = -w * 0.5f + ox, ix1 = w * 0.5f + ox;
            float iy0 = -h * 0.5f + oy, iy1 = h * 0.5f + oy;
            // Wood backdrop extends a few pixels beyond the image — "panneau" look.
            float bx0 = ix0 - FRAME_MARGIN, bx1 = ix1 + FRAME_MARGIN;
            float by0 = iy0 - FRAME_MARGIN, by1 = iy1 + FRAME_MARGIN;
            renderWoodBackground(ps, bufferSource, light, bx0, by0, bx1, by1);
            renderImage(be, ps, bufferSource, light, ix0, iy0, ix1, iy1);
            int lvl = be.getDegradationLevel();
            if (lvl > 0) renderDegradedOverlay(ps, bufferSource, light, ix0, iy0, ix1, iy1, lvl);
        } else {
            // Empty poster face: 1x1 spruce plank matching the inventory item appearance.
            renderWoodBackground(ps, bufferSource, light, -0.5f, -0.5f, 0.5f, 0.5f);
        }

        ps.popPose();
    }

    private void renderImage(PosterBlockEntity be, PoseStack ps, MultiBufferSource bufferSource,
                             int light, float x0, float y0, float x1, float y1) {
        ResourceLocation tex = ImageCache.getOrLoad(be.getImageUrl());
        VertexConsumer vc = bufferSource.getBuffer(RenderType.entityTranslucentCull(tex));
        Matrix4f m = ps.last().pose();
        vc.vertex(m, x0, y0, 0).color(255,255,255,255).uv(0,1).overlayCoords(overlay()).uv2(light).normal(0,0,-1).endVertex();
        vc.vertex(m, x0, y1, 0).color(255,255,255,255).uv(0,0).overlayCoords(overlay()).uv2(light).normal(0,0,-1).endVertex();
        vc.vertex(m, x1, y1, 0).color(255,255,255,255).uv(1,0).overlayCoords(overlay()).uv2(light).normal(0,0,-1).endVertex();
        vc.vertex(m, x1, y0, 0).color(255,255,255,255).uv(1,1).overlayCoords(overlay()).uv2(light).normal(0,0,-1).endVertex();
    }

    private void renderWoodBackground(PoseStack ps, MultiBufferSource bufferSource,
                                       int light, float x0, float y0, float x1, float y1) {
        ps.pushPose();
        ps.translate(0, 0, 0.0008f);

        VertexConsumer vc = bufferSource.getBuffer(RenderType.entityCutoutNoCull(WOOD_TEXTURE));
        Matrix4f m = ps.last().pose();

        // Tile the texture: 1 block = 1 full spruce_planks repeat. Default GL_REPEAT wraps UV>1.
        float uw = x1 - x0;
        float vh = y1 - y0;
        int ov = OverlayTexture.NO_OVERLAY;

        // Front face (-Z normal)
        vc.vertex(m, x0, y0, 0).color(255,255,255,255).uv(0,  vh).overlayCoords(ov).uv2(light).normal(0,0,-1).endVertex();
        vc.vertex(m, x0, y1, 0).color(255,255,255,255).uv(0,  0 ).overlayCoords(ov).uv2(light).normal(0,0,-1).endVertex();
        vc.vertex(m, x1, y1, 0).color(255,255,255,255).uv(uw, 0 ).overlayCoords(ov).uv2(light).normal(0,0,-1).endVertex();
        vc.vertex(m, x1, y0, 0).color(255,255,255,255).uv(uw, vh).overlayCoords(ov).uv2(light).normal(0,0,-1).endVertex();
        ps.popPose();
    }

    private void renderDegradedOverlay(PoseStack ps, MultiBufferSource bufferSource,
                                       int light, float x0, float y0, float x1, float y1, int level) {
        int lvl = Math.max(1, Math.min(LAYER_UVS.length - 1, level));

        ps.pushPose();
        // Negative Z = toward the viewer in this rotated frame. Wood backdrop (cutout) writes depth
        // BEHIND the image, so the overlay must sit IN FRONT of the image to pass the depth test.
        ps.translate(0, 0, -0.0015f);
        // All passes share the same RenderType → single batch → draw order = call order,
        // guaranteeing the darken pass renders below the stacked grunge layers.
        VertexConsumer vc = bufferSource.getBuffer(RenderType.entityTranslucentCull(DEGRADED_TEXTURE));
        Matrix4f m = ps.last().pose();

        // 1) Darken pass: pure-black tint, image looks faded/aged under the grunge.
        //    Color (0,0,0) × texture_rgb = (0,0,0) regardless of texture content → uniform black wash.
        drawOverlayQuad(vc, m, light, x0, y0, x1, y1, 0, 0, 0, DARKEN_ALPHA[lvl], LAYER_UVS[1]);

        // 2) Stack N grunge layers, each with its own UV permutation so the pattern accumulates
        //    rather than just darkening. Stacked alpha grows as 1-(1-a)^N.
        for (int i = 1; i <= lvl; i++) {
            drawOverlayQuad(vc, m, light, x0, y0, x1, y1,
                GRUNGE_TINT_R, GRUNGE_TINT_G, GRUNGE_TINT_B, GRUNGE_LAYER_ALPHA, LAYER_UVS[i]);
        }
        ps.popPose();
    }

    private void drawOverlayQuad(VertexConsumer vc, Matrix4f m, int light,
                                  float x0, float y0, float x1, float y1,
                                  int r, int g, int b, int alpha, float[][] uvs) {
        int ov = OverlayTexture.NO_OVERLAY;
        vc.vertex(m, x0, y0, 0).color(r,g,b,alpha).uv(uvs[0][0], uvs[0][1]).overlayCoords(ov).uv2(light).normal(0,0,-1).endVertex();
        vc.vertex(m, x0, y1, 0).color(r,g,b,alpha).uv(uvs[1][0], uvs[1][1]).overlayCoords(ov).uv2(light).normal(0,0,-1).endVertex();
        vc.vertex(m, x1, y1, 0).color(r,g,b,alpha).uv(uvs[2][0], uvs[2][1]).overlayCoords(ov).uv2(light).normal(0,0,-1).endVertex();
        vc.vertex(m, x1, y0, 0).color(r,g,b,alpha).uv(uvs[3][0], uvs[3][1]).overlayCoords(ov).uv2(light).normal(0,0,-1).endVertex();
    }

    @Override
    public boolean shouldRenderOffScreen(PosterBlockEntity be) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 96;
    }

    private static int overlay() {
        return LightTexture.FULL_BRIGHT;
    }
}
