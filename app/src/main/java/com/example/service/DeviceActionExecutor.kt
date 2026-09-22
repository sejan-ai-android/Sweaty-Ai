package com.example.service

import android.content.Context
import com.example.data.db.ActionLogDao
import com.example.data.model.ActionLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class ExecutionResult(
    val success: Boolean,
    val message: String,
    val screenData: String? = null,
    val requiresConfirmation: Boolean = false,
    val pendingAction: String? = null
)

class DeviceActionExecutor(
    private val context: Context,
    private val actionLogDao: ActionLogDao
) {

    suspend fun executeActionJson(jsonString: String): ExecutionResult = withContext(Dispatchers.Default) {
        val service = SweatyAccessibilityService.instance
        if (service == null) {
            val failure = "Accessibility Service is not active. Please enable it in Settings > Accessibility."
            logAction("service_check", failure, false)
            return@withContext ExecutionResult(false, failure)
        }

        try {
            val json = JSONObject(jsonString)
            val function = json.optString("function")

            // Guardrail check for sensitive operations
            if (isSensitiveAction(json)) {
                return@withContext ExecutionResult(
                    success = false,
                    message = "Safety Confirmation Needed: This action may modify sensitive data.",
                    requiresConfirmation = true,
                    pendingAction = jsonString
                )
            }

            when (function) {
                "go_back" -> {
                    val ok = service.performBack()
                    logAction("go_back", "Navigated back", ok)
                    ExecutionResult(ok, if (ok) "Navigated back" else "Back action failed")
                }
                "go_home" -> {
                    val ok = service.performHome()
                    logAction("go_home", "Navigated home", ok)
                    ExecutionResult(ok, if (ok) "Navigated home" else "Home action failed")
                }
                "open_recents" -> {
                    val ok = service.performRecents()
                    logAction("open_recents", "Opened recents", ok)
                    ExecutionResult(ok, if (ok) "Opened recent apps" else "Failed to open recents")
                }
                "open_notifications" -> {
                    val ok = service.performNotifications()
                    logAction("open_notifications", "Opened notifications", ok)
                    ExecutionResult(ok, if (ok) "Opened notification shade" else "Failed to open notifications")
                }
                "open_quick_settings" -> {
                    val ok = service.performQuickSettings()
                    logAction("open_quick_settings", "Opened quick settings", ok)
                    ExecutionResult(ok, if (ok) "Opened quick settings" else "Failed to open quick settings")
                }
                "lock_screen" -> {
                    val ok = service.performLockScreen()
                    logAction("lock_screen", "Locked device screen", ok)
                    ExecutionResult(ok, if (ok) "Screen locked" else "Lock screen not supported")
                }
                "take_screenshot" -> {
                    val ok = service.performTakeScreenshot()
                    logAction("take_screenshot", "Captured screenshot", ok)
                    ExecutionResult(ok, if (ok) "Screenshot captured" else "Screenshot action failed")
                }
                "tap" -> {
                    val x = json.optDouble("x", 500.0).toFloat()
                    val y = json.optDouble("y", 1000.0).toFloat()
                    val ok = service.tap(x, y)
                    logAction("tap", "Tapped at ($x, $y)", ok)
                    ExecutionResult(ok, "Tapped coordinates ($x, $y)")
                }
                "long_press" -> {
                    val x = json.optDouble("x", 500.0).toFloat()
                    val y = json.optDouble("y", 1000.0).toFloat()
                    val ok = service.longPress(x, y)
                    logAction("long_press", "Long pressed at ($x, $y)", ok)
                    ExecutionResult(ok, "Long pressed at ($x, $y)")
                }
                "swipe" -> {
                    val x1 = json.optDouble("x1", 500.0).toFloat()
                    val y1 = json.optDouble("y1", 1500.0).toFloat()
                    val x2 = json.optDouble("x2", 500.0).toFloat()
                    val y2 = json.optDouble("y2", 500.0).toFloat()
                    val duration = json.optLong("duration", 300L)
                    val ok = service.swipe(x1, y1, x2, y2, duration)
                    logAction("swipe", "Swiped from ($x1,$y1) to ($x2,$y2)", ok)
                    ExecutionResult(ok, "Swiped")
                }
                "click_by_text", "find_and_click" -> {
                    val text = json.optString("text")
                    val ok = service.clickByText(text)
                    logAction("click_by_text", "Clicked text '$text'", ok)
                    ExecutionResult(ok, if (ok) "Clicked '$text'" else "Could not find clickable '$text'")
                }
                "click_by_id" -> {
                    val viewId = json.optString("viewId")
                    val ok = service.clickById(viewId)
                    logAction("click_by_id", "Clicked view ID '$viewId'", ok)
                    ExecutionResult(ok, if (ok) "Clicked view ID" else "View ID not clickable")
                }
                "type_text" -> {
                    val text = json.optString("text")
                    val ok = service.typeText(text)
                    logAction("type_text", "Typed '$text'", ok)
                    ExecutionResult(ok, if (ok) "Typed '$text'" else "No active editable input field")
                }
                "scroll_forward" -> {
                    val ok = service.scrollForward()
                    logAction("scroll_forward", "Scrolled down/forward", ok)
                    ExecutionResult(ok, if (ok) "Scrolled down" else "Screen is not scrollable")
                }
                "scroll_backward" -> {
                    val ok = service.scrollBackward()
                    logAction("scroll_backward", "Scrolled up/backward", ok)
                    ExecutionResult(ok, if (ok) "Scrolled up" else "Screen is not scrollable")
                }
                "read_screen" -> {
                    val screenContent = service.readScreen()
                    logAction("read_screen", "Read screen content", true)
                    ExecutionResult(true, "Screen read successfully", screenData = screenContent)
                }
                "open_app" -> {
                    val pkg = json.optString("packageName")
                    val ok = service.openApp(context, pkg)
                    logAction("open_app", "Opened package $pkg", ok)
                    ExecutionResult(ok, if (ok) "Opened application" else "App $pkg not installed")
                }
                "multi_step" -> {
                    val stepsArray = json.optJSONArray("steps") ?: JSONArray()
                    val stepResults = executeMultiStep(stepsArray)
                    stepResults
                }
                else -> {
                    ExecutionResult(false, "Unknown function: $function")
                }
            }
        } catch (e: Exception) {
            val err = "Action execution error: ${e.message}"
            logAction("error", err, false)
            ExecutionResult(false, err)
        }
    }

    private suspend fun executeMultiStep(steps: JSONArray): ExecutionResult {
        val total = steps.length()
        for (i in 0 until total) {
            val step = steps.optJSONObject(i) ?: continue
            val result = executeActionJson(step.toString())
            if (!result.success) {
                return ExecutionResult(false, "Multi-step halted at step ${i + 1}: ${result.message}")
            }
            delay(600) // Delay between autonomous steps
        }
        return ExecutionResult(true, "Completed all $total autonomous steps successfully.")
    }

    private fun isSensitiveAction(json: JSONObject): Boolean {
        val text = json.toString().lowercase()
        return text.contains("payment") ||
                text.contains("pin") ||
                text.contains("password") ||
                text.contains("delete all") ||
                text.contains("factory reset") ||
                text.contains("transfer money")
    }

    private suspend fun logAction(action: String, details: String, success: Boolean) {
        try {
            actionLogDao.insertLog(ActionLog(actionType = action, details = details, success = success))
        } catch (_: Exception) {}
    }
}
