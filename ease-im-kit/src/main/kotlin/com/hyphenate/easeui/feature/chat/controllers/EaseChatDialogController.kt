package com.hyphenate.easeui.feature.chat.controllers

import android.content.Context
import com.hyphenate.easeui.R
import com.hyphenate.easeui.common.dialog.CustomDialog
import com.hyphenate.easeui.common.impl.OnSuccess
import com.hyphenate.easeui.feature.chat.widgets.EaseChatLayout

class EaseChatDialogController(
    private val context: Context,
    private val chatLayout: EaseChatLayout
) {

    fun showDeleteDialog(context: Context, onSuccess: OnSuccess) {
        val clearDialog = CustomDialog(
            context = context,
            title = context.resources.getString(R.string.ease_chat_dialog_delete_title),
            subtitle = context.resources.getString(R.string.ease_chat_dialog_delete_content),
            isEditTextMode = false,
            onLeftButtonClickListener = {

            },
            onRightButtonClickListener = {
               onSuccess.invoke()
            }
        )
        clearDialog.show()
    }

    fun showRecallDialog(context: Context, onSuccess: OnSuccess) {
        val clearDialog = CustomDialog(
            context = context,
            title = context.resources.getString(R.string.ease_chat_dialog_recall_title),
            isEditTextMode = false,
            onLeftButtonClickListener = {

            },
            onRightButtonClickListener = {
               onSuccess.invoke()
            }
        )
        clearDialog.show()
    }

    fun showResendDialog(context: Context, onSuccess: OnSuccess) {
        val resendDialog = CustomDialog(
            context = context,
            title = context.resources.getString(R.string.ease_chat_dialog_resend_title),
            isEditTextMode = false,
            onLeftButtonClickListener = {

            },
            onRightButtonClickListener = {
                onSuccess.invoke()
            }
        )
        resendDialog.show()
    }
}