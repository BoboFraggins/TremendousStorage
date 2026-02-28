package net.bobofraggins.intellistore.external.mekanism;

import mekanism.api.Action;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.chemical.IChemicalHandler;

/**
 * Exposes the Gas Tank as a single-tank {@link IChemicalHandler} for Mekanism pressure tubes
 * and Pipez gas pipes.
 *
 * <p>Insert rules:
 * <ol>
 *   <li>If the tank is unlocked, it locks to the inserted chemical type and fills.
 *   <li>If locked to a matching chemical, fills up to remaining capacity.
 *   <li>If locked to a different chemical, returns the full amount (nothing inserted).
 * </ol>
 *
 * <p>Extract rules:
 * <ol>
 *   <li>If unlocked or amount == 0, returns EMPTY.
 *   <li>Extracts up to {@code min(requested, amount)} mB.
 *   <li>The tank stays locked at amount 0 after full drain.
 * </ol>
 */
public class GasTankChemicalHandler implements IChemicalHandler {

    private final GasTankBlockEntity be;

    public GasTankChemicalHandler(GasTankBlockEntity be) {
        this.be = be;
    }

    @Override
    public int getChemicalTanks() {
        return 1;
    }

    @Override
    public ChemicalStack getChemicalInTank(int tank) {
        if (be.getStoredChemical().isEmpty() || be.getAmount() == 0) return ChemicalStack.EMPTY;
        return be.getStoredChemical().copyWithAmount(be.getAmount());
    }

    @Override
    public void setChemicalInTank(int tank, ChemicalStack stack) {
        // Direct set for automation/creative
        if (stack.isEmpty()) {
            be.extract(be.getAmount(), false);
        } else {
            long inserted = be.insert(stack, stack.getAmount(), false);
            if (inserted < stack.getAmount()) {
                // partial — shouldn't happen in normal automation, but handle gracefully
            }
        }
    }

    @Override
    public long getChemicalTankCapacity(int tank) {
        return GasTankBlockEntity.CAPACITY;
    }

    @Override
    public boolean isValid(int tank, ChemicalStack stack) {
        if (stack.isEmpty()) return false;
        ChemicalStack stored = be.getStoredChemical();
        if (stored.isEmpty()) return true; // unlocked — accepts anything
        return stored.getChemical().equals(stack.getChemical());
    }

    @Override
    public ChemicalStack insertChemical(int tank, ChemicalStack stack, Action action) {
        if (stack.isEmpty()) return ChemicalStack.EMPTY;
        if (be.isVoidExcess()) {
            // Accept anything the tank can match; excess is silently voided
            ChemicalStack stored = be.getStoredChemical();
            if (!stored.isEmpty() && !stored.getChemical().equals(stack.getChemical())) return stack;
            be.insert(stack, stack.getAmount(), action.simulate());
            return ChemicalStack.EMPTY;
        }
        long inserted = be.insert(stack, stack.getAmount(), action.simulate());
        if (inserted >= stack.getAmount()) return ChemicalStack.EMPTY;
        return stack.copyWithAmount(stack.getAmount() - inserted);
    }

    @Override
    public ChemicalStack extractChemical(int tank, long amount, Action action) {
        return be.extract(amount, action.simulate());
    }
}
