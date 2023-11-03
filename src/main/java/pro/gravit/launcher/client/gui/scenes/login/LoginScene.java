package pro.gravit.launcher.client.gui.scenes.login;

import com.zeydie.launcher.Accounts;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import pro.gravit.launcher.LauncherEngine;
import pro.gravit.launcher.client.StdJavaRuntimeProvider;
import pro.gravit.launcher.client.events.ClientExitPhase;
import pro.gravit.launcher.client.gui.JavaFXApplication;
import pro.gravit.launcher.client.gui.helper.LookupHelper;
import pro.gravit.launcher.client.gui.overlays.AbstractOverlay;
import pro.gravit.launcher.client.gui.scenes.AbstractScene;
import pro.gravit.launcher.client.gui.scenes.login.methods.*;
import pro.gravit.launcher.client.gui.service.AuthService;
import pro.gravit.launcher.events.request.AuthRequestEvent;
import pro.gravit.launcher.events.request.GetAvailabilityAuthRequestEvent;
import pro.gravit.launcher.profiles.ClientProfile;
import pro.gravit.launcher.profiles.Texture;
import pro.gravit.launcher.request.Request;
import pro.gravit.launcher.request.RequestException;
import pro.gravit.launcher.request.WebSocketEvent;
import pro.gravit.launcher.request.auth.AuthRequest;
import pro.gravit.launcher.request.auth.GetAvailabilityAuthRequest;
import pro.gravit.launcher.request.auth.RefreshTokenRequest;
import pro.gravit.launcher.request.auth.details.AuthLoginOnlyDetails;
import pro.gravit.launcher.request.auth.details.AuthPasswordDetails;
import pro.gravit.launcher.request.auth.details.AuthTotpDetails;
import pro.gravit.launcher.request.auth.details.AuthWebViewDetails;
import pro.gravit.launcher.request.auth.password.Auth2FAPassword;
import pro.gravit.launcher.request.auth.password.AuthMultiPassword;
import pro.gravit.launcher.request.auth.password.AuthOAuthPassword;
import pro.gravit.launcher.request.update.LauncherRequest;
import pro.gravit.launcher.request.update.ProfilesRequest;
import pro.gravit.launcher.utils.LauncherUpdater;
import pro.gravit.utils.helper.LogHelper;

import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class LoginScene extends AbstractScene {
    public Map<Class<? extends GetAvailabilityAuthRequestEvent.AuthAvailabilityDetails>, AbstractAuthMethod<? extends GetAvailabilityAuthRequestEvent.AuthAvailabilityDetails>> authMethods = new HashMap<>(8);
    public boolean isLoginStarted;
    //TODO ZeyCodeModified from private to public
    public List<GetAvailabilityAuthRequestEvent.AuthAvailability> auth;
    //TODO ZeyCodeClear
    /*private CheckBox savePasswordCheckBox;
    private CheckBox autoenter;*/
    private LoginAuthButtonComponent authButton;
    private final AuthService authService;
    private VBox authList;
    private ToggleGroup authToggleGroup;
    private GetAvailabilityAuthRequestEvent.AuthAvailability authAvailability;
    private final AuthFlow authFlow;
    private volatile boolean processingEnabled;

    public LoginScene(JavaFXApplication application) {
        super("scenes/login/login.fxml", application);
        this.authService = new AuthService(this.application);
        this.authFlow = new AuthFlow();
        this.processingEnabled = false;
        LoginSceneAccessor accessor = new LoginSceneAccessor();
        this.authMethods.put(AuthPasswordDetails.class, new LoginAndPasswordAuthMethod(accessor));
        this.authMethods.put(AuthWebViewDetails.class, new WebAuthMethod(accessor));
        this.authMethods.put(AuthTotpDetails.class, new TotpAuthMethod(accessor));
        this.authMethods.put(AuthLoginOnlyDetails.class, new LoginOnlyAuthMethod(accessor));
    }

    @Override
    public void doInit() {
        this.authButton = new LoginAuthButtonComponent((Pane) LookupHelper.lookup(this.layout, new String[]{"#authButtonBlock"}), this.application, (e) -> {
            this.contextHelper.runCallback(this::loginWithGui);
        });
        //TODO ZeyCodeStart
        this.application.runtimeSettings.autoAuth = true;
        //TODO ZeyCodeEnd
        //TODO ZeyCodeClear
        /*this.savePasswordCheckBox = (CheckBox) LookupHelper.lookup(this.layout, new String[]{"#leftPane", "#savePassword"});
        if (this.application.runtimeSettings.password != null || this.application.runtimeSettings.oauthAccessToken != null) {
            ((CheckBox) LookupHelper.lookup(this.layout, new String[]{"#leftPane", "#savePassword"})).setSelected(true);
        }

        this.autoenter = (CheckBox) LookupHelper.lookup(this.layout, new String[]{"#autoenter"});
        this.autoenter.setSelected(this.application.runtimeSettings.autoAuth);
        this.autoenter.setOnAction((event) -> {
            this.application.runtimeSettings.autoAuth = this.autoenter.isSelected();
        });*/
        if (this.application.guiModuleConfig.createAccountURL != null) {
            ((Text) LookupHelper.lookup(this.header, new String[]{"#controls", "#registerPane", "#createAccount"})).setOnMouseClicked((e) -> {
                this.application.openURL(this.application.guiModuleConfig.createAccountURL);
            });
        }

        if (this.application.guiModuleConfig.forgotPassURL != null) {
            ((Text) LookupHelper.lookup(this.header, new String[]{"#controls", "#links", "#forgotPass"})).setOnMouseClicked((e) -> {
                this.application.openURL(this.application.guiModuleConfig.forgotPassURL);
            });
        }

        this.authList = (VBox) LookupHelper.lookup(this.layout, new String[]{"#authList"});
        this.authToggleGroup = new ToggleGroup();
        this.authMethods.forEach((k, v) -> {
            v.prepare();
        });
        if (!this.application.isDebugMode()) {
            this.launcherRequest();
        } else {
            this.getAvailabilityAuth();
        }

    }

    private void launcherRequest() {
        LauncherRequest launcherRequest = new LauncherRequest();
        this.processRequest(this.application.getTranslation("runtime.overlay.processing.text.launcher"), launcherRequest, (result) -> {
            if (result.launcherExtendedToken != null) {
                Request.addExtendedToken("launcher", result.launcherExtendedToken);
            }

            if (result.needUpdate) {
                try {
                    LogHelper.debug("Start update processing");
                    this.disable();
                    StdJavaRuntimeProvider.updatePath = LauncherUpdater.prepareUpdate(new URL(result.url));
                    LogHelper.debug("Exit with Platform.exit");
                    Platform.exit();
                    return;
                } catch (Throwable var5) {
                    this.contextHelper.runInFxThread(() -> {
                        this.errorHandle(var5);
                    });

                    try {
                        Thread.sleep(1500L);
                        LauncherEngine.modulesManager.invokeEvent(new ClientExitPhase(0));
                        Platform.exit();
                    } catch (Throwable var4) {
                        LauncherEngine.exitLauncher(0);
                    }
                }
            }

            LogHelper.dev("Launcher update processed");
            this.getAvailabilityAuth();
        }, (event) -> {
            LauncherEngine.exitLauncher(0);
        });
    }

    private void getAvailabilityAuth() {
        GetAvailabilityAuthRequest getAvailabilityAuthRequest = new GetAvailabilityAuthRequest();
        this.processRequest(this.application.getTranslation("runtime.overlay.processing.text.authAvailability"), getAvailabilityAuthRequest, (auth) -> {
            this.contextHelper.runInFxThread(() -> {
                this.auth = auth.list;
                this.authList.setVisible(auth.list.size() != 1);
                Iterator<GetAvailabilityAuthRequestEvent.AuthAvailability> var2 = auth.list.iterator();

                while (true) {
                    GetAvailabilityAuthRequestEvent.AuthAvailability authAvailability;
                    do {
                        if (!var2.hasNext()) {
                            if (this.authAvailability == null && auth.list.size() > 0) {
                                this.changeAuthAvailability((GetAvailabilityAuthRequestEvent.AuthAvailability) auth.list.get(0));
                            }

                            this.hideOverlay(0.0, (event) -> {
                                this.postInit();
                            });
                            return;
                        }

                        authAvailability = (GetAvailabilityAuthRequestEvent.AuthAvailability) var2.next();
                    } while (!authAvailability.visible);

                    if (this.application.runtimeSettings.lastAuth == null) {
                        if (authAvailability.name.equals("std") || this.authAvailability == null) {
                            this.changeAuthAvailability(authAvailability);
                        }
                    } else if (authAvailability.name.equals(this.application.runtimeSettings.lastAuth.name)) {
                        this.changeAuthAvailability(authAvailability);
                    }

                    this.addAuthAvailability(authAvailability);
                }
            });
        }, null);
    }

    //TODO ZeyCodeModified from private to public
    public void postInit() {
        if (this.application.guiModuleConfig.autoAuth || this.application.runtimeSettings.autoAuth) {
            this.contextHelper.runInFxThread(this::loginWithGui);
        }

    }

    public void changeAuthAvailability(GetAvailabilityAuthRequestEvent.AuthAvailability authAvailability) {
        this.authAvailability = authAvailability;
        this.application.stateService.setAuthAvailability(authAvailability);
        this.authFlow.init(authAvailability);
        LogHelper.info("Selected auth: %s", new Object[]{authAvailability.name});
    }

    public void addAuthAvailability(GetAvailabilityAuthRequestEvent.AuthAvailability authAvailability) {
        RadioButton radio = new RadioButton();
        radio.setToggleGroup(this.authToggleGroup);
        radio.setId("authRadio");
        radio.setText(authAvailability.displayName);
        if (this.authAvailability == authAvailability) {
            radio.fire();
        }

        radio.setOnAction((e) -> {
            this.changeAuthAvailability(authAvailability);
        });
        LogHelper.info("Added %s: %s", new Object[]{authAvailability.name, authAvailability.displayName});
        this.authList.getChildren().add(radio);
    }

    public <T extends WebSocketEvent> void processing(Request<T> request, String text, Consumer<T> onSuccess, Consumer<String> onError) {
        Pane root = (Pane) this.scene.getRoot();
        LookupHelper.Point2D authAbsPosition = LookupHelper.getAbsoluteCords(this.authButton.getLayout(), this.layout);
        LogHelper.debug("X: %f, Y: %f", new Object[]{authAbsPosition.x, authAbsPosition.y});
        double authLayoutX = this.authButton.getLayout().getLayoutX();
        double authLayoutY = this.authButton.getLayout().getLayoutY();
        String oldText = this.authButton.getText();
        if (!this.processingEnabled) {
            this.contextHelper.runInFxThread(() -> {
                this.disable();
                this.layout.getChildren().remove(this.authButton.getLayout());
                root.getChildren().add(this.authButton.getLayout());
                this.authButton.getLayout().setLayoutX(authAbsPosition.x);
                this.authButton.getLayout().setLayoutY(authAbsPosition.y);
            });
            this.authButton.disable();
            this.processingEnabled = true;
        }

        this.contextHelper.runInFxThread(() -> {
            this.authButton.setText(text);
        });
        Runnable processingOff = () -> {
            if (this.processingEnabled) {
                this.contextHelper.runInFxThread(() -> {
                    this.enable();
                    root.getChildren().remove(this.authButton.getLayout());
                    this.layout.getChildren().add(this.authButton.getLayout());
                    this.authButton.getLayout().setLayoutX(authLayoutX);
                    this.authButton.getLayout().setLayoutY(authLayoutY);
                    this.authButton.setText(oldText);
                });
                this.authButton.enable();
                this.processingEnabled = false;
            }
        };

        try {
            this.application.service.request(request).thenAccept((result) -> {
                onSuccess.accept(result);
                processingOff.run();
            }).exceptionally((exc) -> {
                onError.accept(exc.getCause().getMessage());
                processingOff.run();
                return null;
            });
        } catch (IOException var14) {
            processingOff.run();
            this.errorHandle(var14);
        }

    }

    @Override
    public void errorHandle(Throwable e) {
        super.errorHandle(e);
        Pane root = (Pane) this.scene.getRoot();
        double authLayoutX = this.authButton.getLayout().getLayoutX();
        double authLayoutY = this.authButton.getLayout().getLayoutY();
        if (this.processingEnabled) {
            this.contextHelper.runInFxThread(() -> {
                this.enable();
                root.getChildren().remove(this.authButton.getLayout());
                this.layout.getChildren().add(this.authButton.getLayout());
                this.authButton.getLayout().setLayoutX(authLayoutX);
                this.authButton.getLayout().setLayoutY(authLayoutY);
                this.authButton.setText("ERROR");
                this.authButton.setError();
            });
            this.authButton.enable();
            this.processingEnabled = false;
        }
    }

    @Override
    public void reset() {
        this.authFlow.reset();
    }

    @Override
    public String getName() {
        return "login";
    }

    //TODO ZeyCodeModified to public from private
    public boolean tryOAuthLogin() {
        if (this.application.runtimeSettings.lastAuth != null && this.authAvailability.name.equals(this.application.runtimeSettings.lastAuth.name) && this.application.runtimeSettings.oauthAccessToken != null) {
            if (this.application.runtimeSettings.oauthExpire != 0L && this.application.runtimeSettings.oauthExpire < System.currentTimeMillis()) {
                RefreshTokenRequest request = new RefreshTokenRequest(this.authAvailability.name, this.application.runtimeSettings.oauthRefreshToken);
                this.processing(request, this.application.getTranslation("runtime.overlay.processing.text.auth"), (result) -> {
                    this.application.runtimeSettings.oauthAccessToken = result.oauth.accessToken;
                    this.application.runtimeSettings.oauthRefreshToken = result.oauth.refreshToken;
                    this.application.runtimeSettings.oauthExpire = result.oauth.expire == 0L ? 0L : System.currentTimeMillis() + result.oauth.expire;
                    Request.setOAuth(this.authAvailability.name, result.oauth);
                    AuthOAuthPassword password = new AuthOAuthPassword(this.application.runtimeSettings.oauthAccessToken);
                    LogHelper.info("Login with OAuth AccessToken");
                    this.loginWithOAuth(password, this.authAvailability);
                }, (error) -> {
                    this.application.runtimeSettings.oauthAccessToken = null;
                    this.application.runtimeSettings.oauthRefreshToken = null;
                    this.contextHelper.runInFxThread(this::loginWithGui);
                });
                return true;
            } else {
                Request.setOAuth(this.authAvailability.name, new AuthRequestEvent.OAuthRequestEvent(this.application.runtimeSettings.oauthAccessToken, this.application.runtimeSettings.oauthRefreshToken, this.application.runtimeSettings.oauthExpire), this.application.runtimeSettings.oauthExpire);
                AuthOAuthPassword password = new AuthOAuthPassword(this.application.runtimeSettings.oauthAccessToken);
                LogHelper.info("Login with OAuth AccessToken");
                this.loginWithOAuth(password, this.authAvailability);
                return true;
            }
        } else {
            return false;
        }
    }

    private void loginWithOAuth(AuthOAuthPassword password, GetAvailabilityAuthRequestEvent.AuthAvailability authAvailability) {
        AuthRequest authRequest = this.authService.makeAuthRequest((String) null, password, authAvailability.name);
        this.processing(authRequest, this.application.getTranslation("runtime.overlay.processing.text.auth"), (result) -> {
            this.contextHelper.runInFxThread(() -> {
                this.onSuccessLogin(new SuccessAuth(result, (String) null, (AuthRequest.AuthPasswordInterface) null));
            });
        }, (error) -> {
            if (error.equals("auth.invalidtoken")) {
                this.application.runtimeSettings.oauthAccessToken = null;
                this.application.runtimeSettings.oauthRefreshToken = null;
                this.contextHelper.runInFxThread(this::loginWithGui);
            } else {
                this.errorHandle(new RequestException(error));
            }

        });
    }

    @SuppressWarnings("unchecked")
    private AbstractAuthMethod<GetAvailabilityAuthRequestEvent.AuthAvailabilityDetails> detailsToMethod(GetAvailabilityAuthRequestEvent.AuthAvailabilityDetails details) {
        return (AbstractAuthMethod<GetAvailabilityAuthRequestEvent.AuthAvailabilityDetails>) this.authMethods.get(details.getClass());
    }

    private void loginWithGui() {
        this.authButton.unsetError();
        if (!this.tryOAuthLogin()) {
            this.authFlow.start().thenAccept((result) -> {
                this.contextHelper.runInFxThread(() -> {
                    this.onSuccessLogin(result);
                });
            });
        }
    }

    private boolean checkSavePasswordAvailable(AuthRequest.AuthPasswordInterface password) {
        if (password instanceof Auth2FAPassword) {
            return false;
        } else if (password instanceof AuthMultiPassword) {
            return false;
        } else {
            return this.authAvailability != null && this.authAvailability.details != null && this.authAvailability.details.size() != 0 && this.authAvailability.details.get(0) instanceof AuthPasswordDetails;
        }
    }

    private void onSuccessLogin(SuccessAuth successAuth) {
        AuthRequestEvent result = successAuth.requestEvent;
        this.application.stateService.setAuthResult(this.authAvailability.name, result);

        //TODO ZeyCodeReplace this.savePasswordCheckBox.isSelected() to true
        boolean savePassword = true;
        if (savePassword) {
            this.application.runtimeSettings.login = successAuth.recentLogin;
            if (result.oauth == null) {
                if (successAuth.recentPassword != null && this.checkSavePasswordAvailable(successAuth.recentPassword)) {
                    this.application.runtimeSettings.password = successAuth.recentPassword;
                } else {
                    LogHelper.warning("2FA/MFA Password not saved");
                }
            } else {
                this.application.runtimeSettings.oauthAccessToken = result.oauth.accessToken;
                this.application.runtimeSettings.oauthRefreshToken = result.oauth.refreshToken;
                this.application.runtimeSettings.oauthExpire = Request.getTokenExpiredTime();

                //TODO ZeyCodeStart
                Accounts.authed(result);
                //TODO ZeyCodeEnd
            }

            this.application.runtimeSettings.lastAuth = this.authAvailability;
        }

        if (result.playerProfile != null && result.playerProfile.assets != null && result.playerProfile.assets.get("SKIN") != null) {
            try {
                this.application.skinManager.addSkin(result.playerProfile.username, new URL(((Texture) result.playerProfile.assets.get("SKIN")).url));
                this.application.skinManager.getSkin(result.playerProfile.username);
            } catch (Exception var5) {
            }
        }

        this.contextHelper.runInFxThread(() -> {
            Optional<Node> player = LookupHelper.lookupIfPossible(this.scene.getRoot(), new String[]{"#player"});
            if (player.isPresent()) {
                LookupHelper.lookupIfPossible((Node) player.get(), new String[]{"#playerName"}).ifPresent((e) -> {
                    ((Label) e).setText(this.application.stateService.getUsername());
                });
                LookupHelper.lookupIfPossible((Node) player.get(), new String[]{"#playerHead"}).ifPresent((h) -> {
                    try {
                        ImageView imageView = (ImageView) h;
                        Rectangle clip = new Rectangle(imageView.getFitWidth(), imageView.getFitHeight());
                        clip.setArcWidth(imageView.getFitWidth() / 2.0);
                        clip.setArcHeight(imageView.getFitHeight() / 2.0);
                        imageView.setClip(clip);
                        Image image = this.application.skinManager.getScaledFxSkinHead(result.playerProfile.username, (int) imageView.getFitWidth(), (int) imageView.getFitHeight());
                        if (image != null) {
                            imageView.setImage(image);
                        }
                    } catch (Throwable var5) {
                        LogHelper.warning("Skin head error");
                    }

                });
                ((Node) player.get()).setVisible(true);
                this.disable();
                fade((Node) player.get(), 2000.0, 0.0, 1.0, (e) -> {
                    this.enable();
                    this.onGetProfiles();
                    ((Node) player.get()).setVisible(false);
                });
            } else {
                this.onGetProfiles();
            }

        });
    }

    public void onGetProfiles() {
        this.processing(new ProfilesRequest(), this.application.getTranslation("runtime.overlay.processing.text.profiles"), (profiles) -> {
            this.application.stateService.setProfilesResult(profiles);
            this.application.runtimeSettings.profiles = profiles.profiles;
            Iterator<ClientProfile> var2 = profiles.profiles.iterator();

            while (var2.hasNext()) {
                ClientProfile profile = (ClientProfile) var2.next();
                this.application.triggerManager.process(profile, this.application.stateService.getOptionalView(profile));
            }

            this.contextHelper.runInFxThread(() -> {
                this.hideOverlay(0.0, null);
                this.application.securityService.startRequest();
                if (this.application.gui.optionsScene != null) {
                    try {
                        this.application.gui.optionsScene.loadAll();
                    } catch (Throwable ex) {
                        this.errorHandle(ex);
                    }
                }

                if (this.application.getCurrentScene() instanceof LoginScene) {
                    ((LoginScene) this.application.getCurrentScene()).isLoginStarted = false;
                }

                this.application.setMainScene(this.application.gui.serverMenuScene);
            });
        }, null);
    }

    public void clearPassword() {
        this.application.runtimeSettings.encryptedPassword = null;
        this.application.runtimeSettings.password = null;
        this.application.runtimeSettings.login = null;
        this.application.runtimeSettings.oauthAccessToken = null;
        this.application.runtimeSettings.oauthRefreshToken = null;
    }

    public class AuthFlow {
        private final List<Integer> authFlow = new ArrayList<>();
        private GetAvailabilityAuthRequestEvent.AuthAvailability authAvailability;

        public AuthFlow() {
        }

        public void init(GetAvailabilityAuthRequestEvent.AuthAvailability authAvailability) {
            this.authAvailability = authAvailability;
            this.reset();
        }

        public void reset() {
            this.authFlow.clear();
            this.authFlow.add(0);
        }

        private CompletableFuture<LoginAndPasswordResult> tryLogin(String resentLogin, AuthRequest.AuthPasswordInterface resentPassword) {
            CompletableFuture<LoginScene.LoginAndPasswordResult> authFuture = null;
            if (resentPassword != null) {
                authFuture = new CompletableFuture<>();
                authFuture.complete(new LoginAndPasswordResult(resentLogin, resentPassword));
            }
            for (int i : authFlow) {
                GetAvailabilityAuthRequestEvent.AuthAvailabilityDetails details = authAvailability.details.get(i);
                final AbstractAuthMethod<GetAvailabilityAuthRequestEvent.AuthAvailabilityDetails> authMethod = detailsToMethod(details);
                if (authFuture == null)
                    authFuture = authMethod.show(details).thenCompose((e) -> authMethod.auth(details));
                else {
                    authFuture = authFuture.thenCompose(e -> authMethod.show(details).thenApply(x -> e));
                    authFuture = authFuture.thenCompose(first -> authMethod.auth(details).thenApply(second -> {
                        AuthRequest.AuthPasswordInterface password;
                        String login = null;
                        if (first.login != null) {
                            login = first.login;
                        }
                        if (second.login != null) {
                            login = second.login;
                        }
                        if (first.password instanceof AuthMultiPassword) {
                            password = first.password;
                            ((AuthMultiPassword) password).list.add(second.password);
                        } else if (first.password instanceof Auth2FAPassword) {
                            password = new AuthMultiPassword();
                            ((AuthMultiPassword) password).list = new ArrayList<>();
                            ((AuthMultiPassword) password).list.add(((Auth2FAPassword) first.password).firstPassword);
                            ((AuthMultiPassword) password).list.add(((Auth2FAPassword) first.password).secondPassword);
                            ((AuthMultiPassword) password).list.add(second.password);
                        } else {
                            password = new Auth2FAPassword();
                            ((Auth2FAPassword) password).firstPassword = first.password;
                            ((Auth2FAPassword) password).secondPassword = second.password;
                        }
                        return new LoginAndPasswordResult(login, password);
                    }));
                }
                authFuture = authFuture.thenCompose(e -> authMethod.hide().thenApply(x -> e));
            }
            return authFuture;
        }

        private void start(CompletableFuture<SuccessAuth> result, String resentLogin, AuthRequest.AuthPasswordInterface resentPassword) {
            CompletableFuture<LoginAndPasswordResult> authFuture = this.tryLogin(resentLogin, resentPassword);
            authFuture.thenAccept((e) -> {
                this.login(e.login, e.password, this.authAvailability, result);
            }).exceptionally((e) -> {
                e = e.getCause();
                this.reset();
                LoginScene.this.isLoginStarted = false;
                if (e instanceof AbstractAuthMethod.UserAuthCanceledException) {
                    return null;
                } else {
                    LoginScene.this.errorHandle(e);
                    return null;
                }
            });
        }

        private CompletableFuture<SuccessAuth> start() {
            CompletableFuture<SuccessAuth> result = new CompletableFuture<>();
            this.start(result, (String) null, (AuthRequest.AuthPasswordInterface) null);
            return result;
        }

        private void login(String login, AuthRequest.AuthPasswordInterface password, GetAvailabilityAuthRequestEvent.AuthAvailability authId, CompletableFuture<SuccessAuth> result) {
            LoginScene.this.isLoginStarted = true;
            LogHelper.dev("Auth with %s password ***** authId %s", new Object[]{login, authId});
            AuthRequest authRequest = LoginScene.this.authService.makeAuthRequest(login, password, authId.name);
            LoginScene.this.processing(authRequest, LoginScene.this.application.getTranslation("runtime.overlay.processing.text.auth"), (event) -> {
                result.complete(new SuccessAuth(event, login, password));
            }, (error) -> {
                if (error.equals("auth.invalidtoken")) {
                    LoginScene.this.application.runtimeSettings.oauthAccessToken = null;
                    LoginScene.this.application.runtimeSettings.oauthRefreshToken = null;
                    result.completeExceptionally(new RequestException(error));
                } else if (error.equals("auth.require2fa")) {
                    this.authFlow.clear();
                    this.authFlow.add(1);
                    LoginScene.this.contextHelper.runInFxThread(() -> {
                        this.start(result, login, password);
                    });
                } else if (error.startsWith("auth.require.factor.")) {
                    List<Integer> newAuthFlow = new ArrayList<>();
                    String[] var6 = error.substring("auth.require.factor.".length() + 1).split("\\.");
                    int var7 = var6.length;

                    for (int var8 = 0; var8 < var7; ++var8) {
                        String s = var6[var8];
                        newAuthFlow.add(Integer.parseInt(s));
                    }

                    this.authFlow.clear();
                    this.authFlow.addAll(newAuthFlow);
                    LoginScene.this.contextHelper.runInFxThread(() -> {
                        this.start(result, login, password);
                    });
                } else {
                    this.authFlow.clear();
                    this.authFlow.add(0);
                    LoginScene.this.errorHandle(new RequestException(error));
                }

            });
        }
    }

    public class LoginSceneAccessor {
        public LoginSceneAccessor() {
        }

        public void showOverlay(AbstractOverlay overlay, EventHandler<ActionEvent> onFinished) throws Exception {
            LoginScene.this.showOverlay(overlay, onFinished);
        }

        public void hideOverlay(double delay, EventHandler<ActionEvent> onFinished) {
            LoginScene.this.hideOverlay(delay, onFinished);
        }

        public AuthService getAuthService() {
            return LoginScene.this.authService;
        }

        public JavaFXApplication getApplication() {
            return LoginScene.this.application;
        }

        public void errorHandle(Throwable e) {
            LoginScene.this.errorHandle(e);
        }
    }

    public static class SuccessAuth {
        public AuthRequestEvent requestEvent;
        public String recentLogin;
        public AuthRequest.AuthPasswordInterface recentPassword;

        public SuccessAuth(AuthRequestEvent requestEvent, String recentLogin, AuthRequest.AuthPasswordInterface recentPassword) {
            this.requestEvent = requestEvent;
            this.recentLogin = recentLogin;
            this.recentPassword = recentPassword;
        }
    }

    public static class LoginAndPasswordResult {
        public final String login;
        public final AuthRequest.AuthPasswordInterface password;

        public LoginAndPasswordResult(String login, AuthRequest.AuthPasswordInterface password) {
            this.login = login;
            this.password = password;
        }
    }
}
