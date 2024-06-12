
import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.app.Notification
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import com.aros.apron.tools.LogUtil


class BluetoothAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        LogUtil.log("BluetoothAccessibilityService", "无障碍服务连接成功")
    }

    @SuppressLint("SwitchIntDef")
    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        when (event.eventType) {
            //当通知栏发生改变时
            AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED -> {
                if (event.parcelableData == null) {
                    return
                }
                val notification: Notification = event.parcelableData as Notification ?: return
                val title: String? = notification.extras.getString(Notification.EXTRA_TITLE)
                val text: String? = notification.extras.getString(Notification.EXTRA_TEXT)
                if (title != null && text != null
                    && (title.contains(PAIR_REQUEST) || text.contains(PAIR_REQUEST) || text.contains(PAIR_CONNECT))
                ) {
                    notification.contentIntent.send()
                }
            }
            //当窗口内容改变
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                if (event.source == null) {
                    return
                }
                val pairNodes = event.source!!.findAccessibilityNodeInfosByText(PAIR)
                autoClick(pairNodes)
                val confirmNodes = event.source!!.findAccessibilityNodeInfosByText(CONFIRM)
                autoClick(confirmNodes)
                val pairConnectNodes = event.source!!.findAccessibilityNodeInfosByText(PAIR_CONNECT)
                autoClick(pairConnectNodes)
            }
            //处理usb连接自动点击(默认情况下用于该USB配件)
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                if (event.source == null) {
                    return
                }
                val alwaysUseNodes = event.source!!.findAccessibilityNodeInfosByViewId("android:id/alwaysUse")
                autoClick(alwaysUseNodes)
                val confirmNodes = event.source!!.findAccessibilityNodeInfosByText(CONFIRM)
                autoClick(confirmNodes)
            }
        }
    }

    private fun autoClick(stopNodes: MutableList<AccessibilityNodeInfo>?) {
        // 遍历节点
        if (stopNodes != null && stopNodes.isNotEmpty()) {
            for (i in 0 until stopNodes.size) {
                val node: AccessibilityNodeInfo = stopNodes[i]
                // 判断按钮类型
                if (node.className == Button::class.java.name || node.className == TextView::class.java.name || node.className == CheckBox::class.java.name) {
                    //可用则模拟点击
                    if (node.isEnabled) {
                        if (node.className == CheckBox::class.java.name && !node.isChecked) {
                            node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                            return
                        }
                        node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    }
                }
            }
        }
    }

    override fun onInterrupt() {
    }

    companion object {
        const val PAIR = "配对"
        const val PAIR_REQUEST = "配对请求"
        const val PAIR_CONNECT = "配对和连接"
        const val CONFIRM = "确定"
    }
}