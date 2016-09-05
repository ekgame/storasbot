package lt.ekgame.storasbot.test;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;

import lt.ekgame.storasbot.utils.Utils;

public class BlockSlipttingTest {

	@Test
	public void test_list() {
		String bigString = "";
		for (int i = 0; i < 100; i++) {
			if (i != 0) bigString += "\n";
			bigString += (i+1) + ". Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.";
		}
		System.out.println(bigString.length());
		
		List<String> list = Utils.messageSplit(bigString, 2000);
		for (String item : list) {
			System.out.println(item.length());
			System.out.println(item);
		}
	}
	
	@Test
	public void test_blob() {
		String bigString = "";
		for (int i = 0; i < 100; i++) {
			bigString += "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. ";
		}
		System.out.println(bigString.length());
		
		List<String> list = Utils.messageSplit(bigString, 2000);
		for (String item : list) {
			System.out.println(item.length());
			System.out.println(item);
		}
	}
}
