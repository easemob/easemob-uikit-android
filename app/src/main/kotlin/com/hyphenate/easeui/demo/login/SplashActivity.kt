package com.hyphenate.easeui.demo.login

import android.animation.Animator
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.hyphenate.easeui.demo.R
import com.hyphenate.easeui.demo.base.BaseInitActivity
import com.hyphenate.easeui.demo.databinding.DemoSplashActivityBinding

class SplashActivity : BaseInitActivity<DemoSplashActivityBinding>() {
    private var ivSplash: ImageView? = null
    private var ivProduct: ImageView? = null

    override fun getViewBinding(inflater: LayoutInflater): DemoSplashActivityBinding? {
        return DemoSplashActivityBinding.inflate(inflater)
    }

    override fun setActivityTheme() {
        setFitSystemForTheme(false, ContextCompat.getColor(this, R.color.transparent), true)
    }

    override fun initView(savedInstanceState: Bundle?) {
        super.initView(savedInstanceState)
        ivSplash = findViewById(R.id.iv_splash)
        ivProduct = findViewById(R.id.iv_product)
    }

    override fun initData() {
        super.initData()
        ivSplash!!.animate()
            .alpha(1f)
            .setDuration(500)
            .setListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator) {}
                override fun onAnimationEnd(animation: Animator) {
                    checkIfAgreePrivacy()
                }

                override fun onAnimationCancel(animation: Animator) {}
                override fun onAnimationRepeat(animation: Animator) {}
            })
            .start()
        ivProduct!!.animate()
            .alpha(1f)
            .setDuration(500)
            .start()
    }

    private fun checkIfAgreePrivacy() {
        loginSDK()
    }

    private fun loginSDK() {
        LoginActivity.startAction(mContext)
        finish()
    }
}
