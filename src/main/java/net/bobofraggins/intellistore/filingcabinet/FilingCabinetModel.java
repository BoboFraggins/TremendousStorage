package net.bobofraggins.intellistore.filingcabinet;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;

/**
 * Geometry model for the Filing Cabinet, ported from RFC's {@code ModelFilingCabinet}.
 *
 * <p>Texture: 64×64, same UV layout as the original RFC texture.
 *
 * <p>The cabinet body (back, two sides, top, bottom) is static.
 * The drawer (two inner sides, front face, bottom) is translated along +Z to animate open/close.
 */
public class FilingCabinetModel {

    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("intellistore", "filing_cabinet"), "main");

    public static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("intellistore", "textures/model/filing_cabinet.png");

    // Part names
    private static final String CABINET_BACK = "cabinet_back";
    private static final String CABINET_SIDE1 = "cabinet_side1";
    private static final String CABINET_SIDE2 = "cabinet_side2";
    private static final String CABINET_TOP = "cabinet_top";
    private static final String CABINET_BOTTOM = "cabinet_bottom";
    private static final String DRAWER_SIDE1 = "drawer_side1";
    private static final String DRAWER_SIDE2 = "drawer_side2";
    private static final String DRAWER_FRONT = "drawer_front";
    private static final String DRAWER_BOTTOM = "drawer_bottom";

    // Resolved model parts (set in constructor from the baked layer)
    private final ModelPart cabinetBack;
    private final ModelPart cabinetSide1;
    private final ModelPart cabinetSide2;
    private final ModelPart cabinetTop;
    private final ModelPart cabinetBottom;
    private final ModelPart drawerSide1;
    private final ModelPart drawerSide2;
    private final ModelPart drawerFront;
    private final ModelPart drawerBottom;

    public FilingCabinetModel(ModelPart root) {
        cabinetBack = root.getChild(CABINET_BACK);
        cabinetSide1 = root.getChild(CABINET_SIDE1);
        cabinetSide2 = root.getChild(CABINET_SIDE2);
        cabinetTop = root.getChild(CABINET_TOP);
        cabinetBottom = root.getChild(CABINET_BOTTOM);
        drawerSide1 = root.getChild(DRAWER_SIDE1);
        drawerSide2 = root.getChild(DRAWER_SIDE2);
        drawerFront = root.getChild(DRAWER_FRONT);
        drawerBottom = root.getChild(DRAWER_BOTTOM);
    }

    // -------------------------------------------------------------------------
    // Layer definition (registered once, reused for all instances)
    // -------------------------------------------------------------------------

    /**
     * Defines the geometry, matching RFC's ModelFilingCabinet UV/box layout exactly.
     * Texture atlas is 64×64. All coordinates are in 1/16-block units.
     */
    public static LayerDefinition createLayerDefinition() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Cabinet body — static parts
        root.addOrReplaceChild(CABINET_BACK,
                CubeListBuilder.create().texOffs(16, 0).addBox(-7, -8, 7, 14, 16, 1),
                PartPose.ZERO);
        root.addOrReplaceChild(CABINET_SIDE1,
                CubeListBuilder.create().texOffs(0, 0).addBox(-8, -8, -8, 1, 16, 16),
                PartPose.ZERO);
        root.addOrReplaceChild(CABINET_SIDE2,
                CubeListBuilder.create().texOffs(15, 0).addBox(7, -8, -8, 1, 16, 16),
                PartPose.ZERO);
        root.addOrReplaceChild(CABINET_TOP,
                CubeListBuilder.create().texOffs(2, 1).addBox(-7, -8, -8, 14, 2, 15),
                PartPose.ZERO);
        root.addOrReplaceChild(CABINET_BOTTOM,
                CubeListBuilder.create().texOffs(3, 0).addBox(-7, 6, -8, 14, 2, 15),
                PartPose.ZERO);

        // Drawer — animated parts
        root.addOrReplaceChild(DRAWER_SIDE1,
                CubeListBuilder.create().texOffs(18, 0).addBox(-7, -6, -7, 1, 12, 14),
                PartPose.ZERO);
        root.addOrReplaceChild(DRAWER_SIDE2,
                CubeListBuilder.create().texOffs(18, 0).addBox(6, -6, -7, 1, 12, 14),
                PartPose.ZERO);
        root.addOrReplaceChild(DRAWER_FRONT,
                CubeListBuilder.create().texOffs(16, 48).addBox(-7, -6, -8, 14, 12, 1),
                PartPose.ZERO);
        root.addOrReplaceChild(DRAWER_BOTTOM,
                CubeListBuilder.create().texOffs(2, 2).addBox(-7, 5, -7, 14, 1, 14),
                PartPose.ZERO);

        return LayerDefinition.create(mesh, 64, 64);
    }

    // -------------------------------------------------------------------------
    // Rendering
    // -------------------------------------------------------------------------

    /**
     * Renders the cabinet. {@code drawerOffset} drives the drawer Z translation
     * (already interpolated with partial tick by the caller).
     */
    public void renderAll(PoseStack poseStack, VertexConsumer consumer,
            int packedLight, int packedOverlay, float drawerOffset) {
        // Static cabinet body
        cabinetBack.render(poseStack, consumer, packedLight, packedOverlay);
        cabinetSide1.render(poseStack, consumer, packedLight, packedOverlay);
        cabinetSide2.render(poseStack, consumer, packedLight, packedOverlay);
        cabinetTop.render(poseStack, consumer, packedLight, packedOverlay);
        cabinetBottom.render(poseStack, consumer, packedLight, packedOverlay);

        // Animated drawer — translate along Z
        poseStack.pushPose();
        poseStack.translate(0, 0, drawerOffset);
        drawerSide1.render(poseStack, consumer, packedLight, packedOverlay);
        drawerSide2.render(poseStack, consumer, packedLight, packedOverlay);
        drawerFront.render(poseStack, consumer, packedLight, packedOverlay);
        drawerBottom.render(poseStack, consumer, packedLight, packedOverlay);
        poseStack.popPose();
    }
}
