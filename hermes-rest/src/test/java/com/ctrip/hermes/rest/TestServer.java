package com.ctrip.hermes.rest;

import org.eclipse.jetty.webapp.WebAppContext;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class TestServer extends JettyServer {
	public static String HOST = "http://localhost:1357";

	public static void main(String[] args) throws Exception {
		TestServer server = new TestServer();

		server.startServer();
		server.startWebapp();
		server.stopServer();
	}

	@Before
	public void before() throws Exception {
		// System.setProperty("devMode", "true");
		super.startServer();
	}

	@Override
	protected String getContextPath() {
		return "/";
	}

	@Override
	protected int getServerPort() {
		return 1357;
	}

	@Override
	protected void postConfigure(WebAppContext context) {
	}

	@Test
	public void startWebapp() throws Exception {
		// open the page in the default browser
		waitForAnyKey();
	}
}
