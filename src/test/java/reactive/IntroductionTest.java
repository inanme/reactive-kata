package reactive;

import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

public class IntroductionTest {

    /**
     * <h1>Blocking request processing</h1>
     * <img src="https://howtodoinjava.com/wp-content/uploads/2019/02/Blocking-request-processing.png">
     *
     * <p>
     * <h1>Non-blocking request processing</h1>
     * <ul>
     *  <li>Reactive programming</li>
     *  <li>Async programming</li>
     * </ul>
     * <p>
     * <img src="https://howtodoinjava.com/wp-content/uploads/2019/02/Non-blocking-request-processing.png">
     *
     *
     * <img src="https://www.codingame.com/servlet/fileservlet?id=59094907766894">
     * <p>
     * what is a publisher?
     * what is a subscriber?
     * <p>
     */
    @Test
    public void whatsPublisherAndSubscriber() {
        var publisher = new Publisher<Integer>() {
            @Override
            public void subscribe(Subscriber<? super Integer> s) {
                for (int i = 0; i < 10; i++) {
                    if (i == 5) {
                        s.onError(new IllegalArgumentException(String.valueOf(i)));
                    } else {
                        s.onNext(i);
                    }
                }
                s.onComplete();
            }
        };

        var subscriber = new Subscriber<Integer>() {
            @Override
            public void onSubscribe(Subscription s) {
                System.out.println("begin");
            }

            @Override
            public void onNext(Integer i) {
                System.out.println(i);
            }

            @Override
            public void onError(Throwable t) {
                t.printStackTrace();
            }

            @Override
            public void onComplete() {
                System.out.println("done");
            }
        };

        publisher.subscribe(subscriber);
    }

    /**
     * how to cancel?
     */
    @Test
    public void howtoCancel() {
        var publisher = new Publisher<Integer>() {

            @Override
            public void subscribe(Subscriber<? super Integer> s) {
                var subscription = new Subscription() {
                    final AtomicLong maxValue = new AtomicLong();
                    final AtomicBoolean cancelled = new AtomicBoolean();

                    @Override
                    public void request(long n) {
                        maxValue.set(n);
                    }

                    @Override
                    public void cancel() {
                        cancelled.set(true);
                    }
                };
                s.onSubscribe(subscription);
                for (int i = 0; i < subscription.maxValue.intValue() && !subscription.cancelled.get(); i++) {
                    if (i == 5) {
                        s.onError(new IllegalArgumentException(String.valueOf(i)));
                    } else {
                        s.onNext(i);
                    }
                }
                s.onComplete();
            }
        };

        var subscriber = new Subscriber<Integer>() {
            Subscription s;

            @Override
            public void onSubscribe(Subscription s) {
                System.out.println("begin");
                this.s = s;
                s.request(7);
            }

            @Override
            public void onNext(Integer i) {
                if (i == 6) {
                    s.cancel();
                } else {
                    System.out.println(i);
                }
            }

            @Override
            public void onError(Throwable t) {
                t.printStackTrace();
            }

            @Override
            public void onComplete() {
                System.out.println("done");
            }
        };
        publisher.subscribe(subscriber);
    }

    /**
     * how to backpressure?
     */
    @Test
    public void whatIsBackPressure() throws InterruptedException {
        var publisher = new Publisher<Long>() {
            final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

            @Override
            public void subscribe(Subscriber<? super Long> s) {
                var subscription = new Subscription() {
                    final AtomicLong duration = new AtomicLong();
                    final AtomicBoolean cancelled = new AtomicBoolean();

                    @Override
                    public void request(long n) {
                        duration.set(n);
                    }

                    @Override
                    public void cancel() {
                        cancelled.set(true);
                        scheduler.shutdown();
                    }
                };
                s.onSubscribe(subscription);
                class Task implements Runnable {
                    @Override
                    public void run() {
                        s.onNext(System.currentTimeMillis());
                        scheduler.schedule(this, subscription.duration.longValue(), TimeUnit.MILLISECONDS);
                    }
                }
                new Task().run();
            }
        };

        var subscriber = new Subscriber<Long>() {
            Subscription s;
            final AtomicLong counter = new AtomicLong();

            @Override
            public void onSubscribe(Subscription s) {
                System.out.println("begin");
                this.s = s;
                s.request(250);
            }

            @Override
            public void onNext(Long i) {
                var curr = counter.incrementAndGet();
                if (curr == 20) {
                    s.request(1000);
                    System.out.println("slowing down");
                }
                if (curr == 30) {
                    s.cancel();
                }
                System.out.println(i + " " + curr);
            }

            @Override
            public void onError(Throwable t) {
                t.printStackTrace();
            }

            @Override
            public void onComplete() {
                System.out.println("done");
            }
        };
        publisher.subscribe(subscriber);
        publisher.scheduler.awaitTermination(100, TimeUnit.SECONDS);
    }

    /**
     * basic similarly looking mechanisms
     */
    @Test
    public void simpleBlockingIterables() {
        Stream.of(1, 2, 3, 4).forEach(System.out::println);
        Arrays.asList(1, 2, 3, 4).forEach(System.out::println);
    }

    /**
     * {@link reactor.core.publisher.Flux} can emit 0 to n <T> elements
     */
    @Test
    public void flux101() {
        Publisher<Integer> source = Flux.range(1, 100);

        source.subscribe(new Subscriber<Integer>() {
            @Override
            public void onSubscribe(Subscription s) {
                System.out.println("begin");
                s.request(10);
            }

            @Override
            public void onNext(Integer i) {
                System.out.println(i);
            }

            @Override
            public void onError(Throwable t) {
                t.printStackTrace();
            }

            @Override
            public void onComplete() {
                System.out.println("done");
            }
        });
    }

    /**
     * {@link reactor.core.publisher.Flux} can emit 0 to n <T> elements
     */
    @Test
    public void fluxOnNextSimplified() {
        Flux<Integer> source = Flux.range(1, 100);
        source.subscribe(System.out::print);
    }

    /**
     * {@link reactor.core.publisher.Mono} can emit at most 1 <T> element:
     */
    @Test
    public void mono() {
        Mono.just(1l)
                .map(l -> {
                    if (l == 2) {
                        throw new IllegalArgumentException("fail");
                    }
                    return l;
                })
                .subscribe(System.out::println);
    }

    @Test
    public void stepVerifier() {
        StepVerifier.create(Flux.just(1, 2, 3))
                .expectNextMatches(i -> i == 1)
                .expectNextMatches(i -> i == 2)
                .expectNextMatches(i -> i == 3)
                .verifyComplete();
    }


}
