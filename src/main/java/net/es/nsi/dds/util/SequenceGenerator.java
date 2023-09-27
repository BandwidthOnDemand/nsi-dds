package net.es.nsi.dds.util;

import java.util.concurrent.atomic.AtomicLong;

public enum SequenceGenerator {
  INSTANCE;

  private static final AtomicLong value = new AtomicLong(0);

  public long getNext() {
    return value.getAndIncrement();
  }
}
