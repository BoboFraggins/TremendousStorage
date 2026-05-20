package net.bobofraggins.tremendousstorage.storage.networkinterface;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.bobofraggins.tremendousstorage.shared.tank.AbstractTankRenderer;
import net.bobofraggins.tremendousstorage.storage.tank.TankRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;

public class NetworkInterfaceRenderer extends AbstractTankRenderer<NetworkInterfaceBlockEntity> {

    private static final float FILL_FRAC = 0.95f;

    public NetworkInterfaceRenderer(BlockEntityRendererProvider.Context ctx) {}

    public static class NetworkInterfaceState extends State {
        public boolean hasFill;
        public int fr, fg, fb, fa;
        public float fillTop;
        public float uL, uR, vT, vB;
        public int fluidLight;
        public float bob;
        public float rotY;
        public ItemStack brainStack = ItemStack.EMPTY;
    }

    @Override
    public State createRenderState() {
        return new NetworkInterfaceState();
    }

    @Override
    public void extractRenderState(
            NetworkInterfaceBlockEntity be,
            State stateBase,
            float partialTick,
            Vec3 camera,
            ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        super.extractRenderState(be, stateBase, partialTick, camera, breakProgress);
        if (!(stateBase instanceof NetworkInterfaceState state)) return;

        Level level = be.getLevel();
        double time = level != null ? (level.getGameTime() + partialTick) / 20.0 : 0;
        state.bob = (float) Math.sin(time * Math.PI * 0.25) * 0.04f;
        state.rotY = (float) ((time * 10.0) % 360.0);
        state.brainStack = new ItemStack(Registration.BRAIN.get());

        FluidStack vibes = new FluidStack(Registration.POSITIVE_VIBES_SOURCE.get(), 1000);
        net.minecraft.client.renderer.block.FluidModel fluidModel_ = net.minecraft.client.Minecraft.getInstance()
                .getModelManager()
                .getFluidStateModelSet()
                .get(net.bobofraggins.tremendousstorage.shared.register.Registration.POSITIVE_VIBES_SOURCE
                        .get()
                        .defaultFluidState());
        var fluidSprite = fluidModel_.stillMaterial().sprite();
        int tint = fluidModel_.fluidTintSource() != null
                ? fluidModel_
                        .fluidTintSource()
                        .colorAsStack(new net.neoforged.neoforge.fluids.FluidStack(
                                Registration.POSITIVE_VIBES_SOURCE.get(), 1))
                : 0xFFFFFFFF;
        state.fr = (tint >> 16) & 0xFF;
        state.fg = (tint >> 8) & 0xFF;
        state.fb = tint & 0xFF;
        state.fa = (tint >> 24) & 0xFF;
        if (state.fa == 0) state.fa = 160;
        state.fa /= 2;
        state.fillTop = TankRenderer.TANK_FLUID_FLOOR + FILL_FRAC * TankRenderer.TANK_FLUID_H;
        state.uL = fluidSprite.getU0();
        state.uR = fluidSprite.getU1();
        state.vT = fluidSprite.getV0();
        state.vB = Mth.lerp(FILL_FRAC, fluidSprite.getV0(), fluidSprite.getV1());
        state.fluidLight =
                Registration.POSITIVE_VIBES_TYPE.get().getLightLevel() > 0 ? 0xF000F0 : stateBase.lightCoords;
        state.hasFill = true;
    }

    @Override
    protected void renderFill(
            State stateBase, PoseStack poseStack, SubmitNodeCollector collector, int packedLight, int packedOverlay) {
        if (!(stateBase instanceof NetworkInterfaceState state) || !state.hasFill) return;
        int fr = state.fr, fg = state.fg, fb = state.fb, fa = state.fa;
        int fluidLight = state.fluidLight;
        float uL = state.uL, uR = state.uR, vT = state.vT, vB = state.vB;
        float fillTop = state.fillTop;
        collector.submitCustomGeometry(
                poseStack,
                Sheets.translucentBlockSheet(),
                (pose, vc) -> TankRenderer.renderCubeFill(
                        vc, pose.pose(), fr, fg, fb, fa, fluidLight, packedOverlay, uL, vT, uR, vB, fillTop));
    }

    @Override
    public void submit(
            State stateBase, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState cameraState) {
        super.submit(stateBase, poseStack, collector, cameraState);
        if (!(stateBase instanceof NetworkInterfaceState state)) return;

        poseStack.pushPose();
        poseStack.translate(0.5, 0.5 + state.bob, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(state.rotY));
        poseStack.scale(0.6f, 0.6f, 0.6f);
        poseStack.mulPose(Axis.XP.rotationDegrees(3f));

        ItemStackRenderState brainRenderState = new ItemStackRenderState();
        Minecraft.getInstance()
                .getItemModelResolver()
                .updateForTopItem(brainRenderState, state.brainStack, ItemDisplayContext.FIXED, null, null, 0);
        brainRenderState.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);

        poseStack.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public AABB getRenderBoundingBox(NetworkInterfaceBlockEntity be) {
        return new AABB(be.getBlockPos()).inflate(1.0);
    }
}
