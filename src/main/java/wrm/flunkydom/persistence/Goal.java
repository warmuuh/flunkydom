package wrm.flunkydom.persistence;

import java.time.Instant;
import java.util.List;
import java.util.zip.ZipFile;

public record Goal (
    String id,
    String inputQuery,
    Instant creationTime,
    Instant finishTime,
    String status,
    String result,
    String log,
    int steps,
    String agent,
    List<Artifact> artifacts
    ){


   public record Artifact (String filename, byte[] content){}
}
