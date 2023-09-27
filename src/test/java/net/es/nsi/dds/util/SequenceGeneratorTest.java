package net.es.nsi.dds.util;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

public class SequenceGeneratorTest {
  @Test
  public void testGetNext() {
    long initialValue = SequenceGenerator.INSTANCE.getNext();
    long secondValue = SequenceGenerator.INSTANCE.getNext();

    // Ensure that the second value is greater than the initial value
    assertTrue(initialValue < secondValue);
  }

  @Test
  public void testGetNextConcurrent() {
    // Create multiple threads to access the getNext() method concurrently
    int numThreads = 10;
    long[] values = new long[numThreads];
    Thread[] threads = new Thread[numThreads];

    for (int i = 0; i < numThreads; i++) {
      int finalI = i;
      threads[i] = new Thread(() -> {
        long nextValue = SequenceGenerator.INSTANCE.getNext();
        values[finalI] = nextValue;
      });
      threads[i].start();
    }

    // Wait for all threads to complete
    for (Thread thread : threads) {
      try {
        thread.join();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }

    // Values are not sorted due to thread scheduling.
    Arrays.sort(values);

    // Ensure that all values are unique and in increasing order
    for (int i = 0; i < numThreads; i++) {
      for (int j = i + 1; j < numThreads; j++) {
        assert (values[i] < values[j]);
      }
    }
  }
}