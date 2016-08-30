package hello;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Service
public class Publisher {
	@Autowired
	EventBus eventBus;
	
	public void publishQuotes(int number) {
		for (int i = 0; i < number; i++) {
			eventBus.notify("quotes", Event.wrap(i));
		}
	}
}
