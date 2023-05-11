package wrm.flunkydom.persistence;

import java.util.List;

public record AgentConfig(
    String id,
    String name,
    String agentTemplateId,
    List<String> activeTools
){

}
