package com.ctrip.hermes.portal.resource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ctrip.hermes.core.utils.PlexusComponentLocator;
import com.ctrip.hermes.meta.entity.Datasource;
import com.ctrip.hermes.meta.entity.Property;
import com.ctrip.hermes.meta.entity.Server;
import com.ctrip.hermes.meta.entity.Storage;
import com.ctrip.hermes.portal.service.DefaultMetaServiceWrapper;
import com.ctrip.hermes.portal.service.MetaServiceWrapper;

@Path("/metaserver/")
@Singleton
@Produces(MediaType.APPLICATION_JSON)
public class MetaServerResource {

	private static final Logger logger = LoggerFactory.getLogger(MetaServerResource.class);

	private MetaServiceWrapper metaService = PlexusComponentLocator.lookup(MetaServiceWrapper.class,
	      DefaultMetaServiceWrapper.ID);

	@GET
	@Path("servers")
	public List<String> getServers() {
		List<Server> servers = metaService.getServers();
		List<String> result = new ArrayList<>();
		for (Server server : servers) {
			result.add(String.format("%s:%s", server.getHost(), server.getPort()));
		}
		return result;
	}

	@GET
	@Path("kafka/zookeeper")
	public String getZookeeperList() {
		Map<String, Storage> storages = metaService.getStorages();
		for (Storage storage : storages.values()) {
			if ("kafka".equals(storage.getType())) {
				for (Datasource ds : storage.getDatasources()) {
					for (Property property : ds.getProperties().values()) {
						if ("zookeeper.connect".equals(property.getName())) {
							return property.getValue();
						}
					}
				}
			}
		}
		logger.warn("getZookeeperList failed");
		return "";
	}

	@GET
	@Path("kafka/brokers")
	public String getKafkaBrokerList() {
		Map<String, Storage> storages = metaService.getStorages();
		for (Storage storage : storages.values()) {
			if ("kafka".equals(storage.getType())) {
				for (Datasource ds : storage.getDatasources()) {
					for (Property property : ds.getProperties().values()) {
						if ("bootstrap.servers".equals(property.getName())) {
							return property.getValue();
						}
					}
				}
			}
		}
		logger.warn("getKafkaBrokerList failed");
		return "";
	}
}
