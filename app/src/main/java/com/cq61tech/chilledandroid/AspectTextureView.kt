package com.cq61tech.chilledandroid

import android.content.Context
import android.view.TextureView

/** 854x480 oranini koruyan video yuzeyi (dikey telefonda ust-alt siyah bant). */
class AspectTextureView(context: Context) : TextureView(context) {
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(w, (w * 480f / 854f).toInt())
    }
}
