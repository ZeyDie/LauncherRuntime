package com.zeydie.launcher.config;

import com.zeydie.launcher.Reference;
import com.zeydie.sgson.SGsonFile;
import lombok.Data;
import lombok.NonNull;
import pro.gravit.launcher.LauncherNetworkAPI;
import pro.gravit.launcher.client.gui.JavaFXApplication;
import pro.gravit.launcher.client.gui.config.RuntimeSettings;

import java.util.ArrayList;
import java.util.List;

@Data
public final class AccountsConfig {
    @LauncherNetworkAPI
    private List<Account> accounts = new ArrayList<>();

    public void load() {
        this.accounts = new SGsonFile(Reference.accountConfig).fromJsonToObject(this).getAccounts();

        @NonNull final RuntimeSettings runtimeSettings = JavaFXApplication.getInstance().runtimeSettings;

        if (runtimeSettings.oauthRefreshToken == null) return;

        @NonNull final String login = Reference.getLoginOfRefreshToken(runtimeSettings.oauthRefreshToken);

        if (this.accounts.stream().noneMatch(account -> account.getLogin().equals(login))) {
            @NonNull final Account account = new Account();

            account.setLogin(login);
            account.setOauthRefreshToken(runtimeSettings.oauthRefreshToken);
            account.setOauthAccessToken(runtimeSettings.oauthAccessToken);
            account.setOauthExpire(runtimeSettings.oauthExpire);

            this.accounts.add(account);

            this.save();
        }
    }

    public void save() {
        new SGsonFile(Reference.accountConfig).writeJsonFile(this);
    }

    @Data
    public static class Account {
        @LauncherNetworkAPI
        private String login;
        @LauncherNetworkAPI
        private String oauthAccessToken;
        @LauncherNetworkAPI
        private String oauthRefreshToken;
        @LauncherNetworkAPI
        private long oauthExpire;
    }
}
