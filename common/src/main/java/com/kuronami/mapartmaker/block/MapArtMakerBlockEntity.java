package com.kuronami.mapartmaker.block;

import com.kuronami.mapartmaker.register.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.Direction;

/**
 * Holds the blank maps a player feeds in and the finished art waiting to be taken out.
 *
 * <p>{@link WorldlyContainer} so hoppers can load blanks and pull results — map art walls are
 * built in bulk, and making players hand-feed 9 maps at a time would be the wrong kind of friction.
 */
public class MapArtMakerBlockEntity extends BlockEntity implements WorldlyContainer {

    public static final int SLOT_BLANK = 0;
    /** Nine outputs so the default 3x3 limit can be produced in one go. */
    public static final int OUTPUT_SLOTS = 9;
    public static final int SIZE = 1 + OUTPUT_SLOTS;

    private static final int[] BLANK_SIDE = {SLOT_BLANK};
    private static final int[] OUTPUT_SIDES = buildOutputSlots();

    private NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);

    public MapArtMakerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MAP_ART_MAKER.get(), pos, state);
    }

    /** How many blank maps are available right now. */
    public int blankCount() {
        return items.get(SLOT_BLANK).getCount();
    }

    /** Consumes blanks and stores results. Returns false when there is not enough room or stock. */
    public boolean consumeBlanksAndStore(int required, java.util.List<ItemStack> results) {
        if (required <= 0 || results.size() != required) {
            return false;
        }
        if (blankCount() < required || freeOutputSlots() < required) {
            return false;
        }
        items.get(SLOT_BLANK).shrink(required);
        int placed = 0;
        for (int slot = 1; slot < SIZE && placed < results.size(); slot++) {
            if (items.get(slot).isEmpty()) {
                items.set(slot, results.get(placed++));
            }
        }
        setChanged();
        return true;
    }

    public int freeOutputSlots() {
        int free = 0;
        for (int slot = 1; slot < SIZE; slot++) {
            if (items.get(slot).isEmpty()) {
                free++;
            }
        }
        return free;
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items, registries);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
    }

    // --- Container ---

    @Override
    public int getContainerSize() {
        return SIZE;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack removed = ContainerHelper.removeItem(items, slot, amount);
        if (!removed.isEmpty()) {
            setChanged();
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        stack.limitSize(getMaxStackSize(stack));
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        items.clear();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == SLOT_BLANK && stack.is(Items.MAP);
    }

    // --- WorldlyContainer ---

    @Override
    public int[] getSlotsForFace(Direction side) {
        return side == Direction.DOWN ? OUTPUT_SIDES : BLANK_SIDE;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        return canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot != SLOT_BLANK;
    }

    private static int[] buildOutputSlots() {
        int[] slots = new int[OUTPUT_SLOTS];
        for (int i = 0; i < OUTPUT_SLOTS; i++) {
            slots[i] = i + 1;
        }
        return slots;
    }
}
