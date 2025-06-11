package com.aros.apron.tools;

/* loaded from: classes.dex */
public class PIDControl {
    private static final float AC_PID_FILT_HZ_DEFAULT = 20.0f;
    private static final float AC_PID_FILT_HZ_MIN = 0.01f;
    private float iMax;
    private float mDt;
    private float mFF;
    private float mFoltHz;
    private float mKd;
    private float mKi;
    private float mKp;
    private boolean mResetFilter;
    private float mIntegrator = 0.0f;
    private float mInput = 0.0f;
    private float mDerivative = 0.0f;

    public float getmDt() {
        return this.mDt;
    }

    public void setmDt(float mDt) {
        this.mDt = mDt;
    }

    public PIDControl(float kp, float ki, float kd, float initial_imax, float initial_filt_hz, float dt) {
        this.mResetFilter = true;
        this.mDt = dt;
        this.mKp = kp;
        this.mKi = ki;
        this.mKd = kd;
        this.iMax = Math.abs(initial_imax);
        filtHz(initial_filt_hz);
        this.mResetFilter = true;
    }

    private void filtHz(float hz) {
        float abs = Math.abs(hz);
        this.mFoltHz = abs;
        if (abs < AC_PID_FILT_HZ_MIN) {
            this.mFoltHz = AC_PID_FILT_HZ_MIN;
        }
    }

    public void setInputFilterAll(float input) {
        if (Math.abs(input) < 1.0E-5d) {
            return;
        }
        if (this.mResetFilter) {
            this.mResetFilter = false;
            this.mInput = input;
            this.mDerivative = 0.0f;
        }
        float filtAlpha = getFiltAlpha();
        float f = this.mInput;
        float f2 = filtAlpha * (input - f);
        this.mInput = f + f2;
        float f3 = this.mDt;
        if (f3 > 0.0f) {
            this.mDerivative = f2 / f3;
        }
    }

    public void setInputFilter_d(float input) {
        if (Math.abs(input) < 1.0E-5d) {
            return;
        }
        if (this.mResetFilter) {
            this.mResetFilter = false;
            this.mDerivative = 0.0f;
        }
        float f = this.mDt;
        if (f > 0.0f) {
            this.mDerivative += getFiltAlpha() * (((input - this.mInput) / f) - this.mDerivative);
        }
        this.mInput = input;
    }

    float get_p() {
        return this.mInput * this.mKp;
    }

    float get_i() {
        if (Math.abs(this.mKi) <= 0.001d || Math.abs(this.mDt) <= 0.001d) {
            return 0.0f;
        }
        float f = this.mIntegrator + (this.mInput * this.mKi * this.mDt);
        this.mIntegrator = f;
        float f2 = this.iMax;
        if (f < (-f2)) {
            this.mIntegrator = -f2;
        } else if (f > f2) {
            this.mIntegrator = f2;
        }
        return this.mIntegrator;
    }

    float get_d() {
        return this.mKd * this.mDerivative;
    }

    float get_pi() {
        return get_p() + get_i();
    }

    public float get_pid() {
        return get_p() + get_i() + get_d();
    }

    public void reset() {
        this.mIntegrator = 0.0f;
        this.mInput = 0.0f;
        this.mDerivative = 0.0f;
    }

    private float getFiltAlpha() {
        float f = this.mFoltHz;
        if (f < 0.001d) {
            return 1.0f;
        }
        float f2 = 1.0f / (f * 6.2831855f);
        float f3 = this.mDt;
        return f3 / (f2 + f3);
    }
}