package com.hyphenate.easeui.configs

import com.hyphenate.easeui.EaseIM
import com.hyphenate.easeui.R

class EaseMultiDeviceEventConfig {
    var useDefaultMultiDeviceContactEvent: Boolean = true
        get() {
            if (field) return field
            if (EaseIM.isInited()) {
                return EaseIM.getContext()?.resources?.getBoolean(R.bool.ease_default_multi_device_contact_event) ?: false
            }
            return false
        }

    var useDefaultMultiDeviceGroupEvent: Boolean = true
        get() {
            if (field) return field
            if (EaseIM.isInited()) {
                return EaseIM.getContext()?.resources?.getBoolean(R.bool.ease_default_multi_device_group_event) ?: false
            }
            return false
        }
}