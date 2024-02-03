package com.leon.bugreport.extensions;

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

	public static <L, R> BugReportPair<L, R> of(L first, R second) {
		return new BugReportPair<>(first, second);
	}
}
