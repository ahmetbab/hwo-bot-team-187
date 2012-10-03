package redlynx.bots;

import redlynx.bots.finals.dataminer.DataMiner;
import redlynx.bots.finals.sauron.FinalSauron;
import redlynx.bots.finals.zeus.Zeus;
import redlynx.pong.client.Pong;
import redlynx.pong.client.PongGameBot;

public class PongDueler {
    public static void main(String[] args) {
        PongGameBot bot1 = new Zeus();
        createMatch(bot1);
    }

    private static void createMatch(final PongGameBot bot1) {
        String opponentName = "Moukari";
        final String[] args1 = {"Sauron", "boris.helloworldopen.fi", "9090", "-vis", "-match", opponentName};
        Pong.init(args1, bot1);
    }
}
