package rpgbot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import persistence.LogEntry;
import persistence.MessageRepository;
import sx.blah.discord.api.events.Event;
import sx.blah.discord.handle.impl.events.MessageReceivedEvent;

import java.util.function.Consumer;

@Service
public class MsgLogger implements Consumer<Event> {
	private static Logger logger = LoggerFactory.getLogger(MsgLogger.class);

    @Autowired
    private MessageRepository messageRepository;

	public void accept(Event data) {
		logger.info(data.toString());
		/*if (data instanceof MessageReceivedEvent) {
            LogEntry entry = new LogEntry();
            entry.setAuthor(((MessageReceivedEvent) data).getMessage().getAuthor().getName());
            entry.setDate(((MessageReceivedEvent) data).getMessage().getTimestamp());
            entry.setText(((MessageReceivedEvent) data).getMessage().getContent());
            messageRepository.save(entry);
		}*/
	}

}
