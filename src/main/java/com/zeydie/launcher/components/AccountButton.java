package com.zeydie.launcher.components;

import com.zeydie.launcher.Reference;
import javafx.scene.control.Button;
import lombok.NonNull;

public final class AccountButton extends Button {
    public AccountButton(@NonNull final String text) {
        super(text, Reference.getAvatar(text));

        this.setMinWidth(330);
        this.setMinHeight(60);

        this.getStyleClass().add("accountButton");
    }
}
