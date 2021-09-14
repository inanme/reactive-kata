package reactive.web;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

@SpringBootApplication
public class Web {
    public static void main(String[] args) {
        SpringApplication.run(Web.class, args);
    }
}

@RestController
@RequiredArgsConstructor
class GreetingController {
    private final GreetingHandler greetingHandler;

    @GetMapping("/greeting/{name}")
    Mono<Greeting> greet(@PathVariable String name) {
        return greetingHandler.greet(name);
    }

    @GetMapping(value = "/time", produces = {MediaType.TEXT_EVENT_STREAM_VALUE})
    Flux<Long> time() {
        return Flux.interval(Duration.ofMillis(1000));
    }
}

@Component
class GreetingHandler {

    public Mono<Greeting> greet(String name) {
        return Mono.just(new Greeting(name));
    }
}

@Data
@AllArgsConstructor
@NoArgsConstructor
class Greeting {
    private String message;
}
