package net.bobofraggins.intellistore.canvas;

import java.util.List;
import javax.annotation.Nullable;
import net.bobofraggins.intellistore.shared.register.Registration;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockFaceUV;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import org.joml.Vector3f;

/** Baked model for canvas blocks that selects per-face textures based on adjacent canvas blocks. */
public class CanvasBakedModel implements BakedModel {

    /** Per-face connection bitmask: bit 3=u disconnected, 2=d, 1=l, 0=r. */
    public static final ModelProperty<int[]> CONNECTION_PROPERTY = new ModelProperty<>();

    /**
     * For each face (by {@link Direction#ordinal()}), the four neighbors in [u, d, l, r] order.
     * A neighbor that is NOT a canvas block contributes a set bit to the texture mask.
     */
    public static final Direction[][] FACE_NEIGHBORS = {
        // DOWN  (0): viewed from below
        {Direction.SOUTH, Direction.NORTH, Direction.WEST, Direction.EAST},
        // UP    (1): viewed from above
        {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST},
        // NORTH (2): viewed from outside
        {Direction.UP, Direction.DOWN, Direction.WEST, Direction.EAST},
        // SOUTH (3): viewed from outside
        {Direction.UP, Direction.DOWN, Direction.EAST, Direction.WEST},
        // WEST  (4): viewed from outside
        {Direction.UP, Direction.DOWN, Direction.SOUTH, Direction.NORTH},
        // EAST  (5): viewed from outside
        {Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH},
    };

    private static final String[] MASK_NAMES = {
        "interior", // 0b0000
        "r", // 0b0001
        "l", // 0b0010
        "l_r", // 0b0011
        "d", // 0b0100
        "d_r", // 0b0101
        "d_l", // 0b0110
        "d_l_r", // 0b0111
        "u", // 0b1000
        "u_r", // 0b1001
        "u_l", // 0b1010
        "u_l_r", // 0b1011
        "u_d", // 0b1100
        "u_d_r", // 0b1101
        "u_d_l", // 0b1110
        "u_d_l_r", // 0b1111
    };

    public static String maskToName(int mask) {
        return MASK_NAMES[mask & 0xF];
    }

    /** Pre-baked quads indexed as [faceOrdinal][mask 0-15]. */
    @SuppressWarnings("unchecked")
    private final List<BakedQuad>[][] quads = new List[6][16];

    private final TextureAtlasSprite particle;

    public CanvasBakedModel(TextureAtlasSprite[] sprites, TextureAtlasSprite particle) {
        this.particle = particle;
        FaceBakery fb = new FaceBakery();
        for (int faceOrd = 0; faceOrd < 6; faceOrd++) {
            Direction face = Direction.values()[faceOrd];
            for (int mask = 0; mask < 16; mask++) {
                quads[faceOrd][mask] = List.of(buildFaceQuad(fb, face, sprites[mask]));
            }
        }
    }

    private static BakedQuad buildFaceQuad(FaceBakery fb, Direction face, TextureAtlasSprite sprite) {
        BlockFaceUV uv = new BlockFaceUV(new float[] {0, 0, 16, 16}, 0);
        BlockElementFace elementFace = new BlockElementFace(null, -1, "#texture", uv);
        return fb.bakeQuad(
                new Vector3f(0, 0, 0),
                new Vector3f(16, 16, 16),
                elementFace,
                sprite,
                face,
                BlockModelRotation.X0_Y0,
                null,
                true);
    }

    // -------------------------------------------------------------------------
    // BakedModel — vanilla methods
    // -------------------------------------------------------------------------

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
        return List.of();
    }

    @Override
    public boolean useAmbientOcclusion() {
        return true;
    }

    @Override
    public boolean isGui3d() {
        return true;
    }

    @Override
    public boolean usesBlockLight() {
        return true;
    }

    @Override
    public boolean isCustomRenderer() {
        return false;
    }

    @Override
    public TextureAtlasSprite getParticleIcon() {
        return particle;
    }

    @Override
    public ItemOverrides getOverrides() {
        return ItemOverrides.EMPTY;
    }

    @Override
    public ItemTransforms getTransforms() {
        return ItemTransforms.NO_TRANSFORMS;
    }

    // -------------------------------------------------------------------------
    // NeoForge extensions — dynamic rendering
    // -------------------------------------------------------------------------

    @Override
    public List<BakedQuad> getQuads(
            @Nullable BlockState state,
            @Nullable Direction side,
            RandomSource rand,
            ModelData data,
            @Nullable RenderType renderType) {
        if (side == null) return List.of();
        int[] masks = data.get(CONNECTION_PROPERTY);
        int mask = (masks != null) ? masks[side.ordinal()] : 15;
        return quads[side.ordinal()][mask];
    }

    @Override
    public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData modelData) {
        int[] masks = new int[6];
        for (Direction face : Direction.values()) {
            masks[face.ordinal()] = computeMask(level, pos, face);
        }
        return modelData.derive().with(CONNECTION_PROPERTY, masks).build();
    }

    private static int computeMask(BlockAndTintGetter level, BlockPos pos, Direction face) {
        Direction[] neighbors = FACE_NEIGHBORS[face.ordinal()];
        int mask = 0;
        for (int i = 0; i < 4; i++) {
            if (!level.getBlockState(pos.relative(neighbors[i])).is(Registration.CANVAS_BLOCK.get())) {
                mask |= (8 >> i); // bit 3=u, 2=d, 1=l, 0=r
            }
        }
        return mask;
    }
}
