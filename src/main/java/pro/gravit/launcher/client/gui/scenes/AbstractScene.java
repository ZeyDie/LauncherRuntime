package pro.gravit.launcher.client.gui.scenes;

import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import pro.gravit.launcher.Launcher;
import pro.gravit.launcher.LauncherConfig;
import pro.gravit.launcher.client.gui.JavaFXApplication;
import pro.gravit.launcher.client.gui.config.RuntimeSettings;
import pro.gravit.launcher.client.gui.config.RuntimeSettings.LAUNCHER_LOCALE;
import pro.gravit.launcher.client.gui.helper.LookupHelper;
import pro.gravit.launcher.client.gui.impl.AbstractStage;
import pro.gravit.launcher.client.gui.impl.AbstractVisualComponent;
import pro.gravit.launcher.client.gui.impl.ContextHelper;
import pro.gravit.launcher.client.gui.overlays.AbstractOverlay;
import pro.gravit.launcher.request.Request;
import pro.gravit.launcher.request.WebSocketEvent;
import pro.gravit.launcher.request.auth.ExitRequest;
import pro.gravit.utils.helper.LogHelper;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public abstract class AbstractScene extends AbstractVisualComponent {
    protected final LauncherConfig launcherConfig = Launcher.getConfig();
    protected Scene scene;
    protected Pane header;
    protected Pane disablePane;
    private volatile Node currentOverlayNode;
    private volatile AbstractOverlay currentOverlay;
    private AtomicInteger enabled = new AtomicInteger(0);
    private volatile boolean hideTransformStarted = false;

    protected AbstractScene(String fxmlPath, JavaFXApplication application) {
        super(fxmlPath, application);
    }

    protected AbstractStage getCurrentStage() {
        return this.currentStage;
    }

    @Override
    public void init() throws Exception {
        if (this.scene == null) {
            this.scene = new Scene(this.getFxmlRoot());
            this.scene.setFill(Color.TRANSPARENT);
        }

        this.layout = (Pane) LookupHelper.lookupIfPossible(this.scene.getRoot(), new String[]{"#layout"}).orElse(this.scene.getRoot());
        Rectangle rect = new Rectangle(this.layout.getPrefWidth(), this.layout.getPrefHeight());
        rect.setArcHeight(15.0);
        rect.setArcWidth(15.0);
        this.layout.setClip(rect);
        this.header = (Pane) LookupHelper.lookupIfPossible(this.layout, new String[]{"#header"}).orElse(null);
        this.sceneBaseInit();
        super.init();
    }

    @Override
    protected abstract void doInit() throws Exception;

    public void showOverlay(AbstractOverlay overlay, EventHandler<ActionEvent> onFinished) throws Exception {
        this.currentOverlay = overlay;
        this.currentOverlay.show(this.currentStage);
        this.showOverlay(overlay.getLayout(), onFinished);
    }

    private void showOverlay(Pane newOverlay, EventHandler<ActionEvent> onFinished) {
        if (newOverlay == null) {
            throw new NullPointerException();
        } else if (this.currentOverlayNode != null) {
            this.swapOverlay(newOverlay, onFinished);
        } else {
            this.currentOverlayNode = newOverlay;
            Pane root = (Pane) this.scene.getRoot();
            this.disable();
            root.getChildren().add(newOverlay);
            newOverlay.setLayoutX((root.getPrefWidth() - newOverlay.getPrefWidth()) / 2.0);
            newOverlay.setLayoutY((root.getPrefHeight() - newOverlay.getPrefHeight()) / 2.0);
            newOverlay.toFront();
            newOverlay.requestFocus();
            fade(newOverlay, 0.0, 0.0, 1.0, onFinished);
        }
    }

    public void hideOverlay(double delay, EventHandler<ActionEvent> onFinished) {
        if (this.currentOverlayNode != null) {
            if (this.currentOverlay != null) {
                if (this.hideTransformStarted && onFinished != null) {
                    this.contextHelper.runInFxThread(() -> {
                        onFinished.handle(null);
                    });
                }

                this.hideTransformStarted = true;
                Pane root = (Pane) this.scene.getRoot();
                fade(this.currentOverlayNode, delay, 1.0, 0.0, (e) -> {
                    root.getChildren().remove(this.currentOverlayNode);
                    root.requestFocus();
                    this.enable();
                    this.currentOverlayNode = null;
                    if (this.currentOverlay != null) {
                        this.currentOverlay.reset();
                    }

                    this.currentOverlay = null;
                    if (onFinished != null) {
                        onFinished.handle(e);
                    }

                    this.hideTransformStarted = false;
                });
            }
        }
    }

    private void swapOverlay(Pane newOverlay, EventHandler<ActionEvent> onFinished) {
        if (this.currentOverlayNode == null) {
            throw new IllegalStateException("Try swap null overlay");
        } else {
            if (this.hideTransformStarted && onFinished != null) {
                this.contextHelper.runInFxThread(() -> {
                    onFinished.handle(null);
                });
            }

            this.hideTransformStarted = true;
            Pane root = (Pane) this.scene.getRoot();
            fade(this.currentOverlayNode, 0.0, 1.0, 0.0, (e) -> {
                if (this.currentOverlayNode != newOverlay) {
                    ObservableList<Node> child = root.getChildren();
                    int index = child.indexOf(this.currentOverlayNode);
                    child.set(index, newOverlay);
                }

                newOverlay.setLayoutX((root.getPrefWidth() - newOverlay.getPrefWidth()) / 2.0);
                newOverlay.setLayoutY((root.getPrefHeight() - newOverlay.getPrefHeight()) / 2.0);
                this.currentOverlayNode = newOverlay;
                newOverlay.toFront();
                newOverlay.requestFocus();
                fade(newOverlay, 0.0, 0.0, 1.0, (ev) -> {
                    this.hideTransformStarted = false;
                    if (onFinished != null) {
                        onFinished.handle(ev);
                    }

                });
            });
        }
    }

    protected final <T extends WebSocketEvent> void processRequest(String message, Request<T> request, Consumer<T> onSuccess, EventHandler<ActionEvent> onError) {
        this.application.gui.processingOverlay.processRequest(this, message, request, onSuccess, onError);
    }

    protected final <T extends WebSocketEvent> void processRequest(String message, Request<T> request, Consumer<T> onSuccess, Consumer<Throwable> onException, EventHandler<ActionEvent> onError) {
        this.application.gui.processingOverlay.processRequest(this, message, request, onSuccess, onException, onError);
    }

    public AbstractOverlay getCurrentOverlay() {
        return this.currentOverlay;
    }

    @Override
    public void disable() {
        LogHelper.debug("Scene %s disabled (%d)", new Object[]{this.getName(), this.enabled.incrementAndGet()});
        if (this.enabled.get() == 1) {
            Pane root = (Pane) this.scene.getRoot();
            if (this.layout == root) {
                throw new IllegalStateException("AbstractScene.disable() failed: layout == root");
            } else {
                this.layout.setEffect(new GaussianBlur(20.0));
                if (this.disablePane == null) {
                    this.disablePane = new Pane();
                    this.disablePane.setPrefHeight(root.getPrefHeight());
                    this.disablePane.setPrefWidth(root.getPrefWidth());
                    int index = root.getChildren().indexOf(this.layout);
                    root.getChildren().add(index + 1, this.disablePane);
                }

                this.disablePane.setVisible(true);
            }
        }
    }

    @Override
    public void enable() {
        LogHelper.debug("Scene %s enabled (%d)", new Object[]{this.getName(), this.enabled.decrementAndGet()});
        if (this.enabled.get() == 0) {
            this.layout.setEffect(new GaussianBlur(0.0));
            this.disablePane.setVisible(false);
        }
    }

    public boolean isEnabled() {
        return this.enabled.get() == 0;
    }

    @Override
    public abstract void reset();

    public Scene getScene() {
        return this.scene;
    }

    private void sceneBaseInit() {
        if (this.header == null) {
            LogHelper.warning("Scene %s header button(#close, #hide) deprecated", new Object[]{this.getName()});
            LookupHelper.lookupIfPossible(this.layout, new String[]{"#close"}).ifPresent((b) -> {
                ((Button) b).setOnAction((e) -> {
                    this.currentStage.close();
                });
            });
            LookupHelper.lookupIfPossible(this.layout, new String[]{"#hide"}).ifPresent((b) -> {
                ((Button) b).setOnAction((e) -> {
                    this.currentStage.hide();
                });
            });
        } else {
            LookupHelper.lookupIfPossible(this.header, new String[]{"#controls", "#exit"}).ifPresent((b) -> {
                ((Button) b).setOnAction((e) -> {
                    this.currentStage.close();
                });
            });
            LookupHelper.lookupIfPossible(this.header, new String[]{"#controls", "#minimize"}).ifPresent((b) -> {
                ((Button) b).setOnAction((e) -> {
                    this.currentStage.hide();
                });
            });
            LookupHelper.lookupIfPossible(this.header, new String[]{"#controls", "#lang"}).ifPresent((b) -> {
                ((Button) b).setContextMenu(this.makeLangContextMenu());
                b.setOnMousePressed((e) -> {
                    if (e.isPrimaryButtonDown()) {
                        ((Button) b).getContextMenu().show(b, e.getScreenX(), e.getScreenY());
                    }
                });
            });
            LookupHelper.lookupIfPossible(this.header, new String[]{"#controls", "#deauth"}).ifPresent((b) -> {
                ((Button) b).setOnAction((e) -> {
                    this.application.messageManager.showApplyDialog(this.application.getTranslation("runtime.scenes.settings.exitDialog.header"), this.application.getTranslation("runtime.scenes.settings.exitDialog.description"), this::userExit, () -> {
                    }, true);
                });
            });
        }

        this.currentStage.enableMouseDrag(this.layout);
    }

    private ContextMenu makeLangContextMenu() {
        ContextMenu contextMenu = new ContextMenu();
        contextMenu.getStyleClass().add("langChoice");
        RuntimeSettings.LAUNCHER_LOCALE[] var2 = LAUNCHER_LOCALE.values();
        int var3 = var2.length;

        for (int var4 = 0; var4 < var3; ++var4) {
            RuntimeSettings.LAUNCHER_LOCALE locale = var2[var4];
            MenuItem item = new MenuItem(locale.displayName);
            item.setOnAction((e) -> {
                try {
                    this.application.updateLocaleResources(locale.name);
                    this.application.runtimeSettings.locale = locale;
                    this.application.gui.reload();
                } catch (Exception exception) {
                    this.errorHandle(exception);
                }

            });
            contextMenu.getItems().add(item);
        }

        return contextMenu;
    }

    protected void userExit() {
        this.processRequest(this.application.getTranslation("runtime.scenes.settings.exitDialog.processing"), new ExitRequest(), (event) -> {
            ContextHelper.runInFxThreadStatic(() -> {
                this.hideOverlay(0.0, null);

                //TODO ZeyCodeStart
                this.application.gui.fastLoginScene.reset();
                //TODO ZeyCodeEnd
                //TODO ZeyCodeClear
                /*this.application.gui.loginScene.clearPassword();
                this.application.gui.loginScene.reset();*/

                try {
                    this.application.saveSettings();
                    this.application.stateService.exit();
                    //TODO ZeyCodeReplace loginScene on fastLoginScene
                    this.switchScene(this.application.gui.fastLoginScene);
                } catch (Exception var2) {
                    this.errorHandle(var2);
                }

            });
        }, (event) -> {
        });
    }

    //TODO ZeyCodeModified to public from protected
    public void switchScene(AbstractScene scene) throws Exception {
        this.currentStage.setScene(scene);

        //TODO ZeyCodeStart
        this.currentStage.stage.centerOnScreen();
        //TODO ZeyCodeEnd
    }

    public Node getHeader() {
        return this.header;
    }

    public static void runLater(double delay, EventHandler<ActionEvent> callback) {
        fade((Node) null, delay, 0.0, 1.0, callback);
    }
}
