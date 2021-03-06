package com.ctrip.hermes.portal.resource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.codehaus.plexus.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.unidal.dal.jdbc.DalException;

import com.alibaba.fastjson.JSON;
import com.ctrip.hermes.core.utils.PlexusComponentLocator;
import com.ctrip.hermes.meta.entity.Codec;
import com.ctrip.hermes.meta.entity.Datasource;
import com.ctrip.hermes.meta.entity.Endpoint;
import com.ctrip.hermes.meta.entity.Meta;
import com.ctrip.hermes.meta.entity.Property;
import com.ctrip.hermes.meta.entity.Storage;
import com.ctrip.hermes.meta.entity.Topic;
import com.ctrip.hermes.metaservice.service.MetaServiceWrapper;
import com.ctrip.hermes.portal.server.RestException;

@Path("/meta/")
@Singleton
@Produces(MediaType.APPLICATION_JSON)
public class MetaResource {

	private static final Logger logger = LoggerFactory.getLogger(MetaResource.class);

	private MetaServiceWrapper metaService = PlexusComponentLocator.lookup(MetaServiceWrapper.class);

	@GET
	@Path("codecs")
	public Response getCodecs() {
		List<Codec> codecs = new ArrayList<Codec>(metaService.getCodecs().values());
		Collections.sort(codecs, new Comparator<Codec>() {
			@Override
			public int compare(Codec o1, Codec o2) {
				return o1.getType().compareTo(o2.getType());
			}
		});
		return Response.status(Status.OK).entity(codecs).build();
	}

	@GET
	@Path("storages")
	public Response getStorages() {
		List<Storage> storages = new ArrayList<Storage>(metaService.getStorages().values());
		Collections.sort(storages, new Comparator<Storage>() {
			@Override
			public int compare(Storage o1, Storage o2) {
				return o1.getType().compareTo(o2.getType());
			}
		});
		return Response.status(Status.OK).entity(storages).build();
	}

	@GET
	@Path("endpoints")
	public Response getEndpoints() {
		List<Endpoint> endpoints = new ArrayList<Endpoint>(metaService.getEndpoints().values());
		Collections.sort(endpoints, new Comparator<Endpoint>() {
			@Override
			public int compare(Endpoint o1, Endpoint o2) {
				return o1.getType().compareTo(o2.getType());
			}
		});
		return Response.status(Status.OK).entity(endpoints).build();
	}

	@GET
	@Path("topics/names")
	public Response getTopicNames() {
		List<String> topicNames = new ArrayList<String>();
		for (Topic topic : metaService.findTopicsByPattern(".*")) {
			topicNames.add(topic.getName());
		}
		Collections.sort(topicNames);
		return Response.status(Status.OK).entity(topicNames).build();
	}

	@GET
	@Path("refresh")
	public Response refreshMeta() {
		Meta meta = null;
		try {
			meta = metaService.findLatestMeta();
			if (meta == null) {
				throw new RestException("Meta not found", Status.NOT_FOUND);
			}
		} catch (Exception e) {
			logger.warn("refresh meta failed", e);
			throw new RestException(e, Status.INTERNAL_SERVER_ERROR);
		}
		return Response.status(Status.OK).entity(meta).build();
	}

	@DELETE
	@Path("storages/{type}/{id}/delprop")
	public Response delDsProp(@PathParam("type") String type, @PathParam("id") String id, @QueryParam("name") String name) {
		logger.info("Delete datasource property: {} {}", id, name);
		if (StringUtils.isBlank(id) || StringUtils.isBlank(name)) {
			throw new RestException(String.format("ID: %s or Name: %s is blank", id, name), Status.BAD_REQUEST);
		}

		Meta meta = metaService.getMeta();
		List<Datasource> dss = meta.getStorages().get(type).getDatasources();
		for (Datasource ds : dss) {
			if (ds.getId().equals(id) && ds.getProperties().containsKey(name)) {
				ds.getProperties().remove(name);
				try {
					metaService.updateMeta(meta);
					return Response.status(Status.OK).build();
				} catch (DalException e) {
					throw new RestException(e, Status.INTERNAL_SERVER_ERROR);
				}
			}
		}
		throw new RestException(String.format("Property %s not found in %s", name, id), Status.NOT_FOUND);
	}

	@POST
	@Path("storages/{type}/{id}/update")
	public Response updateDatasource(@PathParam("type") String type, String content) {
		if (StringUtils.isEmpty(content)) {
			throw new RestException("HTTP POST body is empty", Status.BAD_REQUEST);
		}
		Meta meta = metaService.getMeta();
		Storage storage = metaService.getStorages().get(type);
		if (storage == null) {
			throw new RestException("Invalid storage type", Status.NOT_FOUND);
		}

		List<Datasource> datasources = storage.getDatasources();
		Datasource dsn = JSON.parseObject(content, Datasource.class);
		normalizeDatasource(dsn);
		for (int idx = 0; idx < datasources.size(); idx++) {
			Datasource ds = datasources.get(idx);
			if (ds.getId().equals(dsn.getId())) {
				datasources.remove(idx);
				datasources.add(dsn);
				try {
					metaService.updateMeta(meta);
				} catch (DalException e) {
					throw new RestException(e, Status.INTERNAL_SERVER_ERROR);
				}
				return Response.status(Status.OK).entity(storage).build();
			}
		}

		throw new RestException("Datasource id not found: " + dsn.getId(), Status.NOT_FOUND);
	}

	private void normalizeDatasource(Datasource ds) {
		List<Property> properties = new ArrayList<Property>(ds.getProperties().values());
		ds.getProperties().clear();
		for (Property p : properties) {
			if (StringUtils.isNotBlank(p.getName())) {
				ds.getProperties().put(p.getName(), p);
			}
		}
	}

	@POST
	@Path("endpoints")
	public Response addEndpoint(String content) {
		logger.info("Add endpoint: " + content);

		if (StringUtils.isEmpty(content)) {
			throw new RestException("HTTP POST body is empty", Status.BAD_REQUEST);
		}

		Endpoint endpoint = null;
		try {
			endpoint = JSON.parseObject(content, Endpoint.class);
		} catch (Exception e) {
			logger.error("Parse consumer failed, content: {}", content, e);
			throw new RestException(e, Status.BAD_REQUEST);
		}

		if (metaService.getEndpoints().containsKey(endpoint.getId())) {
			throw new RestException(String.format("Endpoint %s already exists.", endpoint.getId()), Status.CONFLICT);
		}

		try {
			if (metaService.addEndpoint(endpoint)) {
				return Response.status(Status.CREATED).build();
			}
		} catch (Exception e) {
			logger.error("Add endpoint failed.", e);
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
		}
		return Response.status(Status.INTERNAL_SERVER_ERROR).build();
	}

	@DELETE
	@Path("endpoints/{id}")
	public Response deleteEndpoint(@PathParam("id") String id) {
		logger.info("Delete endpoint: {}", id);
		try {
			metaService.deleteEndpoint(id);
		} catch (Exception e) {
			logger.warn("Delete endpoint failed", e);
			throw new RestException(e, Status.INTERNAL_SERVER_ERROR);
		}
		return Response.status(Status.OK).build();
	}

	@POST
	public Response updateMeta(String content, @Context HttpServletRequest req) {
		logger.debug("update meta, content {}", content);
		if (StringUtils.isEmpty(content)) {
			throw new RestException("HTTP POST body is empty", Status.BAD_REQUEST);
		}

		Meta meta = null;
		try {
			meta = JSON.parseObject(content, Meta.class);
		} catch (Exception e) {
			logger.warn("parse meta failed with content:{}", content);
			throw new RestException(e, Status.BAD_REQUEST);
		}
		try {
			boolean result = metaService.updateMeta(meta);
			if (result == false) {
				return Response.status(Status.NOT_MODIFIED).build();
			}
		} catch (Exception e) {
			logger.warn("update meta failed", e);
			throw new RestException(e, Status.INTERNAL_SERVER_ERROR);
		}
		return Response.status(Status.CREATED).entity(meta).build();
	}
}
