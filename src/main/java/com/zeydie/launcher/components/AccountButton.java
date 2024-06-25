package com.zeydie.launcher.components;

import com.zeydie.launcher.Reference;
import javafx.scene.control.Button;
import lombok.NonNull;

public final class AccountButton extends Button {
    public AccountButton(@NonNull final String text) {
        super(text, Reference.getAvatar(text));

        super.setMnemonicParsing(false);

        super.setMinWidth(330);
        super.setMinHeight(60);

        super.getStyleClass().add("accountButton");
    }
}
