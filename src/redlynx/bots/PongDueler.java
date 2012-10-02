package redlynx.bots;

import redlynx.bots.finals.sauron.FinalSauron;
import redlynx.bots.preliminaries.dataminer.DataMiner;
import redlynx.pong.client.Pong;
import redlynx.pong.client.PongGameBot;

public class PongDueler {
    public static void main(String[] args) {
        PongGameBot bot1 = new FinalSauron();
        createMatch(bot1);
    }

    private static void createMatch(final PongGameBot bot1) {
        String opponentName = "jebin";
        final String[] args1 = {"sauron", "boris.helloworldopen.fi", "9090", "-vis", "-match", opponentName};
        Pong.init(args1, bot1);
    }
}
