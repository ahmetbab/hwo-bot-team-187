package redlynx.pong.ui;

import java.awt.Color;

import redlynx.pong.util.Vector2i;

public class UILine {
	private Vector2i start,end;
	private Color color;
	public UILine(Vector2i start, Vector2i end, Color c) {
		this.start = start;
		this.end = end;
		color = c;
	}
	public Vector2i getStart() {
		return start;
	}
	public Vector2i getEnd() {
		return end;
	}
	public Color getColor() {
		return color;
	}
}
