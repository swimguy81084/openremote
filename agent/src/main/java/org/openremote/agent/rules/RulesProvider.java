package org.openremote.agent.rules;

import org.kie.api.KieServices;
import org.kie.api.io.Resource;

import java.util.stream.Stream;

public interface RulesProvider {

    Stream<Resource> getResources(KieServices kieServices);

}
