package com.vikingposter.client.web;

import com.cinemamod.mcef.MCEF;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.io.InputStream;
import java.nio.file.*;

@OnlyIn(Dist.CLIENT)
public final class PosterMCEFBootstrap {

    private static volatile boolean ready = false;
    private static Path webRoot;

    public static void init() {
        try {
            webRoot = Files.createTempDirectory("vikingposter-web-");
            webRoot.toFile().deleteOnExit();
            for (String name : new String[]{"poster_edit.html", "poster_edit.css", "poster_edit.js"}) {
                extractAsset("assets/vikingposter/web/" + name, webRoot.resolve(name));
            }
        } catch (Exception e) {
            System.err.println("[VikingPoster] Impossible d'extraire les assets web: " + e);
        }

        MCEF.scheduleForInit(success -> {
            ready = success;
            System.out.println("[VikingPoster] MCEF init: " + (success ? "OK" : "ÉCHEC"));
        });
    }

    private static void extractAsset(String resourcePath, Path dest) {
        try (InputStream in = PosterMCEFBootstrap.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (in == null) {
                System.err.println("[VikingPoster] Asset manquant: " + resourcePath);
                return;
            }
            Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            System.err.println("[VikingPoster] Erreur extraction " + resourcePath + ": " + e);
        }
    }

    public static boolean isReady() { return ready; }

    public static String urlFor(String fileName) {
        if (webRoot == null) return "about:blank";
        return webRoot.resolve(fileName).toUri().toString();
    }
}
