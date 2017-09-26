package ru.asm.api.baratine;

import io.baratine.service.Modify;
import io.baratine.service.Result;
import io.baratine.service.Service;
import io.baratine.vault.Asset;
import io.baratine.web.*;

/**
 * User: artem.smirnov
 * Date: 21.08.2017
 * Time: 11:50
 */
@Asset
@Service
public class AppService {

    private String greeting = "Hello, world";

    @Get("/hello")
    public void doHello(RequestWeb request) {
        request.ok(this);
    }

    @Post("/hello")
    @Modify
    public void update(@Body AppService hello, Result<String> result) {
        this.greeting = hello.greeting;
        result.ok("ok");
    }

    public static void main(String[] args) throws Exception {
        Web.include(AppService.class);

        Web.include(TrackVault.class);
        Web.include(TrackRest.class);

        Web.port(8010);
        Web.go(args);
    }

}
