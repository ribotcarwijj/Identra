package dev.identra.app.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

/**
 * 0.7.36 Home status animation: a calm "radar" pulse around a badge.
 * OK = green rings + check mark, WARN = amber + "!", OFF = red + cross, CHECKING = spinning arc.
 */
public class StatusPulseView extends View {
    public static final int CHECKING = 0, OK = 1, WARN = 2, OFF = 3;

    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mark = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF arc = new RectF();
    private final Path path = new Path();
    private ValueAnimator anim;
    private float t;          // 0..1 animation phase
    private float appear = 1f; // pop-in when the state changes
    private int state = CHECKING;

    public StatusPulseView(Context c) { this(c, null); }
    public StatusPulseView(Context c, AttributeSet a) {
        super(c, a);
        ring.setStyle(Paint.Style.STROKE);
        mark.setStyle(Paint.Style.STROKE);
        mark.setStrokeCap(Paint.Cap.ROUND);
        mark.setStrokeJoin(Paint.Join.ROUND);
        mark.setColor(0xFFFFFFFF);
    }

    private int color() {
        switch (state) {
            case OK: return 0xFF2E9E5B;
            case WARN: return 0xFFE0A100;
            case OFF: return 0xFFD84343;
            default: return 0xFF6B7A99;
        }
    }

    public void setState(int s) {
        if (s == state) return;
        state = s;
        ValueAnimator pop = ValueAnimator.ofFloat(0.6f, 1.08f, 1f);
        pop.setDuration(450);
        pop.addUpdateListener(v -> { appear = (float) v.getAnimatedValue(); invalidate(); });
        pop.start();
        invalidate();
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        anim = ValueAnimator.ofFloat(0f, 1f);
        anim.setDuration(2400);
        anim.setRepeatCount(ValueAnimator.INFINITE);
        anim.setInterpolator(new LinearInterpolator());
        anim.addUpdateListener(v -> { t = (float) v.getAnimatedValue(); invalidate(); });
        anim.start();
    }

    @Override protected void onDetachedFromWindow() {
        if (anim != null) anim.cancel();
        super.onDetachedFromWindow();
    }

    @Override protected void onDraw(Canvas c) {
        float w = getWidth(), h = getHeight(), cx = w / 2f, cy = h / 2f;
        float max = Math.min(w, h) / 2f;
        float core = max * 0.42f * appear;
        int col = color();
        float d = getResources().getDisplayMetrics().density;

        if (state == CHECKING) {
            ring.setColor(col);
            ring.setStrokeWidth(3 * d);
            ring.setAlpha(255);
            float r = max * 0.62f;
            arc.set(cx - r, cy - r, cx + r, cy + r);
            c.drawArc(arc, t * 360f, 100f, false, ring);
            ring.setAlpha(60);
            c.drawCircle(cx, cy, r, ring);
            fill.setColor(col);
            fill.setAlpha(255);
            c.drawCircle(cx, cy, max * 0.18f, fill);
            return;
        }
        // two expanding rings, half a cycle apart (OFF pulses slower and fainter)
        ring.setColor(col);
        ring.setStrokeWidth(2 * d);
        for (int i = 0; i < 2; i++) {
            float p = (t + i * 0.5f) % 1f;
            float r = core + (max - core) * p;
            ring.setAlpha((int) ((state == OFF ? 110 : 170) * (1f - p)));
            c.drawCircle(cx, cy, r, ring);
        }
        fill.setColor(col);
        fill.setAlpha(255);
        float breathe = state == OK ? 1f + 0.04f * (float) Math.sin(t * Math.PI * 2) : 1f;
        c.drawCircle(cx, cy, core * breathe, fill);

        mark.setStrokeWidth(Math.max(2.5f * d, core * 0.16f));
        float s = core * 0.45f;
        path.reset();
        if (state == OK) {
            path.moveTo(cx - s, cy + s * 0.05f);
            path.lineTo(cx - s * 0.25f, cy + s * 0.7f);
            path.lineTo(cx + s, cy - s * 0.6f);
            c.drawPath(path, mark);
        } else if (state == WARN) {
            c.drawLine(cx, cy - s, cx, cy + s * 0.25f, mark);
            fill.setColor(0xFFFFFFFF);
            c.drawCircle(cx, cy + s * 0.8f, mark.getStrokeWidth() * 0.6f, fill);
        } else {
            c.drawLine(cx - s * 0.7f, cy - s * 0.7f, cx + s * 0.7f, cy + s * 0.7f, mark);
            c.drawLine(cx + s * 0.7f, cy - s * 0.7f, cx - s * 0.7f, cy + s * 0.7f, mark);
        }
    }
}
