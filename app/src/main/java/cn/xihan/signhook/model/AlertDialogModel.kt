package cn.xihan.signhook.model


data class AlertDialogModel(
    val title: String,
    val message: String,
    val positiveMessage: String? = null,
    val positiveObject: Any? = null,
    val negativeMessage: String? = null,
    val negativeObject: Any? = null,
)
