package com.birchbox.app;

import com.google.common.base.Splitter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.URI;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.stream.BaseStream;

@ShellComponent
public class ClientShell {

    Logger log = LoggerFactory.getLogger(ClientShell.class);

    private String host(boolean reactive){
        return reactive ? "server-reactive:8080" : "server-nonreactive:8080";
    }


    @ShellMethod("Get nonreactive server.")
    public String getNonreactive(){
        return call(false);
    }

    @ShellMethod("Get reactive server.")
    public String getReactive(){
        return call(true);
    }

    @ShellMethod("Uppercase on nonreactive server.")
    public String uppercaseNonreactive(String input){
        return uppercase(input, false);
    }

    @ShellMethod("Uppercase on reactive server.")
    public String uppercaseReactive(String input){
        return uppercase(input, true);
    }

    @ShellMethod("Uppercase on nonreactive server with delay.")
    public void uppercaseNonreactiveWithDelay(String input, int num){
        uppercaseWithDelay(input, num, false);
    }

    @ShellMethod("Uppercase on reactive server with delay.")
    public void uppercaseReactiveWithDelay(String input, int num){
        uppercaseWithDelay(input, num, true);
    }

    @ShellMethod("Uppercase Wikipedia on reactive server.")
    public void uppercaseReactiveWikipedia() throws IOException {
        uppercaseWikipedia();
    }


    private String call(boolean reactive){
        return WebClient.create("http://" + host(reactive) + "/get")
            .get()
            .exchange()
            .blockOptional().orElseThrow()
            .bodyToMono(String.class)
            .block();
    }

    private String uppercase(String input, boolean reactive){
        return WebClient.create("http://" + host(reactive) + "/uppercase")
            .post()
            .body(BodyInserters.fromObject(input))
            .exchange()
            .blockOptional().orElseThrow()
            .bodyToMono(String.class)
            .block();
    }

    private void uppercaseWithDelay(String input, int num, boolean reactive){

        var responses = new ArrayList<Mono<ClientResponse>>();

        for(var i = 1; i <= num; i++) {
            responses.add(
                WebClient.create("http://" + host(reactive) + "/uppercase-with-delay")
                    .post()
                    .body(BodyInserters.fromObject(input + " " + i))
                    .exchange()
            );
        }

        Flux.mergeSequential(responses, Integer.MAX_VALUE, Integer.MAX_VALUE)
            .flatMapSequential(response -> response.bodyToMono(String.class), Integer.MAX_VALUE)
            .map(str -> { log.info(str); return str; })
            .blockLast();

    }

    private void uppercaseWikipedia() throws IOException {

        WritableByteChannel fileChannel =
            Files.newByteChannel(
                Paths.get("/data/wikipedia-upper.xml").toAbsolutePath(),
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE);

        new ReactorNettyWebSocketClient().execute(
            URI.create("ws://" + host(true) + "/uppercase/ws"),
            session -> {

                var filePublisher =
                    Flux.using(
                        () -> Files.lines(
                            Paths.get("/data/wikipedia.xml")
                        ),
                        Flux::fromStream,
                        BaseStream::close);

                var send = filePublisher
                    .flatMap(str -> Flux.fromIterable(
                        Splitter.fixedLength(32 * 1024).split(str + "\n"))
                    )
                    .map(str -> session.textMessage(str))
                    .as(stream -> session.send(stream))
                    .then();

                var receive = session
                    .receive()
                    .map(message -> message.getPayload())
                    .as(source -> DataBufferUtils.write(source, fileChannel))
                    .doFinally(signal -> wrap(() ->
                        fileChannel.close()
                    ))
                    .then();

                return Mono.zip(send, receive).then();
            })
            .block();
    }


    @FunctionalInterface interface CheckedRunnable {
        void run() throws Throwable;
    }

    private void wrap(CheckedRunnable runnable){
        try {
            runnable.run();
        } catch (Throwable e){
            throw new RuntimeException(e);
        }
    }
}
