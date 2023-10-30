package com.zeydie.launcher.components;

import com.zeydie.launcher.Accounts;
import com.zeydie.launcher.config.AccountsConfig;
import javafx.collections.ObservableList;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import lombok.NonNull;
import pro.gravit.launcher.client.gui.JavaFXApplication;
import pro.gravit.launcher.client.gui.config.RuntimeSettings;
import pro.gravit.launcher.request.Request;

import java.util.List;

public final class AccountScroll {
    private final ScrollPane scrollPane;
    private final GridPane gridPane;

    public AccountScroll(
            @NonNull final ScrollPane scrollPane
    ) {
        this.scrollPane = scrollPane;
        this.gridPane = (GridPane) scrollPane.getContent();

        this.gridPane.setHgap(0);
        this.gridPane.setVgap(0);

        this.updateGrid();
    }

    public void updateGrid() {
        this.gridPane.getChildren().clear();

        @NonNull final AccountsConfig accountsConfig = Accounts.getAccountsConfig();
        @NonNull final List<AccountsConfig.Account> accountList = accountsConfig.getAccounts();

        for (int i = 0; i < accountList.size(); i++) {
            @NonNull final AccountsConfig.Account account = accountList.get(i);

            this.gridPane.add(this.getLoginButton(account), 0, i);
            this.gridPane.add(this.getServerIcon(account), 1, i);
            this.gridPane.add(this.getExitButton(account), 2, i);
        }
    }

    private Button getLoginButton(@NonNull final AccountsConfig.Account account) {
        @NonNull final AccountButton loginButton = new AccountButton(account.getLogin());

        loginButton.setOnAction(event -> {
            this.gridPane.getChildren().forEach(node -> {
                if (node instanceof AccountButton) {
                    @NonNull final ObservableList<String> styles = node.getStyleClass();

                    styles.remove("accountButton-hovered");

                    if (!styles.contains("accountButton"))
                        styles.add("accountButton");
                }
            });
            @NonNull final ObservableList<String> styles = loginButton.getStyleClass();

            styles.remove("accountButton");
            styles.add("accountButton-hovered");

            JavaFXApplication.getInstance().gui.fastLoginScene.setSelectedAccount(account);
        });

        return loginButton;
    }

    private ImageView getServerIcon(@NonNull final AccountsConfig.Account account) {
        @NonNull final ImageView serverImage = new ImageView(
                new Image(
                        //this.getClass().getResourceAsStream("/runtime/images/buttons/exit/exit.png")
                        this.getClass().getResourceAsStream(String.format("/runtime/images/fastlogin/servers/%d.png", account.getServerId()))
                )
        );

        final int width = 30;
        final int height = 30;

        serverImage.setFitHeight(height);
        serverImage.setFitWidth(width);
        serverImage.maxHeight(height);
        serverImage.maxWidth(width);

        serverImage.getStyleClass().add("serverImage");

        return serverImage;
    }

    private Button getExitButton(@NonNull final AccountsConfig.Account account) {
        @NonNull final JavaFXApplication javaFXApplication = JavaFXApplication.getInstance();
        @NonNull final RuntimeSettings runtimeSettings = javaFXApplication.runtimeSettings;

        @NonNull final AccountsConfig accountsConfig = Accounts.getAccountsConfig();
        @NonNull final List<AccountsConfig.Account> accountList = accountsConfig.getAccounts();

        @NonNull final ImageView exitImage = new ImageView(
                new Image(
                        //this.getClass().getResourceAsStream("/runtime/images/buttons/exit/exit.png")
                        this.getClass().getResourceAsStream("/runtime/images/fastlogin/trash.png")
                )
        );

        final int width = 18;
        final int height = 21;

        exitImage.setFitHeight(height);
        exitImage.setFitWidth(width);
        exitImage.maxHeight(height);
        exitImage.maxWidth(width);

        @NonNull final Button exitButton = new Button("", exitImage);

        exitButton.setOnMouseClicked(event -> {
            runtimeSettings.oauthAccessToken = null;
            runtimeSettings.oauthRefreshToken = null;
            runtimeSettings.oauthExpire = 0;

            accountList.removeIf(acc -> acc.getLogin().equals(account.getLogin()) && acc.getServerId() == account.getServerId());
            accountsConfig.save();

            this.updateGrid();
        });

        exitButton.getStyleClass().add("exitButton");

        return exitButton;
    }
}
