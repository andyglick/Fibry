package eu.lucaventuri.fibry;

import org.junit.jupiter.api.Test;

import eu.lucaventuri.fibry.fsm.FsmBuilderActor;
import eu.lucaventuri.fibry.fsm.FsmBuilderConsumer;
import eu.lucaventuri.fibry.fsm.FsmContext;
import eu.lucaventuri.fibry.fsm.FsmTemplate;
import eu.lucaventuri.fibry.fsm.FsmTemplateActor;

import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

enum States {
    A, B, C
}

class TestFsm {
    private final Consumer<FsmContext<States, String, String>> consumerPrint = m -> System.out.println("From " + m.previousState + " to " + m.newState + " - message: " + m.message);
    private final MessageOnlyActor<FsmContext<States, String, String>, String, Void> actorPrint = ActorSystem.anonymous().newActorWithReturn(m -> {
        System.out.println("From " + m.previousState + " to " + m.newState + " - message: " + m.message);

        return "RET: " + m.message;
    });

    @Test
    void notEnoughStateShouldRaiseAnException() throws IllegalArgumentException {
        assertThrows(IllegalArgumentException.class, () -> {
            var fsm = new FsmBuilderConsumer<States, String, String>()
                .addState(States.A, consumerPrint).goTo(States.B, "b").goTo(States.C, "c")
                .build();//...
        });
    }

    @Test
    void unknownStateShouldRaiseAnException() throws IllegalArgumentException {
        assertThrows(IllegalArgumentException.class, () -> {
            var fsm = new FsmBuilderConsumer<States, String, String>()
                .addState(States.A, consumerPrint).goTo(States.B, "b").goTo(States.C, "c")
                .addState(States.B, consumerPrint).goTo(States.B, "b").goTo(States.C, "c")
                .build();
        });
    }

    @Test
    void testCreation() {
        standardFsm(consumerPrint);
    }

    @Test
    void testTransitions() {
        var fsm = standardFsm(consumerPrint).newFsmConsumer(States.A);

        assertEquals(fsm.onEvent("b", "Test"), States.B);
        assertEquals(fsm.getCurrentState(), States.B);
        assertEquals(fsm.onEvent("c", "Test"), States.C);
        assertEquals(fsm.getCurrentState(), States.C);
        assertEquals(fsm.onEvent("a", "Test"), States.A);
        assertEquals(fsm.getCurrentState(), States.A);
    }

    @Test
    void testActor() {
        var fsm = standardFsm(consumerPrint).newFsmConsumer(States.A);

        fsm.getActor();
    }

    private FsmTemplate<States, String, ? extends Consumer<FsmContext<States, String, String>>, String> standardFsm(Consumer<FsmContext<States, String, String>> actor) {
        return new FsmBuilderConsumer<States, String, String>()
                .addState(States.A, actor).goTo(States.B, "b").goTo(States.C, "c")
                .addState(States.B, actor).goTo(States.A, "a").goTo(States.C, "c")
                .addState(States.C, actor).goTo(States.A, "a").goTo(States.B, "b")
                .build();
    }

    private FsmTemplateActor<States, String, String, MessageOnlyActor<FsmContext<States, String, String>, String, Void>, String> actorFsm(MessageOnlyActor<FsmContext<States, String, String>, String, Void> actor) {
        return new FsmBuilderActor<States, String, String, MessageOnlyActor<FsmContext<States, String, String>, String, Void>, String>()
                .addState(States.A, actor).goTo(States.B, "b").goTo(States.C, "c")
                .addState(States.B, actor).goTo(States.A, "a").goTo(States.C, "c")
                .addState(States.C, actor).goTo(States.A, "a").goTo(States.B, "b")
                .build();
    }

    @Test
    void testActorsAfter() throws ExecutionException, InterruptedException {
        var fsmTemplate = actorFsm(actorPrint);
        var fsm = fsmTemplate.newFsmActor(States.A);
        var fsm2 = fsmTemplate.newFsmActorReplace(States.A, null);

        assertEquals("RET: b", fsm.onEventAfter("b", "Test", true).get());
        assertEquals(fsm.getCurrentState(), States.B);
        assertEquals("RET: c", fsm.onEventAfter("c", "Test", true).get());
        assertEquals(fsm.getCurrentState(), States.C);
        assertEquals("RET: a", fsm.onEventAfter("a", "Test", true).get());
        assertEquals(fsm.getCurrentState(), States.A);

        assertNull(fsm2.onEventAfter("b", "Test", true));
        assertEquals(fsm2.getCurrentState(), States.B);
        assertNull(fsm2.onEventAfter("c", "Test", true));
        assertEquals(fsm2.getCurrentState(), States.C);
        assertNull(fsm2.onEventAfter("a", "Test", true));
        assertEquals(fsm2.getCurrentState(), States.A);
    }

    @Test
    void testActorsBefore() throws ExecutionException, InterruptedException {
        var fsmTemplate = actorFsm(actorPrint);
        var fsm = fsmTemplate.newFsmActor(States.A);
        var fsm2 = fsmTemplate.newFsmActorReplace(States.A, null);

        assertEquals("RET: b", fsm.onEventBefore("b", "Test", true));
        assertEquals(fsm.getCurrentState(), States.B);
        assertEquals("RET: c", fsm.onEventBefore("c", "Test", true));
        assertEquals(fsm.getCurrentState(), States.C);
        assertEquals("RET: a", fsm.onEventBefore("a", "Test", true));
        assertEquals(fsm.getCurrentState(), States.A);

        assertNull(fsm2.onEventBefore("b", "Test", true));
        assertEquals(fsm2.getCurrentState(), States.B);
        assertNull(fsm2.onEventBefore("c", "Test", true));
        assertEquals(fsm2.getCurrentState(), States.C);
        assertNull(fsm2.onEventBefore("a", "Test", true));
        assertEquals(fsm2.getCurrentState(), States.A);
    }
}
