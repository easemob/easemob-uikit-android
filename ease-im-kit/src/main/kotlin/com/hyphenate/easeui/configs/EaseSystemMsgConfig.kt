package com.hyphenate.easeui.configs

import com.hyphenate.easeui.EaseIM
import com.hyphenate.easeui.R

class EaseSystemMsgConfig {

    var useDefaultContactSystemMsg: Boolean = true
        get() {
            if (field) return field
            if (EaseIM.isInited()) {
                return EaseIM.getContext()?.resources?.getBoolean(R.bool.ease_use_default_contact_system_msg) ?: false
            }
            return false
        }

}