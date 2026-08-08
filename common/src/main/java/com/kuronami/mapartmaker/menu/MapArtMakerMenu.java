package com.kuronami.mapartmaker.menu;

import com.kuronami.mapartmaker.block.MapArtMakerBlockEntity;
import com.kuronami.mapartmaker.network.MapArtTileAccumulator;
import com.kuronami.mapartmaker.register.ModMenus;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
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

    /**
     * Panel geometry, shared with the screen so slots and widgets cannot drift apart.
     *
     * <p>The screen stacks a URL field and a button row above the slots, so the panel is taller
     * than the vanilla 166: the working area has to fit 16 (field) + 18 (buttons) + 54 (the 3x3
     * output) plus gaps. Every output slot has to stay reachable by hand — {@code
     * MapArtMakerBlockEntity.consumeBlanksAndStore} needs all nine free, so a covered slot would
     * strand the block permanently for anyone without hoppers.
     */
    public static final int PANEL_WIDTH = 176;
    public static final int PANEL_HEIGHT = 224;

    public static final int URL_BOX_X = 8;
    public static final int URL_BOX_Y = 20;
    /** Narrowed to leave room for the paste button beside it. */
    public static final int URL_BOX_WIDTH = 124;
    public static final int URL_BOX_HEIGHT = 16;

    /** Pasting a link is the normal way in, so it gets a button rather than only a shortcut. */
    public static final int PASTE_BUTTON_X = 134;
    public static final int PASTE_BUTTON_WIDTH = 34;

    public static final int BUTTON_Y = 40;
    public static final int BUTTON_WIDTH = 52;
    public static final int BUTTON_HEIGHT = 18;

    public static final int FEEDBACK_Y = 60;

    /** Vanilla's map sprite is 66x66; centring it on the output grid leaves a 6px paper margin. */
    public static final int SHEET_SIZE = 66;
    public static final int SHEET_X = 91;
    public static final int SHEET_Y = 67;

    public static final int BLANK_SLOT_X = 26;
    public static final int BLANK_SLOT_Y = 92;
    public static final int OUTPUT_X = 98;
    public static final int OUTPUT_Y = 74;
    public static final int SLOT_PITCH = 18;

    /** Vanilla spacing measured from the bottom of the panel, so it holds at any panel height. */
    public static final int INVENTORY_LABEL_Y = PANEL_HEIGHT - 94;
    private static final int PLAYER_INVENTORY_Y = PANEL_HEIGHT - 82;
    private static final int HOTBAR_Y = PANEL_HEIGHT - 24;

    private MapArtMakerMenu(int id, Inventory playerInventory, Container container, BlockPos pos) {
        super(ModMenus.MAP_ART_MAKER.get(), id);
        checkContainerSize(container, MapArtMakerBlockEntity.SIZE);
        this.container = container;
        this.pos = pos;

        addSlot(new Slot(container, MapArtMakerBlockEntity.SLOT_BLANK, BLANK_SLOT_X, BLANK_SLOT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(Items.MAP);
            }
        });

        // 3x3 of finished art, mirroring the default tile limit.
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                addSlot(new OutputSlot(container, 1 + row * 3 + column,
                        OUTPUT_X + column * SLOT_PITCH, OUTPUT_Y + row * SLOT_PITCH));
            }
        }

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(playerInventory, column + row * 9 + 9,
                        8 + column * SLOT_PITCH, PLAYER_INVENTORY_Y + row * SLOT_PITCH));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(playerInventory, column, 8 + column * SLOT_PITCH, HOTBAR_Y));
        }
    }

    public BlockPos pos() {
        return pos;
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }

    /**
     * Closing the GUI is one of the three ways an in-flight file-drop upload gets discarded (the
     * others: disconnect, and a periodic timeout sweep) — otherwise a half-finished drop would sit
     * in the server's accumulator until it happened to time out.
     */
    @Override
    public void removed(Player player) {
        super.removed(player);
        if (player instanceof ServerPlayer serverPlayer) {
            MapArtTileAccumulator.discard(serverPlayer.getUUID());
        }
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
