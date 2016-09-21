package rpgbot;

import sx.blah.discord.handle.obj.IChannel;

public class Game {
    public enum State {
        STOPPED,
        ASSEMBLING_PARTY,
        RUNNING
    }

    private IChannel channel;

}

