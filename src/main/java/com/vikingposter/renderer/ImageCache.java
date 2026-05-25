package com.vikingposter.renderer;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL30;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@OnlyIn(Dist.CLIENT)
public class ImageCache {

    private static final Map<String, ResourceLocation> READY   = new ConcurrentHashMap<>();
    private static final Set<String>                   LOADING = ConcurrentHashMap.newKeySet();
    private static final Set<String>                   FAILED  = ConcurrentHashMap.newKeySet();

    public static final ResourceLocation PLACEHOLDER =
        ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/oak_planks.png");

    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "PosterImageLoader");
        t.setDaemon(true);
        return t;
    });

    public static ResourceLocation getOrLoad(String url) {
        if (url == null || url.isBlank()) return PLACEHOLDER;
        ResourceLocation cached = READY.get(url);
        if (cached != null) return cached;
        if (FAILED.contains(url)) return PLACEHOLDER;
        if (LOADING.add(url)) EXECUTOR.submit(() -> download(url));
        return PLACEHOLDER;
    }

    public static void invalidate(String url) {
        ResourceLocation loc = READY.remove(url);
        LOADING.remove(url);
        FAILED.remove(url);
        if (loc != null) {
            Minecraft.getInstance().execute(() ->
                Minecraft.getInstance().getTextureManager().release(loc));
        }
    }

    private static void download(String rawUrl) {
        try {
            URL url = new URL(rawUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("User-Agent", "VikingPosterMod/1.0 (Minecraft)");
            conn.setConnectTimeout(10_000);
            conn.setReadTimeout(30_000);
            conn.setInstanceFollowRedirects(true);

            try (InputStream is = conn.getInputStream()) {
                byte[] bytes = is.readAllBytes();
                BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes));
                if (img == null) { LOADING.remove(rawUrl); return; }

                final int w = img.getWidth();
                final int h = img.getHeight();
                final int[] argbPixels = img.getRGB(0, 0, w, h, null, 0, w);

                Minecraft.getInstance().execute(() -> uploadTexture(rawUrl, w, h, argbPixels));
            }
        } catch (Exception e) {
            System.err.println("[VikingPoster] Échec téléchargement image " + rawUrl + ": " + e.getMessage());
            LOADING.remove(rawUrl);
            FAILED.add(rawUrl);
        }
    }

    private static void uploadTexture(String url, int w, int h, int[] argbPixels) {
        try {
            NativeImage ni = new NativeImage(NativeImage.Format.RGBA, w, h, false);

            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int argb = argbPixels[y * w + x];
                    // Convert ARGB (Java) → ABGR (Minecraft NativeImage)
                    int abgr = (argb & 0xFF00FF00)
                             | ((argb & 0x00FF0000) >> 16)
                             | ((argb & 0x000000FF) << 16);
                    ni.setPixelRGBA(x, y, abgr);
                }
            }

            DynamicTexture dt = new DynamicTexture(ni);
            ResourceLocation loc = ResourceLocation.fromNamespaceAndPath("vikingposter",
                "dynamic/" + Integer.toHexString(url.hashCode() & 0x7FFFFFFF));
            Minecraft.getInstance().getTextureManager().register(loc, dt);

            // Enable linear filtering + mipmaps for smooth scaling
            dt.bind();
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR_MIPMAP_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);

            READY.put(url, loc);
            LOADING.remove(url);
        } catch (Exception e) {
            System.err.println("[VikingPoster] Échec upload texture: " + e.getMessage());
            LOADING.remove(url);
        }
    }
}
