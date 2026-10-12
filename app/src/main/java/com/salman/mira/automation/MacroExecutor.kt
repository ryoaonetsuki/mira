package com.salman.mira.automation

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Runs a macro's steps against the live accessibility service. */
object MacroExecutor {

    sealed interface Result {
        data object Ok : Result
        data class Failed(val stepIndex: Int, val reason: String) : Result
        data object NoService : Result
    }

    suspend fun run(macro: Macro, onStep: (Int, MacroStep) -> Unit = { _, _ -> }): Result =
        withContext(Dispatchers.IO) {
            val svc = MiraAccessibilityService.instance ?: return@withContext Result.NoService
            macro.steps.forEachIndexed { i, step ->
                onStep(i, step)
                val ok = runCatching {
                    when (step.action) {
                        "tap_text" -> svc.tapText(step.arg)
                        "type_text" -> svc.typeText(step.arg)
                        "open_app" -> svc.openApp(step.arg)
                        "wait_text" -> svc.waitForText(step.arg, step.timeoutMs)
                        "back" -> svc.pressBack()
                        "scroll" -> svc.scrollDown()
                        "delay" -> {
                            Thread.sleep(step.arg.toLongOrNull() ?: 1000); true
                        }.invoke()
                        else -> false
                    }
                }.getOrDefault(false)
                if (!ok) return@withContext Result.Failed(i, "Step ${i + 1} (${step.action}) failed")
                // Small settle delay between steps.
                Thread.sleep(600)
            }
            Result.Ok
        }
}
