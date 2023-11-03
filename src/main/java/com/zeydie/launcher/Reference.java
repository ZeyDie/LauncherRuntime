package com.zeydie.launcher;

import javafx.scene.image.ImageView;
import lombok.Cleanup;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pro.gravit.launcher.client.DirBridge;
import pro.gravit.launcher.client.gui.JavaFXApplication;
import pro.gravit.launcher.client.gui.SkinManager;
import pro.gravit.launcher.client.gui.scenes.servermenu.ServerMenuScene;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Path;
import java.util.UUID;

public final class Reference {
    public static final Path launcherDirectory = DirBridge.defaultUpdatesDir;

    public static final Path accountConfig = launcherDirectory.resolve("accounts.cfg");

    public static final String skinUrl = "https://minefite.net/ExtraModules/sac/skins/%s.png";
    private static final String serverIdUrl = "https://admin.minefite.net/fs/launcher-serverId.php?uuid=%s";
    private static final String uuidUrl = "https://admin.minefite.net/fs/launcher-uuid.php?player=%s";
    private static final String serverNameUrl = "https://admin.minefite.net/fs/launcher-serverName.php?serverId=%d";

    public static String getLoginOfRefreshToken(@NotNull final String token) {
        return token.split("\\.")[0];
    }

    public static ImageView getAvatar(@NotNull final String login) {
        @NotNull final ImageView imageView = new ImageView();

        imageView.setFitHeight(35);
        imageView.setFitWidth(35);
        imageView.maxHeight(35);
        imageView.maxWidth(35);

        cacheSkin(login);

        if (JavaFXApplication.getInstance().skinManager.getSkin(login) != null)
            ServerMenuScene.putAvatarToImageView(JavaFXApplication.getInstance(), login, imageView);
        else {
            cacheSkin("default");
            ServerMenuScene.putAvatarToImageView(JavaFXApplication.getInstance(), "default", imageView);
        }

        return imageView;
    }

    @SneakyThrows
    public static void cacheSkin(@NotNull final String login) {
        @NotNull final SkinManager skinManager = JavaFXApplication.getInstance().skinManager;

        skinManager.addSkin(login, new URL(String.format(skinUrl, login)));
        skinManager.getSkin(login);
    }

    @SneakyThrows
    public static int getServerIdForUUID(@NotNull final UUID uuid) {
        @NotNull final URL url = new URL(String.format(serverIdUrl, uuid));
        @NotNull final HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setRequestMethod("GET");

        @Cleanup @NotNull final InputStreamReader inputStreamReader = new InputStreamReader(connection.getInputStream());
        @Cleanup @NotNull final BufferedReader reader = new BufferedReader(inputStreamReader);

        return Integer.parseInt(reader.readLine());
    }

    @SneakyThrows
    public static @NotNull UUID getUUIDForLogin(@NotNull final String login) {
        @NotNull final URL url = new URL(String.format(uuidUrl, login));
        @NotNull final HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setRequestMethod("GET");

        @Cleanup @NotNull final InputStreamReader inputStreamReader = new InputStreamReader(connection.getInputStream());
        @Cleanup @NotNull final BufferedReader reader = new BufferedReader(inputStreamReader);
        @Nullable final String uuid = reader.readLine();

        return (uuid == null || uuid.isEmpty()) ? UUID.randomUUID() : UUID.fromString(uuid);
    }

    public static boolean isPlayerServer(final int serverId) {
        return true;
        /*@NotNull final JavaFXApplication javaFXApplication = JavaFXApplication.getInstance();
        @NotNull final RuntimeSettings runtimeSettings = javaFXApplication.runtimeSettings;

        @NotNull final List<AccountsConfig.Account> accountList = Accounts.getAccountsConfig().getAccounts();
        @NotNull final Optional<AccountsConfig.Account> filtered = accountList.stream()
                .filter(account -> account.getOauthAccessToken().equals(runtimeSettings.oauthAccessToken) && account.getOauthRefreshToken().equals(runtimeSettings.oauthRefreshToken))
                .findAny();

        return filtered.filter(account -> account.getServerId() == serverId || account.getServerId() == -1).isPresent();*/
    }

    @SneakyThrows
    public static @NotNull String getServerOfServerId(final int serverId) {
        @NotNull final URL url = new URL(String.format(serverNameUrl, serverId));
        @NotNull final HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setRequestMethod("GET");

        @Cleanup @NotNull final InputStreamReader inputStreamReader = new InputStreamReader(connection.getInputStream());
        @Cleanup @NotNull final BufferedReader reader = new BufferedReader(inputStreamReader);

        return reader.readLine();
    }
}
