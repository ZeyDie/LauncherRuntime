package com.zeydie.launcher.scene;

import com.zeydie.launcher.components.AccountScroll;
import com.zeydie.launcher.config.AccountsConfig;
import javafx.scene.control.Button;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.SneakyThrows;
import org.jetbrains.annotations.Nullable;
import pro.gravit.launcher.client.gui.JavaFXApplication;
import pro.gravit.launcher.client.gui.config.RuntimeSettings;
import pro.gravit.launcher.client.gui.helper.LookupHelper;
import pro.gravit.launcher.client.gui.scenes.AbstractScene;

public final class FastLoginScene extends AbstractScene {
    private AccountScroll accountsScroll;
    private Button addAccountButton;
    private Button authButton;
    @Setter
    @Getter
    @Nullable
    private AccountsConfig.Account selectedAccount;

    public FastLoginScene(@NonNull final JavaFXApplication application) {
        super("scenes/login/fastlogin.fxml", application);
    }

    @Override
    public String getName() {
        return "fastlogin";
    }

    @Override
    protected void doInit() throws Exception {
        this.accountsScroll = new AccountScroll(LookupHelper.lookup(super.layout, "#authPane", "#accountsScrollPane"));

        if (this.accountsScroll.getGridPane().getChildren().isEmpty()) {
            this.switchToLoginning();
            return;
        }

        this.addAccountButton = LookupHelper.lookup(super.layout, "#authPane", "#addAccountButton");
        this.authButton = LookupHelper.lookup(super.layout, "#authPane", "#authButton");

        this.addAccountButton.setOnAction(event -> switchToLoginning());
        this.authButton.setOnAction(event -> switchAuth());
    }

    @Override
    public void reset() {
        this.selectedAccount = null;
        this.accountsScroll.updateGrid();
    }

    @SneakyThrows
    public void switchAuth() {
        if (this.selectedAccount == null) return;

        @NonNull final JavaFXApplication javaFXApplication = JavaFXApplication.getInstance();
        @NonNull final RuntimeSettings runtimeSettings = javaFXApplication.runtimeSettings;

        runtimeSettings.oauthAccessToken = this.selectedAccount.getOauthAccessToken();
        runtimeSettings.oauthRefreshToken = this.selectedAccount.getOauthRefreshToken();
        runtimeSettings.oauthExpire = this.selectedAccount.getOauthExpire();

        super.switchScene(JavaFXApplication.getInstance().gui.loginScene);
        super.currentStage.stage.centerOnScreen();

        JavaFXApplication.getInstance().gui.loginScene.tryOAuthLogin();
    }

    @SneakyThrows
    public void switchToLoginning() {
        @NonNull final JavaFXApplication javaFXApplication = JavaFXApplication.getInstance();
        @NonNull final RuntimeSettings runtimeSettings = javaFXApplication.runtimeSettings;

        runtimeSettings.lastAuth = null;
        runtimeSettings.oauthAccessToken = null;
        runtimeSettings.oauthRefreshToken = null;
        runtimeSettings.oauthExpire = 0;

        super.switchScene(JavaFXApplication.getInstance().gui.loginScene);
        super.currentStage.stage.centerOnScreen();
    }
}
