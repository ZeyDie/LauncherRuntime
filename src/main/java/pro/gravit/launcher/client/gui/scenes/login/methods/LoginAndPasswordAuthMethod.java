package pro.gravit.launcher.client.gui.scenes.login.methods;

import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.TextField;
import javafx.scene.layout.Pane;
import javafx.scene.text.Text;
import org.jetbrains.annotations.NotNull;
import pro.gravit.launcher.client.gui.JavaFXApplication;
import pro.gravit.launcher.client.gui.helper.LookupHelper;
import pro.gravit.launcher.client.gui.overlays.AbstractOverlay;
import pro.gravit.launcher.client.gui.scenes.login.LoginAuthButtonComponent;
import pro.gravit.launcher.client.gui.scenes.login.LoginScene;
import pro.gravit.launcher.request.auth.AuthRequest;
import pro.gravit.launcher.request.auth.details.AuthPasswordDetails;

import java.util.concurrent.CompletableFuture;

public class LoginAndPasswordAuthMethod extends AbstractAuthMethod<AuthPasswordDetails> {
    private final LoginAndPasswordOverlay overlay;
    private final JavaFXApplication application;
    private final LoginScene.LoginSceneAccessor accessor;

    public LoginAndPasswordAuthMethod(LoginScene.LoginSceneAccessor accessor) {
        this.accessor = accessor;
        this.application = accessor.getApplication();
        this.overlay = (LoginAndPasswordOverlay) this.application.gui.registerOverlay(LoginAndPasswordOverlay.class);
        this.overlay.accessor = accessor;
    }

    @Override
    public void prepare() {
    }

    @Override
    public void reset() {
        this.overlay.reset();
    }

    @Override
    public CompletableFuture<Void> show(AuthPasswordDetails details) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        try {
            this.accessor.showOverlay(this.overlay, (e) -> {
                future.complete(null);
            });
        } catch (Exception var4) {
            this.accessor.errorHandle(var4);
        }

        return future;
    }

    @Override
    public CompletableFuture<LoginScene.LoginAndPasswordResult> auth(AuthPasswordDetails details) {
        this.overlay.future = new CompletableFuture<>();
        String login = this.overlay.login.getText();
        if (this.overlay.password.getText().isEmpty() && this.overlay.password.getPromptText().equals(this.application.getTranslation("runtime.scenes.login.password.saved"))) {
            AuthRequest.AuthPasswordInterface password = this.application.runtimeSettings.password;
            return CompletableFuture.completedFuture(new LoginScene.LoginAndPasswordResult(login, password));
        } else {
            return this.overlay.future;
        }
    }

    @Override
    public CompletableFuture<Void> hide() {
        CompletableFuture<Void> future = new CompletableFuture<>();
        this.accessor.hideOverlay(0.0, (e) -> {
            future.complete(null);
        });
        return future;
    }

    @Override
    public boolean isSavable() {
        return true;
    }

    public static class LoginAndPasswordOverlay extends AbstractOverlay {
        private static final AbstractAuthMethod.UserAuthCanceledException USER_AUTH_CANCELED_EXCEPTION = new AbstractAuthMethod.UserAuthCanceledException();
        private TextField login;
        private TextField password;
        private LoginAuthButtonComponent authButton;
        private LoginScene.LoginSceneAccessor accessor;
        private CompletableFuture<LoginScene.LoginAndPasswordResult> future;

        public LoginAndPasswordOverlay(JavaFXApplication application) {
            super("scenes/login/loginpassword.fxml", application);
        }

        @Override
        public String getName() {
            return "loginandpassword";
        }

        @Override
        protected void doInit() {
            this.login = (TextField) LookupHelper.lookup(this.layout, new String[]{"#login"});
            this.password = (TextField) LookupHelper.lookup(this.layout, new String[]{"#password"});
            this.authButton = new LoginAuthButtonComponent((Pane) LookupHelper.lookup(this.layout, new String[]{"#authButtonBlock"}), this.application, (e) -> {
                String rawLogin = this.login.getText();
                String rawPassword = this.password.getText();
                this.future.complete(new LoginScene.LoginAndPasswordResult(rawLogin, this.accessor.getAuthService().makePassword(rawPassword)));
            });
            ((ButtonBase) LookupHelper.lookup(this.layout, new String[]{"#header", "#controls", "#exit"})).setOnAction((e) -> {
                this.accessor.hideOverlay(0.0, null);
                this.future.completeExceptionally(USER_AUTH_CANCELED_EXCEPTION);

                //TODO ZeyCodeStart
                @NotNull final JavaFXApplication javaFXApplication = JavaFXApplication.getInstance();

                try {
                    javaFXApplication.getCurrentScene().switchScene(javaFXApplication.gui.fastLoginScene);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                //TODO ZeyCodeStart
            });
            this.login.textProperty().addListener((l) -> {
                this.authButton.setActive(!this.login.getText().isEmpty());
            });
            if (this.application.guiModuleConfig.createAccountURL != null) {
                ((Text) LookupHelper.lookup(this.layout, new String[]{"#createAccount"})).setOnMouseClicked((e) -> {
                    this.application.openURL(this.application.guiModuleConfig.createAccountURL);
                });
            }

            if (this.application.guiModuleConfig.forgotPassURL != null) {
                ((Text) LookupHelper.lookup(this.layout, new String[]{"#forgotPass"})).setOnMouseClicked((e) -> {
                    this.application.openURL(this.application.guiModuleConfig.forgotPassURL);
                });
            }

            ((Button) LookupHelper.lookup(this.layout, new String[]{"#regButton000"})).setOnMouseClicked((e) -> {
                this.application.openURL(this.application.guiModuleConfig.createAccountURL);
            });
            if (this.application.runtimeSettings.login != null) {
                this.login.setText(this.application.runtimeSettings.login);
                this.authButton.setActive(true);
            } else {
                this.authButton.setActive(false);
            }

            if (this.application.runtimeSettings.password != null) {
                this.password.getStyleClass().add("hasSaved");
                this.password.setPromptText(this.application.getTranslation("runtime.scenes.login.password.saved"));
            }

        }

        @Override
        public void reset() {
            if (this.password != null) {
                this.password.getStyleClass().removeAll(new String[]{"hasSaved"});
                this.password.setPromptText(this.application.getTranslation("runtime.scenes.login.password"));
                this.password.setText("");
                this.login.setText("");
            }
        }
    }
}
