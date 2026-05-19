package com.hayatkafeef.game.game;

import java.util.Random;

/**
 * Generates flavor + impact events as the player walks around.
 * Each call to maybeFire() returns a string for the TTS, or null.
 */
public class EventSystem {

    public interface Effects {
        void applyHaptic(int kind);   // 1=tick 2=warn 3=danger 4=stairs
        void applyDelay(int minutes);
    }

    private final Random rng = new Random();
    private long lastEventMs;

    public String maybeFire(GameState gs, Effects fx) {
        long now = System.currentTimeMillis();
        if (now - lastEventMs < 9000) return null;  // throttle
        if (rng.nextFloat() < 0.18f) {
            lastEventMs = now;
            return roll(gs, fx);
        }
        return null;
    }

    private String roll(GameState gs, Effects fx) {
        Scene.Id sc = gs.currentSceneId;
        int r = rng.nextInt(100);
        if (sc == Scene.Id.STREET) {
            if (r < 25) { fx.applyHaptic(2); return "تنبيه: دراجة هوائية تمر على يمينك بسرعة."; }
            if (r < 45) { fx.applyHaptic(3); return "احذر، سيارة تتوقف بجانبك على اليسار."; }
            if (r < 65) return "صوت بائع متجوّل ينادي من بعيد.";
            if (r < 80) { fx.applyDelay(2); return "قطرات مطر خفيفة تبدأ بالنزول."; }
            return "إشارة المرور تصفر للعبور الآن.";
        }
        if (sc == Scene.Id.UNIVERSITY) {
            if (r < 25) { fx.applyHaptic(4); return "تنبيه: يوجد درج صغير أمامك. خطوتان للأسفل."; }
            if (r < 45) { gs.flags.add("elevator_broken"); return "إعلان عبر السماعات: المصعد معطل اليوم."; }
            if (r < 65) { fx.applyDelay(3); return "ازدحام أمام القاعة، انتظر قليلًا."; }
            if (r < 80) return "صوت طباشير على السبورة قادم من الداخل.";
            return "زميلة تسأل: هل وجدت القاعة الجديدة؟";
        }
        if (sc == Scene.Id.CAFE) {
            if (r < 30) return "ماكينة القهوة تطحن قهوة جديدة، صوت قوي للحظات.";
            if (r < 55) return "موسيقى هادئة تتغير لأغنية أخرى.";
            if (r < 75) return "صوت ضحكات من الطاولة المجاورة.";
            return "النادل ينادي: من طلب لاتيه؟";
        }
        // HOME
        if (r < 30) return "ساعة الحائط تدق.";
        if (r < 55) return "هاتفك يهتز على المكتب.";
        if (r < 75) return "صوت غسالة تنتهي من دورتها في غرفة بعيدة.";
        return "نسمة من نافذة مفتوحة.";
    }
}
