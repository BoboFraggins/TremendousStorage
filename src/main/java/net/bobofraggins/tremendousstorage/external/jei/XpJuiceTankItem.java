package net.bobofraggins.tremendousstorage.external.jei;

import net.bobofraggins.tremendousstorage.external.mobgrindinutils.MobGrindingUtilsIntegration;
import net.bobofraggins.tremendousstorage.shared.register.Registration;
import net.bobofraggins.tremendousstorage.storage.tank.TankBlockEntity;
import net.bobofraggins.tremendousstorage.storage.tank.TankContents;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

/** Produces an {@link ItemStack} representing a Wood-tier Tank full of XP Juice, for JEI displays. */
public final class XpJuiceTankItem {

    private XpJuiceTankItem() {}

    public static ItemStack create() {
        ItemStack stack = new ItemStack(Registration.TANK_ITEM.get());
        FluidStack fluid = new FluidStack(MobGrindingUtilsIntegration.getXpFluid(), 1);
        stack.set(Registration.TANK_CONTENTS.get(), new TankContents(fluid, TankBlockEntity.BASE_CAPACITY));
        return stack;
    }
}
