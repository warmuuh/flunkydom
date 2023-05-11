package wrm.llm.tools;

import static java.util.Optional.of;

import java.time.Instant;
import java.time.temporal.ChronoUnit;


public class WaitFunction extends Tool<Void> {

  public WaitFunction() {
    super("wait", """
        a service to wait for a given time. input should be the time to wait in format 'Hours:minutes'""");
  }

  @Override
  public ToolOutcome execute(String input, Void configuration) {
    String[] split = input.split(":");
    if (split.length != 2) {
      return ToolOutcome.of("Format not known. It should be HH:mm.");
    }

    int hour = Integer.parseInt(split[0]);
    int minute = Integer.parseInt(split[1]);
    Instant waitUntil = Instant.now().plus(minute, ChronoUnit.MINUTES).plus(hour, ChronoUnit.HOURS);
    return new ToolOutcome("Done waiting for " + input, of(waitUntil));
  }

}
