package com.zeydie.launcher.config;

import com.zeydie.launcher.Reference;
import com.zeydie.sgson.SGsonFile;
import lombok.Data;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;
import pro.gravit.launcher.LauncherNetworkAPI;
import pro.gravit.launcher.client.gui.JavaFXApplication;
import pro.gravit.launcher.client.gui.config.RuntimeSettings;

import java.util.ArrayList;
import java.util.List;

@Data
public final class AccountsConfig {
    @LauncherNetworkAPI
    @NotNull
    private List<Account> accounts = new ArrayList<>();

    public void load() {
        this.accounts = new SGsonFile(Reference.accountConfig).fromJsonToObject(this).getAccounts();
    }

    public void save() {
        @NonNull final SGsonFile file = new SGsonFile(Reference.accountConfig);

        file.getFile().mkdirs();
        file.writeJsonFile(this);
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
