package com.gt30pro.launcher;

/** Recentered, filtered gravity-based reflection motion in screen coordinates. */
final class GlassParallaxMotion {
    private boolean initialized;
    private float baseX,baseY,filteredX,filteredY,x,y;
    private int rotation=-1;
    static float clamp(float value) { return Math.max(-1f,Math.min(1f,value)); }
    void reset() { initialized=false; rotation=-1; x=0; y=0; }
    boolean update(float sx,float sy,int displayRotation) {
        if(Float.isNaN(sx) || Float.isNaN(sy) || Float.isInfinite(sx) || Float.isInfinite(sy)
                || Math.abs(sx)>20f || Math.abs(sy)>20f || displayRotation<0 || displayRotation>3) return false;
        float nx,ny;
        switch(displayRotation) {
            case 1: nx=-sy; ny=sx; break;
            case 2: nx=-sx; ny=-sy; break;
            case 3: nx=sy; ny=-sx; break;
            default: nx=sx; ny=sy;
        }
        if(!initialized || rotation!=displayRotation) {
            initialized=true; rotation=displayRotation; baseX=filteredX=nx; baseY=filteredY=ny; x=y=0f;
        } else {
            filteredX+=(nx-filteredX)*.2f; filteredY+=(ny-filteredY)*.2f;
            x=clamp((filteredX-baseX)/4f); y=clamp((filteredY-baseY)/4f);
        }
        return true;
    }
    float x() { return x; }
    float y() { return y; }
}
