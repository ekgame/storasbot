package lt.ekgame.storasbot.commands.engine;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE}) 
public @interface CommandReference {
	public boolean isPrivate() default false;
	public boolean isGuild() default false;
	public String[] labels();
}
