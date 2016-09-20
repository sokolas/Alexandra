package main;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import reactor.core.publisher.*;
import reactor.util.function.Tuple2;
import rpgbot.MessageReceiver;
import rpgbot.MsgLogger;
import sx.blah.discord.api.ClientBuilder;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.events.Event;
import sx.blah.discord.api.events.IListener;
import sx.blah.discord.handle.impl.events.MessageReceivedEvent;
import sx.blah.discord.handle.impl.obj.Channel;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.MissingPermissionsException;
import sx.blah.discord.util.RateLimitException;

import java.util.function.Consumer;

@Configuration
@ComponentScan(basePackages={"main", "rpgbot", "persistence"})
@EnableAutoConfiguration
@EnableJpaRepositories("persistence")
@EntityScan("persistence")
public class Application implements CommandLineRunner {
    private static Logger logger = LoggerFactory.getLogger(Application.class);

	@Autowired
	private MessageReceiver receiver;

	@Autowired
	private MsgLogger msgLogger;

    /**
     * Emit Discord events to this sink
     * TODO remove the bean and create it in run()
     */
    @Autowired
    private BlockingSink<Event> eventBlockingSink;

    /**
     * Discord events come outta here
     */
    @Autowired
    private EmitterProcessor<Event> discordEmitterProcessor;
    
    @Autowired
    private Consumer<Tuple2<IChannel, String>> sender;

	@Value("${app.token}")
	private String token;

	private IDiscordClient client;

	@Bean
    public BlockingSink<Event> eventBlockingSink() {
        return discordEmitterProcessor().connectSink();
    }

    @Bean
    public Consumer<Tuple2<IChannel, String>> sender() {
        Consumer<Tuple2<IChannel, String>> sender = (t -> {
            try {
                t.getT1().sendMessage(t.getT2());
            } catch (MissingPermissionsException | RateLimitException | DiscordException e) {
                logger.error("Can't send message ", e);
            }
        });
        return sender;
    }

    @Bean
    public EmitterProcessor<Event> discordEmitterProcessor() {
        return EmitterProcessor.create();
    }

    // ====================================================================

    public static void main(String[] args) throws InterruptedException {
        ConfigurableApplicationContext ctx = SpringApplication.run(Application.class, args);
    }

    @Override
	public void run(String... arg0) throws Exception {
		ClientBuilder builder = new ClientBuilder();
		builder.withToken(token);
		client = builder.login();

        connectListeners();

        Flux
            .<Event>create(emitter -> {
                // need explicit lambda declaration here
                // it doesn't work with method reference
                IListener<Event> fluxListener = event -> emitter.next(event);
                client.getDispatcher().registerListener(fluxListener);
                emitter.setCancellation(() -> {
                    client.getDispatcher().unregisterListener(fluxListener);
                    logger.info("Unregistered");
                });
            }, FluxSink.OverflowStrategy.LATEST)
//            .log()
            .doOnComplete(() -> logger.info("Done"))
            .subscribe(eventBlockingSink);
    }

    /**
     * TODO @PostConstruct?
     */
    private void connectListeners() {
        discordEmitterProcessor.subscribe(msgLogger);
        discordEmitterProcessor
                .filter(event -> event instanceof MessageReceivedEvent)
                .map(event -> (MessageReceivedEvent) event)
                .subscribe(receiver);
    }
}
