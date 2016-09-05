package lt.ekgame.storasbot.commands.eval;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.apache.commons.lang3.StringUtils;

import jdk.nashorn.api.scripting.NashornScriptEngineFactory;
import lt.ekgame.storasbot.StorasDiscord;
import lt.ekgame.storasbot.utils.Utils;
import net.dv8tion.jda.entities.Message;
import net.dv8tion.jda.entities.TextChannel;
import net.dv8tion.jda.events.message.guild.GuildMessageUpdateEvent;
import net.dv8tion.jda.hooks.ListenerAdapter;
import net.dv8tion.jda.utils.SimpleLog;

@SuppressWarnings("restriction")
public class CodeExecutor extends ListenerAdapter  {
	
	private static Pattern MATCH_CODE_BLOCK = Pattern.compile("```(\\w*\\h*\\n)?(((.*)\\n?)*)```");
	
	public static final SimpleLog LOG = SimpleLog.getLog("Code Executor");
	
	private Message original;
	private Message response;
	private int timesRunned = 0;
	private long lastUpdate;
	
	private ScriptEngine engine;
	private Console console;
	
	public CodeExecutor(Message original) {
		this.original = original;
		this.lastUpdate = System.currentTimeMillis();
		
		StorasDiscord.sendMessage((TextChannel)original.getChannel(), "```Executing...```", (message) -> {
			response = message;
			ExecutorService executor = Executors.newSingleThreadExecutor();
			executor.execute(() -> runCode(false));
		});
	}
	
	public String getCode(Message message, String label) {
		Matcher matcher = MATCH_CODE_BLOCK.matcher(message.getContent());
		String code;
		
		if (matcher.find())
			code = matcher.group(2).trim();
		else
			code = message.getContent().substring(label.length()+2).trim();
		
		return code.isEmpty() ? null : code;
	}
	
	private void setupEngine() throws ScriptException {
		NashornScriptEngineFactory factory = new NashornScriptEngineFactory();
        engine = factory.getScriptEngine(new String[]{"-strict", "--no-java", "--no-syntax-extensions"});
        Bindings bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);
        bindings.remove("print");
        bindings.remove("echo");
        bindings.remove("load");
        bindings.remove("loadWithNewGlobal");
        bindings.remove("exit");
        bindings.remove("quit");
        console = new Console();
        
        engine.put("print", console);
	}
	
	private void runCode(boolean update) {
		timesRunned++;
		if (update)
			StorasDiscord.editMessage(response, "```Please wait...```");
		
		String code = getCode(original, "eval");
		if (code == null) {
			StorasDiscord.editMessage(response, "Result: ```Nothing to execute.```");
		}
		else {
			ExecutorService executor = Executors.newSingleThreadExecutor();
			Future<?> future = executor.submit(() -> {
				try {
					setupEngine();
					
					long time = System.currentTimeMillis();
			        Object value = engine.eval(code);
			        String output = console.finalizeOutput();
			        long elapsed = System.currentTimeMillis() - time;
			        
			        String result = original.getAuthor().getAsMention() + " **Result:** ```";
			        
			        int maxLines = 15;
			        int maxCharacters = 1500;
			        
			        if (!output.isEmpty()) {
			        	boolean truncated = false;
			        	if (StringUtils.countMatches(output, "\n") > (maxLines-1)) {
			        		truncated = true;
			        		String[] lines = output.split("\n");
			        		output = "";
			        		for (int i = 0; i < maxLines; i++)
			        			output += lines[i] + "\n";
			        	}
			        	
			        	if (output.length() > maxCharacters) {
			        		truncated = true;
			        		output = output.substring(0, maxCharacters);
				        }
			        	
			        	result += Utils.escapeMarkdownBlock(output);
			        	if (truncated)
			        		result += "[OUTPUT TRUNCATED]";
			        }
			        
			        if (value != null) {
			        	if (!output.isEmpty())
			        		result += "\n-- VALUE (" + value.getClass().getName() + ") --\n";
			        	result += value.toString();
			        }
			        
			        if (value == null && output.isEmpty()) {
			        	result += "diff\n!== NO OUTPUT ==!";
			        }
			        
			        result += "```";
			        result += String.format(" `Run time: %dms` ", elapsed); 
			        
			        if (timesRunned > 1)
			        	result += String.format(" `Executions: %d` ", timesRunned); 
			        
			        StorasDiscord.editMessage(response, result);
				} catch (Exception e) {
					StorasDiscord.editMessage(response, String.format("**Error:** ```diff\n- %s```", e.getMessage()));
				}
			});
			
			ExecutorService executor2 = Executors.newSingleThreadExecutor();
			executor2.submit(() -> {
				try {
					Thread.sleep(5000);
					if (!future.isDone() && !future.isCancelled()) {
						future.cancel(true);
						StorasDiscord.editMessage(response, String.format("**Error:** ```diff\n- %s```", "Timeout - code took too long to complete."));
					}
				} catch (Exception e) {}
			});
		}
	}
	
	@Override
	public void onGuildMessageUpdate(GuildMessageUpdateEvent event) {
		if (System.currentTimeMillis() - lastUpdate > 10*60*1000) {
			StorasDiscord.getJDA().removeEventListener(this);
		}
		else if (event.getMessage().getId().equals(original.getId())) {
			lastUpdate = System.currentTimeMillis();
			original = event.getMessage();
			runCode(true);
		}
	}
	
	public class Console {
		
		private StringWriter output;
		private BufferedWriter writer;
		
		Console() {
			output = new StringWriter();
			writer = new BufferedWriter(output);
		}
		
		public void line(Object input) throws IOException {
			writer.write(input.toString());
			writer.newLine();
		}
		
		public void format(String format, Object... input) throws IOException {
			writer.write(String.format(format, input));
		}
		
		private String finalizeOutput() throws IOException {
			writer.flush();
			writer.close();
			return output.toString();
		}
	}
}
