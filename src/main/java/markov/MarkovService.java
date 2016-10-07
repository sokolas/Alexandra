package markov;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;
import sx.blah.discord.api.events.Event;
import sx.blah.discord.handle.impl.events.MessageReceivedEvent;
import sx.blah.discord.handle.impl.events.ReadyEvent;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.handle.obj.IVoiceChannel;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class MarkovService implements Consumer<Event> {
    private static final Logger logger = LoggerFactory.getLogger(MarkovService.class);

    private Markov everyone;

    private Map<IUser, Markov> markovs;

    private boolean started = false;

    @Value("#{'${allowedChannels}'.split(',')}")
    private List<String> allowedChannels;

    @Value("${resetCount}")
    private int resetCount;

    @Value("${minLogs}")
    private int minLogs;

    @Value("${maxLogs}")
    private int maxLogs;

    @Autowired
    private Consumer<Tuple2<IChannel, String>> sender;

    private IUser me;

    private int postsCount = 0;

    private int processed = 0;

    private Random random = new Random();

    @Override
    public void accept(Event event) {
        if (event instanceof MessageReceivedEvent) {
            if (started) {
                onMessage(((MessageReceivedEvent) event).getMessage());
            } else {
                logger.warn("Received message before ready");
            }
        } else if (event instanceof ReadyEvent) {
            started = true;
            me = event.getClient().getOurUser();
            everyone = new Markov();
            markovs = new ConcurrentHashMap<>();
            event.getClient().getChannels(false).forEach(channel -> {
                if (!(channel instanceof IVoiceChannel)
                        && (allowedChannels.contains(channel.getName())) || allowedChannels.contains(channel.getID())) {
                    logger.info("Processing {}", channel.getName());
                    reset(channel);
                } else {
                    logger.info("Skipping {}", channel.getName());
                }
            });
            logger.info("Markov started");
        }
    }

    private boolean isGood(IMessage message) {
        if (message.getAuthor().isBot()
                || message.getAuthor().equals(me)
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

    public String generateForName(String target) {
        return markovs.keySet().stream()
                .filter(user -> user.getName().equals(target))
                .findFirst()
                .map(user -> markovs.get(user).generate())
                .orElse("Not found");
    }

    public String generateForAll() {
        return everyone.generate();
    }

    public List<String> getUsers() {
        return new ArrayList<>(markovs.keySet().stream().map(IUser::getName).collect(Collectors.toList()));
    }

    private void onMessage(IMessage message) {
        if (!isGood(message))
            return;
        logger.debug(message.getContent());
        IChannel channel = message.getChannel();
        if (message.getMentions().contains(me)) {
            Markov markov = markovs.get(message.getAuthor());
            String ins = "";
            String msg = "";
            if (!message.getContent().isEmpty()) {
                if (message.getContent().contains("all") || message.getContent().contains("все")) {
                    markov = everyone;
                    ins = " (все)";
                }
            }
            IUser target = message.getMentions().stream()
                    .filter(user -> !user.equals(me))
                    .findAny()
                    .orElse(null);
            if (target != null) {
                markov = markovs.get(target);
                ins = String.format(" **%s**:", target.getName());
            }
            if (markov == null) {
                msg = String.format("%s: No data", message.getAuthor().mention());
            } else {
                msg = String.format("%s:%s %s", message.getAuthor().mention(), ins, markov.generate());
            }
            send(channel, msg);
            return;
        }

        if (message.getContent().startsWith("--stats")) {
            String msg = String.format("База: %d, пересчет через %d сообщений", processed + postsCount, resetCount - postsCount);
            send(channel, msg);
            return;
        }

        if (message.getContent().startsWith("--debug")) {
            StringBuilder sb = new StringBuilder();
            for (IUser user: markovs.keySet()) {
                sb.append(user.getName())
                        .append("\n")
                        .append(markovs.get(user).debug())
                        .append("\n");
            }
            send(channel, sb.toString());
            return;
        }

        if (message.getContent().startsWith("--decide")) {
            String[] decisions = message.getContent().substring(8).trim().split(",");
            if (decisions.length == 0) {
                send(channel, String.format("%s: слишком мало вариантов", message.getAuthor().mention()));
            } else {
                int d = random.nextInt(decisions.length);
                String msg = String.format("%s: %s", message.getAuthor().mention(), decisions[d]);
                send(channel, msg);
            }
            return;
        }

        if (message.getMentions().isEmpty() && !isCommand(message)) {
            Markov markov = markovs.get(message.getAuthor());
            if (markov == null) {
                markov = new Markov();
                markovs.put(message.getAuthor(), markov);
            }
            markov.add(message);
            everyone.add(message);
            postsCount += 1;

            if (postsCount >= resetCount) {
                reset(channel);
            }
        }
    }

    private void send(IChannel channel, String msg) {
        sender.accept(Tuples.of(channel, msg));
    }

    private void reset(IChannel channel) {
        processed = 0;
        postsCount = 0;
        everyone = new Markov();
        markovs = new ConcurrentHashMap<>();
        int i = 0;
        while (processed < minLogs) {
            try {
//                logger.info("{}", i);
                if (i > 0 && i % 256 == 0) {
                    for (int j = 0; j < 255; j++) {
                        channel.getMessages().remove(0);
                    }
                    i = 1;
                }
                IMessage message = channel.getMessages().get(i);
                i++;
                logger.debug(message.toString());
                if (!isGood(message) || isCommand(message) || !message.getMentions().isEmpty()) {
                    continue;
                }
                Markov markov = markovs.get(message.getAuthor());
                if (markov == null) {
                    markov = new Markov();
                    markovs.put(message.getAuthor(), markov);
                }
                markov.add(message);
                everyone.add(message);
                processed += 1;
            } catch (Exception e) {
                logger.error("Can't load messages", e);
                break;
            }
        }
        logger.info("Loaded {} messages out of {}", processed, maxLogs);
    }
}
