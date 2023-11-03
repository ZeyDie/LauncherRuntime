package pro.gravit.launcher.client.gui;

import com.zeydie.launcher.Accounts;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import pro.gravit.launcher.Launcher;
import pro.gravit.launcher.LauncherConfig;
import pro.gravit.launcher.LauncherEngine;
import pro.gravit.launcher.NewLauncherSettings;
import pro.gravit.launcher.api.DialogService;
import pro.gravit.launcher.client.*;
import pro.gravit.launcher.client.events.ClientExitPhase;
import pro.gravit.launcher.client.events.ClientGuiPhase;
import pro.gravit.launcher.client.gui.commands.RuntimeCommand;
import pro.gravit.launcher.client.gui.commands.VersionCommand;
import pro.gravit.launcher.client.gui.config.GuiModuleConfig;
import pro.gravit.launcher.client.gui.config.RuntimeSettings;
import pro.gravit.launcher.client.gui.config.RuntimeSettings.ProfileSettings;
import pro.gravit.launcher.client.gui.config.StdSettingsManager;
import pro.gravit.launcher.client.gui.helper.EnFSHelper;
import pro.gravit.launcher.client.gui.impl.GuiEventHandler;
import pro.gravit.launcher.client.gui.impl.GuiObjectsContainer;
import pro.gravit.launcher.client.gui.impl.MessageManager;
import pro.gravit.launcher.client.gui.scenes.AbstractScene;
import pro.gravit.launcher.client.gui.service.*;
import pro.gravit.launcher.client.gui.stage.PrimaryStage;
import pro.gravit.launcher.client.gui.utils.FXMLFactory;
import pro.gravit.launcher.client.gui.utils.TriggerManager;
import pro.gravit.launcher.debug.DebugMain;
import pro.gravit.launcher.managers.ConsoleManager;
import pro.gravit.launcher.managers.SettingsManager;
import pro.gravit.launcher.profiles.ClientProfile;
import pro.gravit.launcher.request.Request;
import pro.gravit.launcher.request.RequestService;
import pro.gravit.launcher.request.auth.AuthRequest;
import pro.gravit.utils.command.BaseCommandCategory;
import pro.gravit.utils.command.CommandCategory;
import pro.gravit.utils.command.CommandHandler;
import pro.gravit.utils.helper.IOHelper;
import pro.gravit.utils.helper.LogHelper;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class JavaFXApplication extends Application {
    private static final AtomicReference<JavaFXApplication> INSTANCE = new AtomicReference<>();
    private static final AtomicBoolean IS_NOGUI = new AtomicBoolean(false);
    private static Path runtimeDirectory = null;
    private static Path enfsDirectory;
    public final LauncherConfig config = Launcher.getConfig();
    public final ExecutorService workers = Executors.newWorkStealingPool(4);
    public RuntimeSettings runtimeSettings;
    public RequestService service;
    public GuiObjectsContainer gui;
    public StateService stateService;
    public GuiModuleConfig guiModuleConfig;
    public MessageManager messageManager;
    public RuntimeSecurityService securityService;
    public SkinManager skinManager;
    public FXMLFactory fxmlFactory;
    public JavaService javaService;
    public PingService pingService;
    public OfflineService offlineService;
    public TriggerManager triggerManager;
    private SettingsManager settingsManager;
    private PrimaryStage mainStage;
    private boolean debugMode;
    private ResourceBundle resources;
    private CommandCategory runtimeCategory;

    public JavaFXApplication() {
        INSTANCE.set(this);
    }

    public static JavaFXApplication getInstance() {
        return INSTANCE.get();
    }

    public static URL getResourceURL(String name) throws IOException {
        if (runtimeDirectory != null) {
            Path target = runtimeDirectory.resolve(name);
            if (!Files.exists(target)) {
                throw new FileNotFoundException(String.format("File runtime/%s not found", name));
            } else {
                return target.toUri().toURL();
            }
        } else {
            return enfsDirectory != null ? getResourceEnFs(name) : Launcher.getResourceURL(name);
        }
    }

    private static URL getResourceEnFs(String name) throws IOException {
        return EnFSHelper.getURL(enfsDirectory.resolve(name).toString().replaceAll("\\\\", "/"));
    }

    public static void setNoGUIMode(boolean isNogui) {
        IS_NOGUI.set(isNogui);
    }

    public AbstractScene getCurrentScene() {
        return (AbstractScene) this.mainStage.getVisualComponent();
    }

    public PrimaryStage getMainStage() {
        return this.mainStage;
    }

    @Override
    public void init() throws Exception {
        System.setProperty("prism.lcdtext", "false");
        this.guiModuleConfig = new GuiModuleConfig();
        this.settingsManager = new StdSettingsManager();
        UserSettings.providers.register("stdruntime", RuntimeSettings.class);
        this.settingsManager.loadConfig();
        NewLauncherSettings settings = this.settingsManager.getConfig();
        if (settings.userSettings.get("stdruntime") == null) {
            settings.userSettings.put("stdruntime", RuntimeSettings.getDefault(this.guiModuleConfig));
        }

        try {
            this.settingsManager.loadHDirStore();
        } catch (Exception var3) {
            LogHelper.error(var3);
        }

        this.runtimeSettings = (RuntimeSettings) settings.userSettings.get("stdruntime");
        this.runtimeSettings.apply();
        DirBridge.dirUpdates = this.runtimeSettings.updatesDir == null ? DirBridge.defaultUpdatesDir : this.runtimeSettings.updatesDir;
        this.service = Request.getRequestService();
        this.service.registerEventHandler(new GuiEventHandler(this));
        this.stateService = new StateService();
        this.messageManager = new MessageManager(this);
        this.securityService = new RuntimeSecurityService(this);
        this.skinManager = new SkinManager(this);
        this.triggerManager = new TriggerManager(this);
        this.javaService = new JavaService(this);
        this.offlineService = new OfflineService(this);
        this.pingService = new PingService();
        this.registerCommands();

        //TODO ZeyCodeStart
        Accounts.load();
        //TODO ZeyCodeEnd
    }

    @Override
    public void start(Stage stage) {
        try {
            Class.forName("pro.gravit.launcher.debug.DebugMain", false, JavaFXApplication.class.getClassLoader());
            if (DebugMain.IS_DEBUG.get()) {
                runtimeDirectory = IOHelper.WORKING_DIR.resolve("runtime");
                this.debugMode = true;
            }
        } catch (Throwable var6) {
            if (!(var6 instanceof ClassNotFoundException)) {
                LogHelper.error(var6);
            }
        }

        try {
            Class.forName("pro.gravit.utils.enfs.EnFS", false, JavaFXApplication.class.getClassLoader());
            if (runtimeDirectory == null) {
                EnFSHelper.initEnFS();
                enfsDirectory = EnFSHelper.initEnFSDirectory(this.config, this.runtimeSettings.theme);
            }

            if (!EnFSHelper.checkEnFSUrl()) {
                JavaRuntimeModule.noEnFSAlert();
            }
        } catch (Throwable var5) {
            if (!(var5 instanceof ClassNotFoundException)) {
                LogHelper.error(var5);
            }
        }

        if (this.runtimeSettings.locale == null) {
            this.runtimeSettings.locale = RuntimeSettings.DEFAULT_LOCALE;
        }

        try {
            this.updateLocaleResources(this.runtimeSettings.locale.name);
        } catch (Throwable var4) {
            JavaRuntimeModule.noLocaleAlert(this.runtimeSettings.locale.name);
            if (!(var4 instanceof FileNotFoundException)) {
                LogHelper.error(var4);
            }

            Platform.exit();
        }

        RuntimeDialogService dialogService = new RuntimeDialogService(this.messageManager);
        DialogService.setDialogImpl(dialogService);
        DialogService.setNotificationImpl(dialogService);
        if (this.offlineService.isOfflineMode() && !this.offlineService.isAvailableOfflineMode() && !this.debugMode) {
            this.messageManager.showDialog(this.getTranslation("runtime.offline.dialog.header"), this.getTranslation("runtime.offline.dialog.text"), Platform::exit, Platform::exit, false);
        } else {
            try {
                this.mainStage = new PrimaryStage(stage, String.format("%s Launcher", this.config.projectName));
                this.gui = new GuiObjectsContainer(this);
                this.gui.init();
                if (!IS_NOGUI.get()) {
                    this.mainStage.setScene(
                            //TODO ZeyCodeStart
                            Accounts.getAccountsConfig().getAccounts().isEmpty() ? this.gui.loginScene : this.gui.fastLoginScene
                            //TODO ZeyCodeEnd
                            //TODO ZeyCodeClear
                            //this.gui.loginScene
                    );
                    this.mainStage.show();
                    if (this.offlineService.isOfflineMode()) {
                        this.messageManager.createNotification(this.getTranslation("runtime.offline.notification.header"), this.getTranslation("runtime.offline.notification.text"));
                    }
                } else {
                    Platform.setImplicitExit(false);
                }

                LauncherEngine.modulesManager.invokeEvent(new ClientGuiPhase(StdJavaRuntimeProvider.getInstance()));
                AuthRequest.registerProviders();
            } catch (Throwable var3) {
                LogHelper.error(var3);
                JavaRuntimeModule.errorHandleAlert(var3);
                Platform.exit();
            }

        }
    }

    public void updateLocaleResources(String locale) throws IOException {
        InputStream input = this.getResource(String.format("runtime_%s.properties", locale));

        try {
            this.resources = new PropertyResourceBundle(input);
        } catch (Throwable var6) {
            if (input != null) {
                try {
                    input.close();
                } catch (Throwable var5) {
                    var6.addSuppressed(var5);
                }
            }

            throw var6;
        }

        if (input != null) {
            input.close();
        }

        this.fxmlFactory = new FXMLFactory(this.resources, this.workers);
    }

    public void resetDirectory() throws IOException {
        if (enfsDirectory != null) {
            enfsDirectory = EnFSHelper.initEnFSDirectory(this.config, this.runtimeSettings.theme);
        }

    }

    private void registerCommands() {
        this.runtimeCategory = new BaseCommandCategory();
        this.runtimeCategory.registerCommand("version", new VersionCommand());
        if (ConsoleManager.isConsoleUnlock) {
            this.registerPrivateCommands();
        }

        ConsoleManager.handler.registerCategory(new CommandHandler.Category(this.runtimeCategory, "runtime"));
    }

    public void registerPrivateCommands() {
        if (this.runtimeCategory != null) {
            this.runtimeCategory.registerCommand("runtime", new RuntimeCommand(this));
        }
    }

    @Override
    public void stop() {
        LogHelper.debug("JavaFX method stop invoked");
        LauncherEngine.modulesManager.invokeEvent(new ClientExitPhase(0));
    }

    public boolean isDebugMode() {
        return this.debugMode;
    }

    private InputStream getResource(String name) throws IOException {
        return IOHelper.newInput(getResourceURL(name));
    }

    public URL tryResource(String name) {
        try {
            return getResourceURL(name);
        } catch (IOException var3) {
            return null;
        }
    }

    public RuntimeSettings.ProfileSettings getProfileSettings() {
        return this.getProfileSettings(this.stateService.getProfile());
    }

    public RuntimeSettings.ProfileSettings getProfileSettings(ClientProfile profile) {
        if (profile == null) {
            throw new NullPointerException("ClientProfile not selected");
        } else {
            UUID uuid = profile.getUUID();
            RuntimeSettings.ProfileSettings settings = this.runtimeSettings.profileSettings.get(uuid);
            if (settings == null) {
                settings = ProfileSettings.getDefault(this.javaService, profile);
                this.runtimeSettings.profileSettings.put(uuid, settings);
            }

            return settings;
        }
    }

    public void setMainScene(AbstractScene scene) throws Exception {
        this.mainStage.setScene(scene);
    }

    public Stage newStage() {
        return this.newStage(StageStyle.TRANSPARENT);
    }

    public Stage newStage(StageStyle style) {
        Stage ret = new Stage();
        ret.initStyle(style);
        ret.setResizable(false);
        return ret;
    }

    public final String getTranslation(String name) {
        return this.getTranslation(name, String.format("'%s'", name));
    }

    public final String getTranslation(String key, String defaultValue) {
        try {
            return this.resources.getString(key);
        } catch (Throwable var4) {
            return defaultValue;
        }
    }

    public boolean openURL(String url) {
        try {
            this.getHostServices().showDocument(url);
            return true;
        } catch (Throwable var3) {
            LogHelper.error(var3);
            return false;
        }
    }

    public void saveSettings() throws IOException {
        this.settingsManager.saveConfig();
        this.settingsManager.saveHDirStore();
        if (this.gui != null && this.gui.optionsScene != null && this.stateService != null && this.stateService.getProfiles() != null) {
            try {
                this.gui.optionsScene.saveAll();
            } catch (Throwable var2) {
                LogHelper.error(var2);
            }
        }

    }
}
