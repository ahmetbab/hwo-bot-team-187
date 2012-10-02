package redlynx.pong.client.ui;

import redlynx.pong.ui.UILine;
import redlynx.pong.ui.UIString;

import java.util.ArrayList;

public interface LineVisualizer {
    public abstract ArrayList<UILine> getDrawLines();
    public abstract ArrayList<UIString> getDrawStrings();
}
