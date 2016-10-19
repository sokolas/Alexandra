package slotmachine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;
import rpgbot.MessageReceiver;
import sx.blah.discord.handle.impl.events.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.handle.obj.IVoiceChannel;

import java.util.*;
import java.util.function.Consumer;

public class SlotReceiver implements Consumer<MessageReceivedEvent> {
    private static Logger logger = LoggerFactory.getLogger(MessageReceiver.class);

    private static final int SLOTS = 3;

    @Value("#{'${allowedChannels}'.split(',')}")
    private List<String> allowedChannels;

    @Autowired
    private Consumer<Tuple2<IChannel, String>> sender;

    private boolean isGood(IMessage message) {
        if (message.getAuthor().isBot()
                || message.getChannel().isPrivate()
                || message.getChannel() instanceof IVoiceChannel
                || message.getContent() == null
                || message.getContent().isEmpty()
                || message.getContent().startsWith(">")
                || message.getContent().startsWith("`")
                || (!allowedChannels.contains(message.getChannel().getName())
                && !allowedChannels.contains(message.getChannel().getID()))
                ) {
            return false;
        } else {
            return true;
        }
    }

    private boolean isCommand(IMessage message) {
        return message.getContent().startsWith("!")
                || message.getContent().startsWith("--");
    }

    private Random random = new Random();

    private List<String> slots = new ArrayList<>();

    @Override
    public void accept(MessageReceivedEvent messageReceivedEvent) {
        IMessage message = messageReceivedEvent.getMessage();
        IChannel channel = message.getChannel();
        IUser author = message.getAuthor();
        if (!isGood(message))
            return;
        String text = message.getContent();
        if (!StringUtils.hasText(text)) {
            return;
        }
        if (text.startsWith("--roll")) {
            StringBuilder sb = new StringBuilder(author.mention()).append(": ");
            for (int i = 0; i < SLOTS; i++) {
                int k = random.nextInt(slots.size());
//                System.out.println("" + k + "/" + slots.size() + ": " + slots.get(k));
                sb.append(slots.get(k)).append(" ");
            }
            sender.accept(Tuples.of(channel, sb.toString()));
        } else if (text.startsWith("--list")) {
            StringBuilder sb = new StringBuilder(author.mention()).append(": ");
            for (String slot: slots) {
                sb.append(slot).append("-");
            }
            sender.accept(Tuples.of(channel, sb.toString()));
        } else if (text.startsWith("--slots")) {
            List<String> newSlots = Arrays.asList(text.substring(7).trim().split(" "));
            HashSet<String> tmp = new HashSet<>();
            for (String s: newSlots) {
                tmp.add(s);
            }
            slots.clear();
            slots.addAll(tmp);
            StringBuilder sb = new StringBuilder("OK").append(": ");
            for (String slot: slots) {
                sb.append(slot).append("-");
            }
            sender.accept(Tuples.of(channel, sb.toString()));
            System.out.println(sb.toString());
        }
    }
}
