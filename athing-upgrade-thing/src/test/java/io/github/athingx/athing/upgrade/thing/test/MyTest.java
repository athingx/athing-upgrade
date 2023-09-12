package io.github.athingx.athing.upgrade.thing.test;

import org.junit.Test;

import java.util.concurrent.CompletableFuture;

public class MyTest {

    @Test
    public void test() {

        final var future = CompletableFuture.supplyAsync(()-> {
            try {
                Thread.sleep(1000*3);
                System.out.println("future1 is done");
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            throw new RuntimeException("error");
        }).thenCombine(CompletableFuture.supplyAsync(()-> {
            try {
                Thread.sleep(1000*30);
                System.out.println("future2 is done");
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return "ok";
        }), (r1, r2)-> r1 + r2);

        System.out.println(future.join());

    }

}
