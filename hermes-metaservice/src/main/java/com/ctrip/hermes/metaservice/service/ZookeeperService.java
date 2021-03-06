package com.ctrip.hermes.metaservice.service;

import com.ctrip.hermes.meta.entity.Topic;

/**
 * @author Leo Liang(jhliang@ctrip.com)
 *
 */
public interface ZookeeperService {
	public void ensureConsumerLeaseZkPath(Topic topic);

	public void ensureBrokerLeaseZkPath(Topic topic);

	public void deleteConsumerLeaseZkPath(String topicName);

	public void deleteBrokerLeaseZkPath(String topicName);

	public void deleteConsumerLeaseZkPath(Topic t, String consumerGroupName);

	public void updateZkMetaVersion(int version) throws Exception;

	public void persist(String path, Object data) throws Exception;

}
