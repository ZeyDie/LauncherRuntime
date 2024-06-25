package com.zeydie.launcher.config;

import com.zeydie.launcher.Reference;
import com.zeydie.sgson.SGsonFile;
import lombok.Data;
import lombok.NonNull;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import pro.gravit.launcher.LauncherNetworkAPI;

import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

@Data
public final class AccountsConfig {
    @LauncherNetworkAPI
    @NotNull
    private List<Account> accounts = new ArrayList<>();

    @SneakyThrows
    public void load() {
        if (Reference.defaultAccountConfig.toFile().exists()) {
            Files.move(Reference.defaultLauncherDirectory, Reference.accountConfig, StandardCopyOption.REPLACE_EXISTING);
            Reference.defaultLauncherDirectory.toFile().delete();
        }

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
