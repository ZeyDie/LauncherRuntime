package com.zeydie.launcher;

import com.zeydie.launcher.config.AccountsConfig;
import javafx.scene.image.ImageView;
import lombok.Cleanup;
import lombok.NonNull;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pro.gravit.launcher.client.DirBridge;
import pro.gravit.launcher.client.gui.JavaFXApplication;
import pro.gravit.launcher.client.gui.SkinManager;
import pro.gravit.launcher.client.gui.scenes.servermenu.ServerMenuScene;
import pro.gravit.utils.helper.LogHelper;

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
    @NonNull
    private static final String twoFACheckUrl = "https://admin.minefite.net/api/Google2FA.php?login=%s&action=activated";
    @NonNull
    private static final String twoFAValidationUrl = "https://admin.minefite.net/api/Google2FA.php?login=%s&code=%s";

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
        @Cleanup @NonNull final BufferedReader reader = getReaderURL(String.format(serverIdUrl, uuid));

        return Integer.parseInt(reader.readLine());
    }

    @SneakyThrows
    public static @NotNull UUID getUUIDForLogin(@NonNull final String login) {
        @Cleanup @NonNull final BufferedReader reader = getReaderURL(String.format(uuidUrl, login));
        @Nullable final String uuid = reader.readLine();

        return (uuid == null || uuid.isEmpty()) ? UUID.randomUUID() : UUID.fromString(uuid);
    }

    public static boolean isPlayerServer(final int sortIndex) {
        final @Nullable AccountsConfig.Account account = getAuthedAccount();

        if (account == null) return false;

        final int serverId = account.getServerId();

        LogHelper.debug("ServerId %d, account %s", serverId, account);

        return serverId == -1 || serverId == sortIndex;
    }

    public static @Nullable AccountsConfig.Account getAuthedAccount() {
        @Nullable final String oauthRefreshToken = JavaFXApplication.getInstance().runtimeSettings.oauthRefreshToken;

        LogHelper.debug("OauthRefreshToken %s", oauthRefreshToken);

        if (oauthRefreshToken == null) return null;

        return Accounts.getAccountsConfig()
                .getAccounts()
                .stream()
                .filter(account -> account.getLogin().equals(oauthRefreshToken.split("\\.")[0]))
                .findFirst()
                .orElse(null);
    }

    @SneakyThrows
    public static @NotNull String getServerOfServerId(final int serverId) {
        @Cleanup @NonNull final BufferedReader reader = getReaderURL(String.format(serverNameUrl, serverId));

        return reader.readLine();
    }

    @SneakyThrows
    public static boolean has2FA(@NonNull final String login) {
        @Cleanup @NonNull final BufferedReader reader = getReaderURL(String.format(twoFACheckUrl, login));
        @NonNull final String answer = reader.readLine();

        return answer.equalsIgnoreCase("yes");
    }

    @SneakyThrows
    public static boolean isValid2FA(
            @NonNull final String login,
            @NonNull final String code
    ) {
        @Cleanup @NonNull final BufferedReader reader = getReaderURL(String.format(twoFAValidationUrl, login, code));
        @NonNull final String answer = reader.readLine();

        return answer.equalsIgnoreCase("true");
    }

    @SneakyThrows
    private static @NotNull BufferedReader getReaderURL(@NonNull final String site) {
        @NonNull final URL url = new URL(site);
        @NonNull final HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setRequestMethod("GET");

        @NonNull final InputStreamReader inputStreamReader = new InputStreamReader(connection.getInputStream());

        return new BufferedReader(inputStreamReader);
    }
}
