package Events;

import il.cshaifasweng.OCSFMediatorExample.entities.Warning;
public class WarningEvent { // Kept from TicTacToe
	private Warning warning;

	public Warning getWarning() {
		return warning;
	}

	public WarningEvent(Warning warning) {
		this.warning = warning;
	}
}
