package main;

import java.util.concurrent.CountDownLatch;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import hello.Publisher;
import reactor.Environment;
import reactor.bus.Event;
import reactor.bus.EventBus;
import reactor.bus.selector.Selectors;
import rpgbot.MSGReceiver;
import sx.blah.discord.api.ClientBuilder;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.events.IListener;
import sx.blah.discord.handle.impl.events.MessageReceivedEvent;

@Configuration
@ComponentScan
@EnableAutoConfiguration
public class Application implements CommandLineRunner {

	private static final int NUMBER = 5;

	@Bean
	Environment env() {
		return Environment.initializeIfEmpty().assignErrorJournal();
	}
	
	@Bean
	EventBus eventBus(Environment env) {
		return EventBus.create(env);
	}
	
	@Bean
	CountDownLatch latch() {
		return new CountDownLatch(NUMBER);
	}
	
	@Autowired
	private EventBus eventBus;
	
//	@Autowired
//	private Publisher publisher;
	
	@Autowired
	private MSGReceiver receiver;
	
	@Value("${app.token}")
	private String token;

	private IDiscordClient client;
	
	
	public static void main(String[] args) throws InterruptedException {
		ConfigurableApplicationContext ctx = SpringApplication.run(Application.class, args);
//		Environment env = ctx.getBean(Environment.class);
//		CountDownLatch latch = ctx.getBean(CountDownLatch.class);
//		latch.await();
//		env.shutdown();
	}
	
	@Override
	public void run(String... arg0) throws Exception {
		eventBus.on(Selectors.$("messages"), receiver);
//		publisher.publishQuotes(NUMBER);
		
		ClientBuilder builder = new ClientBuilder();
		builder.withToken(token);
		client = builder.login();
		client.getDispatcher().registerListener(new IListener<MessageReceivedEvent>() {

			@Override
			public void handle(MessageReceivedEvent event) {
				eventBus.notify("messages", Event.wrap(event));
			}
		});
	}
	
}
