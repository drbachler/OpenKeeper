/*
 * Copyright (C) 2014-2015 OpenKeeper
 *
 * OpenKeeper is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenKeeper is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenKeeper.  If not, see <http://www.gnu.org/licenses/>.
 */
package toniarts.openkeeper.game.state;

import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetManager;
import com.jme3.asset.DesktopAssetManager;
import com.jme3.asset.TextureKey;
import com.jme3.font.Rectangle;
import com.jme3.input.InputManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.texture.plugins.AWTLoader;
import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.NiftyEventSubscriber;
import de.lessvoid.nifty.NiftyIdCreator;
import de.lessvoid.nifty.builder.EffectBuilder;
import de.lessvoid.nifty.builder.HoverEffectBuilder;
import de.lessvoid.nifty.builder.ImageBuilder;
import de.lessvoid.nifty.controls.Label;
import de.lessvoid.nifty.controls.TabSelectedEvent;
import de.lessvoid.nifty.controls.label.builder.LabelBuilder;
import de.lessvoid.nifty.effects.EffectEventId;
import de.lessvoid.nifty.elements.Element;
import de.lessvoid.nifty.elements.render.ImageRenderer;
import de.lessvoid.nifty.render.NiftyImage;
import de.lessvoid.nifty.render.image.ImageModeFactory;
import de.lessvoid.nifty.render.image.ImageModeHelper;
import de.lessvoid.nifty.screen.Screen;
import de.lessvoid.nifty.screen.ScreenController;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import toniarts.openkeeper.Main;
import toniarts.openkeeper.game.PlayerManaControl;
import toniarts.openkeeper.gui.nifty.NiftyUtils;
import toniarts.openkeeper.gui.nifty.icontext.IconTextBuilder;
import toniarts.openkeeper.tools.convert.AssetsConverter;
import toniarts.openkeeper.tools.convert.ConversionUtils;
import toniarts.openkeeper.tools.convert.map.Door;
import toniarts.openkeeper.tools.convert.map.KeeperSpell;
import toniarts.openkeeper.tools.convert.map.Player;
import toniarts.openkeeper.tools.convert.map.Room;
import toniarts.openkeeper.tools.convert.map.Trap;
import toniarts.openkeeper.utils.Utils;
import toniarts.openkeeper.view.PlayerCameraState;
import toniarts.openkeeper.view.PlayerInteractionState;
import toniarts.openkeeper.view.PlayerInteractionState.InteractionState;

/**
 * The player state! GUI, camera, etc. Player interactions
 *
 * @author Toni Helenius <helenius.toni@gmail.com>
 */
public class PlayerState extends AbstractAppState implements ScreenController {

    private enum PauseMenuState {

        MAIN, QUIT, CONFIRMATION;
    }
    private Main app;
    private Node rootNode;
    private AssetManager assetManager;
    private AppStateManager stateManager;
    private InputManager inputManager;
    private ViewPort viewPort;
    private GameState gameState;
    private Nifty nifty;
    private Screen screen;
    private boolean paused = false;
    private boolean backgroundSet = false;
    private static final String HUD_SCREEN_ID = "hud";
    private List<AbstractPauseAwareState> appStates = new ArrayList<>();
    private PlayerInteractionState interactionState;
    private Label goldCurrent;
    private Label tooltip;
    private static final Logger logger = Logger.getLogger(PlayerState.class.getName());

    @Override
    public void initialize(final AppStateManager stateManager, final Application app) {
        super.initialize(stateManager, app);
        this.app = (Main) app;
        rootNode = this.app.getRootNode();
        assetManager = this.app.getAssetManager();
        this.stateManager = this.app.getStateManager();
        inputManager = this.app.getInputManager();
        viewPort = this.app.getViewPort();
    }

    @Override
    public void cleanup() {

        // Detach states
        for (AbstractAppState state : appStates) {
            stateManager.detach(state);
        }

        super.cleanup();
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (enabled) {

            // Get the game state
            gameState = stateManager.getState(GameState.class);

            // Load the HUD
            app.getNifty().getNifty().gotoScreen(HUD_SCREEN_ID);

            // Init the HUD items
            initHudItems();

            // Cursor
            app.getInputManager().setCursorVisible(true);

            // Get GUI area constraints
            Element middle = app.getNifty().getNifty().getScreen(HUD_SCREEN_ID).findElementByName("middle");
            Rectangle guiConstraint = new Rectangle(middle.getX(), middle.getY(), middle.getWidth(), middle.getHeight());

            // Set the pause state
            if (nifty != null) {
                paused = false;
                nifty.getScreen(HUD_SCREEN_ID).findElementByName("optionsMenu").setVisible(paused);
            }

            // Create app states
            Player player = gameState.getLevelData().getPlayer((short) 3); // Keeper 1
            appStates.add(new PlayerCameraState(player));
            interactionState = new PlayerInteractionState(player, gameState, guiConstraint) {
                @Override
                protected void onInteractionStateChange(InteractionState interactionState, int id) {
                    PlayerState.this.updateSelectedItem(interactionState, id);
                }
            };
            appStates.add(interactionState);

            // Load the state
            for (AbstractAppState state : appStates) {
                stateManager.attach(state);
            }
        } else {

            // Detach states
            for (AbstractAppState state : appStates) {
                stateManager.detach(state);
            }
            gameState = null;
            appStates.clear();
            if (nifty != null) {
                nifty.gotoScreen("empty");
            }
        }
    }

    /**
     * Initializes the HUD items, such as buildings, spells etc. to level
     * initials
     */
    private void initHudItems() {
        Screen hud = nifty.getScreen(HUD_SCREEN_ID);

        gameState.getPlayerManaControl().addListener(hud.findNiftyControl("mana", Label.class), PlayerManaControl.Type.CURRENT);
        gameState.getPlayerManaControl().addListener(hud.findNiftyControl("manaGet", Label.class), PlayerManaControl.Type.GET);
        gameState.getPlayerManaControl().addListener(hud.findNiftyControl("manaLose", Label.class), PlayerManaControl.Type.LOSE);

        Element contentPanel = hud.findElementByName("tab-room-content");
        removeAllChildElements(contentPanel);
        for (final Room room : getAvailableRoomsToBuild()) {
            createRoomIcon(room).build(nifty, hud, contentPanel);
        }

        contentPanel = hud.findElementByName("tab-spell-content");
        removeAllChildElements(contentPanel);
        for (final KeeperSpell spell : getAvailableKeeperSpells()) {
            createSpellIcon(spell).build(nifty, hud, contentPanel);
        }

        contentPanel = hud.findElementByName("tab-door-content");
        removeAllChildElements(contentPanel);
        for (final Door door : getAvailableDoors()) {
            createDoorIcon(door).build(nifty, hud, contentPanel);
        }

        contentPanel = hud.findElementByName("tab-trap-content");
        removeAllChildElements(contentPanel);
        for (final Trap trap : getAvailableTraps()) {
            createTrapIcon(trap).build(nifty, hud, contentPanel);
        }

        if (goldCurrent == null) {
            goldCurrent = hud.findNiftyControl("gold", Label.class);
        }

        if (tooltip == null) {
            tooltip = hud.findNiftyControl("tooltip", Label.class);
        }
    }

    /**
     * Deletes all children from element
     *
     * @param element parent
     */
    private void removeAllChildElements(Element element) {
        for (Element e : element.getElements()) {
            e.markForRemoval();
        }
    }

    @Override
    public void bind(Nifty nifty, Screen screen) {
        this.nifty = nifty;
        this.screen = screen;
    }

    @Override
    public void onStartScreen() {
        switch (nifty.getCurrentScreen().getScreenId()) {
            case HUD_SCREEN_ID: {

                if (!backgroundSet) {

                    // Stretch the background image (height-wise) on the background image panel
                    try {
                        BufferedImage img = ImageIO.read(assetManager.locateAsset(new AssetKey("Textures/GUI/Windows/Panel-BG.png")).openStream());

                        // Scale the backgroung image to the panel height, keeping the aspect ratio
                        Element panel = nifty.getCurrentScreen().findElementByName("bottomBackgroundPanel");
                        BufferedImage newImage = new BufferedImage(panel.getHeight() * img.getWidth() / img.getHeight(), panel.getHeight(), img.getType());
                        Graphics2D g = newImage.createGraphics();
                        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                        g.drawImage(img, 0, 0, newImage.getWidth(), newImage.getHeight(), 0, 0, img.getWidth(), img.getHeight(), null);
                        g.dispose();

                        // Convert the new image to a texture, and add a dummy cached entry to the asset manager
                        AWTLoader loader = new AWTLoader();
                        Texture tex = new Texture2D(loader.load(newImage, false));
                        ((DesktopAssetManager) assetManager).addToCache(new TextureKey("HUDBackground"), tex);

                        // Add the scaled one
                        NiftyImage niftyImage = nifty.createImage("HUDBackground", true);
                        String resizeString = "repeat:0,0," + newImage.getWidth() + "," + newImage.getHeight();
                        String areaProviderProperty = ImageModeHelper.getAreaProviderProperty(resizeString);
                        String renderStrategyProperty = ImageModeHelper.getRenderStrategyProperty(resizeString);
                        niftyImage.setImageMode(ImageModeFactory.getSharedInstance().createImageMode(areaProviderProperty, renderStrategyProperty));
                        ImageRenderer renderer = panel.getRenderer(ImageRenderer.class);
                        renderer.setImage(niftyImage);

                        backgroundSet = true;
                    } catch (IOException ex) {
                        logger.log(Level.SEVERE, "Failed to open the background image!", ex);
                    }
                }

                break;
            }
        }
    }

    private ImageBuilder createRoomIcon(final Room room) {
        return new ImageBuilder() {
            {
                filename(ConversionUtils.getCanonicalAssetKey(AssetsConverter.TEXTURES_FOLDER.concat(File.separator).concat(room.getGuiIcon().getName()).concat(".png")));
                valignCenter();
                marginRight("3px");
                interactOnClick("select(room, " + room.getRoomId() + ")");
                id("room_" + room.getRoomId());
                onHoverEffect(new HoverEffectBuilder("imageOverlay") {
                    {
                        effectParameter("filename", ConversionUtils.getCanonicalAssetKey(AssetsConverter.TEXTURES_FOLDER.concat(File.separator).concat("GUI/Icons/frame.png")));
                        post(true);
                    }
                });
                onCustomEffect(new EffectBuilder("imageOverlay") {
                    {
                        effectParameter("filename", ConversionUtils.getCanonicalAssetKey(AssetsConverter.TEXTURES_FOLDER.concat(File.separator).concat("GUI/Icons/selected-spell.png")));
                        effectParameter("customKey", "select");
                        post(true);
                        neverStopRendering(true);
                    }
                });
            }
        };
    }

    private ImageBuilder createSpellIcon(final KeeperSpell spell) {
        return new ImageBuilder() {
            {
                filename(ConversionUtils.getCanonicalAssetKey(AssetsConverter.TEXTURES_FOLDER + File.separator + spell.getGuiIcon().getName() + ".png"));
                valignCenter();
                marginRight("3px");
                interactOnClick("select(spell, " + spell.getKeeperSpellId() + ")");
                id("spell_" + spell.getKeeperSpellId());
                onHoverEffect(new HoverEffectBuilder("imageOverlay") {
                    {
                        effectParameter("filename", ConversionUtils.getCanonicalAssetKey(AssetsConverter.TEXTURES_FOLDER + File.separator + "GUI/Icons/frame.png"));
                        post(true);
                    }
                });
                onCustomEffect(new EffectBuilder("imageOverlay") {
                    {
                        effectParameter("filename", ConversionUtils.getCanonicalAssetKey(AssetsConverter.TEXTURES_FOLDER + File.separator + "GUI/Icons/selected-room.png"));
                        effectParameter("customKey", "select");
                        post(true);
                        neverStopRendering(true);
                    }
                });
            }
        };
    }

    private ImageBuilder createDoorIcon(final Door door) {
        return new ImageBuilder() {
            {
                filename(ConversionUtils.getCanonicalAssetKey(AssetsConverter.TEXTURES_FOLDER + File.separator + door.getGuiIcon().getName() + ".png"));
                valignCenter();
                marginRight("3px");
                interactOnClick("select(door, " + door.getDoorId() + ")");
                id("door_" + door.getDoorId());
                onHoverEffect(new HoverEffectBuilder("imageOverlay") {
                    {
                        effectParameter("filename", ConversionUtils.getCanonicalAssetKey(AssetsConverter.TEXTURES_FOLDER + File.separator + "GUI/Icons/frame.png"));
                        post(true);
                    }
                });
                onCustomEffect(new EffectBuilder("imageOverlay") {
                    {
                        effectParameter("filename", ConversionUtils.getCanonicalAssetKey(AssetsConverter.TEXTURES_FOLDER + File.separator + "GUI/Icons/selected-door.png"));
                        effectParameter("customKey", "select");
                        post(true);
                        neverStopRendering(true);
                    }
                });
            }
        };
    }

    private ImageBuilder createTrapIcon(final Trap trap) {
        return new ImageBuilder() {
            {
                filename(ConversionUtils.getCanonicalAssetKey(AssetsConverter.TEXTURES_FOLDER + File.separator + trap.getGuiIcon().getName() + ".png"));
                valignCenter();
                marginRight("3px");
                interactOnClick("select(trap, " + trap.getTrapId() + ")");
                id("trap_" + trap.getTrapId());
                onHoverEffect(new HoverEffectBuilder("imageOverlay") {
                    {
                        effectParameter("filename", ConversionUtils.getCanonicalAssetKey(AssetsConverter.TEXTURES_FOLDER + File.separator + "GUI/Icons/frame.png"));
                        post(true);
                    }
                });
                onCustomEffect(new EffectBuilder("imageOverlay") {
                    {
                        effectParameter("filename", ConversionUtils.getCanonicalAssetKey(AssetsConverter.TEXTURES_FOLDER + File.separator + "GUI/Icons/selected-trap.png"));
                        effectParameter("customKey", "select");
                        post(true);
                        neverStopRendering(true);
                    }
                });
            }
        };
    }

    @Override
    public void onEndScreen() {
    }

    private List<Room> getAvailableRoomsToBuild() {
        List<Room> rooms = gameState.getLevelData().getRooms();
        for (Iterator<Room> iter = rooms.iterator(); iter.hasNext();) {
            Room room = iter.next();
            if (!room.getFlags().contains(Room.RoomFlag.BUILDABLE)) {
                iter.remove();
            }
        }
        return rooms;
    }

    private List<KeeperSpell> getAvailableKeeperSpells() {
        List<KeeperSpell> spells = gameState.getLevelData().getKeeperSpells();
        return spells;
    }

    private List<Door> getAvailableDoors() {
        List<Door> doors = gameState.getLevelData().getDoors();
        return doors;
    }

    private List<Trap> getAvailableTraps() {
        List<Trap> traps = new ArrayList<>();
        for (Trap trap : gameState.getLevelData().getTraps()) {
            if (trap.getGuiIcon() == null) {
                continue;
            }
            traps.add(trap);
        }
        return traps;
    }

    public void pauseMenu() {

        // Pause state
        paused = !paused;

        // Pause / unpause
        gameState.setEnabled(!paused);
        for (AbstractPauseAwareState state : appStates) {
            if (state.isPauseable()) {
                state.setEnabled(!paused);
            }
        }

        // Set the menuButton
        Element menuButton = nifty.getCurrentScreen().findElementByName("menuButton");
        if (paused) {
            menuButton.startEffect(EffectEventId.onCustom, null, "select");
        } else {
            menuButton.stopEffect(EffectEventId.onCustom);
        }

        nifty.getCurrentScreen().findElementByName("optionsMenu").setVisible(paused);
        if (paused) {
            pauseMenuNavigate(PauseMenuState.MAIN.name(), null, null, null);
        }
    }

    public void pauseMenuNavigate(String menu, String backMenu, String confirmationTitle, String confirmMethod) {
        Element optionsMenu = nifty.getCurrentScreen().findElementByName("optionsMenu");
        Label optionsMenuTitle = optionsMenu.findNiftyControl("optionsMenuTitle", Label.class);
        Element optionsColumnOne = optionsMenu.findElementByName("optionsColumnOne");
        for (Element element : optionsColumnOne.getElements()) {
            element.markForRemoval();
        }
        Element optionsColumnTwo = optionsMenu.findElementByName("optionsColumnTwo");
        for (Element element : optionsColumnTwo.getElements()) {
            element.markForRemoval();
        }
        Element optionsNavigationColumnOne = optionsMenu.findElementByName("optionsNavigationColumnOne");
        for (Element element : optionsNavigationColumnOne.getElements()) {
            element.markForRemoval();
        }
        Element optionsNavigationColumnTwo = optionsMenu.findElementByName("optionsNavigationColumnTwo");
        for (Element element : optionsNavigationColumnTwo.getElements()) {
            element.markForRemoval();
        }

        // TODO: @ArchDemon: I think we need to do modern (not original) game menu
        List<GameMenu> items = new ArrayList<>();
        switch (PauseMenuState.valueOf(menu)) {
            case MAIN:
                optionsMenuTitle.setText("${menu.94}");

                items.add(new GameMenu("i-objective", "${menu.537}", "pauseMenu()", optionsColumnOne));
                items.add(new GameMenu("i-game", "${menu.97}", "pauseMenu()", optionsColumnOne));
                items.add(new GameMenu("i-load", "${menu.143}", "pauseMenu()", optionsColumnOne));
                items.add(new GameMenu("i-save", "${menu.201}", "pauseMenu()", optionsColumnOne));
                items.add(new GameMenu("i-quit", "${menu.1266}", String.format("pauseMenuNavigate(%s,%s,null,null)",
                        PauseMenuState.QUIT.name(), PauseMenuState.MAIN.name()), optionsColumnTwo));
                items.add(new GameMenu("i-restart", "${menu.1269}", "pauseMenu()", optionsColumnTwo));
                items.add(new GameMenu("i-accept", "${menu.142}", "pauseMenu()", optionsNavigationColumnOne));

                break;

            case QUIT:
                optionsMenuTitle.setText("${menu.1266}");

                items.add(new GameMenu("i-quit", "${menu.12}", String.format("pauseMenuNavigate(%s,%s,${menu.12},quitToMainMenu())",
                        PauseMenuState.CONFIRMATION.name(), PauseMenuState.QUIT.name()), optionsColumnOne));
                items.add(new GameMenu(Utils.isWindows() ? "i-exit_to_windows" : "i-quit", Utils.isWindows() ? "${menu.13}" : "${menu.14}",
                        String.format("pauseMenuNavigate(%s,%s,%s,quitToOS())", PauseMenuState.CONFIRMATION.name(),
                        PauseMenuState.QUIT.name(), (Utils.isWindows() ? "${menu.13}" : "${menu.14}")), optionsColumnOne));
                items.add(new GameMenu("i-accept", "${menu.142}", "pauseMenu()", optionsNavigationColumnOne));
                items.add(new GameMenu("i-back", "${menu.20}", String.format("pauseMenuNavigate(%s,null,null,null)", PauseMenuState.MAIN),
                        optionsNavigationColumnTwo));
                break;

            case CONFIRMATION:
                optionsMenuTitle.setText(confirmationTitle);
                // Column one
                new LabelBuilder("confirmLabel", "${menu.15}") {
                    {
                        style("textNormal");
                    }
                }.build(nifty, screen, optionsColumnOne);

                items.add(new GameMenu("i-accept", "${menu.21}", confirmMethod, optionsColumnOne));
                items.add(new GameMenu("i-accept", "${menu.142}", "pauseMenu()", optionsNavigationColumnOne));
                items.add(new GameMenu("i-back", "${menu.20}", String.format("pauseMenuNavigate(%s,null,null,null)", backMenu),
                        optionsNavigationColumnTwo));

                break;
        }

        // build menu items
        // FIXME id="#image" and "#text" already exist
        for (GameMenu item : items) {
            new IconTextBuilder("menu-" + NiftyIdCreator.generate(), String.format("Textures/GUI/Options/%s.png", item.id), item.title, item.action) {
                {
                }
            }.build(nifty, screen, item.parent);
        }

        // Fix layout
        NiftyUtils.resetContraints(optionsMenuTitle);
        optionsMenu.layoutElements();
    }

    public void quitToMainMenu() {

        // Disable us, detach game and enable start
        stateManager.detach(gameState);
        setEnabled(false);
        stateManager.getState(MainMenuState.class).setEnabled(true);
    }

    public void quitToOS() {
        app.stop();
    }

    public void select(String state, String id) {
        interactionState.setInteractionState(InteractionState.valueOf(state.toUpperCase()), Integer.valueOf(id));
    }

    @NiftyEventSubscriber(id = "tabs-hud")
    public void onTabChange(String id, TabSelectedEvent event) {
        // TODO: maybe change selected item state when tab change
    }

    private void updateSelectedItem(InteractionState state, int id) {

        for (InteractionState interaction : InteractionState.values()) {
            Element content = screen.findElementByName("tab-" + interaction.toString().toLowerCase() + "-content");
            if (content == null) {
                continue;
            }

            for (Element e : content.getElements()) {
                boolean visible = e.isVisible();
                if (!visible) { // FIXME: do not remove this. Nifty hack
                    e.show();
                }
                e.stopEffect(EffectEventId.onCustom);
                if (!visible) { // FIXME: do not remove this. Nifty hack
                    e.hide();
                }
            }
        }

        String itemId = state.toString().toLowerCase() + "_" + id;
        Element item = screen.findElementByName(itemId);
        if (item == null) {
            System.err.println(itemId + " not found"); // FIXME remove this line after debug
            return;
        }
        item.startEffect(EffectEventId.onCustom, null, "select");
    }

    private class GameMenu {

        protected String title;
        protected String action;
        protected String id;
        protected Element parent;

        public GameMenu() {
        }

        public GameMenu(String id, String title, String action, Element parent) {
            this.id = id;
            this.title = title;
            this.action = action;
            this.parent = parent;
        }
    }
}
