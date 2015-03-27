package com.ctrip.hermes.message.internal;

import java.util.List;

import org.unidal.lookup.annotation.Inject;
import org.unidal.tuple.Pair;

import com.ctrip.hermes.core.pipeline.DefaultPipelineContext;
import com.ctrip.hermes.core.pipeline.Pipeline;
import com.ctrip.hermes.core.pipeline.PipelineContext;
import com.ctrip.hermes.core.pipeline.PipelineSink;
import com.ctrip.hermes.core.pipeline.ValveRegistry;
import com.ctrip.hermes.message.StoredMessage;

public class DeliverPipeline implements Pipeline<Void> {

	@Inject
	private ValveRegistry m_valveRegistry;

	@SuppressWarnings("unchecked")
	@Override
	public Void put(Object payload) {
		Pair<List<StoredMessage<byte[]>>, PipelineSink<Void>> pair = (Pair<List<StoredMessage<byte[]>>, PipelineSink<Void>>) payload;

		PipelineContext<Void> ctx = new DefaultPipelineContext<>(m_valveRegistry.getValveList(), pair.getValue());

		ctx.next(pair.getKey());

		return null;
	}

}
