package com.s2000coaching.service;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Runs something only once the surrounding transaction has committed — here, removing a file whose
 * row was just changed, so that a rollback never leaves a row pointing at a file that is already gone.
 * Outside a transaction (a unit test, a script) it simply runs at once.
 */
final class AfterCommit {

	private AfterCommit() {
	}

	static void run(Runnable action) {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					action.run();
				}
			});
		} else {
			action.run();
		}
	}
}
