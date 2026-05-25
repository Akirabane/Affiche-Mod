package com.vikingposter.client.web;

import com.cinemamod.mcef.MCEF;
import com.cinemamod.mcef.MCEFBrowser;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.vikingposter.blockentity.PosterBlockEntity;
import com.vikingposter.network.C2SUpdatePosterPacket;
import com.vikingposter.network.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PosterEditScreen extends Screen {

    private final BlockPos    pos;
    private final CompoundTag existingData;

    private MCEFBrowser      browser;
    private PosterEditBridge bridge;

    private PosterEditScreen(BlockPos pos, CompoundTag existingData) {
        super(Component.literal("Viking Poster"));
        this.pos          = pos;
        this.existingData = existingData;
    }

    public static void open(BlockPos pos, CompoundTag data) {
        Minecraft.getInstance().setScreen(new PosterEditScreen(pos, data));
    }

    @Override
    protected void init() {
        super.init();
        if (!PosterMCEFBootstrap.isReady()) return;

        String url = PosterMCEFBootstrap.urlFor("poster_edit.html");

        bridge = new PosterEditBridge(this::handleBridgeClose);

        if (browser == null) {
            browser = MCEF.createBrowser(url, true);
            bridge.attach(browser);
        }
        resizeBrowser();
        try { browser.setFocus(true); } catch (Throwable ignored) {}

        injectDataWhenReady();
    }

    private void injectDataWhenReady() {
        new Thread(() -> {
            try { Thread.sleep(800); } catch (InterruptedException ignored) {}
            Minecraft.getInstance().execute(this::injectData);
        }, "PosterDataInjector").start();
    }

    private void injectData() {
        if (browser == null) return;
        PosterBlockEntity dummy = new PosterBlockEntity(pos, null);
        if (existingData != null && !existingData.isEmpty()) dummy.readData(existingData);

        String js = String.format(
            "if(typeof loadData==='function') loadData(%s,%s,%s,%s,%s);",
            jsStr(dummy.getImageUrl()),
            dummy.getDisplayWidth(),
            dummy.getDisplayHeight(),
            dummy.getDisplayOffsetX(),
            dummy.getDisplayOffsetY()
        );
        browser.executeJavaScript(js, "", 0);
    }

    private static String jsStr(String s) {
        return "'" + s.replace("\\", "\\\\").replace("'", "\\'").replace("\n", "\\n") + "'";
    }

    private void handleBridgeClose(String reason) {
        if ("submit".equals(reason) && bridge != null) {
            NetworkHandler.sendToServer(new C2SUpdatePosterPacket(
                pos,
                bridge.getImageUrl(),
                bridge.getDisplayWidth(),
                bridge.getDisplayHeight(),
                bridge.getDisplayOffsetX(),
                bridge.getDisplayOffsetY()
            ));
        }

        if (minecraft != null) minecraft.execute(() -> minecraft.setScreen(null));
    }

    @Override
    public void resize(Minecraft mc, int w, int h) {
        super.resize(mc, w, h);
        resizeBrowser();
    }

    private void resizeBrowser() {
        if (browser == null) return;
        double s = minecraft.getWindow().getGuiScale();
        int pw = (int)(this.width  * s);
        int ph = (int)(this.height * s);
        if (pw > 0 && ph > 0) browser.resize(pw, ph);
    }

    @Override
    public void onClose() {
        if (bridge != null) bridge.fireClose("escape");
        super.onClose();
    }

    @Override
    public void removed() {
        if (browser != null) {
            if (bridge != null) bridge.detach();
            browser.close();
            browser = null;
        }
        super.removed();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float pt) {
        if (browser == null) {
            String msg = PosterMCEFBootstrap.isReady()
                ? "Chargement de l'éditeur..."
                : "Initialisation MCEF (premier démarrage — téléchargement Chromium)...";
            g.drawCenteredString(font, msg, this.width/2, this.height/2, 0xFFFFFF);
            return;
        }
        double s = minecraft.getWindow().getGuiScale();
        int bw = (int)(this.width  * s);
        int bh = (int)(this.height * s);
        int lw = Math.min(720, (int)(bw * 0.94));
        int ly = (int)(bh * 0.03);
        int lh = bh - ly - (int)(bh * 0.02);
        int lx = (bw - lw) / 2;
        int gx = (int)(lx / s);
        int gy = (int)(ly / s);
        int gw = (int)(lw / s);
        int gh = (int)(lh / s);
        g.enableScissor(gx, gy, gx + gw, gy + gh);
        drawBrowserTexture(browser.getRenderer().getTextureID());
        g.disableScissor();
    }

    private void drawBrowserTexture(int texId) {
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, texId);

        Tesselator t = Tesselator.getInstance();
        BufferBuilder b = t.getBuilder();
        b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        b.vertex(0,           this.height, 0).uv(0,1).color(255,255,255,255).endVertex();
        b.vertex(this.width,  this.height, 0).uv(1,1).color(255,255,255,255).endVertex();
        b.vertex(this.width,  0,           0).uv(1,0).color(255,255,255,255).endVertex();
        b.vertex(0,           0,           0).uv(0,0).color(255,255,255,255).endVertex();
        t.end();

        RenderSystem.setShaderTexture(0, 0);
        RenderSystem.enableDepthTest();
    }

    private int px(double mx) { return (int)(mx * minecraft.getWindow().getGuiScale()); }
    private int py(double my) { return (int)(my * minecraft.getWindow().getGuiScale()); }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (browser != null) {
            try { browser.setFocus(true); } catch (Throwable ignored) {}
            browser.sendMouseMove(px(mx), py(my));
            browser.sendMousePress(px(mx), py(my), btn);
        }
        return true;
    }

    @Override
    public boolean mouseReleased(double mx, double my, int btn) {
        if (browser != null) {
            browser.sendMouseMove(px(mx), py(my));
            browser.sendMouseRelease(px(mx), py(my), btn);
        }
        return true;
    }

    @Override
    public void mouseMoved(double mx, double my) {
        if (browser != null) browser.sendMouseMove(px(mx), py(my));
    }

    @Override
    public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
        if (browser != null) browser.sendMouseMove(px(mx), py(my));
        return true;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (browser != null) browser.sendMouseWheel(px(mx), py(my), delta * 100.0, 0);
        return true;
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (key == 256) { onClose(); return true; }
        if (browser != null) browser.sendKeyPress(key, scan, mods);
        return true;
    }

    @Override
    public boolean keyReleased(int key, int scan, int mods) {
        if (browser != null) browser.sendKeyRelease(key, scan, mods);
        return true;
    }

    @Override
    public boolean charTyped(char c, int mods) {
        if (browser != null) browser.sendKeyTyped(c, mods);
        return true;
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
