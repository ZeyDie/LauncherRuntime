package pro.gravit.launcher.client.gui;

import com.zeydie.launcher.Reference;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import pro.gravit.utils.helper.LogHelper;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.awt.image.SinglePixelPackedSampleModel;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.IntBuffer;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class SkinManager {
    private final JavaFXApplication application;
    private final Map<String, SkinEntry> map = new ConcurrentHashMap<>();

    public SkinManager(JavaFXApplication application) {
        this.application = application;
    }

    public static BufferedImage sumBufferedImage(BufferedImage img1, BufferedImage img2) {
        int wid = Math.max(img1.getWidth(), img2.getWidth());
        int height = Math.max(img1.getHeight(), img2.getHeight());
        BufferedImage result = new BufferedImage(wid, height, 2);
        Graphics2D g2 = result.createGraphics();
        Color oldColor = g2.getColor();
        g2.setPaint(Color.WHITE);
        g2.fillRect(0, 0, wid, height);
        g2.setColor(oldColor);
        g2.drawImage(img1, null, 0, 0);
        g2.drawImage(img2, null, 0, 0);
        g2.dispose();
        return result;
    }

    private static BufferedImage scaleImage(BufferedImage origImage, int width, int height) {
        if (origImage == null) {
            return null;
        } else {
            java.awt.Image resized = origImage.getScaledInstance(width, height, 2);
            BufferedImage image = new BufferedImage(width, height, 3);
            Graphics2D graphics2D = image.createGraphics();
            graphics2D.drawImage(resized, 0, 0, null);
            graphics2D.dispose();
            return image;
        }
    }

    private static BufferedImage downloadSkin(URL url) {
        HttpURLConnection connection;

        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.addRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/45.0.2454.85 Safari/537.36");
            connection.setConnectTimeout(10000);
            connection.connect();

            try {
                InputStream input = connection.getInputStream();

                BufferedImage var3;
                try {
                    var3 = ImageIO.read(input);
                } catch (Throwable var6) {
                    if (input != null) {
                        try {
                            input.close();
                        } catch (Throwable var5) {
                            var6.addSuppressed(var5);
                        }
                    }

                    throw var6;
                }

                input.close();

                return var3;
            } catch (FileNotFoundException var7) {
                LogHelper.dev("User texture not found" + var7.getMessage());

                //TODO ZeyCodeStart
                if (!url.getPath().endsWith("default.png"))
                    downloadSkin(new URL(String.format(Reference.skinUrl, "default")));
                //TODO ZeyCodeEnd

                return null;
            }
        } catch (IOException var8) {
            LogHelper.error(var8);
            return null;
        }
    }

    private static BufferedImage getHeadLayerFromSkinImage(BufferedImage image) {
        int width = image.getWidth();
        int renderScale = width / 64;
        int size = 8 * renderScale;
        int x_offset = 40 * renderScale;
        int y_offset = 8 * renderScale;
        LogHelper.debug("ShinHead debug: W: %d Scale: %d Offset: %d", width, renderScale, size);
        return image.getSubimage(x_offset, y_offset, size, size);
    }

    private static BufferedImage getHeadFromSkinImage(BufferedImage image) {
        int width = image.getWidth();
        int renderScale = width / 64;
        int offset = 8 * renderScale;
        LogHelper.debug("ShinHead debug: W: %d Scale: %d Offset: %d", width, renderScale, offset);
        return image.getSubimage(offset, offset, offset, offset);
    }

    private static Image convertToFxImage(BufferedImage image) {
        if (image == null) {
            return null;
        } else {
            try {
                return SwingFXUtils.toFXImage(image, null);
            } catch (Throwable var2) {
                if (LogHelper.isDebugEnabled()) {
                    LogHelper.error(var2);
                }

                return convertToFxImageJava8(image);
            }
        }
    }

    private static Image convertToFxImageJava8(BufferedImage image) {
        int bw = image.getWidth();
        int bh = image.getHeight();
        switch (image.getType()) {
            default:
                BufferedImage converted = new BufferedImage(bw, bh, 3);
                Graphics2D graphics2D = converted.createGraphics();
                graphics2D.drawImage(image, 0, 0, null);
                graphics2D.dispose();
                image = converted;
            case 2:
            case 3:
                WritableImage writableImage = new WritableImage(bw, bh);
                DataBufferInt raster = (DataBufferInt) image.getRaster().getDataBuffer();
                int scan = image.getRaster().getSampleModel() instanceof SinglePixelPackedSampleModel ? ((SinglePixelPackedSampleModel) image.getRaster().getSampleModel()).getScanlineStride() : 0;
                PixelFormat<IntBuffer> pf = image.isAlphaPremultiplied() ? PixelFormat.getIntArgbPreInstance() : PixelFormat.getIntArgbInstance();
                writableImage.getPixelWriter().setPixels(0, 0, bw, bh, pf, raster.getData(), raster.getOffset(), scan);
                return writableImage;
        }
    }

    public void addSkin(String username, URL url) {
        this.map.put(username, new SkinEntry(url));
    }

    public BufferedImage getSkin(String username) {
        SkinEntry entry = this.map.get(username);
        return entry == null ? null : entry.getFullImage();
    }

    public BufferedImage getSkinHead(String username) {
        SkinEntry entry = this.map.get(username);
        return entry == null ? null : entry.getHeadImage();
    }

    public Image getFxSkin(String username) {
        SkinEntry entry = this.map.get(username);
        return entry == null ? null : entry.getFullFxImage();
    }

    public Image getFxSkinHead(String username) {
        SkinEntry entry = this.map.get(username);
        return entry == null ? null : entry.getHeadFxImage();
    }

    public BufferedImage getScaledSkin(String username, int width, int height) {
        BufferedImage image = this.getSkin(username);
        return scaleImage(image, width, height);
    }

    public BufferedImage getScaledSkinHead(String username, int width, int height) {
        BufferedImage image = this.getSkinHead(username);
        return scaleImage(image, width, height);
    }

    public Image getScaledFxSkin(String username, int width, int height) {
        BufferedImage image = this.getSkin(username);
        return convertToFxImage(scaleImage(image, width, height));
    }

    public Image getScaledFxSkinHead(String username, int width, int height) {
        BufferedImage image = this.getSkinHead(username);
        return image == null ? null : convertToFxImage(scaleImage(image, width, height));
    }

    private static class SkinEntry {
        final URL url;
        final String imageUrl;
        SoftReference<Optional<BufferedImage>> imageRef;
        SoftReference<Optional<BufferedImage>> avatarRef;
        SoftReference<Optional<Image>> fxImageRef;
        SoftReference<Optional<Image>> fxAvatarRef;

        private SkinEntry(URL url) {
            this.imageRef = new SoftReference<>(null);
            this.avatarRef = new SoftReference<>(null);
            this.fxImageRef = new SoftReference<>(null);
            this.fxAvatarRef = new SoftReference<>(null);
            this.url = url;
            this.imageUrl = null;
        }

        synchronized BufferedImage getFullImage() {
            Optional<BufferedImage> result = this.imageRef.get();
            if (result == null) {
                result = Optional.ofNullable(SkinManager.downloadSkin(this.url));
                this.imageRef = new SoftReference<>(result);
            }

            return result.orElse(null);
        }

        synchronized Image getFullFxImage() {
            Optional<Image> result = this.fxImageRef.get();
            if (result == null) {
                BufferedImage image = this.getFullImage();
                if (image == null) {
                    return null;
                }

                result = Optional.ofNullable(SkinManager.convertToFxImage(image));
                this.fxImageRef = new SoftReference<>(result);
            }

            return result.orElse(null);
        }

        synchronized BufferedImage getHeadImage() {
            Optional<BufferedImage> result = this.avatarRef.get();
            if (result == null) {
                BufferedImage image = this.getFullImage();
                if (image == null) {
                    return null;
                }

                result = Optional.of(SkinManager.sumBufferedImage(SkinManager.getHeadFromSkinImage(image), SkinManager.getHeadLayerFromSkinImage(image)));
                this.avatarRef = new SoftReference<>(result);
            }

            return result.orElse(null);
        }

        synchronized Image getHeadFxImage() {
            Optional<Image> result = this.fxAvatarRef.get();
            if (result == null) {
                BufferedImage image = this.getHeadImage();
                if (image == null) {
                    return null;
                }

                result = Optional.ofNullable(SkinManager.convertToFxImage(image));
                this.fxAvatarRef = new SoftReference<>(result);
            }

            return result.orElse(null);
        }
    }
}
