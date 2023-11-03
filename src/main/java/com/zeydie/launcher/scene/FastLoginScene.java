package com.zeydie.launcher.scene;

import com.zeydie.launcher.Accounts;
import com.zeydie.launcher.components.AccountScroll;
import com.zeydie.launcher.config.AccountsConfig;
import javafx.scene.control.Button;
import javafx.scene.layout.Pane;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pro.gravit.launcher.client.gui.JavaFXApplication;
import pro.gravit.launcher.client.gui.config.RuntimeSettings;
import pro.gravit.launcher.client.gui.helper.LookupHelper;
import pro.gravit.launcher.client.gui.impl.ContextHelper;
import pro.gravit.launcher.client.gui.scenes.AbstractScene;
import pro.gravit.launcher.client.gui.scenes.login.LoginScene;

import java.util.Timer;
import java.util.TimerTask;

public final class FastLoginScene extends AbstractScene {
    private final Timer timer = new Timer();
    private AccountScroll accountsScroll;
    private Button addAccountButton;
    private Button authButton;
    private Pane newyearPane;
    @Setter
    @Getter
    @Nullable
    private AccountsConfig.Account selectedAccount;
    private int scene = 1;

    public FastLoginScene(@NotNull final JavaFXApplication application) {
        super("scenes/login/fastlogin.fxml", application);
    }

    @Override
    public String getName() {
        return "fastlogin";
    }

    @Override
    protected void doInit() throws Exception {
        this.accountsScroll = new AccountScroll(LookupHelper.lookup(super.layout, "#authPane", "#accountsScrollPane"));

        this.addAccountButton = LookupHelper.lookup(super.layout, "#authPane", "#addAccountButton");
        this.authButton = LookupHelper.lookup(super.layout, "#authPane", "#authButton");

        this.addAccountButton.setOnAction(event -> switchToLoginning());
        this.authButton.setOnAction(event -> switchAuth());

        this.newyearPane = LookupHelper.lookup(super.layout, "#newyearPane");

        if (Accounts.getAccountsConfig().getAccounts().isEmpty())
            this.switchToLoginning();

        this.timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                newyearPane.getStyleClass().clear();
                newyearPane.getStyleClass().add("tm-" + scene);

                scene += 1;

                if (scene > 3) scene = 1;
            }
        }, 0, 500);
    }

    @Override
    public void reset() {
        this.selectedAccount = null;
        this.accountsScroll.updateGrid();
    }

    @SneakyThrows
    public void switchAuth() {
        if (this.selectedAccount == null) return;

        @NotNull final JavaFXApplication javaFXApplication = JavaFXApplication.getInstance();
        @NotNull final RuntimeSettings runtimeSettings = javaFXApplication.runtimeSettings;

        runtimeSettings.oauthAccessToken = this.selectedAccount.getOauthAccessToken();
        runtimeSettings.oauthRefreshToken = this.selectedAccount.getOauthRefreshToken();
        runtimeSettings.oauthExpire = this.selectedAccount.getOauthExpire();

        ContextHelper.runInFxThreadStatic(() -> {
            @NotNull final LoginScene loginScene = JavaFXApplication.getInstance().gui.loginScene;

            if (loginScene.auth == null)
                super.switchScene(loginScene);
            else {
                super.switchScene(loginScene);
                loginScene.tryOAuthLogin();
            }
        });
    }

    @SneakyThrows
    public void switchToLoginning() {
        @NotNull final JavaFXApplication javaFXApplication = JavaFXApplication.getInstance();
        @NotNull final RuntimeSettings runtimeSettings = javaFXApplication.runtimeSettings;

        runtimeSettings.oauthAccessToken = null;
        runtimeSettings.oauthRefreshToken = null;
        runtimeSettings.oauthExpire = 0;

        ContextHelper.runInFxThreadStatic(() -> {
            @NotNull final LoginScene loginScene = JavaFXApplication.getInstance().gui.loginScene;

            if (loginScene.auth == null)
                super.switchScene(loginScene);
            else {
                super.switchScene(loginScene);
                loginScene.postInit();
            }
        });
    }
}
