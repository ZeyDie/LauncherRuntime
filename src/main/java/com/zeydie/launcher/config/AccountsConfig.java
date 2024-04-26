package com.zeydie.launcher.config;

import com.zeydie.launcher.Reference;
import com.zeydie.sgson.SGsonFile;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import pro.gravit.launcher.LauncherNetworkAPI;

import java.util.ArrayList;
import java.util.List;

@Data
public final class AccountsConfig {
    @LauncherNetworkAPI
    private @NotNull List<Account> accounts = new ArrayList<>();

    public void load() {
        this.accounts = this.getSGSon().fromJsonToObject(this).getAccounts();
    }

    public void save() {
        this.getSGSon().writeJsonFile(this);

        this.load();
    }

    public @NotNull SGsonFile getSGSon() {
        return new SGsonFile(Reference.accountConfig).setDebug().setPretty();
    }

    @Data
    public static class Account {
        @LauncherNetworkAPI
        private String login;
        @LauncherNetworkAPI
        private int serverId;
        @LauncherNetworkAPI
        private String oauthAccessToken;
        @LauncherNetworkAPI
        private String oauthRefreshToken;
        @LauncherNetworkAPI
        private long oauthExpire;
    }
}
