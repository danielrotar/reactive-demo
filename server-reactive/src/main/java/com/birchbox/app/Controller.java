package com.birchbox.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;

@Configuration
@RestController
public class Controller {

    Logger log = LoggerFactory.getLogger(Controller.class);

    @GetMapping("/get")
    public Mono<String> get(){
        log.info("get");
        return Mono.just("reactive response");
    }

    @PostMapping("/uppercase")
    public Flux<String> uppercase(@RequestBody Flux<String> body){
        log.info("to uppercase");
        return body.map(str -> str.toUpperCase());
    }

    @PostMapping("/uppercase-with-delay")
    public Flux<String> uppercaseWithDelay(@RequestBody Flux<String> body){
        log.info("to uppercase with delay");
        return body
            .delaySequence(Duration.ofSeconds(3))
            .doOnComplete(() -> log.info("delay complete"))
            .map(str -> str.toUpperCase());
    }

    class SocketHandler implements WebSocketHandler {

        @Override
        public Mono<Void> handle(WebSocketSession session) {
            log.info("websocket established");
            return session.receive()
                    .map(message -> message.getPayloadAsText())
                    .map(str -> str.toUpperCase())
                    .map(str -> session.textMessage(str))
                    .as(output -> session.send(output))
                    .then();
        }

    }

    @Bean
    public HandlerMapping handlerMapping() {
        var map = new HashMap<String, WebSocketHandler>();
        map.put("/uppercase/ws", new SocketHandler());

        var mapping = new SimpleUrlHandlerMapping();
        mapping.setUrlMap(map);
        mapping.setOrder(-1);
        return mapping;
    }

    @Bean
    public WebSocketHandlerAdapter handlerAdapter() {
        return new WebSocketHandlerAdapter();
    }



}
