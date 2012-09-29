package redlynx.bots;

import redlynx.bots.semifinals.SFSauron;
import redlynx.pong.client.BaseBot;
import redlynx.pong.client.Pong;

public class PongDueler {
    public static void main(String[] args) {
        BaseBot bot1 = new SFSauron();
        createMatch(bot1);
    }

    private static void createMatch(final BaseBot bot1) {
        String opponentName = "Naamiokostaja";
        final String[] args1 = {"Sauron", "boris.helloworldopen.fi", "9090", "-vis", "-match", opponentName};
        Pong.init(args1, bot1);
    }
}
