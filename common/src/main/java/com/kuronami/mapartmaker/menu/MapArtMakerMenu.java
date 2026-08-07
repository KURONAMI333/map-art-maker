package com.kuronami.mapartmaker.menu;

import com.kuronami.mapartmaker.block.MapArtMakerBlockEntity;
import com.kuronami.mapartmaker.register.ModMenus;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class MapArtMakerMenu extends AbstractContainerMenu {

    private final Container container;
    private final BlockPos pos;

    /** Client side: the container is a stand-in, contents arrive through the usual slot sync. */
    public MapArtMakerMenu(int id, Inventory playerInventory, BlockPos pos) {
        this(id, playerInventory, new SimpleContainer(MapArtMakerBlockEntity.SIZE), pos);
    }

    /** Server side. */
    public MapArtMakerMenu(int id, Inventory playerInventory, MapArtMakerBlockEntity blockEntity) {
        this(id, playerInventory, blockEntity, blockEntity.getBlockPos());
    }

    private MapArtMakerMenu(int id, Inventory playerInventory, Container container, BlockPos pos) {
        super(ModMenus.MAP_ART_MAKER.get(), id);
        checkContainerSize(container, MapArtMakerBlockEntity.SIZE);
        this.container = container;
        this.pos = pos;

        addSlot(new Slot(container, MapArtMakerBlockEntity.SLOT_BLANK, 26, 24) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(Items.MAP);
            }
        });

        // 3x3 of finished art, mirroring the default tile limit.
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                addSlot(new OutputSlot(container, 1 + row * 3 + column, 98 + column * 18, 17 + row * 18));
            }
        }

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(playerInventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(playerInventory, column, 8 + column * 18, 142));
        }
    }

    public BlockPos pos() {
        return pos;
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack original = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot.hasItem()) {
            ItemStack stack = slot.getItem();
            original = stack.copy();
            int inventoryStart = MapArtMakerBlockEntity.SIZE;
            if (index < inventoryStart) {
                if (!moveItemStackTo(stack, inventoryStart, slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(stack, MapArtMakerBlockEntity.SLOT_BLANK,
                    MapArtMakerBlockEntity.SLOT_BLANK + 1, false)) {
                return ItemStack.EMPTY;
            }

            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return original;
    }

    /** Finished maps can be taken out but never put back in. */
    private static final class OutputSlot extends Slot {
        private OutputSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }
}
