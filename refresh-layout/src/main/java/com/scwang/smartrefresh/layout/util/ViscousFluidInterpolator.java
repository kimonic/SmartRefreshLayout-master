package com.scwang.smartrefresh.layout.util;

import android.view.animation.Interpolator;

/**
 * 粘性流体插值器
 */

public class ViscousFluidInterpolator implements Interpolator {
    /**
     * Controls the viscous fluid effect (how much of it).
     * 粘性流体系数
     */
    private static final float VISCOUS_FLUID_SCALE = 8.0f;

    /**
     * 正常状态的粘性流体系数
     */
    private static final float VISCOUS_FLUID_NORMALIZE;
    /**
     * 粘性流体偏移量
     */
    private static final float VISCOUS_FLUID_OFFSET;

    static {
        // must be set to 1.0 (used in viscousFluid())
        VISCOUS_FLUID_NORMALIZE = 1.0f / viscousFluid(1.0f);
        // account for very small floating-point error
        VISCOUS_FLUID_OFFSET = 1.0f - VISCOUS_FLUID_NORMALIZE * viscousFluid(1.0f);
    }

    /**返回粘性系数*/
    private static float viscousFluid(float x) {
        x *= VISCOUS_FLUID_SCALE;
        if (x < 1.0f) {
            /*参数类型：System.Double   指定幂的数字
            返回值   类型：System.Double  数字 e 的 d 次幂。
            如果 d 等于 NaN 或 PositiveInfinity，则返回该值。
            如果 d 等于 NegativeInfinity，则返回 0。 */
            x -= (1.0f - (float) Math.exp(-x));
        } else {
            float start = 0.36787944117f;   // 1/e == exp(-1)
            x = 1.0f - (float) Math.exp(1.0f - x);
            x = start + x * (1.0f - start);
        }
        return x;
    }

    /**获取插值*/
    @Override
    public float getInterpolation(float input) {
        final float interpolated = VISCOUS_FLUID_NORMALIZE * viscousFluid(input);
        if (interpolated > 0) {
            return interpolated + VISCOUS_FLUID_OFFSET;
        }
        return interpolated;
    }
}