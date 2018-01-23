package automode.db;

import java.io.IOException;


import automode.util.Constants;
import org.apache.log4j.Logger;
import org.voltdb.VoltTable;
import org.voltdb.VoltType;
import org.voltdb.client.Client;
import org.voltdb.client.ClientConfig;
import org.voltdb.client.ProcCallException;

public class VoltDBQuery {

	private Client client  = null;
	final static Logger logger = Logger.getLogger(VoltDBQuery.class);

	public boolean isConstantColumn(String table, String col, int threshold, String thresholdType, String serverURL){
		Long distinctColCount = 0l;
		Long totalRowsCount = 0l;
		String distinctColQuery = "SELECT COUNT(DISTINCT ("+col+")) FROM "+table;
		String totalRowsQuery = "SELECT COUNT(*) FROM "+table;
		if (client == null){
			client = connectToVoltDB(serverURL);
		}
		try {
			VoltTable[] colResults = client.callProcedure("@AdHoc", distinctColQuery).getResults();
			distinctColCount = (Long) colResults[0].fetchRow(0).get(0, VoltType.BIGINT);

			//Absolute threshold type
			if(Constants.ThresholdType.ABSOLUTE.getValue().equals(thresholdType)) {
				if(distinctColCount<threshold)
					return true;
				return false;
			}

			VoltTable[] tabResults = client.callProcedure("@AdHoc", totalRowsQuery).getResults();
			totalRowsCount = (Long) tabResults[0].fetchRow(0).get(0, VoltType.BIGINT);

			//logger.debug(" Distinct Percentage "+(distinctColCount*1.0/totalRowsCount)*100);

			//Percentage threshold value
			if(Constants.ThresholdType.PERCENTAGE.getValue().equals(thresholdType)) {
				if((distinctColCount*1.0/totalRowsCount)*100 < threshold)
					return true;
			}
		} catch (IOException | ProcCallException e) {
			e.printStackTrace();
		}
		//Close connection
		//closeConnection(client)
		return false;
	}
	
	/**
	 * Connect to a single server with retry. Limited exponential backoff.
	 * No timeout. This will run until the process is killed if it's not
	 * able to connect.
	 *
	 * @param server hostname:port or just hostname (hostname can be ip).
	 */
	Client connectToVoltDB(String server) {
		int sleep = Constants.VoltdbNumber.SLEEP_TIME.getValue();
		ClientConfig clientConfig = new ClientConfig(Constants.Voltdb.USERNAME.getValue(), Constants.Voltdb.PASSWORD.getValue());
		Client client = org.voltdb.client.ClientFactory.createClient(clientConfig);
		while (true) {
			try {
				client.createConnection(server);
				logger.debug("Connected to VoltDB node at: %s.\n" + server);
				return client;			}
			catch (Exception e) {
				logger.debug("Connection failed - retrying in %d second(s).\n" + sleep / 1000);
				try { Thread.sleep(sleep); } catch (Exception interruted) {}
				if (sleep < Constants.VoltdbNumber.MAX_SLEEP_TIME.getValue()) sleep += sleep;
			}
		}
	}

	public void closeConnection(){
		if(client!=null){
			try {
				logger.debug("Closing Voltdb connection");
				client.close();
				this.client = null;
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
