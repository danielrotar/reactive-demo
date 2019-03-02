package com.birchbox.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.web.client.RestTemplate;

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


    private String call(boolean reactive){
        return new RestTemplate()
            .getForObject(
                "http://" + host(reactive) + "/get",
                String.class);
    }

    private String uppercase(String input, boolean reactive){
        return new RestTemplate()
            .postForObject(
                "http://" + host(reactive) + "/uppercase",
                input,
                String.class);
    }

    private void uppercaseWithDelay(String input, int num, boolean reactive){
        for(var i = 1; i <= num; i++) {
            log.info(
                new RestTemplate()
                    .postForObject(
                        "http://" + host(reactive) + "/uppercase-with-delay",
                        input + " " + i,
                        String.class)
            );
        }
    }
}
