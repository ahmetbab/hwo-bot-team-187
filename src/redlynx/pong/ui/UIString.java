package redlynx.pong.ui;

import java.awt.Color;

import redlynx.pong.util.Vector2i;

public class UIString {
	private String text;
	private Vector2i pos;
	private Color color;
	public UIString(String text, Vector2i pos, Color color) {
		this.pos = pos;
		this.text = text;
	}
	public Vector2i getPos() {
		return pos;
	}
	public String getText() {
		return text;
	}
	public Color getColor() {
		return color;
	}
}
