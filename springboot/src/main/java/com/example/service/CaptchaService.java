package com.example.service;

import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CaptchaService {
    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final long TTL_MILLIS = 5 * 60 * 1000L;
    private static final int WIDTH = 132;
    private static final int HEIGHT = 44;

    private final SecureRandom random = new SecureRandom();
    private final Map<String, CaptchaEntry> store = new ConcurrentHashMap<>();

    public Map<String, Object> generate() {
        cleanupExpired();

        String code = randomCode();
        String captchaId = UUID.randomUUID().toString().replace("-", "");
        store.put(captchaId, new CaptchaEntry(code, System.currentTimeMillis() + TTL_MILLIS));

        Map<String, Object> data = new HashMap<>();
        data.put("captchaId", captchaId);
        data.put("image", "data:image/png;base64," + renderBase64(code));
        data.put("expiresIn", TTL_MILLIS / 1000);
        return data;
    }

    public boolean validate(String captchaId, String input) {
        if (isBlank(captchaId) || isBlank(input)) {
            return false;
        }

        CaptchaEntry entry = store.remove(captchaId);
        if (entry == null || entry.expiresAt < System.currentTimeMillis()) {
            return false;
        }
        return entry.code.equalsIgnoreCase(input.trim());
    }

    private String randomCode() {
        StringBuilder builder = new StringBuilder(4);
        for (int i = 0; i < 4; i++) {
            builder.append(CHARS.charAt(random.nextInt(CHARS.length())));
        }
        return builder.toString();
    }

    private String renderBase64(String code) {
        try {
            BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(244, 248, 255));
            g.fillRoundRect(0, 0, WIDTH, HEIGHT, 12, 12);

            for (int i = 0; i < 5; i++) {
                g.setColor(new Color(148 + random.nextInt(60), 163 + random.nextInt(45), 184 + random.nextInt(45), 120));
                g.setStroke(new BasicStroke(1.2f));
                int y1 = 8 + random.nextInt(HEIGHT - 16);
                int y2 = 8 + random.nextInt(HEIGHT - 16);
                g.drawLine(8, y1, WIDTH - 8, y2);
            }

            Font font = new Font(Font.SANS_SERIF, Font.BOLD, 27);
            g.setFont(font);
            FontMetrics metrics = g.getFontMetrics();
            int charWidth = WIDTH / 5;
            for (int i = 0; i < code.length(); i++) {
                String value = String.valueOf(code.charAt(i));
                int x = 15 + i * charWidth + random.nextInt(5);
                int y = 30 + random.nextInt(5);
                double angle = Math.toRadians(-12 + random.nextInt(25));

                g.rotate(angle, x + metrics.stringWidth(value) / 2.0, y - metrics.getAscent() / 2.0);
                g.setColor(new Color(15 + random.nextInt(35), 23 + random.nextInt(45), 42 + random.nextInt(55)));
                g.drawString(value, x, y);
                g.rotate(-angle, x + metrics.stringWidth(value) / 2.0, y - metrics.getAscent() / 2.0);
            }

            for (int i = 0; i < 18; i++) {
                g.setColor(new Color(37, 99, 235, 80 + random.nextInt(80)));
                g.fillOval(random.nextInt(WIDTH), random.nextInt(HEIGHT), 2, 2);
            }
            g.dispose();

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(image, "png", output);
            return Base64.getEncoder().encodeToString(output.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("生成验证码失败");
        }
    }

    private void cleanupExpired() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<String, CaptchaEntry>> iterator = store.entrySet().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getValue().expiresAt < now) {
                iterator.remove();
            }
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static class CaptchaEntry {
        private final String code;
        private final long expiresAt;

        private CaptchaEntry(String code, long expiresAt) {
            this.code = code;
            this.expiresAt = expiresAt;
        }
    }
}
