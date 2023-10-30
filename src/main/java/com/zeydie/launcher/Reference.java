package com.zeydie.launcher;

import javafx.scene.image.ImageView;
import lombok.NonNull;
import lombok.SneakyThrows;
import pro.gravit.launcher.client.DirBridge;
import pro.gravit.launcher.client.gui.JavaFXApplication;
import pro.gravit.launcher.client.gui.SkinManager;
import pro.gravit.launcher.client.gui.scenes.servermenu.ServerMenuScene;

import java.net.URL;
import java.nio.file.Path;

public final class Reference {
    public static final Path launcherDirectory = DirBridge.dir;

    public static final Path accountConfig = launcherDirectory.resolve("accounts.cfg");

    public static final String skinUrl = "https://minefite.net/ExtraModules/sac/skins/%s.png";

    public static String getLoginOfRefreshToken(@NonNull final String token) {
        return token.split("\\.")[0];
    }

    public static ImageView getAvatar(@NonNull final String login) {
        @NonNull final ImageView imageView = new ImageView();

        imageView.setFitHeight(35);
        imageView.setFitWidth(35);
        imageView.maxHeight(35);
        imageView.maxWidth(35);

        cacheSkin(login);
        ServerMenuScene.putAvatarToImageView(JavaFXApplication.getInstance(), login, imageView);

        return imageView;
    }

    @SneakyThrows
    public static void cacheSkin(@NonNull final String login) {
        @NonNull final SkinManager skinManager = JavaFXApplication.getInstance().skinManager;

        skinManager.addSkin(login, new URL(String.format(skinUrl, login)));
        skinManager.getSkin(login);
    }
}
