package net.bobofraggins.tremendousstorage.storage.tube;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.bobofraggins.tremendousstorage.storage.tubeattachments.AttachmentType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.extensions.IBlockEntityRendererExtension;
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
public class TubeRenderer
        implements BlockEntityRenderer<TubeBlockEntity, TubeRenderer.State>,
                IBlockEntityRendererExtension<TubeBlockEntity> {

    public static class State extends BlockEntityRenderState {
        public int r, g, b;
        public BlockState blockState;
        public AttachmentType[] attachments = new AttachmentType[6];
    }

    /** Identifier for the white tube texture (tinted by DyeColor). */
    public static final Identifier TUBE_TEXTURE = Identifier.fromNamespaceAndPath("tremendousstorage", "block/tube");

    // Core face textures — selected by perpendicular connection count/pattern
    private static final Identifier TUBE_FACE_0 =
            Identifier.fromNamespaceAndPath("tremendousstorage", "block/tube_face");
    private static final Identifier TUBE_FACE_1 =
            Identifier.fromNamespaceAndPath("tremendousstorage", "block/tube_face_1");
    private static final Identifier TUBE_FACE_2ADJ =
            Identifier.fromNamespaceAndPath("tremendousstorage", "block/tube_face_2adj");
    private static final Identifier TUBE_FACE_2OPP =
            Identifier.fromNamespaceAndPath("tremendousstorage", "block/tube_face_2opp");
    private static final Identifier TUBE_FACE_3 =
            Identifier.fromNamespaceAndPath("tremendousstorage", "block/tube_face_3");
    private static final Identifier TUBE_FACE_4 =
            Identifier.fromNamespaceAndPath("tremendousstorage", "block/tube_face_4");

    // Attachment face textures — steel border + accent-color random pattern
    private static final Identifier ATTACH_IMPORT_TEXTURE =
            Identifier.fromNamespaceAndPath("tremendousstorage", "block/attachment_import");
    private static final Identifier ATTACH_EXPORT_TEXTURE =
            Identifier.fromNamespaceAndPath("tremendousstorage", "block/attachment_export");
    private static final Identifier ATTACH_STORAGE_TEXTURE =
            Identifier.fromNamespaceAndPath("tremendousstorage", "block/attachment_storage");

    /** Small offset to prevent Z-fighting with adjacent block faces. */
    private static final float EPS = 1e-4f;

    // Block-unit coordinate constants (in [0..1] space)
    private static final float C_MIN = 6f / 16f; // Core start
    private static final float C_MAX = 10f / 16f; // Core end
    private static final float A_MIN = C_MIN; // Arm cross-section min (same as core)
    private static final float A_MAX = C_MAX; // Arm cross-section max
    // Arms stop just short of the block face to avoid Z-fighting with the neighbour's face
    private static final float A_FACE_LO = 2 * EPS;
    private static final float A_FACE_HI = 1f - 2 * EPS;

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

    // Dark steel trim color (#262E3A)
    private static final int TRIM_R = 38;
    private static final int TRIM_G = 46;
    private static final int TRIM_B = 58;

    public TubeRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(
            TubeBlockEntity be,
            State state,
            float partialTick,
            Vec3 camera,
            ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderState.extractBase(be, state, breakProgress);
        state.blockState = be.getBlockState();
        int argb = be.getNetworkTier().getColor();
        state.r = (argb >> 16) & 0xFF;
        state.g = (argb >> 8) & 0xFF;
        state.b = argb & 0xFF;
        for (int i = 0; i < 6; i++) {
            state.attachments[i] = be.getAttachmentType(i);
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        if (!(state.blockState.getBlock() instanceof TubeBlock)) return;

        var atlas = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(TextureAtlas.LOCATION_BLOCKS);
        TextureAtlasSprite sprite = atlas.getSprite(TUBE_TEXTURE);
        TextureAtlasSprite tubeFace0 = atlas.getSprite(TUBE_FACE_0);
        TextureAtlasSprite tubeFace1 = atlas.getSprite(TUBE_FACE_1);
        TextureAtlasSprite tubeFace2adj = atlas.getSprite(TUBE_FACE_2ADJ);
        TextureAtlasSprite tubeFace2opp = atlas.getSprite(TUBE_FACE_2OPP);
        TextureAtlasSprite tubeFace3 = atlas.getSprite(TUBE_FACE_3);
        TextureAtlasSprite tubeFace4 = atlas.getSprite(TUBE_FACE_4);
        TextureAtlasSprite importSprite = atlas.getSprite(ATTACH_IMPORT_TEXTURE);
        TextureAtlasSprite exportSprite = atlas.getSprite(ATTACH_EXPORT_TEXTURE);
        TextureAtlasSprite storageSprite = atlas.getSprite(ATTACH_STORAGE_TEXTURE);

        int light = state.lightCoords;
        int overlay = net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY;
        int r = state.r, g = state.g, b = state.b;
        BlockState blockState = state.blockState;
        AttachmentType[] attachments = state.attachments;

        collector.submitCustomGeometry(poseStack, Sheets.cutoutBlockSheet(), (pose, vc) -> {
            Matrix4f mat = pose.pose();
            drawCore(
                    vc,
                    mat,
                    blockState,
                    sprite,
                    tubeFace0,
                    tubeFace1,
                    tubeFace2adj,
                    tubeFace2opp,
                    tubeFace3,
                    tubeFace4,
                    r,
                    g,
                    b,
                    light,
                    overlay);
            for (Direction dir : Direction.values()) {
                if (blockState.getValue(dirProp(dir))) {
                    drawArm(vc, mat, dir, sprite, r, g, b, light, overlay);
                }
            }
            for (int i = 0; i < 6; i++) {
                AttachmentType aType = attachments[i];
                if (aType == AttachmentType.NONE) continue;
                TextureAtlasSprite faceSprite =
                        switch (aType) {
                            case IMPORT_INTERFACE -> importSprite;
                            case EXPORT_INTERFACE -> exportSprite;
                            default -> storageSprite;
                        };
                drawAttachmentPlate(vc, mat, Direction.values()[i], faceSprite, sprite, r, g, b, light, overlay);
            }
        });
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
            TextureAtlasSprite face0,
            TextureAtlasSprite face1,
            TextureAtlasSprite face2adj,
            TextureAtlasSprite face2opp,
            TextureAtlasSprite face3,
            TextureAtlasSprite face4,
            int r,
            int g,
            int b,
            int light,
            int overlay) {
        int rUp = shade(r, SHADE_UP), gUp = shade(g, SHADE_UP), bUp = shade(b, SHADE_UP);
        int rDn = shade(r, SHADE_DOWN), gDn = shade(g, SHADE_DOWN), bDn = shade(b, SHADE_DOWN);
        int rNS = shade(r, SHADE_NORTH_SOUTH), gNS = shade(g, SHADE_NORTH_SOUTH), bNS = shade(b, SHADE_NORTH_SOUTH);
        int rEW = shade(r, SHADE_EAST_WEST), gEW = shade(g, SHADE_EAST_WEST), bEW = shade(b, SHADE_EAST_WEST);

        TextureAtlasSprite[] faceSprites = {face0, face1, face2adj, face2opp, face3, face4};

        if (!state.getValue(TubeBlock.UP))
            drawCoreFace(
                    vc,
                    mat,
                    Direction.UP,
                    C_MAX,
                    C_MIN,
                    C_MAX,
                    C_MIN,
                    C_MAX,
                    state,
                    faceSprites,
                    rUp,
                    gUp,
                    bUp,
                    light,
                    overlay);
        if (!state.getValue(TubeBlock.DOWN))
            drawCoreFace(
                    vc,
                    mat,
                    Direction.DOWN,
                    C_MIN,
                    C_MIN,
                    C_MAX,
                    C_MIN,
                    C_MAX,
                    state,
                    faceSprites,
                    rDn,
                    gDn,
                    bDn,
                    light,
                    overlay);
        if (!state.getValue(TubeBlock.SOUTH))
            drawCoreFace(
                    vc,
                    mat,
                    Direction.SOUTH,
                    C_MAX,
                    C_MIN,
                    C_MAX,
                    C_MIN,
                    C_MAX,
                    state,
                    faceSprites,
                    rNS,
                    gNS,
                    bNS,
                    light,
                    overlay);
        if (!state.getValue(TubeBlock.NORTH))
            drawCoreFace(
                    vc,
                    mat,
                    Direction.NORTH,
                    C_MIN,
                    C_MIN,
                    C_MAX,
                    C_MIN,
                    C_MAX,
                    state,
                    faceSprites,
                    rNS,
                    gNS,
                    bNS,
                    light,
                    overlay);
        if (!state.getValue(TubeBlock.EAST))
            drawCoreFace(
                    vc,
                    mat,
                    Direction.EAST,
                    C_MAX,
                    C_MIN,
                    C_MAX,
                    C_MIN,
                    C_MAX,
                    state,
                    faceSprites,
                    rEW,
                    gEW,
                    bEW,
                    light,
                    overlay);
        if (!state.getValue(TubeBlock.WEST))
            drawCoreFace(
                    vc,
                    mat,
                    Direction.WEST,
                    C_MIN,
                    C_MIN,
                    C_MAX,
                    C_MIN,
                    C_MAX,
                    state,
                    faceSprites,
                    rEW,
                    gEW,
                    bEW,
                    light,
                    overlay);
    }

    /**
     * Returns the 4 perpendicular directions of a face in clockwise order
     * [top, right, bottom, left] as seen from outside (used for UV rotation).
     *
     * <p>These match the UV mappings of {@link #faceQuad}: for each face direction,
     * texture-top aligns with the first entry, texture-right with the second, etc.
     */
    private static Direction[] facePerps(Direction face) {
        return switch (face) {
            case DOWN -> new Direction[] {Direction.SOUTH, Direction.EAST, Direction.NORTH, Direction.WEST};
            case UP -> new Direction[] {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
            case NORTH -> new Direction[] {Direction.UP, Direction.WEST, Direction.DOWN, Direction.EAST};
            case SOUTH -> new Direction[] {Direction.UP, Direction.EAST, Direction.DOWN, Direction.WEST};
            case WEST -> new Direction[] {Direction.UP, Direction.SOUTH, Direction.DOWN, Direction.NORTH};
            case EAST -> new Direction[] {Direction.UP, Direction.NORTH, Direction.DOWN, Direction.SOUTH};
        };
    }

    /**
     * Selects the correct face texture and UV rotation for a core face based on which of
     * the four perpendicular directions have connected arms.
     *
     * <p>Rotation r (0–3) means the texture is rotated r×90° CW on the surface, so that
     * what was at texture-top appears at face-direction[r % 4].
     *
     * @param faceDir the direction of this core face
     * @param state   the tube's block state
     * @param sp      array [face0, face1, face2adj, face2opp, face3, face4]
     * @return {spriteIndex, rotation} pair
     */
    private static int[] pickCoreFaceSprite(Direction faceDir, BlockState state, TextureAtlasSprite[] sp) {
        Direction[] perps = facePerps(faceDir);
        boolean c0 = state.getValue(dirProp(perps[0]));
        boolean c1 = state.getValue(dirProp(perps[1]));
        boolean c2 = state.getValue(dirProp(perps[2]));
        boolean c3 = state.getValue(dirProp(perps[3]));
        int count = (c0 ? 1 : 0) + (c1 ? 1 : 0) + (c2 ? 1 : 0) + (c3 ? 1 : 0);
        return switch (count) {
            case 4 -> new int[] {5, 0};
            case 3 -> {
                // find the one closed direction; it goes to texture-right (position 1)
                if (!c0) yield new int[] {4, (0 - 1 + 4) % 4};
                if (!c1) yield new int[] {4, (1 - 1 + 4) % 4};
                if (!c2) yield new int[] {4, (2 - 1 + 4) % 4};
                yield new int[] {4, (3 - 1 + 4) % 4};
            }
            case 2 -> {
                // 2 opposite
                if (c0 && c2) yield new int[] {3, 0};
                if (c1 && c3) yield new int[] {3, 1};
                // 2 adjacent: find pair (i, i+1); rotate so i+1 → top(0), i → left(3)
                if (c0 && c1) yield new int[] {2, 1};
                if (c1 && c2) yield new int[] {2, 2};
                if (c2 && c3) yield new int[] {2, 3};
                yield new int[] {2, 0}; // c3 && c0
            }
            case 1 -> {
                // the single connected direction goes to texture-top; rotation = its index
                if (c0) yield new int[] {1, 0};
                if (c1) yield new int[] {1, 1};
                if (c2) yield new int[] {1, 2};
                yield new int[] {1, 3};
            }
            default -> new int[] {0, 0};
        };
    }

    /**
     * Draws one core face as a single flat quad using the texture selected by connection
     * pattern. The sprite's pixel pattern (dark=walls, white=tube-color openings) is
     * rendered with UV rotation so each opening aligns with the connected arm direction.
     */
    private static void drawCoreFace(
            VertexConsumer vc,
            Matrix4f mat,
            Direction face,
            float f,
            float s0,
            float s1,
            float t0,
            float t1,
            BlockState state,
            TextureAtlasSprite[] faceSprites,
            int r,
            int g,
            int b,
            int light,
            int overlay) {
        int[] pick = pickCoreFaceSprite(face, state, faceSprites);
        TextureAtlasSprite sp = faceSprites[pick[0]];
        int rot = pick[1];
        float tu0 = sp.getU0(), tu1 = sp.getU1(), tv0 = sp.getV0(), tv1 = sp.getV1();

        // 4 UV pairs for vertices [x0, x1, x2, x3] based on clockwise rotation
        float u0v, u1v, u2v, u3v, v0v, v1v, v2v, v3v;
        switch (rot & 3) {
            case 1 -> { // 90° CW: texture-top → face-right
                u0v = tu0;
                v0v = tv1;
                u1v = tu0;
                v1v = tv0;
                u2v = tu1;
                v2v = tv0;
                u3v = tu1;
                v3v = tv1;
            }
            case 2 -> { // 180°: texture-top → face-bottom
                u0v = tu1;
                v0v = tv1;
                u1v = tu0;
                v1v = tv1;
                u2v = tu0;
                v2v = tv0;
                u3v = tu1;
                v3v = tv0;
            }
            case 3 -> { // 270° CW: texture-top → face-left
                u0v = tu1;
                v0v = tv0;
                u1v = tu1;
                v1v = tv1;
                u2v = tu0;
                v2v = tv1;
                u3v = tu0;
                v3v = tv0;
            }
            default -> { // 0°: texture-top → face-top
                u0v = tu0;
                v0v = tv0;
                u1v = tu1;
                v1v = tv0;
                u2v = tu1;
                v2v = tv1;
                u3v = tu0;
                v3v = tv1;
            }
        }

        // World positions and normals for each face direction (matching faceQuad winding)
        float x0, y0, z0, x1, y1, z1, x2, y2, z2, x3, y3, z3, nx, ny, nz;
        switch (face) {
            case UP -> {
                x0 = s0;
                y0 = f;
                z0 = t0;
                x1 = s1;
                y1 = f;
                z1 = t0;
                x2 = s1;
                y2 = f;
                z2 = t1;
                x3 = s0;
                y3 = f;
                z3 = t1;
                nx = 0;
                ny = 1;
                nz = 0;
            }
            case DOWN -> {
                x0 = s0;
                y0 = f;
                z0 = t1;
                x1 = s1;
                y1 = f;
                z1 = t1;
                x2 = s1;
                y2 = f;
                z2 = t0;
                x3 = s0;
                y3 = f;
                z3 = t0;
                nx = 0;
                ny = -1;
                nz = 0;
            }
            case SOUTH -> {
                x0 = s0;
                y0 = t1;
                z0 = f;
                x1 = s1;
                y1 = t1;
                z1 = f;
                x2 = s1;
                y2 = t0;
                z2 = f;
                x3 = s0;
                y3 = t0;
                z3 = f;
                nx = 0;
                ny = 0;
                nz = 1;
            }
            case NORTH -> {
                x0 = s1;
                y0 = t1;
                z0 = f;
                x1 = s0;
                y1 = t1;
                z1 = f;
                x2 = s0;
                y2 = t0;
                z2 = f;
                x3 = s1;
                y3 = t0;
                z3 = f;
                nx = 0;
                ny = 0;
                nz = -1;
            }
            case EAST -> {
                x0 = f;
                y0 = t1;
                z0 = s1;
                x1 = f;
                y1 = t1;
                z1 = s0;
                x2 = f;
                y2 = t0;
                z2 = s0;
                x3 = f;
                y3 = t0;
                z3 = s1;
                nx = 1;
                ny = 0;
                nz = 0;
            }
            default -> { // WEST
                x0 = f;
                y0 = t1;
                z0 = s0;
                x1 = f;
                y1 = t1;
                z1 = s1;
                x2 = f;
                y2 = t0;
                z2 = s1;
                x3 = f;
                y3 = t0;
                z3 = s0;
                nx = -1;
                ny = 0;
                nz = 0;
            }
        }

        // Emit vertices in order x3, x2, x1, x0 (matching existing quad() winding)
        vc.addVertex(mat, x3, y3, z3)
                .setColor(r, g, b, 255)
                .setUv(u3v, v3v)
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(nx, ny, nz);
        vc.addVertex(mat, x2, y2, z2)
                .setColor(r, g, b, 255)
                .setUv(u2v, v2v)
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(nx, ny, nz);
        vc.addVertex(mat, x1, y1, z1)
                .setColor(r, g, b, 255)
                .setUv(u1v, v1v)
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(nx, ny, nz);
        vc.addVertex(mat, x0, y0, z0)
                .setColor(r, g, b, 255)
                .setUv(u0v, v0v)
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(nx, ny, nz);
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
     * z-fighting with adjacent blocks. The outward and inward faces use {@code faceSprite} tinted
     * by {@code r,g,b}; the four side edges use {@code sideSprite} tinted dark steel.
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
        for (Direction face : Direction.values()) {
            boolean isTexturedFace = (face == dir || face == dir.getOpposite());
            TextureAtlasSprite sp = isTexturedFace ? faceSprite : sideSprite;
            int cr = isTexturedFace ? r : TRIM_R;
            int cg = isTexturedFace ? g : TRIM_G;
            int cb = isTexturedFace ? b : TRIM_B;
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
