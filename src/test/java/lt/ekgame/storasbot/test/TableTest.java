package lt.ekgame.storasbot.test;

import static org.junit.Assert.*;

import org.junit.Test;

import lt.ekgame.storasbot.utils.TableRenderer;

public class TableTest {

	@Test
	public void test() {
		TableRenderer table = new TableRenderer();
		table.setHeader("test", "test 2", "test 3");
		table.addRow("a", "b", "c");
		table.addRow("aa", "bb", "cc");
		table.addRow("aaa", "bbb", "ccc");
		table.addRow("aaaa", "bbbb", "cccc");
		table.addRow("aaaaa", "bbbbb", "ccccc");
		table.addRow("aaaaaa", "bbbbbb", "cccccc");
		table.addRow("aaaaaaa", "bbbbbbb", "ccccccc");
		System.out.println();
		assertEquals("test    │test 2  │test 3 \n"
					+"════════╪════════╪═══════\n"
					+"a       │b       │c      \n"
					+"aa      │bb      │cc     \n"
					+"aaa     │bbb     │ccc    \n"
					+"aaaa    │bbbb    │cccc   \n"
					+"aaaaa   │bbbbb   │ccccc  \n"
					+"aaaaaa  │bbbbbb  │cccccc \n"
					+"aaaaaaa │bbbbbbb │ccccccc",
		table.build());
	}
	
	@Test
	public void test_noHeader() {
		TableRenderer table = new TableRenderer();
		table.addRow("a", "b", "c");
		table.addRow("aa", "bb", "cc");
		table.addRow("aaa", "bbb", "ccc");
		table.addRow("aaaa", "bbbb", "cccc");
		table.addRow("aaaaa", "bbbbb", "ccccc");
		table.addRow("aaaaaa", "bbbbbb", "cccccc");
		table.addRow("aaaaaaa", "bbbbbbb", "ccccccc");
		System.out.println();
		assertEquals("a       │b       │c      \n"
					+"aa      │bb      │cc     \n"
					+"aaa     │bbb     │ccc    \n"
					+"aaaa    │bbbb    │cccc   \n"
					+"aaaaa   │bbbbb   │ccccc  \n"
					+"aaaaaa  │bbbbbb  │cccccc \n"
					+"aaaaaaa │bbbbbbb │ccccccc",
		table.build());
	}

}
