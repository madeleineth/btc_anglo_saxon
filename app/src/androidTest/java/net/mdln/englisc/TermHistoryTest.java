package net.mdln.englisc;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class TermHistoryTest {

    @Test
    public void history() {
        Context ctx = InstrumentationRegistry.getInstrumentation().getTargetContext();
        try (TermHistory history = new TermHistory(ctx, TermHistory.Location.IN_MEMORY, 0)) {
            history.recordId(100, 1000);
            history.recordId(101, 1001);
            Set<Integer> expectedIds = new HashSet();
            assertEquals(expectedIds, history.getIds(0));
            expectedIds.add(101);
            assertEquals(expectedIds, history.getIds(1));
            expectedIds.add(100);
            assertEquals(expectedIds, history.getIds(2));
            assertEquals(expectedIds, history.getIds(3));
        }
    }
}