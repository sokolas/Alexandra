package hello;

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

import reactor.Environment;
import reactor.bus.EventBus;
import reactor.bus.selector.Selectors;

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
	
	@Autowired
	private Publisher publisher;
	
	@Autowired
	private Receiver receiver;
	
	@Value("${app.token}")
	private String token;
	
	
	public static void main(String[] args) throws InterruptedException {
		ConfigurableApplicationContext ctx = SpringApplication.run(Application.class, args);
//		Environment env = ctx.getBean(Environment.class);
//		CountDownLatch latch = ctx.getBean(CountDownLatch.class);
//		latch.await();
//		env.shutdown();
	}
	
	@Override
	public void run(String... arg0) throws Exception {
		eventBus.on(Selectors.$("quotes"), receiver);
		publisher.publishQuotes(NUMBER);
	}
	
}
