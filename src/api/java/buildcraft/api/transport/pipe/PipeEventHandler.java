package buildcraft.api.transport.pipe;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/** Designates a method that will receive a pipe event. The method must be public and take a single parameter that
 * extends {@link PipeEvent}. <br>
 * An example is:<br>
 * <code>
    &#64;PipeEventHandler <br>
    public void sideCheck(PipeEventItem.SideCheck sideCheck) {<br>
     // Logic ommited<br>
    }
 *  
 *   </code> */
@Retention(RUNTIME)
@Target(METHOD)
public @interface PipeEventHandler {
    /** Designates the priority that the handler will be given. All handlers in vanilla BuildCraft use
     * {@link PipeEventPriority#NORMAL}, so this is given for other pipe mods to fire before or after all BC logic has
     * taken place. */
    PipeEventPriority priority() default PipeEventPriority.NORMAL;

    /** If true then the event handler will be called even if an event has already been cancelled. */
    boolean receiveCancelled() default false;
}
