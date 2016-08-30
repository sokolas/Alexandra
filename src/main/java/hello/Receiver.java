package hello;

import java.util.concurrent.CountDownLatch;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import reactor.bus.Event;
import reactor.fn.Consumer;

@Service
public class Receiver implements Consumer<Event<Integer>> {
	
	RestTemplate template = new RestTemplate();
	
	@Autowired
	private CountDownLatch latch;
	
	@Override
	public void accept(Event<Integer> event) {
		QuoteResource resource = template
				.getForObject("http://gturnquist-quoters.cfapps.io/api/random", QuoteResource.class);
		System.out.println(Thread.currentThread().getName() + " Quote " + event.getData() + ": " + resource.getValue().getQuote());
		latch.countDown();
	}
	
}
