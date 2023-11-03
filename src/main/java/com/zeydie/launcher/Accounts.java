package com.zeydie.launcher;

import com.zeydie.launcher.config.AccountsConfig;
import lombok.Getter;
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

    public static void authed(@NotNull final AuthRequestEvent authRequestEvent) {
        @NotNull final JavaFXApplication javaFXApplication = JavaFXApplication.getInstance();
        @NotNull final RuntimeSettings runtimeSettings = javaFXApplication.runtimeSettings;
        @NotNull final List<AccountsConfig.Account> accountList = accountsConfig.getAccounts();
        @NotNull final Optional<AccountsConfig.Account> filtered = accountList.stream()
                .filter(account -> account.getLogin().equals(runtimeSettings.login))
                .findAny();

        if (filtered.isPresent()) {
            @NotNull final AccountsConfig.Account account = filtered.get();

            account.setOauthAccessToken(runtimeSettings.oauthAccessToken);
            account.setOauthRefreshToken(runtimeSettings.oauthRefreshToken);
            account.setOauthExpire(runtimeSettings.oauthExpire);
            account.setLogin(Reference.getLoginOfRefreshToken(runtimeSettings.oauthRefreshToken));
            account.setServerId(Reference.getServerIdForUUID(authRequestEvent.playerProfile.uuid));
        } else {
            @NotNull final AccountsConfig.Account account = new AccountsConfig.Account();

            account.setOauthAccessToken(runtimeSettings.oauthAccessToken);
            account.setOauthRefreshToken(runtimeSettings.oauthRefreshToken);
            account.setOauthExpire(runtimeSettings.oauthExpire);
            account.setLogin(Reference.getLoginOfRefreshToken(runtimeSettings.oauthRefreshToken));
            account.setServerId(Reference.getServerIdForUUID(authRequestEvent.playerProfile.uuid));

            accountList.add(account);
        }

        accountsConfig.save();
    }
}
