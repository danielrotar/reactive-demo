package com.birchbox.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Controller {

    Logger log = LoggerFactory.getLogger(Controller.class);


    @GetMapping("/get")
    public String get(){

        log.info("get");
        return "non-reactive response";
    }

    @PostMapping("/uppercase")
    public String uppercase(@RequestBody String body){

        log.info("to uppercase");
        return body.toUpperCase();
    }

    @PostMapping("/uppercase-with-delay")
    public String uppercaseWithDelay(@RequestBody String body) throws InterruptedException {

        log.info("to uppercase with delay");
        Thread.sleep(3000);

        log.info("delay complete");
        return body.toUpperCase();
    }

}
