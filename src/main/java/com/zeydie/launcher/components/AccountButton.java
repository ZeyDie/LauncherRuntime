package com.zeydie.launcher.components;

import com.zeydie.launcher.Reference;
import javafx.scene.control.Button;
import org.jetbrains.annotations.NotNull;

public final class AccountButton extends Button {
    public AccountButton(@NotNull final String text) {
        super(text, Reference.getAvatar(text));

        this.setMinWidth(330);
        this.setMinHeight(60);

        this.getStyleClass().add("accountButton");
    }
}
