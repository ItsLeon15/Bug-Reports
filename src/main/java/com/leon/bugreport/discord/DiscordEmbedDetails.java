package com.leon.bugreport.discord;

public class DiscordEmbedDetails {
	private String name;
	private Integer id;
	private String value;
	private Boolean inline;

	public DiscordEmbedDetails(String name, Integer id, String value, Boolean inline) {
		this.name = name;
		this.id = id;
		this.value = value;
		this.inline = inline;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public Boolean getInline() {
		return inline;
	}

	public void setInline(Boolean inline) {
		this.inline = inline;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
