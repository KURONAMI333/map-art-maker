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

    /** Output slots are a 3x3 grid, so a tile's position in the picture picks its slot. */
    public static final int OUTPUT_GRID_SIDE = 3;

    /** The slot a tile at column {@code x}, row {@code y} of the picture belongs in. */
    public static int outputSlot(int x, int y) {
        return 1 + y * OUTPUT_GRID_SIDE + x;
    }

    /**
     * Consumes blanks and stores the finished tiles where they belong.
     *
     * <p>Tiles keep their position: the top-right piece of the picture goes in the top-right slot,
     * so a 2x2 fills the top-left corner of the grid rather than running along the first four
     * slots. That means the target slots specifically have to be free, not just any four of them.
     *
     * @param results one tile per grid cell in reading order, {@code tilesX * tilesY} of them
     */
    public boolean consumeBlanksAndStore(int tilesX, int tilesY, java.util.List<ItemStack> results) {
        if (!canAccept(tilesX, tilesY, results.size())) {
            return false;
        }

        int required = tilesX * tilesY;
        items.get(SLOT_BLANK).shrink(required);
        for (int y = 0; y < tilesY; y++) {
            for (int x = 0; x < tilesX; x++) {
                items.set(outputSlot(x, y), results.get(y * tilesX + x));
            }
        }
        setChanged();
        return true;
    }

    /**
     * Non-mutating version of the check {@link #consumeBlanksAndStore} performs, so callers can
     * confirm a grid will fit <em>before</em> paying the cost of building the tiles that would go
     * in it (map ids are permanent world state the moment they are minted, so nothing should be
     * built for a request that is going to be refused anyway).
     */
    public boolean canPlace(int tilesX, int tilesY) {
        return canAccept(tilesX, tilesY, tilesX * tilesY);
    }

    private boolean canAccept(int tilesX, int tilesY, int resultsSize) {
        int required = tilesX * tilesY;
        if (tilesX <= 0 || tilesY <= 0
                || tilesX > OUTPUT_GRID_SIDE || tilesY > OUTPUT_GRID_SIDE
                || resultsSize != required) {
            return false;
        }
        if (blankCount() < required) {
            return false;
        }
        for (int y = 0; y < tilesY; y++) {
            for (int x = 0; x < tilesX; x++) {
                if (!items.get(outputSlot(x, y)).isEmpty()) {
                    return false;
                }
            }
        }
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
