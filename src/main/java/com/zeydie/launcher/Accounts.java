package com.zeydie.launcher;

import com.zeydie.launcher.config.AccountsConfig;
import lombok.Getter;
import lombok.NonNull;
import pro.gravit.launcher.client.gui.JavaFXApplication;
import pro.gravit.launcher.client.gui.config.RuntimeSettings;
import pro.gravit.launcher.events.request.AuthRequestEvent;

import java.util.List;
import java.util.Optional;

public final class Accounts {
    @Getter
    @NonNull
    private static final AccountsConfig accountsConfig = new AccountsConfig();

    public static void load() {
        accountsConfig.load();
    }

    public static void authed(@NonNull final AuthRequestEvent authRequestEvent) {
        @NonNull final JavaFXApplication javaFXApplication = JavaFXApplication.getInstance();
        @NonNull final RuntimeSettings runtimeSettings = javaFXApplication.runtimeSettings;
        @NonNull final List<AccountsConfig.Account> accountList = accountsConfig.getAccounts();
        @NonNull final Optional<AccountsConfig.Account> filtered = accountList.stream()
                .filter(account -> account.getLogin().equals(runtimeSettings.login))
                .findAny();

        if (filtered.isPresent()) {
            @NonNull final AccountsConfig.Account account = filtered.get();

            account.setOauthAccessToken(runtimeSettings.oauthAccessToken);
            account.setOauthRefreshToken(runtimeSettings.oauthRefreshToken);
            account.setOauthExpire(runtimeSettings.oauthExpire);
            account.setLogin(Reference.getLoginOfRefreshToken(runtimeSettings.oauthRefreshToken));
            account.setServerId(Reference.getServerIdForUUID(authRequestEvent.playerProfile.uuid));
        } else {
            @NonNull final AccountsConfig.Account account = new AccountsConfig.Account();

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
