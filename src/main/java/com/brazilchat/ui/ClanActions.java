package com.brazilchat.ui;

import javax.annotation.Nullable;

/** What the panel can ask the clan service to do. Keeps Swing code free of service types. */
public interface ClanActions
{
	void refresh();

	void signUp(String eventId);

	void cancelSignUp(String eventId);

	/** action: mark_paid | mark_unpaid | remove | restore */
	void staffSignupAction(String signupId, String action, @Nullable String reason);

	void staffSetSignupsOpen(String eventId, boolean open);

	void createAnnouncement(String title, String body, boolean pinned);

	void updateAnnouncement(String announcementId, String title, String body, boolean pinned);

	void deleteAnnouncement(String announcementId);
}
