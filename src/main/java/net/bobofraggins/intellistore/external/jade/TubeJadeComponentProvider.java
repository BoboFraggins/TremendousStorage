package net.bobofraggins.intellistore.external.jade;

import net.bobofraggins.intellistore.shared.register.Registration;
import net.bobofraggins.intellistore.storage.tube.TubeBlock;
import net.bobofraggins.intellistore.storage.tubeattachments.AttachmentType;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.JadeIds;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.IElement;
import snownee.jade.api.ui.IElementHelper;

/**
 * When the player looks at a tube face that has an attachment, replaces the "Tube" block
 * name in the Jade HUD with the attachment type's name and shows its item icon.
 */
public enum TubeJadeComponentProvider implements IComponentProvider<BlockAccessor> {
    INSTANCE;

    @Override
    public ResourceLocation getUid() {
        return TubeJadeDataProvider.UID;
    }

    @Override
    public IElement getIcon(BlockAccessor accessor, IPluginConfig config, IElement currentIcon) {
        AttachmentType type = getAttachmentOnSide(accessor);
        if (type == AttachmentType.NONE) return currentIcon;
        ItemStack stack = attachmentItem(type);
        if (stack.isEmpty()) return currentIcon;
        return IElementHelper.get().item(stack);
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        AttachmentType type = getAttachmentOnSide(accessor);
        if (type == AttachmentType.NONE) return;

        // Replace the block name line with the attachment's name
        Component name = attachmentItem(type).getHoverName();
        tooltip.remove(JadeIds.MC_BLOCK_DISPLAY);
        tooltip.add(0, name);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static AttachmentType getAttachmentOnSide(BlockAccessor accessor) {
        CompoundTag data = accessor.getServerData();
        if (!data.contains("AttachmentTypes")) return AttachmentType.NONE;
        byte[] types = data.getByteArray("AttachmentTypes");

        // Build a temporary block entity view from synced data so we can reuse
        // TubeBlock.attachmentIndexAt for consistent AABB hit testing.
        BlockPos pos = accessor.getPosition();
        BlockHitResult hit = (BlockHitResult) accessor.getHitResult();
        Vec3 hitLocal = hit.getLocation().subtract(pos.getX(), pos.getY(), pos.getZ());

        for (int i = 0; i < 6 && i < types.length; i++) {
            AttachmentType type = AttachmentType.fromOrdinal(types[i] & 0xFF);
            if (type == AttachmentType.NONE) continue;
            if (TubeBlock.attachmentAABB(i).inflate(1e-3).contains(hitLocal)) return type;
        }
        return AttachmentType.NONE;
    }

    private static ItemStack attachmentItem(AttachmentType type) {
        return switch (type) {
            case STORAGE_INTERFACE -> new ItemStack(Registration.STORAGE_INTERFACE.get());
            case IMPORT_INTERFACE -> new ItemStack(Registration.IMPORT_INTERFACE.get());
            case EXPORT_INTERFACE -> new ItemStack(Registration.EXPORT_INTERFACE.get());
            case PLACER_INTERFACE -> new ItemStack(Registration.PLACER_INTERFACE.get());
            case BREAKER_INTERFACE -> new ItemStack(Registration.BREAKER_INTERFACE.get());
            default -> ItemStack.EMPTY;
        };
    }
}
