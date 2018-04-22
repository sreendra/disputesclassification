package com.abc.common.utils.parallel.computation;

import java.util.List;
import java.util.concurrent.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.abc.disputes.classification.ml.models.OnlineSVM;

import static java.util.stream.Collectors.toList;

/**
 * Created by rachikkala on 4/22/18.
 */
public class ExecutorSvc {

    private static final Executor executor =  Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors(), new ThreadFactory() {

        @Override
        public Thread newThread(Runnable r) {

            Thread thread = new Thread(r);
            thread.setDaemon(true);

            return thread;
        }
    });

    public static <T> List<T> run(List<Supplier<T>> suppliers){

        List<CompletableFuture<T>> futures =suppliers.stream()
                .map(supplier -> CompletableFuture.supplyAsync( supplier, executor)).
                        collect(toList());

        return futures.stream().map(CompletableFuture::join).collect(Collectors.toList());
    }

    public static void shutdown() {

       /* if (executor != null)
            executor.shutdown();*/

    }
}
