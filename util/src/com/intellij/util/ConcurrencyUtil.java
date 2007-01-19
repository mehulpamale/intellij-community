package com.intellij.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.NonNls;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;

/**
 * @author cdr
 */
public class ConcurrencyUtil {
  /**
   * invokes and waits all tasks using threadPool, avoiding thread starvation on the way
   * @lookat http://gafter.blogspot.com/2006/11/thread-pool-puzzler.html
   */
  public static <T> List<Future<T>> invokeAll(@NotNull Collection<Callable<T>> tasks, ExecutorService executorService) throws Throwable {
    if (executorService == null) { 
      for (Callable<T> task : tasks) {
        task.call();
      }
      return null;
    }

    List<Future<T>> futures = new ArrayList<Future<T>>(tasks.size());
    boolean done = false;
    try {
      for (Callable<T> t : tasks) {
        Future<T> future = executorService.submit(t);
        futures.add(future);
      }
      // force unstarted futures to execute using the current thread
      for (Future f : futures) ((FutureTask)f).run();
      for (Future f : futures) {
        try {
          f.get();
        }
        catch (CancellationException ignore) {
        }
        catch (ExecutionException e) {
          Throwable cause = e.getCause();
          if (cause != null) {
            throw cause;
          }
        }
      }
      done = true;
    }
    finally {
      if (!done) {
        for (Future f : futures) {
          f.cancel(false);
        }
      }
    }
    return futures;
  }

  /**
   * @return defaultValue if there is no entry in the map (in that case defaultValue is placed into the map), or corresponding value if entry already exists
   */
  @NotNull
  public static <K,V> V cacheOrGet(ConcurrentMap<K, V> map, @NotNull final K key, @NotNull final V defaultValue) {
    V prev = map.putIfAbsent(key, defaultValue);
    return prev == null ? defaultValue : prev;
  }
  public static ThreadPoolExecutor newSingleThreadExecutor(@NonNls final String threadFactoryName) {
    return new ThreadPoolExecutor(1, 1,
                                    0L, TimeUnit.MILLISECONDS,
                                    new LinkedBlockingQueue<Runnable>(), new ThreadFactory() {
      public Thread newThread(final Runnable r) {
        return new Thread(r, threadFactoryName);
      }
    });
  }
  public static ScheduledExecutorService newSingleScheduledThreadExecutor(@NonNls final String threadFactoryName) {
    ScheduledThreadPoolExecutor threadPoolExecutor = new ScheduledThreadPoolExecutor(1);
    threadPoolExecutor.setThreadFactory(new ThreadFactory() {
      public Thread newThread(final Runnable r) {
        return new Thread(r, threadFactoryName);
      }
    });
    return threadPoolExecutor;
  }
}
