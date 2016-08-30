package rpgbot;

import org.springframework.stereotype.Service;

import reactor.bus.Event;
import reactor.fn.Consumer;
import sx.blah.discord.handle.impl.events.MessageReceivedEvent;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.MissingPermissionsException;
import sx.blah.discord.util.RateLimitException;

@Service
public class MSGReceiver implements Consumer<Event<MessageReceivedEvent>>{
	
	
	@Override
	public void accept(Event<MessageReceivedEvent> t) {
		MessageReceivedEvent data = t.getData();
		System.out.println(data.getMessage().getContent());
		try {
			data.getMessage().getChannel().sendMessage("OK");
		} catch (MissingPermissionsException | RateLimitException | DiscordException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
}
