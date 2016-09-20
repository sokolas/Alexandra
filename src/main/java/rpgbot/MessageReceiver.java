package rpgbot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import persistence.LogEntry;
import persistence.MessageRepository;
import reactor.bus.Event;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;
import sx.blah.discord.handle.impl.events.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.RateLimitException;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service
public class MessageReceiver implements Consumer<MessageReceivedEvent> {
    private static Logger logger = LoggerFactory.getLogger(MessageReceiver.class);

	@Autowired
	private MessageRepository messageRepository;

    @Autowired
    private Consumer<Tuple2<IChannel, String>> sender;

	@Override
	public void accept(MessageReceivedEvent data) {
		if (data.getMessage().getContent().contains("test")) {
            List<LogEntry> all = messageRepository.findAll();
            List<String> strings = all.stream()
                    .map(entry -> entry.getAuthor() + ": " + entry.getText())
                    .collect(Collectors.toList());
            String msg = String.join("\n", strings);
            sender.accept(Tuples.of(data.getMessage().getChannel(), msg));
        } else if (data.getMessage().getContent().contains("exit")) {
            try {
                data.getMessage().getClient().logout();
            } catch (RateLimitException | DiscordException e) {
                e.printStackTrace();
            }
        } else {
        	logger.info(data.getMessage().getContent());
		}
	}
	
}
