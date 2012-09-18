package redlynx.pong.ui;

import java.awt.Color;

import redlynx.pong.util.Vector2;
import redlynx.pong.util.Vector2i;

public class UILine {
	private Vector2i start,end;
	private Color color;
	
	public UILine(double x, double y, double x2, double y2, Color c) {
		this(new Vector2i((int)x,(int)y),new Vector2i((int)x2,(int)y2), c);
	}
	
	public UILine(Vector2i start, Vector2i end, Color c) {
		this.start = start;
		this.end = end;
		color = c;
	}

    public static UILine createFromDirection(Vector2 pos, Vector2 dir, double mul, Color c) {
		return new UILine(new Vector2i((int)pos.x, (int)pos.y), new Vector2i((int)(pos.x+dir.x*mul),(int)( pos.y+dir.y*mul)), Color.red);
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
