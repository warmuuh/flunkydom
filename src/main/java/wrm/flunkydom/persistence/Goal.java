package wrm.flunkydom.persistence;

import java.time.Instant;

public record Goal (
    String id,
    String inputQuery,
    Instant creationTime,
    Instant finishTime,
    String status,
    String result,
    String log,
    int steps
    ){

}
