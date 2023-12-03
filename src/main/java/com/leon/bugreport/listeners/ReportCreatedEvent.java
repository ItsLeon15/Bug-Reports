package com.leon.bugreport.listeners;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class ReportCreatedEvent extends Event {
	private static final HandlerList HANDLERS = new HandlerList();
	private final String reportDetails;

	public ReportCreatedEvent(String reportDetails) {
		this.reportDetails = reportDetails;
	}

	public String getReportDetails() {
		return reportDetails;
	}

	@Override
	public @NotNull HandlerList getHandlers() {
		return HANDLERS;
	}

	public static HandlerList getHandlerList() {
		return HANDLERS;
	}
}
