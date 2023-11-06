package com.zeydie.launcher;

import com.zeydie.launcher.config.AccountsConfig;
import lombok.Getter;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;
import pro.gravit.launcher.client.gui.JavaFXApplication;
import pro.gravit.launcher.client.gui.config.RuntimeSettings;
import pro.gravit.launcher.events.request.AuthRequestEvent;

import java.util.List;
import java.util.Optional;

public final class Accounts {
    @Getter
    @NotNull
    private static final AccountsConfig accountsConfig = new AccountsConfig();

    public static void load() {
        accountsConfig.load();
    }

    public static void authed(@NonNull final AuthRequestEvent authRequestEvent) {
        @NonNull final String login = authRequestEvent.playerProfile.username;

        @NonNull final JavaFXApplication javaFXApplication = JavaFXApplication.getInstance();
        @NonNull final RuntimeSettings runtimeSettings = javaFXApplication.runtimeSettings;
        @NonNull final List<AccountsConfig.Account> accountList = accountsConfig.getAccounts();
        @NonNull final Optional<AccountsConfig.Account> filtered = accountList.stream()
                .filter(account -> account.getLogin().equals(login))
                .findAny();

        @NonNull final String accessToken = runtimeSettings.oauthAccessToken;
        @NonNull final String refreshToken = runtimeSettings.oauthRefreshToken;
        final long expire = runtimeSettings.oauthExpire;
        final int serverId = Reference.getServerIdForUUID(authRequestEvent.playerProfile.uuid);

        if (filtered.isPresent()) {
            @NonNull final AccountsConfig.Account account = filtered.get();

            account.setLogin(login);
            account.setOauthAccessToken(accessToken);
            account.setOauthRefreshToken(refreshToken);
            account.setOauthExpire(expire);
            account.setServerId(serverId);
        } else {
            @NonNull final AccountsConfig.Account account = new AccountsConfig.Account();

            account.setLogin(login);
            account.setOauthAccessToken(accessToken);
            account.setOauthRefreshToken(refreshToken);
            account.setOauthExpire(expire);
            account.setServerId(serverId);

            accountList.add(account);
        }

        accountsConfig.save();
    }
}
