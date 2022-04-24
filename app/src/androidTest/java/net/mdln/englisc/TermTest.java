package net.mdln.englisc;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TermTest {

    @Test
    public void unlinkifyTermHtml() {
        assertEquals("foo baz", Term.unlinkifyTermHtml("<a href=\"florp\">foo</a> baz"));
    }
}
