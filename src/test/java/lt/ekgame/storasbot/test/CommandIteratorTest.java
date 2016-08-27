package lt.ekgame.storasbot.test;

import static org.junit.Assert.*;

import org.junit.Test;

import lt.ekgame.storasbot.commands.engine.CommandFlags;
import lt.ekgame.storasbot.commands.engine.CommandIterator;
import lt.ekgame.storasbot.commands.engine.DuplicateFlagException;

public class CommandIteratorTest {

	@Test
	public void test() {
		String command = "command token #$%@#$ .... \"fsfsdf sdfsdf ssdf sdfs ffdsf\" 78985465 654.65 \nsdfsdfsdf sdfsdf sd fsd f\nsfsdf\nsdfsdf";
		CommandIterator iterator = new CommandIterator(command);
		assertEquals("command", iterator.getToken().get());
		assertEquals("token", iterator.getToken().get());
		assertEquals("#$%@#$", iterator.getToken().get());
		assertEquals("....", iterator.getToken().get());
		assertEquals("fsfsdf sdfsdf ssdf sdfs ffdsf", iterator.getString().get());
		assertEquals(78985465, (int)iterator.getInteger().get());
		assertEquals(654.65, (double)iterator.getDouble().get(), 0.0001);
		assertEquals("sdfsdfsdf sdfsdf sd fsd f", iterator.getLine().get());
		assertEquals("sfsdf\nsdfsdf", iterator.getEverything().get());
		assertFalse(iterator.getToken().isPresent());
		assertFalse(iterator.getDouble().isPresent());
		assertFalse(iterator.getInteger().isPresent());
		assertFalse(iterator.getString().isPresent());
	}
	
	@Test
	public void test2() {
		String command = "\"qweqwe\" sdfsdf    zxczxczxc";
		CommandIterator iterator = new CommandIterator(command);
		assertEquals("qweqwe", iterator.getString().get());
		assertEquals("sdfsdf", iterator.getString().get());
		assertEquals("zxczxczxc", iterator.getString().get());
	}
	
	@Test
	public void test3() {
		String command = "test asd asd asd asd asd asd\nqe qeq rwerrqeqwe qweqwe qe q\nnv vbnvb nbv nv nbv nbv nbv n";
		CommandIterator iterator = new CommandIterator(command);
		assertEquals("test asd asd asd asd asd asd", iterator.getLine().get());
		assertEquals("qe qeq rwerrqeqwe qweqwe qe q", iterator.getLine().get());
		assertEquals("nv vbnvb nbv nv nbv nbv nbv n", iterator.getLine().get());
	}

	@Test
	public void test4() {
		String command = "sdf\nsdf sdf\nsdf sf";
		CommandIterator iterator = new CommandIterator(command);
		assertEquals("sdf\nsdf sdf\nsdf sf", iterator.getEverything().get());
	}
	
	@Test
	public void test5() {
		String command = "";
		CommandIterator iterator = new CommandIterator(command);
		assertFalse(iterator.getToken().isPresent());
	}
	
	@Test
	public void test6() throws DuplicateFlagException {
		String command = "test -g:789 -h:\"aasd asdasd asdd\" -j -p:test";
		CommandIterator iterator = new CommandIterator(command);
		assertEquals("test", iterator.getToken().get());
		CommandFlags flags = iterator.getFlags();
		assertEquals(789, flags.getFlagInt("g", -1));
		assertEquals("aasd asdasd asdd", flags.getFlag("h", ""));
		assertEquals(true, flags.getFlagBool("j"));
		assertEquals(false, flags.getFlagBool("n"));
		assertEquals("test", flags.getFlag("p", ""));
	}
}
