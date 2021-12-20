package eu.lucaventuri.fibry;

import org.junit.jupiter.api.Test;

import org.reactivestreams.tck.TestEnvironment;
import org.reactivestreams.tck.flow.FlowSubscriberBlackboxVerification;

import java.util.concurrent.Flow;

public class TestReactiveSubscribersBlackBox extends FlowSubscriberBlackboxVerification<Integer> {

    public TestReactiveSubscribersBlackBox() {
        super(new TestEnvironment());
    }

    @Override
    public Flow.Subscriber<Integer> createFlowSubscriber() {
        return ActorSystem.anonymous().newActor((Integer n) -> {
        }).asReactiveSubscriber(100, null, null);
    }

    @Override
    public Integer createElement(int element) {
        return element;
    }
}
