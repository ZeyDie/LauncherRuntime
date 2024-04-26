package pro.gravit.launcher.client.gui.scenes.servermenu;

import com.zeydie.launcher.Reference;
import javafx.event.EventHandler;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.shape.Rectangle;
import pro.gravit.launcher.client.ServerPinger;
import pro.gravit.launcher.client.gui.JavaFXApplication;
import pro.gravit.launcher.client.gui.helper.LookupHelper;
import pro.gravit.launcher.client.gui.scenes.AbstractScene;
import pro.gravit.launcher.profiles.ClientProfile;
import pro.gravit.utils.helper.CommonHelper;
import pro.gravit.utils.helper.LogHelper;

import java.io.IOException;
import java.util.*;

public class ServerMenuScene extends AbstractScene {
    private static final String SERVER_BUTTON_FXML = "components/serverButton.fxml";
    private static final String SERVER_BUTTON_CUSTOM_FXML = "components/serverButton/%s.fxml";
    private ImageView avatar;
    private List<ClientProfile> lastProfiles;
    private Image originalAvatarImage;

    public ServerMenuScene(JavaFXApplication application) {
        super("scenes/servermenu/servermenu.fxml", application);
    }

    public void doInit() throws Exception {
        this.avatar = LookupHelper.lookup(this.layout, new String[]{"#avatar"});
        this.originalAvatarImage = this.avatar.getImage();
        LookupHelper.<ImageView>lookupIfPossible(this.layout, new String[]{"#avatar"}).ifPresent((h) -> {
            try {
                Rectangle clip = new Rectangle(h.getFitWidth(), h.getFitHeight());
                clip.setArcWidth(h.getFitWidth() / 2.0);
                clip.setArcHeight(h.getFitHeight() / 2.0);
                h.setClip(clip);
                h.setImage(this.originalAvatarImage);
            } catch (Throwable var3) {
                LogHelper.warning("Skin head error");
            }

        });
        ScrollPane scrollPane = LookupHelper.lookup(this.layout, new String[]{"#servers"});
        scrollPane.setOnScroll((e) -> {
            double offset = e.getDeltaY() / scrollPane.getWidth();
            scrollPane.setHvalue(scrollPane.getHvalue() - offset);
        });
        this.reset();
        this.isResetOnShow = true;
    }

    public static boolean putAvatarToImageView(JavaFXApplication application, String username, ImageView imageView) {
        int width = (int) imageView.getFitWidth();
        int height = (int) imageView.getFitHeight();
        Image head = application.skinManager.getScaledFxSkinHead(username, width, height);
        if (head == null) {
            return false;
        } else {
            imageView.setImage(head);
            return true;
        }
    }

    public static ServerButtonComponent getServerButton(JavaFXApplication application, ClientProfile profile) {
        return new ServerButtonComponent(application, profile);
    }

    public void reset() {
        if (this.lastProfiles != this.application.stateService.getProfiles()) {
            this.lastProfiles = this.application.stateService.getProfiles();
            Map<ClientProfile, ServerButtonCache> serverButtonCacheMap = new LinkedHashMap<>();
            LookupHelper.<Label>lookupIfPossible(this.layout, new String[]{"#nickname"}).ifPresent((e) -> e.setText(this.application.stateService.getUsername()));
            LookupHelper.<Label>lookupIfPossible(this.layout, new String[]{"#role"}).ifPresent((e) -> e.setText(this.application.stateService.getMainRole()));
            this.avatar.setImage(this.originalAvatarImage);
            List<ClientProfile> profiles = new ArrayList<>(this.lastProfiles);

            //TODO ZeyCodeStart
            profiles.removeIf(clientProfile -> !Reference.isPlayerServer(clientProfile.getSortIndex()));
            //TODO ZeyCodeEnd

            profiles.sort(Comparator.comparingInt(ClientProfile::getSortIndex).thenComparing(ClientProfile::getTitle));

            int position = 0;

            for (Iterator var4 = profiles.iterator(); var4.hasNext(); ++position) {
                ClientProfile profile = (ClientProfile) var4.next();
                ServerButtonCache cache = new ServerButtonCache();
                cache.serverButton = getServerButton(this.application, profile);
                cache.position = position;
                serverButtonCacheMap.put(profile, cache);
                profile.updateOptionalGraph();
            }

            ScrollPane scrollPane = LookupHelper.lookup(this.layout, new String[]{"#servers"});
            HBox serverList = (HBox) scrollPane.getContent();
            serverList.setSpacing(20.0);
            serverList.getChildren().clear();
            this.application.pingService.clear();
            serverButtonCacheMap.forEach((profilex, serverButtonCache) -> {
                EventHandler<? super MouseEvent> handle = (event) -> {
                    if (event.getButton().equals(MouseButton.PRIMARY)) {
                        this.changeServer(profilex);

                        try {
                            this.switchScene(this.application.gui.serverInfoScene);
                            this.application.gui.serverInfoScene.reset();
                        } catch (Exception var4) {
                            this.errorHandle(var4);
                        }

                    }
                };
                serverButtonCache.serverButton.addTo(serverList, serverButtonCache.position);
                serverButtonCache.serverButton.setOnMouseClicked(handle);
            });
            CommonHelper.newThread("ServerPinger", true, () -> {
                Iterator var1 = this.lastProfiles.iterator();

                while (var1.hasNext()) {
                    ClientProfile profile = (ClientProfile) var1.next();
                    Iterator var3 = profile.getServers().iterator();

                    while (var3.hasNext()) {
                        ClientProfile.ServerProfile serverProfile = (ClientProfile.ServerProfile) var3.next();
                        if (serverProfile.socketPing && serverProfile.serverAddress != null) {
                            try {
                                ServerPinger pinger = new ServerPinger(serverProfile, profile.getVersion());
                                ServerPinger.Result result = pinger.ping();
                                this.contextHelper.runInFxThread(() -> {
                                    this.application.pingService.addReport(serverProfile.name, result);
                                });
                            } catch (IOException var7) {
                                var7.printStackTrace();
                            }
                        }
                    }
                }

            }).start();
            putAvatarToImageView(this.application, this.application.stateService.getUsername(), this.avatar);
        }
    }

    public String getName() {
        return "serverMenu";
    }

    private void changeServer(ClientProfile profile) {
        this.application.stateService.setProfile(profile);
        this.application.runtimeSettings.lastProfile = profile.getUUID();
    }

    static class ServerButtonCache {
        public ServerButtonComponent serverButton;
        public int position;

        ServerButtonCache() {
        }
    }
}
