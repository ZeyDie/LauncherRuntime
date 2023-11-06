package com.zeydie.launcher;

import javafx.scene.image.ImageView;
import lombok.NonNull;
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
    @NotNull
    public static final Path launcherDirectory = DirBridge.defaultUpdatesDir;

    @NotNull
    public static final Path accountConfig = launcherDirectory.resolve("accounts.cfg");

    @NotNull
    public static final String skinUrl = "https://minefite.net/ExtraModules/sac/skins/%s.png";
    @NotNull
    private static final String serverIdUrl = "https://admin.minefite.net/fs/launcher-serverId.php?uuid=%s";
    @NotNull
    private static final String uuidUrl = "https://admin.minefite.net/fs/launcher-uuid.php?player=%s";
    @NotNull
    private static final String serverNameUrl = "https://admin.minefite.net/fs/launcher-serverName.php?serverId=%d";

    public static @NotNull ImageView getAvatar(@NonNull final String login) {
        @NonNull final ImageView imageView = new ImageView();

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
    public static void cacheSkin(@NonNull final String login) {
        @NonNull final SkinManager skinManager = JavaFXApplication.getInstance().skinManager;

        skinManager.addSkin(login, new URL(String.format(skinUrl, login)));
        skinManager.getSkin(login);
    }

    @SneakyThrows
    public static int getServerIdForUUID(@NonNull final UUID uuid) {
        @NonNull final URL url = new URL(String.format(serverIdUrl, uuid));
        @NonNull final HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setRequestMethod("GET");

        try (
                @NonNull final InputStreamReader inputStreamReader = new InputStreamReader(connection.getInputStream());
                @NonNull final BufferedReader reader = new BufferedReader(inputStreamReader)
        ) {
            return Integer.parseInt(reader.readLine());
        }
    }

    @SneakyThrows
    public static @NotNull UUID getUUIDForLogin(@NonNull final String login) {
        @NonNull final URL url = new URL(String.format(uuidUrl, login));
        @NonNull final HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setRequestMethod("GET");

        try (
                @NonNull final InputStreamReader inputStreamReader = new InputStreamReader(connection.getInputStream());
                @NonNull final BufferedReader reader = new BufferedReader(inputStreamReader)
        ) {
            @Nullable final String uuid = reader.readLine();

            return (uuid == null || uuid.isEmpty()) ? UUID.randomUUID() : UUID.fromString(uuid);
        }
    }

    public static boolean isPlayerServer(final int serverId) {
        return true;
        /*@NonNull final JavaFXApplication javaFXApplication = JavaFXApplication.getInstance();
        @NonNull final RuntimeSettings runtimeSettings = javaFXApplication.runtimeSettings;

        @NonNull final List<AccountsConfig.Account> accountList = Accounts.getAccountsConfig().getAccounts();
        @NonNull final Optional<AccountsConfig.Account> filtered = accountList.stream()
                .filter(account -> account.getOauthAccessToken().equals(runtimeSettings.oauthAccessToken) && account.getOauthRefreshToken().equals(runtimeSettings.oauthRefreshToken))
                .findAny();

        return filtered.filter(account -> account.getServerId() == serverId || account.getServerId() == -1).isPresent();*/
    }

    @SneakyThrows
    public static @NotNull String getServerOfServerId(final int serverId) {
        @NonNull final URL url = new URL(String.format(serverNameUrl, serverId));
        @NonNull final HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setRequestMethod("GET");

        try (
                @NonNull final InputStreamReader inputStreamReader = new InputStreamReader(connection.getInputStream());
                @NonNull final BufferedReader reader = new BufferedReader(inputStreamReader)
        ) {
            return reader.readLine();
        }
    }
}
