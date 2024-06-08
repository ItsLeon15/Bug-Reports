package com.bugreportmc.bugreport.extensions

import org.jetbrains.annotations.Contract

class BugReportPair<L, R>(val first: L, val second: R) {
	companion object {
		@Contract(value = "_, _ -> new", pure = true)
		fun <L, R> of(first: L, second: R): BugReportPair<L, R> {
			return BugReportPair(first, second)
		}
	}
}
