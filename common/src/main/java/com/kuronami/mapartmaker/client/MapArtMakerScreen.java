package com.kuronami.mapartmaker.client;

import com.kuronami.mapartmaker.Constants;
import com.kuronami.mapartmaker.menu.MapArtMakerMenu;
import com.kuronami.mapartmaker.network.CreateMapArtPayload;
import com.kuronami.mapartmaker.config.ModConfig;
import com.kuronami.mapartmaker.platform.Services;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import org.lwjgl.glfw.GLFW;

import java.nio.file.Path;
import java.util.List;

/**
 * 貼る・押す・待つの3操作しか無い。サイズは1辺のタイル数で、実際の画素は 128×タイル数。
 */
public class MapArtMakerScreen extends AbstractContainerScreen<MapArtMakerMenu> {

    private static final ResourceLocation BACKGROUND =
            ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "textures/gui/map_art_maker.png");

    /** The cartography table's parchment, borrowed from vanilla's sprite atlas at draw time. */
    private static final ResourceLocation MAP_SHEET =
            ResourceLocation.withDefaultNamespace("container/cartography_table/map");

    /** Vanilla's slot well, so the nine cells on the sheet match every other slot in the game. */
    private static final ResourceLocation SLOT = ResourceLocation.withDefaultNamespace("container/slot");

    private EditBox urlBox;
    private int tiles = 1;
    private boolean dither = true;

    public MapArtMakerScreen(MapArtMakerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = MapArtMakerMenu.PANEL_WIDTH;
        this.imageHeight = MapArtMakerMenu.PANEL_HEIGHT;
        this.inventoryLabelY = MapArtMakerMenu.INVENTORY_LABEL_Y;
    }

    @Override
    protected void init() {
        super.init();
        MapArtFeedbackHolder.clear();

        urlBox = new EditBox(font, leftPos + MapArtMakerMenu.URL_BOX_X, topPos + MapArtMakerMenu.URL_BOX_Y,
                MapArtMakerMenu.URL_BOX_WIDTH, MapArtMakerMenu.URL_BOX_HEIGHT,
                Component.translatable("gui.map_art_maker.url"));
        urlBox.setMaxLength(CreateMapArtPayload.MAX_URL_LENGTH);
        urlBox.setHint(Component.translatable("gui.map_art_maker.url_hint"));
        addRenderableWidget(urlBox);
        // The box is the first thing anyone touches, so it starts focused: otherwise the paste
        // shortcut silently does nothing until the box happens to be clicked.
        setInitialFocus(urlBox);

        addRenderableWidget(Button.builder(Component.translatable("gui.map_art_maker.paste"),
                        button -> pasteFromClipboard())
                .bounds(leftPos + MapArtMakerMenu.PASTE_BUTTON_X, topPos + MapArtMakerMenu.URL_BOX_Y,
                        MapArtMakerMenu.PASTE_BUTTON_WIDTH, MapArtMakerMenu.URL_BOX_HEIGHT)
                .build());

        int buttonY = topPos + MapArtMakerMenu.BUTTON_Y;
        int buttonWidth = MapArtMakerMenu.BUTTON_WIDTH;
        int buttonHeight = MapArtMakerMenu.BUTTON_HEIGHT;

        addRenderableWidget(Button.builder(sizeLabel(), button -> {
            tiles = tiles % ModConfig.maxTilesPerSide() + 1;
            button.setMessage(sizeLabel());
        }).bounds(leftPos + 8, buttonY, buttonWidth, buttonHeight).build());

        addRenderableWidget(Button.builder(ditherLabel(), button -> {
            dither = !dither;
            button.setMessage(ditherLabel());
        }).bounds(leftPos + 62, buttonY, buttonWidth, buttonHeight).build());

        addRenderableWidget(Button.builder(Component.translatable("gui.map_art_maker.create"),
                button -> submit()).bounds(leftPos + 116, buttonY, buttonWidth, buttonHeight).build());
    }

    private Component sizeLabel() {
        return Component.translatable("gui.map_art_maker.size", tiles, tiles);
    }

    private Component ditherLabel() {
        return Component.translatable(dither ? "gui.map_art_maker.dither_on" : "gui.map_art_maker.dither_off");
    }

    private void submit() {
        String url = urlBox.getValue().trim();
        if (url.isEmpty()) {
            return;
        }
        Services.NETWORK.sendToServer(new CreateMapArtPayload(menu.pos(), url, tiles, tiles, dither));
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(BACKGROUND, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        // Vanilla's own parchment, drawn from the game's sprite atlas rather than copied into our
        // texture: identical to the cartography table by construction, and nothing of Mojang's is
        // redistributed because the sprite never leaves the game.
        graphics.blitSprite(MAP_SHEET,
                leftPos + MapArtMakerMenu.SHEET_X, topPos + MapArtMakerMenu.SHEET_Y,
                MapArtMakerMenu.SHEET_SIZE, MapArtMakerMenu.SHEET_SIZE);
        // The sheet covers the output area, so the nine wells go back on top of it.
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                graphics.blitSprite(SLOT,
                        leftPos + MapArtMakerMenu.OUTPUT_X + column * MapArtMakerMenu.SLOT_PITCH - 1,
                        topPos + MapArtMakerMenu.OUTPUT_Y + row * MapArtMakerMenu.SLOT_PITCH - 1,
                        18, 18);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        Component feedback = MapArtFeedbackHolder.message();
        if (!feedback.getString().isEmpty()) {
            int colour = MapArtFeedbackHolder.success() ? 0x4CAF50 : 0xC62828;
            graphics.drawString(font, feedback, 8, MapArtMakerMenu.FEEDBACK_Y, colour, false);
        }
    }

    /**
     * A file dropped on the window while this screen is open. Only the first file is used — a
     * player dropping several images at once almost certainly meant the top one, and there is
     * nowhere in this GUI to ask which they meant without adding a display this block does not have.
     */
    @Override
    public void onFilesDrop(List<Path> paths) {
        if (paths.isEmpty()) {
            return;
        }
        MapArtDropHandler.handleDrop(menu.pos(), paths.get(0), tiles, dither);
    }

    /** Reads the system clipboard into the URL box, replacing whatever is there. */
    private void pasteFromClipboard() {
        String clipboard = Minecraft.getInstance().keyboardHandler.getClipboard();
        if (clipboard == null || clipboard.isBlank()) {
            return;
        }
        urlBox.setValue(clipboard.trim());
        setFocused(urlBox);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // While the box has focus every key belongs to it, escape aside. Falling through to the
        // container screen would let the inventory key close the GUI mid-URL, and "e" appears in
        // most links.
        if (urlBox != null && urlBox.isFocused() && keyCode != GLFW.GLFW_KEY_ESCAPE) {
            urlBox.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
