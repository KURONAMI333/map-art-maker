package com.kuronami.mapartmaker.client;

import com.kuronami.mapartmaker.Constants;
import com.kuronami.mapartmaker.menu.MapArtMakerMenu;
import com.kuronami.mapartmaker.network.CreateMapArtPayload;
import com.kuronami.mapartmaker.config.ModConfig;
import com.kuronami.mapartmaker.platform.Services;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import org.lwjgl.glfw.GLFW;

import java.nio.file.Path;
import java.util.List;

/**
 * 貼る・押す・待つの3操作しか無い。サイズは1辺のタイル数で、実際の画素は 128×タイル数。
 *
 * <p>26.x は extract 方式（{@code render}/{@code renderBg}/{@code renderLabels} は存在しない）。
 * 同一 stratum 内の提出順は z 順ではないので、背景に属するもの（パネル PNG・羊皮紙・9枠）は
 * 全て {@link #extractBackground} に集約し、{@link #extractLabels} には文字だけを残す。
 */
public class MapArtMakerScreen extends AbstractContainerScreen<MapArtMakerMenu> {

    private static final Identifier BACKGROUND =
            Identifier.fromNamespaceAndPath(Constants.MOD_ID, "textures/gui/map_art_maker.png");

    /** 背景 PNG の実寸。26.x の blit はテクスチャ実寸の明示が要る。 */
    private static final int BACKGROUND_TEXTURE_SIZE = 256;

    /** The cartography table's parchment, borrowed from vanilla's sprite atlas at draw time. */
    private static final Identifier MAP_SHEET =
            Identifier.withDefaultNamespace("container/cartography_table/map");

    /** Vanilla's slot well, so the nine cells on the sheet match every other slot in the game. */
    private static final Identifier SLOT = Identifier.withDefaultNamespace("container/slot");

    private EditBox urlBox;
    private int tiles = 1;
    private boolean dither = true;

    public MapArtMakerScreen(MapArtMakerMenu menu, Inventory inventory, Component title) {
        // 26.x の imageWidth / imageHeight は final。5引数 super で渡す。
        super(menu, inventory, title, MapArtMakerMenu.PANEL_WIDTH, MapArtMakerMenu.PANEL_HEIGHT);
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
        }).bounds(leftPos + MapArtMakerMenu.BUTTON_X[0], buttonY, buttonWidth, buttonHeight).build());

        addRenderableWidget(Button.builder(ditherLabel(), button -> {
            dither = !dither;
            button.setMessage(ditherLabel());
        }).bounds(leftPos + MapArtMakerMenu.BUTTON_X[1], buttonY, buttonWidth, buttonHeight).build());

        addRenderableWidget(Button.builder(Component.translatable("gui.map_art_maker.create"),
                button -> submit())
                .bounds(leftPos + MapArtMakerMenu.BUTTON_X[2], buttonY, buttonWidth, buttonHeight).build());
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

    /**
     * 背景 stratum。{@code super} が世界のぼかしとメニュー背景（開いている間の暗転）を出すので、
     * 呼び忘れると暗転が黙って消える（バニラの同型 {@code CartographyTableScreen} も先頭で super を呼ぶ）。
     */
    @Override
    public void extractBackground(GuiGraphicsExtractor extractor, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(extractor, mouseX, mouseY, partialTick);
        extractor.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos, topPos, 0.0F, 0.0F,
                imageWidth, imageHeight, BACKGROUND_TEXTURE_SIZE, BACKGROUND_TEXTURE_SIZE);
        // Vanilla's own parchment, drawn from the game's sprite atlas rather than copied into our
        // texture: identical to the cartography table by construction, and nothing of Mojang's is
        // redistributed because the sprite never leaves the game.
        extractor.blitSprite(RenderPipelines.GUI_TEXTURED, MAP_SHEET,
                leftPos + MapArtMakerMenu.SHEET_X, topPos + MapArtMakerMenu.SHEET_Y,
                MapArtMakerMenu.SHEET_SIZE, MapArtMakerMenu.SHEET_SIZE);
        // The sheet covers the output area, so the nine wells go back on top of it.
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                extractor.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT,
                        leftPos + MapArtMakerMenu.OUTPUT_X + column * MapArtMakerMenu.SLOT_PITCH - 1,
                        topPos + MapArtMakerMenu.OUTPUT_Y + row * MapArtMakerMenu.SLOT_PITCH - 1,
                        18, 18);
            }
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor extractor, int mouseX, int mouseY) {
        super.extractLabels(extractor, mouseX, mouseY);
        Component feedback = MapArtFeedbackHolder.message();
        if (!feedback.getString().isEmpty()) {
            int colour = MapArtFeedbackHolder.success() ? 0xFF4CAF50 : 0xFFC62828;
            extractor.text(font, feedback, MapArtMakerMenu.FEEDBACK_X, MapArtMakerMenu.FEEDBACK_Y, colour, false);
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
    public boolean keyPressed(KeyEvent event) {
        // While the box has focus every key belongs to it, escape aside. Falling through to the
        // container screen would let the inventory key close the GUI mid-URL, and "e" appears in
        // most links.
        if (urlBox != null && urlBox.isFocused() && event.key() != GLFW.GLFW_KEY_ESCAPE) {
            urlBox.keyPressed(event);
            return true;
        }
        return super.keyPressed(event);
    }
}
