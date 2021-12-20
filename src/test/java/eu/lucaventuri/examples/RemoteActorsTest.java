package eu.lucaventuri.examples;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.lucaventuri.fibry.ActorSystem;
import eu.lucaventuri.fibry.distributed.HttpChannel;
import eu.lucaventuri.fibry.distributed.JacksonSerDeser;
import eu.lucaventuri.fibry.distributed.StringSerDeser;

import java.lang.invoke.MethodHandles;

class RemoteActorsTest {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass().getSimpleName());

    @Test
    @Disabled
    void testRemoteActors() throws Exception {

        LOG.info("Running RemoteActorsTest::testRemoteActors");

        var channel = new HttpChannel("Http://localhost:8093/noAuth/actors", HttpChannel.HttpMethod.GET, null, null, false);
        var stringSerDeser = StringSerDeser.INSTANCE;

        try (
            var act1 = ActorSystem.anonymous()
                .newRemoteActorWithReturn("act1", channel, stringSerDeser);
            var act2 = ActorSystem.anonymous().<TestMessage, String>newRemoteActorWithReturn("act2", channel, new JacksonSerDeser<>(String.class), stringSerDeser);
        ) {
            System.out.println(act1.sendMessageReturn("Message 1").get());
            System.out.println(act1.sendMessageReturn("Message 1.1").get());
            System.out.println(act2.sendMessageReturn(new TestMessage("abc", 123)).get());
            System.out.println(act2.sendMessageReturn(new TestMessage("abcde", 12345)).get());
        }

        //act1.sendPoisonPill();
        //act2.sendPoisonPill();
    }
}
