package rpgbot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import reactor.bus.Event;
import reactor.fn.Consumer;

@Service
public class MsgLogger implements Consumer<Event<sx.blah.discord.api.events.Event>>{
	private static Logger logger = LoggerFactory.getLogger(MsgLogger.class);
	
	@Override
	public void accept(Event<sx.blah.discord.api.events.Event> t) {
		logger.info(t.getData().toString());
	}
	
}
