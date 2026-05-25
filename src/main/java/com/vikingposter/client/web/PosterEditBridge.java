package com.vikingposter.client.web;

import com.cinemamod.mcef.MCEFBrowser;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.cef.CefSettings;
import org.cef.browser.CefBrowser;
import org.cef.handler.CefDisplayHandlerAdapter;

import java.util.function.Consumer;

@OnlyIn(Dist.CLIENT)
public class PosterEditBridge {

    private Consumer<String> onClose;
    private boolean closeFired = false;
    private final Listener listener = new Listener();

    private String imageUrl       = "";
    private float  displayWidth   = 1f;
    private float  displayHeight  = 1f;
    private float  displayOffsetX = 0f;
    private float  displayOffsetY = 0f;

    private final class Listener extends CefDisplayHandlerAdapter {
        volatile boolean detached = false;
        @Override
        public boolean onConsoleMessage(CefBrowser b, CefSettings.LogSeverity level,
                                        String msg, String src, int line) {
            if (detached || msg == null || !msg.startsWith("POSTER::")) return false;
            handleMessage(msg.substring(8));
            return true;
        }
    }

    public PosterEditBridge(Consumer<String> onClose) {
        this.onClose = onClose;
    }

    public void attach(MCEFBrowser b) {
        com.cinemamod.mcef.MCEF.getClient().addDisplayHandler(listener);
    }

    public void detach() {
        listener.detached = true;
        onClose = null;
    }

    public void fireClose(String reason) {
        if (closeFired) return;
        closeFired = true;
        if (onClose != null) onClose.accept(reason);
    }

    private void handleMessage(String rawPayload) {
        int lastPipe = rawPayload.lastIndexOf('|');
        final String payload;
        if (lastPipe >= 0) {
            String tail = rawPayload.substring(lastPipe + 1);
            payload = (!tail.isEmpty() && tail.chars().allMatch(Character::isDigit))
                ? rawPayload.substring(0, lastPipe)
                : rawPayload;
        } else {
            payload = rawPayload;
        }

        net.minecraft.client.Minecraft.getInstance().execute(() -> {
            String[] parts = payload.split("\\|", -1);
            switch (parts[0]) {
                case "ready" -> {}
                case "close" -> fireClose("close");
                case "submit" -> {
                    // "submit|imageUrl|w|h|offsetX|offsetY"
                    if (parts.length >= 6) {
                        try {
                            imageUrl       = parts[1];
                            displayWidth   = Float.parseFloat(parts[2]);
                            displayHeight  = Float.parseFloat(parts[3]);
                            displayOffsetX = Float.parseFloat(parts[4]);
                            displayOffsetY = Float.parseFloat(parts[5]);
                        } catch (NumberFormatException ignored) {}
                    }
                    fireClose("submit");
                }
                default -> {}
            }
        });
    }

    public String getImageUrl()       { return imageUrl; }
    public float  getDisplayWidth()   { return displayWidth; }
    public float  getDisplayHeight()  { return displayHeight; }
    public float  getDisplayOffsetX() { return displayOffsetX; }
    public float  getDisplayOffsetY() { return displayOffsetY; }
}
