package pro.gravit.launcher.client.gui.impl;

import com.zeydie.launcher.scene.FastLoginScene;
import javafx.scene.Scene;
import pro.gravit.launcher.client.gui.JavaFXApplication;
import pro.gravit.launcher.client.gui.overlays.AbstractOverlay;
import pro.gravit.launcher.client.gui.overlays.ProcessingOverlay;
import pro.gravit.launcher.client.gui.scenes.AbstractScene;
import pro.gravit.launcher.client.gui.scenes.console.ConsoleScene;
import pro.gravit.launcher.client.gui.scenes.debug.DebugScene;
import pro.gravit.launcher.client.gui.scenes.login.LoginScene;
import pro.gravit.launcher.client.gui.scenes.options.OptionsScene;
import pro.gravit.launcher.client.gui.scenes.serverinfo.ServerInfoScene;
import pro.gravit.launcher.client.gui.scenes.servermenu.ServerMenuScene;
import pro.gravit.launcher.client.gui.scenes.settings.SettingsScene;
import pro.gravit.launcher.client.gui.scenes.update.UpdateScene;
import pro.gravit.launcher.client.gui.stage.ConsoleStage;
import pro.gravit.utils.helper.LogHelper;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class GuiObjectsContainer {
    private final JavaFXApplication application;
    private final Set<AbstractOverlay> overlays = new HashSet<>();
    private final Set<AbstractScene> scenes = new HashSet<>();
    public ProcessingOverlay processingOverlay;
    public UpdateScene updateScene;
    public DebugScene debugScene;
    public ServerMenuScene serverMenuScene;
    public ServerInfoScene serverInfoScene;
    public LoginScene loginScene;
    public OptionsScene optionsScene;
    public SettingsScene settingsScene;
    public ConsoleScene consoleScene;
    public ConsoleStage consoleStage;

    //TODO ZeyCodeStart
    public FastLoginScene fastLoginScene;
    //TODO ZeyCodeEnd

    public GuiObjectsContainer(JavaFXApplication application) {
        this.application = application;
    }

    public void init() {
        this.loginScene = (LoginScene)this.registerScene(LoginScene.class);
        this.processingOverlay = (ProcessingOverlay)this.registerOverlay(ProcessingOverlay.class);
        this.serverMenuScene = (ServerMenuScene)this.registerScene(ServerMenuScene.class);
        this.serverInfoScene = (ServerInfoScene)this.registerScene(ServerInfoScene.class);
        this.optionsScene = (OptionsScene)this.registerScene(OptionsScene.class);
        this.settingsScene = (SettingsScene)this.registerScene(SettingsScene.class);
        this.consoleScene = (ConsoleScene)this.registerScene(ConsoleScene.class);
        this.updateScene = (UpdateScene)this.registerScene(UpdateScene.class);
        this.debugScene = (DebugScene)this.registerScene(DebugScene.class);

        //TODO ZeyCodeStart
        this.fastLoginScene = this.registerScene(FastLoginScene.class);
        //TODO ZeyCodeEnd
    }

    public void reload() throws Exception {
        Class<? extends AbstractScene> scene = this.application.getCurrentScene().getClass();
        ContextHelper.runInFxThreadStatic(() -> {
            this.application.getMainStage().stage.setScene((Scene)null);
            this.application.resetDirectory();
            this.overlays.clear();
            this.scenes.clear();
            this.init();
            Iterator<AbstractScene> var2 = this.scenes.iterator();

            while(var2.hasNext()) {
                AbstractScene s = (AbstractScene)var2.next();
                if (s.getClass() == scene) {
                    this.application.getMainStage().setScene(s);
                }
            }

        }).get();
    }

    public AbstractScene getSceneByName(String name) {
        Iterator<AbstractScene> var2 = this.scenes.iterator();

        AbstractScene scene;
        do {
            if (!var2.hasNext()) {
                return null;
            }

            scene = (AbstractScene)var2.next();
        } while(!name.equals(scene.getName()));

        return scene;
    }

    public AbstractOverlay getOverlayByName(String name) {
        Iterator<AbstractOverlay> var2 = this.overlays.iterator();

        AbstractOverlay overlay;
        do {
            if (!var2.hasNext()) {
                return null;
            }

            overlay = (AbstractOverlay)var2.next();
        } while(!name.equals(overlay.getName()));

        return overlay;
    }

    @SuppressWarnings("unchecked")
    public <T extends AbstractOverlay> T registerOverlay(Class<T> clazz) {
        try {
            T instance = (T) MethodHandles.publicLookup().findConstructor(clazz, MethodType.methodType(Void.TYPE, JavaFXApplication.class)).invokeWithArguments(this.application);
            this.overlays.add(instance);
            return instance;
        } catch (Throwable var3) {
            LogHelper.error(var3);
            throw new RuntimeException(var3);
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends AbstractScene> T registerScene(Class<T> clazz) {
        try {
            T instance = (T) MethodHandles.publicLookup().findConstructor(clazz, MethodType.methodType(Void.TYPE, JavaFXApplication.class)).invokeWithArguments(this.application);
            this.scenes.add(instance);
            return instance;
        } catch (Throwable var3) {
            LogHelper.error(var3);
            throw new RuntimeException(var3);
        }
    }
}
