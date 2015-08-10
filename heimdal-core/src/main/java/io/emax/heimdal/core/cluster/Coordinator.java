package io.emax.heimdal.core.cluster;

import java.io.IOException;
import java.net.InetAddress;

import org.zeromq.ZBeacon;
import org.zeromq.ZBeacon.Listener;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import io.emax.heimdal.core.Application;
import io.emax.heimdal.core.ApplicationConfiguration;

public class Coordinator {
	// Static resolver
	private static Coordinator coordinator;

	public static Coordinator getInstance() {
		if (coordinator == null) {
			coordinator = new Coordinator();
		}

		return coordinator;
	}
	// End Static resolver, begin actual class.

	private Coordinator() {
		try {
			JsonFactory jsonFact = new JsonFactory();
			ClusterInfo cluster = ClusterInfo.getInstance();
			ApplicationConfiguration config = Application.getConfig();

			
			ObjectMapper mapper = new ObjectMapper(jsonFact);
			ObjectWriter writer = mapper.writerFor(Server.class);
			String beaconString = writer.writeValueAsString(cluster.getThisServer());
			
			ZBeacon beacon = new ZBeacon(config.getClusterLocation(), config.getClusterPort(),
					beaconString.getBytes(), false);
			beacon.setListener(new Listener() {
				@Override
				public void onBeacon(InetAddress arg0, byte[] arg1) {
					try {
						String request = new String(arg1);
						JsonParser jsonParser = jsonFact.createParser(request);
						jsonParser.nextToken();

						Server server = new ObjectMapper().readValue(jsonParser, Server.class);
						server.setLastCommunication(System.currentTimeMillis());

						if (!cluster.getServers().contains(server)) {
							server.setOriginator(false);
							cluster.getServers().add(server);
						} else if (server.isOriginator()) {
							int existingLocation = cluster.getServers().indexOf(server);
							cluster.getServers().get(existingLocation).setLastCommunication(System.currentTimeMillis());
						}
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			});

			beacon.start();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
