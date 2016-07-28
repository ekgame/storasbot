package lt.ekgame.storasbot.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import lt.ekgame.storasbot.commands.engine.BotCommandContext;
import lt.ekgame.storasbot.commands.engine.Command;
import lt.ekgame.storasbot.commands.engine.CommandFlags;
import lt.ekgame.storasbot.commands.engine.CommandIterator;
import lt.ekgame.storasbot.commands.engine.CommandReference;
import lt.ekgame.storasbot.commands.engine.CommandResult;

@CommandReference(isGuild=true, isPrivate=true, labels = {"roll", "dice"})
public class CommandRoll implements Command<BotCommandContext>  {
	
	public Random random = new Random();
	
	@Override
	public String getHelp(CommandFlags flags) {
		return "Usage:\n"
			 + "$roll                          \n"
			 + "$roll <max>[ <max> ...]       \n"
			 + "$roll <n>d<max>[ <n>d<max> ...]\n"
			 + "\n"
			 + "Examples:     \n"
			 + "$roll 10      \n"
		     + "$roll 4d6 2d20\n"
		     + "\n"
		     + "Generates a random number (or multiple) up to a given maximum. If no arguments are given or all of the defined dice are invalid, the maximum is 100.";
		
	}

	@Override
	public CommandResult execute(CommandIterator command, BotCommandContext context) {
		List<Dice> dices = new ArrayList<>();
		List<String> failed = new ArrayList<>();
		Optional<String> rawDice;
		do {
			rawDice = command.getToken();
			if (rawDice.isPresent()) {
				Dice die = Dice.parse(random, rawDice.get());
				if (die != null && die.validate())
					dices.add(die);
				else
					failed.add(rawDice.get());
			}
		}
		while (rawDice.isPresent());
		
		if (dices.size() == 0) 
			dices.add(new Dice(random, 1, 100));
		
		String failedString = "";
		if (failed.size() > 0) {
			failedString = "\n_Invalid " + (failed.size() == 1 ? "die " : "dice: ");
			failedString += failed.stream().map((die) -> "\""+die+"\"").collect(Collectors.joining(", ")) + "._";
		}
		
		String mention = context.getSender().getAsMention();
		
		if (dices.size() == 1 && dices.get(0).number == 1) {
			context.reply(mention + " **" + dices.get(0).getResults()[0] + "**" + failedString);
		}
		else {
			List<Integer> rolls = new ArrayList<>();
			for (Dice dice : dices)
				for (int number : dice.getResults())
					rolls.add(number);
			
			String addition = "**" + rolls.stream().map((num) -> num.toString()).collect(Collectors.joining("** + **")) + "**";
			int sum = rolls.stream().mapToInt(Integer::intValue).sum();
			context.reply(mention + " " + addition + " = ***" + sum + "***" + failedString);
		}
		
		return failed.size() > 0 ? CommandResult.FAIL : CommandResult.OK;
	}
	
	private static class Dice {
		
		Random rng;
		int number, max;
		
		private Dice(Random rng, int number, int max) {
			this.rng = rng;
			this.number = number;
			this.max = max;
		}
		
		private int[] getResults() {
			return IntStream
				.generate(() -> rng.nextInt(max)+1)
				.limit(number)
				.toArray();
		}
		
		private boolean validate() {
			return number >= 1 && max >= 2;
		}
		
		public static Dice parse(Random rng, String dice) {
			dice = dice.trim().toLowerCase();
			try {
				if (dice.contains("d")) {
					int index = dice.indexOf("d");
					int number = Integer.parseInt(dice.substring(0, index));
					int max = Integer.parseInt(dice.substring(index + 1, dice.length()));
					return new Dice(rng, number, max);
				} else {
					int max = Integer.parseInt(dice);
					return new Dice(rng, 1, max);
				}
			} catch (Exception e) {
				return null;
			}
		}
	}
}
