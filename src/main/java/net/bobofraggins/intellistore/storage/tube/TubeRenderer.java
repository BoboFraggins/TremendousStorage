package net.bobofraggins.intellistore.storage.tube;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.bobofraggins.intellistore.storage.tubeattachments.AttachmentType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.joml.Matrix4f;

/**
 * Renders the dynamic geometry of a Tube: core cube, arms toward connected faces,
 * and Storage Interface attachment plates. All geometry is tinted by the tube's dye color.
 *
 * <p>The JSON block model provides only the particle texture (for break effects). All
 * visible geometry is drawn here.
 *
 * <p>Face shading mirrors vanilla's {@code ClientLevel.getShade()} multipliers so the
 * tubes read as 3-dimensional objects:
 * <ul>
 *   <li>Up   — 100% (brightest)
 *   <li>Down — 50%
 *   <li>North/South — 80%
 *   <li>East/West   — 60%
 * </ul>
 */
public class TubeRenderer implements BlockEntityRenderer<TubeBlockEntity> {

    /** ResourceLocation for the white tube texture (tinted by DyeColor). */
    public static final ResourceLocation TUBE_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("intellistore", "block/tube");

    // Attachment face textures — steel border + accent-color random pattern
    private static final ResourceLocation ATTACH_IMPORT_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("intellistore", "block/attachment_import");
    private static final ResourceLocation ATTACH_EXPORT_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("intellistore", "block/attachment_export");
    private static final ResourceLocation ATTACH_PLACER_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("intellistore", "block/attachment_placer");
    private static final ResourceLocation ATTACH_BREAKER_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("intellistore", "block/attachment_breaker");
    private static final ResourceLocation ATTACH_STORAGE_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("intellistore", "block/attachment_storage");

    /** Small offset to prevent Z-fighting with adjacent block faces. */
    private static final float EPS = 1e-4f;

    // Block-unit coordinate constants (in [0..1] space)
    private static final float C_MIN = 6f / 16f; // Core start
    private static final float C_MAX = 10f / 16f; // Core end
    private static final float A_MIN = C_MIN; // Arm cross-section min (same as core)
    private static final float A_MAX = C_MAX; // Arm cross-section max
    // Arms stop just short of the block face to avoid Z-fighting with the neighbour's face
    private static final float A_FACE_LO = EPS;
    private static final float A_FACE_HI = 1f - EPS;

    // One pixel in block-unit space (1/16)
    private static final float PX = 1f / 16f;

    // Attachment plate is 8/16 wide, 2/16 thick
    private static final float P_MIN = 4f / 16f;
    private static final float P_MAX = 12f / 16f;
    private static final float P_THICK = 2f / 16f;

    // Per-face brightness multipliers — mirror vanilla ClientLevel.getShade() values
    private static final float SHADE_UP = 1.00f;
    private static final float SHADE_DOWN = 0.50f;
    private static final float SHADE_NORTH_SOUTH = 0.80f;
    private static final float SHADE_EAST_WEST = 0.60f;

    // Dark steel trim color (#2A313A)
    private static final int TRIM_R = 42;
    private static final int TRIM_G = 49;
    private static final int TRIM_B = 58;

    public TubeRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(
            TubeBlockEntity be,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay) {

        BlockState state = be.getBlockState();
        if (!(state.getBlock() instanceof TubeBlock tubeBlock)) return;

        DyeColor dye = tubeBlock.getColor();
        int texColor = dye.getTextureDiffuseColor();
        int r = (texColor >> 16) & 0xFF;
        int g = (texColor >> 8) & 0xFF;
        int b = texColor & 0xFF;

        var atlas = Minecraft.getInstance().getModelManager().getAtlas(InventoryMenu.BLOCK_ATLAS);
        TextureAtlasSprite sprite = atlas.getSprite(TUBE_TEXTURE);
        TextureAtlasSprite importSprite = atlas.getSprite(ATTACH_IMPORT_TEXTURE);
        TextureAtlasSprite exportSprite = atlas.getSprite(ATTACH_EXPORT_TEXTURE);
        TextureAtlasSprite placerSprite = atlas.getSprite(ATTACH_PLACER_TEXTURE);
        TextureAtlasSprite breakerSprite = atlas.getSprite(ATTACH_BREAKER_TEXTURE);
        TextureAtlasSprite storageSprite = atlas.getSprite(ATTACH_STORAGE_TEXTURE);

        VertexConsumer vc = bufferSource.getBuffer(RenderType.solid());
        poseStack.pushPose();

        Matrix4f mat = poseStack.last().pose();

        // Core cube — only draw faces not covered by an arm
        drawCore(vc, mat, state, sprite, r, g, b, packedLight, packedOverlay);

        // Arms toward connected faces
        for (Direction dir : Direction.values()) {
            if (state.getValue(dirProp(dir))) {
                drawArm(vc, mat, dir, sprite, r, g, b, packedLight, packedOverlay);
            }
        }

        // Attachment plates — outward face uses type texture, sides use steel trim
        for (int i = 0; i < 6; i++) {
            AttachmentType aType = be.getAttachmentType(i);
            if (aType == AttachmentType.NONE) continue;
            // Typed attachments: white vertex color (accent encoded in texture).
            // Storage: tube-color vertex color (texture has white pattern, tinted at render time).
            TextureAtlasSprite faceSprite =
                    switch (aType) {
                        case IMPORT_INTERFACE -> importSprite;
                        case EXPORT_INTERFACE -> exportSprite;
                        case PLACER_INTERFACE -> placerSprite;
                        case BREAKER_INTERFACE -> breakerSprite;
                        default -> storageSprite;
                    };
            drawAttachmentPlate(
                    vc, mat, Direction.values()[i], faceSprite, sprite, r, g, b, packedLight, packedOverlay);
        }

        poseStack.popPose();
    }

    private static BooleanProperty dirProp(Direction dir) {
        return switch (dir) {
            case DOWN -> TubeBlock.DOWN;
            case UP -> TubeBlock.UP;
            case NORTH -> TubeBlock.NORTH;
            case SOUTH -> TubeBlock.SOUTH;
            case WEST -> TubeBlock.WEST;
            case EAST -> TubeBlock.EAST;
        };
    }

    /**
     * Draws the 4×4×4 core cube, skipping any face covered by a connected arm.
     * Each exposed face is drawn as a framed 5-quad face (1px trim border + 2×2 center).
     */
    private static void drawCore(
            VertexConsumer vc,
            Matrix4f mat,
            BlockState state,
            TextureAtlasSprite sprite,
            int r,
            int g,
            int b,
            int light,
            int overlay) {
        int rUp = shade(r, SHADE_UP), gUp = shade(g, SHADE_UP), bUp = shade(b, SHADE_UP);
        int rDn = shade(r, SHADE_DOWN), gDn = shade(g, SHADE_DOWN), bDn = shade(b, SHADE_DOWN);
        int rNS = shade(r, SHADE_NORTH_SOUTH), gNS = shade(g, SHADE_NORTH_SOUTH), bNS = shade(b, SHADE_NORTH_SOUTH);
        int rEW = shade(r, SHADE_EAST_WEST), gEW = shade(g, SHADE_EAST_WEST), bEW = shade(b, SHADE_EAST_WEST);
        int trUp = shade(TRIM_R, SHADE_UP), tgUp = shade(TRIM_G, SHADE_UP), tbUp = shade(TRIM_B, SHADE_UP);
        int trDn = shade(TRIM_R, SHADE_DOWN), tgDn = shade(TRIM_G, SHADE_DOWN), tbDn = shade(TRIM_B, SHADE_DOWN);
        int trNS = shade(TRIM_R, SHADE_NORTH_SOUTH),
                tgNS = shade(TRIM_G, SHADE_NORTH_SOUTH),
                tbNS = shade(TRIM_B, SHADE_NORTH_SOUTH);
        int trEW = shade(TRIM_R, SHADE_EAST_WEST),
                tgEW = shade(TRIM_G, SHADE_EAST_WEST),
                tbEW = shade(TRIM_B, SHADE_EAST_WEST);

        if (!state.getValue(TubeBlock.UP))
            drawFramedFace(
                    vc,
                    mat,
                    Direction.UP,
                    C_MAX,
                    C_MIN,
                    C_MAX,
                    C_MIN,
                    C_MAX,
                    rUp,
                    gUp,
                    bUp,
                    trUp,
                    tgUp,
                    tbUp,
                    light,
                    overlay,
                    sprite);
        if (!state.getValue(TubeBlock.DOWN))
            drawFramedFace(
                    vc,
                    mat,
                    Direction.DOWN,
                    C_MIN,
                    C_MIN,
                    C_MAX,
                    C_MIN,
                    C_MAX,
                    rDn,
                    gDn,
                    bDn,
                    trDn,
                    tgDn,
                    tbDn,
                    light,
                    overlay,
                    sprite);
        if (!state.getValue(TubeBlock.SOUTH))
            drawFramedFace(
                    vc,
                    mat,
                    Direction.SOUTH,
                    C_MAX,
                    C_MIN,
                    C_MAX,
                    C_MIN,
                    C_MAX,
                    rNS,
                    gNS,
                    bNS,
                    trNS,
                    tgNS,
                    tbNS,
                    light,
                    overlay,
                    sprite);
        if (!state.getValue(TubeBlock.NORTH))
            drawFramedFace(
                    vc,
                    mat,
                    Direction.NORTH,
                    C_MIN,
                    C_MIN,
                    C_MAX,
                    C_MIN,
                    C_MAX,
                    rNS,
                    gNS,
                    bNS,
                    trNS,
                    tgNS,
                    tbNS,
                    light,
                    overlay,
                    sprite);
        if (!state.getValue(TubeBlock.EAST))
            drawFramedFace(
                    vc,
                    mat,
                    Direction.EAST,
                    C_MAX,
                    C_MIN,
                    C_MAX,
                    C_MIN,
                    C_MAX,
                    rEW,
                    gEW,
                    bEW,
                    trEW,
                    tgEW,
                    tbEW,
                    light,
                    overlay,
                    sprite);
        if (!state.getValue(TubeBlock.WEST))
            drawFramedFace(
                    vc,
                    mat,
                    Direction.WEST,
                    C_MIN,
                    C_MIN,
                    C_MAX,
                    C_MIN,
                    C_MAX,
                    rEW,
                    gEW,
                    bEW,
                    trEW,
                    tgEW,
                    tbEW,
                    light,
                    overlay,
                    sprite);
    }

    /**
     * Draws a trimmed 4×4 arm from the core to the given face.
     * Each side face is split into three strips (1px trim / 2px body / 1px trim) along the
     * cross-section axis. The end cap is drawn as a 5-quad framed face (border + center).
     */
    private static void drawArm(
            VertexConsumer vc,
            Matrix4f mat,
            Direction dir,
            TextureAtlasSprite sprite,
            int r,
            int g,
            int b,
            int light,
            int overlay) {
        int rUp = shade(r, SHADE_UP), gUp = shade(g, SHADE_UP), bUp = shade(b, SHADE_UP);
        int rDn = shade(r, SHADE_DOWN), gDn = shade(g, SHADE_DOWN), bDn = shade(b, SHADE_DOWN);
        int rNS = shade(r, SHADE_NORTH_SOUTH), gNS = shade(g, SHADE_NORTH_SOUTH), bNS = shade(b, SHADE_NORTH_SOUTH);
        int rEW = shade(r, SHADE_EAST_WEST), gEW = shade(g, SHADE_EAST_WEST), bEW = shade(b, SHADE_EAST_WEST);
        int trUp = shade(TRIM_R, SHADE_UP), tgUp = shade(TRIM_G, SHADE_UP), tbUp = shade(TRIM_B, SHADE_UP);
        int trDn = shade(TRIM_R, SHADE_DOWN), tgDn = shade(TRIM_G, SHADE_DOWN), tbDn = shade(TRIM_B, SHADE_DOWN);
        int trNS = shade(TRIM_R, SHADE_NORTH_SOUTH),
                tgNS = shade(TRIM_G, SHADE_NORTH_SOUTH),
                tbNS = shade(TRIM_B, SHADE_NORTH_SOUTH);
        int trEW = shade(TRIM_R, SHADE_EAST_WEST),
                tgEW = shade(TRIM_G, SHADE_EAST_WEST),
                tbEW = shade(TRIM_B, SHADE_EAST_WEST);

        // For each arm direction: 4 trimmed side faces + 1 framed end cap.
        // Face-coord convention per Direction: UP/DOWN → s=X,t=Z; NS → s=X,t=Y; EW → s=Z,t=Y.
        // splitS=true splits the s-axis (cross-section); splitS=false splits the t-axis.
        switch (dir) {
            case DOWN -> {
                float yLo = A_FACE_LO, yHi = C_MIN;
                // Arm along Y=t; cross-section on Z=s (EAST/WEST) or X=s (SOUTH/NORTH)
                drawTrimmedStrip(
                        vc,
                        mat,
                        Direction.EAST,
                        A_MAX,
                        A_MIN,
                        A_MAX,
                        yLo,
                        yHi,
                        true,
                        rEW,
                        gEW,
                        bEW,
                        trEW,
                        tgEW,
                        tbEW,
                        light,
                        overlay,
                        sprite);
                drawTrimmedStrip(
                        vc,
                        mat,
                        Direction.WEST,
                        A_MIN,
                        A_MIN,
                        A_MAX,
                        yLo,
                        yHi,
                        true,
                        rEW,
                        gEW,
                        bEW,
                        trEW,
                        tgEW,
                        tbEW,
                        light,
                        overlay,
                        sprite);
                drawTrimmedStrip(
                        vc,
                        mat,
                        Direction.SOUTH,
                        A_MAX,
                        A_MIN,
                        A_MAX,
                        yLo,
                        yHi,
                        true,
                        rNS,
                        gNS,
                        bNS,
                        trNS,
                        tgNS,
                        tbNS,
                        light,
                        overlay,
                        sprite);
                drawTrimmedStrip(
                        vc,
                        mat,
                        Direction.NORTH,
                        A_MIN,
                        A_MIN,
                        A_MAX,
                        yLo,
                        yHi,
                        true,
                        rNS,
                        gNS,
                        bNS,
                        trNS,
                        tgNS,
                        tbNS,
                        light,
                        overlay,
                        sprite);
                drawFramedFace(
                        vc,
                        mat,
                        Direction.DOWN,
                        A_FACE_LO,
                        A_MIN,
                        A_MAX,
                        A_MIN,
                        A_MAX,
                        rDn,
                        gDn,
                        bDn,
                        trDn,
                        tgDn,
                        tbDn,
                        light,
                        overlay,
                        sprite);
            }
            case UP -> {
                float yLo = C_MAX, yHi = A_FACE_HI;
                drawTrimmedStrip(
                        vc,
                        mat,
                        Direction.EAST,
                        A_MAX,
                        A_MIN,
                        A_MAX,
                        yLo,
                        yHi,
                        true,
                        rEW,
                        gEW,
                        bEW,
                        trEW,
                        tgEW,
                        tbEW,
                        light,
                        overlay,
                        sprite);
                drawTrimmedStrip(
                        vc,
                        mat,
                        Direction.WEST,
                        A_MIN,
                        A_MIN,
                        A_MAX,
                        yLo,
                        yHi,
                        true,
                        rEW,
                        gEW,
                        bEW,
                        trEW,
                        tgEW,
                        tbEW,
                        light,
                        overlay,
                        sprite);
                drawTrimmedStrip(
                        vc,
                        mat,
                        Direction.SOUTH,
                        A_MAX,
                        A_MIN,
                        A_MAX,
                        yLo,
                        yHi,
                        true,
                        rNS,
                        gNS,
                        bNS,
                        trNS,
                        tgNS,
                        tbNS,
                        light,
                        overlay,
                        sprite);
                drawTrimmedStrip(
                        vc,
                        mat,
                        Direction.NORTH,
                        A_MIN,
                        A_MIN,
                        A_MAX,
                        yLo,
                        yHi,
                        true,
                        rNS,
                        gNS,
                        bNS,
                        trNS,
                        tgNS,
                        tbNS,
                        light,
                        overlay,
                        sprite);
                drawFramedFace(
                        vc,
                        mat,
                        Direction.UP,
                        A_FACE_HI,
                        A_MIN,
                        A_MAX,
                        A_MIN,
                        A_MAX,
                        rUp,
                        gUp,
                        bUp,
                        trUp,
                        tgUp,
                        tbUp,
                        light,
                        overlay,
                        sprite);
            }
            case NORTH -> {
                float zLo = A_FACE_LO, zHi = C_MIN;
                // UP/DOWN: arm along Z=t, cross on X=s → splitS=true
                drawTrimmedStrip(
                        vc,
                        mat,
                        Direction.UP,
                        A_MAX,
                        A_MIN,
                        A_MAX,
                        zLo,
                        zHi,
                        true,
                        rUp,
                        gUp,
                        bUp,
                        trUp,
                        tgUp,
                        tbUp,
                        light,
                        overlay,
                        sprite);
                drawTrimmedStrip(
                        vc,
                        mat,
                        Direction.DOWN,
                        A_MIN,
                        A_MIN,
                        A_MAX,
                        zLo,
                        zHi,
                        true,
                        rDn,
                        gDn,
                        bDn,
                        trDn,
                        tgDn,
                        tbDn,
                        light,
                        overlay,
                        sprite);
                // EAST/WEST: arm along Z=s, cross on Y=t → splitS=false
                drawTrimmedStrip(
                        vc,
                        mat,
                        Direction.EAST,
                        A_MAX,
                        zLo,
                        zHi,
                        A_MIN,
                        A_MAX,
                        false,
                        rEW,
                        gEW,
                        bEW,
                        trEW,
                        tgEW,
                        tbEW,
                        light,
                        overlay,
                        sprite);
                drawTrimmedStrip(
                        vc,
                        mat,
                        Direction.WEST,
                        A_MIN,
                        zLo,
                        zHi,
                        A_MIN,
                        A_MAX,
                        false,
                        rEW,
                        gEW,
                        bEW,
                        trEW,
                        tgEW,
                        tbEW,
                        light,
                        overlay,
                        sprite);
                drawFramedFace(
                        vc,
                        mat,
                        Direction.NORTH,
                        A_FACE_LO,
                        A_MIN,
                        A_MAX,
                        A_MIN,
                        A_MAX,
                        rNS,
                        gNS,
                        bNS,
                        trNS,
                        tgNS,
                        tbNS,
                        light,
                        overlay,
                        sprite);
            }
            case SOUTH -> {
                float zLo = C_MAX, zHi = A_FACE_HI;
                drawTrimmedStrip(
                        vc,
                        mat,
                        Direction.UP,
                        A_MAX,
                        A_MIN,
                        A_MAX,
                        zLo,
                        zHi,
                        true,
                        rUp,
                        gUp,
                        bUp,
                        trUp,
                        tgUp,
                        tbUp,
                        light,
                        overlay,
                        sprite);
                drawTrimmedStrip(
                        vc,
                        mat,
                        Direction.DOWN,
                        A_MIN,
                        A_MIN,
                        A_MAX,
                        zLo,
                        zHi,
                        true,
                        rDn,
                        gDn,
                        bDn,
                        trDn,
                        tgDn,
                        tbDn,
                        light,
                        overlay,
                        sprite);
                drawTrimmedStrip(
                        vc,
                        mat,
                        Direction.EAST,
                        A_MAX,
                        zLo,
                        zHi,
                        A_MIN,
                        A_MAX,
                        false,
                        rEW,
                        gEW,
                        bEW,
                        trEW,
                        tgEW,
                        tbEW,
                        light,
                        overlay,
                        sprite);
                drawTrimmedStrip(
                        vc,
                        mat,
                        Direction.WEST,
                        A_MIN,
                        zLo,
                        zHi,
                        A_MIN,
                        A_MAX,
                        false,
                        rEW,
                        gEW,
                        bEW,
                        trEW,
                        tgEW,
                        tbEW,
                        light,
                        overlay,
                        sprite);
                drawFramedFace(
                        vc,
                        mat,
                        Direction.SOUTH,
                        A_FACE_HI,
                        A_MIN,
                        A_MAX,
                        A_MIN,
                        A_MAX,
                        rNS,
                        gNS,
                        bNS,
                        trNS,
                        tgNS,
                        tbNS,
                        light,
                        overlay,
                        sprite);
            }
            case WEST -> {
                float xLo = A_FACE_LO, xHi = C_MIN;
                // UP/DOWN: arm along X=s, cross on Z=t → splitS=false
                drawTrimmedStrip(
                        vc,
                        mat,
                        Direction.UP,
                        A_MAX,
                        xLo,
                        xHi,
                        A_MIN,
                        A_MAX,
                        false,
                        rUp,
                        gUp,
                        bUp,
                        trUp,
                        tgUp,
                        tbUp,
                        light,
                        overlay,
                        sprite);
                drawTrimmedStrip(
                        vc,
                        mat,
                        Direction.DOWN,
                        A_MIN,
                        xLo,
                        xHi,
                        A_MIN,
                        A_MAX,
                        false,
                        rDn,
                        gDn,
                        bDn,
                        trDn,
                        tgDn,
                        tbDn,
                        light,
                        overlay,
                        sprite);
                // SOUTH/NORTH: arm along X=s, cross on Y=t → splitS=false
                drawTrimmedStrip(
                        vc,
                        mat,
                        Direction.SOUTH,
                        A_MAX,
                        xLo,
                        xHi,
                        A_MIN,
                        A_MAX,
                        false,
                        rNS,
                        gNS,
                        bNS,
                        trNS,
                        tgNS,
                        tbNS,
                        light,
                        overlay,
                        sprite);
                drawTrimmedStrip(
                        vc,
                        mat,
                        Direction.NORTH,
                        A_MIN,
                        xLo,
                        xHi,
                        A_MIN,
                        A_MAX,
                        false,
                        rNS,
                        gNS,
                        bNS,
                        trNS,
                        tgNS,
                        tbNS,
                        light,
                        overlay,
                        sprite);
                drawFramedFace(
                        vc,
                        mat,
                        Direction.WEST,
                        A_FACE_LO,
                        A_MIN,
                        A_MAX,
                        A_MIN,
                        A_MAX,
                        rEW,
                        gEW,
                        bEW,
                        trEW,
                        tgEW,
                        tbEW,
                        light,
                        overlay,
                        sprite);
            }
            case EAST -> {
                float xLo = C_MAX, xHi = A_FACE_HI;
                drawTrimmedStrip(
                        vc,
                        mat,
                        Direction.UP,
                        A_MAX,
                        xLo,
                        xHi,
                        A_MIN,
                        A_MAX,
                        false,
                        rUp,
                        gUp,
                        bUp,
                        trUp,
                        tgUp,
                        tbUp,
                        light,
                        overlay,
                        sprite);
                drawTrimmedStrip(
                        vc,
                        mat,
                        Direction.DOWN,
                        A_MIN,
                        xLo,
                        xHi,
                        A_MIN,
                        A_MAX,
                        false,
                        rDn,
                        gDn,
                        bDn,
                        trDn,
                        tgDn,
                        tbDn,
                        light,
                        overlay,
                        sprite);
                drawTrimmedStrip(
                        vc,
                        mat,
                        Direction.SOUTH,
                        A_MAX,
                        xLo,
                        xHi,
                        A_MIN,
                        A_MAX,
                        false,
                        rNS,
                        gNS,
                        bNS,
                        trNS,
                        tgNS,
                        tbNS,
                        light,
                        overlay,
                        sprite);
                drawTrimmedStrip(
                        vc,
                        mat,
                        Direction.NORTH,
                        A_MIN,
                        xLo,
                        xHi,
                        A_MIN,
                        A_MAX,
                        false,
                        rNS,
                        gNS,
                        bNS,
                        trNS,
                        tgNS,
                        tbNS,
                        light,
                        overlay,
                        sprite);
                drawFramedFace(
                        vc,
                        mat,
                        Direction.EAST,
                        A_FACE_HI,
                        A_MIN,
                        A_MAX,
                        A_MIN,
                        A_MAX,
                        rEW,
                        gEW,
                        bEW,
                        trEW,
                        tgEW,
                        tbEW,
                        light,
                        overlay,
                        sprite);
            }
        }
    }

    /**
     * Draws an 8×8×2 attachment plate flush with the outside face, slightly inset to avoid
     * z-fighting with adjacent blocks. The outward face uses {@code faceSprite} tinted by
     * {@code r,g,b}; the four side edges use {@code sideSprite} tinted dark steel. The inner
     * face is omitted (not visible).
     */
    private static void drawAttachmentPlate(
            VertexConsumer vc,
            Matrix4f mat,
            Direction dir,
            TextureAtlasSprite faceSprite,
            TextureAtlasSprite sideSprite,
            int r,
            int g,
            int b,
            int light,
            int overlay) {
        float inset = EPS;
        float x0, y0, z0, x1, y1, z1;
        switch (dir) {
            case DOWN -> {
                x0 = P_MIN;
                y0 = inset;
                z0 = P_MIN;
                x1 = P_MAX;
                y1 = P_THICK + inset;
                z1 = P_MAX;
            }
            case UP -> {
                x0 = P_MIN;
                y0 = 1 - P_THICK - inset;
                z0 = P_MIN;
                x1 = P_MAX;
                y1 = 1 - inset;
                z1 = P_MAX;
            }
            case NORTH -> {
                x0 = P_MIN;
                y0 = P_MIN;
                z0 = inset;
                x1 = P_MAX;
                y1 = P_MAX;
                z1 = P_THICK + inset;
            }
            case SOUTH -> {
                x0 = P_MIN;
                y0 = P_MIN;
                z0 = 1 - P_THICK - inset;
                x1 = P_MAX;
                y1 = P_MAX;
                z1 = 1 - inset;
            }
            case WEST -> {
                x0 = inset;
                y0 = P_MIN;
                z0 = P_MIN;
                x1 = P_THICK + inset;
                y1 = P_MAX;
                z1 = P_MAX;
            }
            default -> {
                x0 = 1 - P_THICK - inset;
                y0 = P_MIN;
                z0 = P_MIN;
                x1 = 1 - inset;
                y1 = P_MAX;
                z1 = P_MAX;
            } // EAST
        }
        Direction inner = dir.getOpposite();
        for (Direction face : Direction.values()) {
            if (face == inner) continue;
            boolean isOutward = (face == dir);
            TextureAtlasSprite sp = isOutward ? faceSprite : sideSprite;
            int cr = isOutward ? r : TRIM_R;
            int cg = isOutward ? g : TRIM_G;
            int cb = isOutward ? b : TRIM_B;
            float shadeF =
                    switch (face) {
                        case UP -> SHADE_UP;
                        case DOWN -> SHADE_DOWN;
                        case NORTH, SOUTH -> SHADE_NORTH_SOUTH;
                        default -> SHADE_EAST_WEST;
                    };
            int sr = shade(cr, shadeF);
            int sg = shade(cg, shadeF);
            int sb = shade(cb, shadeF);
            switch (face) {
                case DOWN -> quadFlat(
                        vc, mat, sr, sg, sb, light, overlay, sp, 0, -1, 0, x0, y0, z1, x1, y0, z1, x1, y0, z0, x0, y0,
                        z0);
                case UP -> quadFlat(
                        vc, mat, sr, sg, sb, light, overlay, sp, 0, 1, 0, x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1,
                        z1);
                case NORTH -> quadFlat(
                        vc, mat, sr, sg, sb, light, overlay, sp, 0, 0, -1, x1, y1, z0, x0, y1, z0, x0, y0, z0, x1, y0,
                        z0);
                case SOUTH -> quadFlat(
                        vc, mat, sr, sg, sb, light, overlay, sp, 0, 0, 1, x0, y1, z1, x1, y1, z1, x1, y0, z1, x0, y0,
                        z1);
                case WEST -> quadFlat(
                        vc, mat, sr, sg, sb, light, overlay, sp, -1, 0, 0, x0, y1, z0, x0, y1, z1, x0, y0, z1, x0, y0,
                        z0);
                default -> quadFlat(
                        vc, mat, sr, sg, sb, light, overlay, sp, 1, 0, 0, x1, y1, z1, x1, y1, z0, x1, y0, z0, x1, y0,
                        z1);
            }
        }
    }

    /**
     * Draws all 6 faces of an axis-aligned box with per-face shading.
     * Each face receives a flat brightness multiplier based on its orientation:
     * Up/Down=90%, East/West=95%, North/South=100%.
     */
    private static void drawBox(
            VertexConsumer vc,
            Matrix4f mat,
            float x0,
            float y0,
            float z0,
            float x1,
            float y1,
            float z1,
            TextureAtlasSprite sprite,
            int r,
            int g,
            int b,
            int light,
            int overlay) {

        float u0 = sprite.getU0(), u1 = sprite.getU1();
        float v0 = sprite.getV0(), v1 = sprite.getV1();

        int rUp = shade(r, SHADE_UP), gUp = shade(g, SHADE_UP), bUp = shade(b, SHADE_UP);
        int rDn = shade(r, SHADE_DOWN), gDn = shade(g, SHADE_DOWN), bDn = shade(b, SHADE_DOWN);
        int rNS = shade(r, SHADE_NORTH_SOUTH), gNS = shade(g, SHADE_NORTH_SOUTH), bNS = shade(b, SHADE_NORTH_SOUTH);
        int rEW = shade(r, SHADE_EAST_WEST), gEW = shade(g, SHADE_EAST_WEST), bEW = shade(b, SHADE_EAST_WEST);

        // -Y (down)
        quad(
                vc, mat, rDn, gDn, bDn, light, overlay, u0, v0, u1, v1, x0, y0, z1, x1, y0, z1, x1, y0, z0, x0, y0, z0,
                0, -1, 0);
        // +Y (up)
        quad(
                vc, mat, rUp, gUp, bUp, light, overlay, u0, v0, u1, v1, x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1,
                0, 1, 0);
        // -Z (north)
        quad(
                vc, mat, rNS, gNS, bNS, light, overlay, u0, v0, u1, v1, x1, y1, z0, x0, y1, z0, x0, y0, z0, x1, y0, z0,
                0, 0, -1);
        // +Z (south)
        quad(
                vc, mat, rNS, gNS, bNS, light, overlay, u0, v0, u1, v1, x0, y1, z1, x1, y1, z1, x1, y0, z1, x0, y0, z1,
                0, 0, 1);
        // -X (west)
        quad(
                vc, mat, rEW, gEW, bEW, light, overlay, u0, v0, u1, v1, x0, y1, z0, x0, y1, z1, x0, y0, z1, x0, y0, z0,
                -1, 0, 0);
        // +X (east)
        quad(
                vc, mat, rEW, gEW, bEW, light, overlay, u0, v0, u1, v1, x1, y1, z1, x1, y1, z0, x1, y0, z0, x1, y0, z1,
                1, 0, 0);
    }

    /**
     * Emits a single quad from 4 explicit corner positions, using the full sprite UV.
     * Vertices are given in CCW order when viewed from the outside (same winding as {@link #quad}).
     * Prefer this over {@link #quad} when computing sub-face geometry directly.
     */
    private static void quadFlat(
            VertexConsumer vc,
            Matrix4f mat,
            int r,
            int g,
            int b,
            int light,
            int overlay,
            TextureAtlasSprite sprite,
            float nx,
            float ny,
            float nz,
            float x0,
            float y0,
            float z0,
            float x1,
            float y1,
            float z1,
            float x2,
            float y2,
            float z2,
            float x3,
            float y3,
            float z3) {
        quad(
                vc,
                mat,
                r,
                g,
                b,
                light,
                overlay,
                sprite.getU0(),
                sprite.getV0(),
                sprite.getU1(),
                sprite.getV1(),
                x0,
                y0,
                z0,
                x1,
                y1,
                z1,
                x2,
                y2,
                z2,
                x3,
                y3,
                z3,
                nx,
                ny,
                nz);
    }

    /**
     * Emits one rectangle on a face of a given Direction at fixed coordinate {@code f}.
     * Coordinate systems: UP/DOWN → s=X,t=Z; NORTH/SOUTH → s=X,t=Y; EAST/WEST → s=Z,t=Y.
     * Winding matches {@link #drawBox} (CCW when viewed from outside).
     */
    private static void faceQuad(
            VertexConsumer vc,
            Matrix4f mat,
            int r,
            int g,
            int b,
            int light,
            int overlay,
            TextureAtlasSprite sprite,
            Direction face,
            float f,
            float s0,
            float t0,
            float s1,
            float t1) {
        switch (face) {
            case UP -> quadFlat(
                    vc, mat, r, g, b, light, overlay, sprite, 0, 1, 0, s0, f, t0, s1, f, t0, s1, f, t1, s0, f, t1);
            case DOWN -> quadFlat(
                    vc, mat, r, g, b, light, overlay, sprite, 0, -1, 0, s0, f, t1, s1, f, t1, s1, f, t0, s0, f, t0);
            case SOUTH -> quadFlat(
                    vc, mat, r, g, b, light, overlay, sprite, 0, 0, 1, s0, t1, f, s1, t1, f, s1, t0, f, s0, t0, f);
            case NORTH -> quadFlat(
                    vc, mat, r, g, b, light, overlay, sprite, 0, 0, -1, s1, t1, f, s0, t1, f, s0, t0, f, s1, t0, f);
            case EAST -> quadFlat(
                    vc, mat, r, g, b, light, overlay, sprite, 1, 0, 0, f, t1, s1, f, t1, s0, f, t0, s0, f, t0, s1);
            case WEST -> quadFlat(
                    vc, mat, r, g, b, light, overlay, sprite, -1, 0, 0, f, t1, s0, f, t1, s1, f, t0, s1, f, t0, s0);
        }
    }

    /**
     * Draws three quads on a face — trim / body / trim — splitting either the s-axis
     * ({@code splitS=true}) or the t-axis ({@code splitS=false}) into 1px trim at each edge
     * and 2px body in the middle.
     */
    private static void drawTrimmedStrip(
            VertexConsumer vc,
            Matrix4f mat,
            Direction face,
            float f,
            float s0,
            float s1,
            float t0,
            float t1,
            boolean splitS,
            int cr,
            int cg,
            int cb,
            int trR,
            int trG,
            int trB,
            int light,
            int overlay,
            TextureAtlasSprite sprite) {
        if (splitS) {
            faceQuad(vc, mat, trR, trG, trB, light, overlay, sprite, face, f, s0, t0, s0 + PX, t1);
            faceQuad(vc, mat, cr, cg, cb, light, overlay, sprite, face, f, s0 + PX, t0, s1 - PX, t1);
            faceQuad(vc, mat, trR, trG, trB, light, overlay, sprite, face, f, s1 - PX, t0, s1, t1);
        } else {
            faceQuad(vc, mat, trR, trG, trB, light, overlay, sprite, face, f, s0, t0, s1, t0 + PX);
            faceQuad(vc, mat, cr, cg, cb, light, overlay, sprite, face, f, s0, t0 + PX, s1, t1 - PX);
            faceQuad(vc, mat, trR, trG, trB, light, overlay, sprite, face, f, s0, t1 - PX, s1, t1);
        }
    }

    /**
     * Draws a 4×4 face as 5 quads: a 1px border frame (trim color) with a 2×2 center (tube color).
     * Used for end caps and unconnected core faces.
     */
    private static void drawFramedFace(
            VertexConsumer vc,
            Matrix4f mat,
            Direction face,
            float f,
            float s0,
            float s1,
            float t0,
            float t1,
            int cr,
            int cg,
            int cb,
            int trR,
            int trG,
            int trB,
            int light,
            int overlay,
            TextureAtlasSprite sprite) {
        faceQuad(vc, mat, trR, trG, trB, light, overlay, sprite, face, f, s0, t0, s1, t0 + PX); // bottom
        faceQuad(vc, mat, trR, trG, trB, light, overlay, sprite, face, f, s0, t1 - PX, s1, t1); // top
        faceQuad(vc, mat, trR, trG, trB, light, overlay, sprite, face, f, s0, t0 + PX, s0 + PX, t1 - PX); // left
        faceQuad(vc, mat, trR, trG, trB, light, overlay, sprite, face, f, s1 - PX, t0 + PX, s1, t1 - PX); // right
        faceQuad(vc, mat, cr, cg, cb, light, overlay, sprite, face, f, s0 + PX, t0 + PX, s1 - PX, t1 - PX); // center
    }

    /** Multiplies a channel value by a shade factor and clamps to [0, 255]. */
    private static int shade(int channel, float factor) {
        return Math.min(255, Math.max(0, Math.round(channel * factor)));
    }

    /** Emits a single CCW quad (4 vertices) with the given UV corners. */
    private static void quad(
            VertexConsumer vc,
            Matrix4f mat,
            int r,
            int g,
            int b,
            int light,
            int overlay,
            float u0,
            float v0,
            float u1,
            float v1,
            float x0,
            float y0,
            float z0,
            float x1,
            float y1,
            float z1,
            float x2,
            float y2,
            float z2,
            float x3,
            float y3,
            float z3,
            float nx,
            float ny,
            float nz) {
        vc.addVertex(mat, x3, y3, z3)
                .setColor(r, g, b, 255)
                .setUv(u0, v1)
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(nx, ny, nz);
        vc.addVertex(mat, x2, y2, z2)
                .setColor(r, g, b, 255)
                .setUv(u1, v1)
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(nx, ny, nz);
        vc.addVertex(mat, x1, y1, z1)
                .setColor(r, g, b, 255)
                .setUv(u1, v0)
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(nx, ny, nz);
        vc.addVertex(mat, x0, y0, z0)
                .setColor(r, g, b, 255)
                .setUv(u0, v0)
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(nx, ny, nz);
    }
}
