package wrm.llm.agent;

import java.time.Instant;
import java.util.Optional;

public record NextTask (
    AgentTask task,
    Optional<Instant> scheduleContinuation
){

}
