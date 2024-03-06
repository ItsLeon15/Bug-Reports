package com.leon.bugreport.extensions;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public class BugReportPair<L, R> {
	private final L first;
	private final R second;

	public BugReportPair(L first, R second) {
		this.first = first;
		this.second = second;
	}

	public L getFirst() {
		return first;
	}

	public R getSecond() {
		return second;
	}

	@Contract(value = "_, _ -> new", pure = true)
	public static <L, R> @NotNull BugReportPair<L, R> of(L first, R second) {
		return new BugReportPair<>(first, second);
	}
}
