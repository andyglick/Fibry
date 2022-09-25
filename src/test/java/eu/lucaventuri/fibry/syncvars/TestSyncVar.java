package eu.lucaventuri.fibry.syncvars;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.assertj.core.api.Assertions;

import eu.lucaventuri.fibry.ActorSystem;
import eu.lucaventuri.fibry.distributed.TcpChannel;
import eu.lucaventuri.fibry.distributed.TcpReceiver;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class TestSyncVar {
  @Test
  void testSetValue() {
    SyncVar<String> sv = new SyncVar<>();

    Assertions.assertThat(sv.getValue()).isNull();

    String testString = "abc";
    sv.setValue("abc");
    Assertions.assertThat(sv.getValue()).isEqualTo(testString);

    testString = "def";
    sv.setValue("def");
    Assertions.assertThat(sv.getValue()).isEqualTo(testString);
  }

  @Test
  void testSubscribe() {
    SyncVar<String> sv = new SyncVar<>();
    AtomicBoolean ok = new AtomicBoolean(false);

    sv.subscribe(v -> {
      if (v.getNewValue().equals("def")) {
        Assertions.assertThat(v.getOldValue()).isEqualTo("abc");
        ok.set(true);
      }
    });

    sv.subscribe((oldValue, newValue) -> {
      if (newValue.equals("def")) {
        Assertions.assertThat(oldValue).isEqualTo("abc");
      }
    });

    sv.setValue("abc");
    Assertions.assertThat(ok.get()).isFalse();

    sv.setValue("def");
    Assertions.assertThat(ok.get()).isTrue();
  }

  @Test
  void testValueHolderActor() {
    SyncVar<String> sv = new SyncVar<>();
    SyncVarConsumer<String> vh = new SyncVarConsumer<>();

    sv.subscribe(vh);

    sv.setValue("abc");
    Assertions.assertThat(vh.getValue()).isEqualTo("abc");

    sv.setValue("def");
    Assertions.assertThat(vh.getValue()).isEqualTo("def");
  }

  @Test
  void testSubscribeValueHolder() {
    SyncVar<String> sv = new SyncVar<>();
    AtomicBoolean ok = new AtomicBoolean(false);

    SyncVarConsumer<String> vh = new SyncVarConsumer<>();

    sv.subscribe(vh);
    vh.subscribe(v -> {
      if (v.getNewValue().equals("def")) {
        Assertions.assertThat(v.getOldValue()).isEqualTo("abc");
        ok.set(true);
      }
    });

    sv.setValue("abc");
    Assertions.assertThat(ok.get()).isFalse();

    sv.setValue("def");
    Assertions.assertThat(ok.get()).isTrue();
  }

  @Disabled
  @Test
  void testRemoteActors() throws IOException, InterruptedException {
    int port = 20001;
    var ser = OldAndNewValue.<String>getJacksonSerDeser();
    SyncVarConsumer<String> vh = new SyncVarConsumer<>();
    CountDownLatch latch = new CountDownLatch(1);

    // Will receive value notifications
    TcpReceiver.startTcpReceiverProxy(port, "abc", ser, ser, false);
    ActorSystem.named("SyncVarActor1").newActor(vh);

    vh.subscribe(v -> {
      if (v.getNewValue().equals("xyz"))
        latch.countDown();
    });

    // Will change the value and notify the consumer
    var ch = new TcpChannel<OldAndNewValue<String>, Void>(new InetSocketAddress(port), "abc", null, null, true, "chNoReturn");
    var remoteConsumer = ActorSystem.anonymous().<OldAndNewValue<String>>newRemoteActor("SyncVarActor1", ch, ser);
    SyncVar<String> sv = new SyncVar<>();

    sv.subscribe(remoteConsumer);
    sv.setValue("xyz");

    latch.await(2, TimeUnit.SECONDS);
  }

  @Test
  void testWriteFromThreads() {
    SyncVar<String> sv = new SyncVar<>(); // Messages sent on the same thread
    AtomicBoolean ok = new AtomicBoolean(false);
    var actor = sv.newCompareAndSetActor();
    SyncVarConsumer<String> vh = new SyncVarConsumer<>();

    sv.subscribe(vh);
    vh.subscribe(v -> {
      if (v.getNewValue().equals("def")) {
        Assertions.assertThat(v.getOldValue()).isEqualTo("abc");
        Assertions.assertThat(ok.get()).isFalse();

        try {
          // After the message ends, "xyz" will be processed. This is discouraged, as it could create race conditions
          actor.sendMessageReturn(new OldAndNewValue<>("def", "xyz")).get();
        }
        catch (Exception e) {
          e.printStackTrace();
        }
        Assertions.assertThat(ok.get()).isTrue();
      }

      if (v.getNewValue().equals("xyz")) {
        Assertions.assertThat(v.getOldValue()).isEqualTo("def");
        ok.set(true);
      }
    });

    sv.setValue("abc");
    Assertions.assertThat(ok.get()).isFalse();

    sv.setValue("def");
    Assertions.assertThat(ok.get()).isTrue();
  }
}
