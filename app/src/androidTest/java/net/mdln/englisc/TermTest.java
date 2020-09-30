package net.mdln.englisc;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TermTest {

    @Test
    public void unlinkifyTermHtml() {
        assertEquals("foo baz", Term.unlinkifyTermHtml("<a href=\"florp\">foo</a> baz"));
    }
}
