package com.zeydie.launcher.components;

import com.zeydie.launcher.Accounts;
import com.zeydie.launcher.config.AccountsConfig;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import lombok.NonNull;
import pro.gravit.launcher.client.gui.JavaFXApplication;

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

    private void updateGrid() {
        this.gridPane.getChildren().clear();

        @NonNull final AccountsConfig accountsConfig = Accounts.getAccountsConfig();
        @NonNull final List<AccountsConfig.Account> accountList = accountsConfig.getAccounts();

        for (int i = 0; i < accountList.size(); i++) {
            @NonNull final Pane accountPane = new Pane();

            accountPane.getStyleClass().add("accountPane");

            @NonNull final AccountsConfig.Account account = accountList.get(i);
            @NonNull final AccountButton loginButton = new AccountButton(account.getLogin());

            loginButton.setOnAction(event -> {
                this.gridPane.getChildren().forEach(node -> {
                    if (node instanceof AccountButton) {
                        node.getStyleClass().remove("accountButton-hovered");

                        if (!node.getStyleClass().contains("accountButton"))
                            node.getStyleClass().add("accountButton");
                    }
                });
                loginButton.getStyleClass().remove("accountButton");
                loginButton.getStyleClass().add("accountButton-hovered");

                JavaFXApplication.getInstance().gui.fastLoginScene.setSelectedAccount(account);
            });

            @NonNull final ImageView exitImage = new ImageView(
                    new Image(
                            //this.getClass().getResourceAsStream("/runtime/images/buttons/exit/exit.png")
                            this.getClass().getResourceAsStream("/runtime/images/fastlogin/trash.png")
                    )
            );

            exitImage.setFitHeight(21);
            exitImage.setFitWidth(18);
            exitImage.maxHeight(21);
            exitImage.maxWidth(18);

            @NonNull final Button exitButton = new Button("", exitImage);

            exitButton.setOnMouseClicked(event -> {
                accountList.removeIf(acc -> acc.getLogin().equals(account.getLogin()));
                accountsConfig.save();

                this.updateGrid();
            });

            exitButton.getStyleClass().set(0, "exitButton");

            //accountPane.getChildren().add(loginButton);
            //accountPane.getChildren().add(exitButton);

            this.gridPane.add(loginButton, 0, i);
            this.gridPane.add(exitButton, 1, i);
        }
    }
}
