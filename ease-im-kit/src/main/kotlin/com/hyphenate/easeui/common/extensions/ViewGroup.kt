package com.hyphenate.easeui.common.extensions

import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.lifecycleScope

val ViewGroup.lifecycleScope: LifecycleCoroutineScope
    get() = (context as AppCompatActivity).lifecycleScope ?: throw IllegalStateException("ViewGroup's context is not an AppCompatActivity")