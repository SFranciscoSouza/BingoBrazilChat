package com.brazilchat.clan;

import java.util.Collections;
import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ClanSnapshotTest
{
	private static ClanSnapshot snapshot(String clan, String title)
	{
		return new ClanSnapshot(true, 123L, "Zezinho", clan, title, 10, null, 492, Collections.emptyList(), null);
	}

	@Test
	public void onlyListedMembersOfBrazilChatAreConfirmed()
	{
		assertTrue(snapshot("Brazil Chat", "Recruit").isConfirmedMember());
		// In the clan channel but not on the member list (a guest): no title.
		assertFalse(snapshot("Brazil Chat", null).isConfirmedMember());
		assertFalse(snapshot("Outro Cla", "Recruit").isConfirmedMember());
		assertFalse(ClanSnapshot.loggedOut().isConfirmedMember());
	}
}
