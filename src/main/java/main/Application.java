package main;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.stereotype.Repository;
import reactor.Environment;
import reactor.bus.Event;
import reactor.bus.EventBus;
import reactor.bus.selector.Selectors;
import rpgbot.MessageReceiver;
import rpgbot.MsgLogger;
import sx.blah.discord.api.ClientBuilder;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.events.IListener;
import sx.blah.discord.handle.impl.events.MessageReceivedEvent;

@Configuration
@ComponentScan(basePackages={"main", "rpgbot", "persistence"})
@EnableAutoConfiguration
@EnableJpaRepositories("persistence")
@EntityScan("persistence")
public class Application implements CommandLineRunner {

	@Bean
	Environment env() {
		return Environment.initializeIfEmpty().assignErrorJournal();
	}
	
	@Bean
	EventBus eventBus(Environment env) {
		return EventBus.create(env);
	}
	
	@Autowired
	private EventBus eventBus;
	
	@Autowired
	private MessageReceiver receiver;

	@Autowired
	private MsgLogger msgLogger;
	
	@Value("${app.token}")
	private String token;

	private IDiscordClient client;
	
	
	public static void main(String[] args) throws InterruptedException {
		ConfigurableApplicationContext ctx = SpringApplication.run(Application.class, args);
	}
	
	@Override
	public void run(String... arg0) throws Exception {
		eventBus.on(Selectors.$("messages"), receiver);
		eventBus.on(Selectors.$("messages"), msgLogger);

		ClientBuilder builder = new ClientBuilder();
		builder.withToken(token);
		client = builder.login();
		client.getDispatcher().registerListener(event -> eventBus.notify("messages", Event.wrap(event)));
	}
	
}
