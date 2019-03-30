package moneymgr.ui;

import moneymgr.model.Account;

/** Interface for paying attention to selections in the account list */
public interface AccountSelectionListener {

	/**
	 * Process an account selection
	 *
	 * @param account The selected account
	 * @param update  If false, we may skip if the account was already selected<br>
	 *                If true, force the selection action regardless.
	 */
	void accountSelected(Account account, boolean update);

}