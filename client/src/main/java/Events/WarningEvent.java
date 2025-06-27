package Events;

import il.cshaifasweng.OCSFMediatorExample.entities.Warning;

public class WarningEvent {
	private Warning warning;

	public WarningEvent(Warning warning) {
		this.warning = warning;
	}

	public Warning getWarning() {
		return warning;
	}
}
