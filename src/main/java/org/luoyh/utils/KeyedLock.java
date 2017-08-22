package org.luoyh.utils;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;

/**
 *
 * @author luoyh(Roy) - Aug 22, 2017
 */
public enum KeyedLock {
	
	LOCK;
	
	private static final ConcurrentMap<Object, Lock> LOCKS = new ConcurrentHashMap<>();
	
	public <T> Lock acquire(T key) throws InterruptedException {
		Lock lock = new Lock() {
			
			@Override
			public void close() throws Exception {
				if (Thread.currentThread().getId() != threadId) {
					throw new IllegalStateException("Illegal thread");
				}
				if (-- lockedCount <= 0) {
					LOCKS.remove(key);
					mutex.countDown();
				}
			}
		};
		
		for (;;) {
			Lock l = LOCKS.putIfAbsent(key, lock);
			if (null == l) {
				return lock;
			}
			if (Thread.currentThread().getId() == l.threadId) {
				l.lockedCount ++;
				return l;
			}
			l.mutex.await();
		}
	}
	
	
	public abstract static class Lock implements AutoCloseable {
		protected final CountDownLatch mutex = new CountDownLatch(1);
        protected final long threadId = Thread.currentThread().getId();
        protected int lockedCount = 1;
	}

}
