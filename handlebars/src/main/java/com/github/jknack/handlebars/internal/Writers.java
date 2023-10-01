/**
 * Copyright (c) 2012-2015 Edgar Espina
 * <p>
 * This file is part of Handlebars.java.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.github.jknack.handlebars.internal;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayDeque;
import java.util.Deque;

final class Writers {
  /**
   * The {@link WriterProvider} to which {@link #withWriter(IOFunction)} delegates.
   */
  private static final WriterProvider DELEGATE = Boolean.getBoolean(
      "hbs.useThreadLocalWriterPools") ? new PooledWriterProvider() : new FreshWriterProvider();

  private Writers() {
  }

  static <T> T withWriter(final IOFunction<Writer, T> consumer) throws IOException {
    return DELEGATE.withWriter(consumer);
  }

  @FunctionalInterface
  interface IOFunction<T, R> {
    R apply(T t) throws IOException;
  }

  interface WriterProvider {
    <T> T withWriter(IOFunction<Writer, T> consumer) throws IOException;
  }

  private static final class FreshWriterProvider implements WriterProvider {
    @Override
    public <T> T withWriter(final IOFunction<Writer, T> consumer) throws IOException {
      return consumer.apply(new FastStringWriter());
    }
  }

  private static final class PooledWriterProvider implements WriterProvider {
    /**
     * The thread-local writer pools.
     */
    private static final ThreadLocal<Deque<FastStringWriter>> WRITERS = ThreadLocal.withInitial(
        ArrayDeque::new);

    @Override
    public <T> T withWriter(final IOFunction<Writer, T> consumer) throws IOException {
      Deque<FastStringWriter> pool = WRITERS.get();

      FastStringWriter pooledWriter = pool.pollLast();
      FastStringWriter writer = pooledWriter != null ? pooledWriter : new FastStringWriter();

      try {
        return consumer.apply(writer);
      } finally {
        writer.reset();
        pool.addLast(writer);
      }
    }
  }
}
