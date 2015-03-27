package com.ctrip.hermes.broker.channel;

import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;

import com.ctrip.hermes.core.pipeline.AbstractValveRegistry;

public class BrokerReceiverValveRegistry extends AbstractValveRegistry implements Initializable {

	public static final String ID = "broker-receiver";

	@Override
	public void initialize() throws InitializationException {
	}

}
