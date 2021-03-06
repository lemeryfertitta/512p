package tcp;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Vector;

import org.junit.Ignore;
import org.junit.Test;

import client.tcp.Client;
import client.tcp.TCPClient;

public class TestIntegration {
	
	private static final String SERVICE_HOST = "localhost";
	private static final int SERVICE_PORT = 9083;
	private static final Object[][] COMMAND_LIST = {
		// delete customer
		{"newCustomer",1},
		{"addRooms", 1, "room-1", 10, 10},
		{"reserveRoom", 1, "custid", "room-1"},
		{"queryRooms", 1, "room-1"},
		{"deleteCustomer", 1, "custid"},
		{"queryRooms", 1, "room-1"},
		
		//itinerary
		{"newCustomer", 1},
		{"addFlight", 1, 55, 10, 10},
		{"addFlight", 1, 66, 10, 10},
		{"addRooms", 1, "montreal", 10, 10},
		{"addCars", 1, "montreal", 10, 10},
		{"reserveItinerary", 1, "custid", new Vector(Arrays.asList(new String[]{"55","66"})), "montreal", true, true},
		{"queryCustomerInfo", 1, "custid"},
		{"queryFlight", 1, 55},
		{"queryFlight", 1, 66},
		{"queryRooms", 1, "montreal"},
		{"queryCars", 1, "montreal"},
		{"deleteCustomer", 1, "custid"},
		{"queryCustomerInfo", 1, "custid"},
		{"queryFlight", 1, 55},
		{"queryFlight", 1, 66},
		{"queryRooms", 1, "montreal"},
		{"queryCars", 1, "montreal"},
		
		//failed itinerary
		{"newCustomer", 1},
		{"addFlight", 1, 55, 10, 10},
		{"queryFlight", 1, 55},
		{"reserveItinerary", 1, "custid", new Vector(Arrays.asList(new String[]{"55"})), "boston", true, true},
		{"queryFlight", 1, 55}
	};
	private static final Object[] RESULT_LIST = new Object[]
	{
		null, // test 1
		true,
		true,
		9,
		true,
		10,
		
		null, // test 2
		true,
		true,
		true,
		true,
		true,
		null, // silly to test querycustomer
		9,
		9,
		9,
		9,
		true,
		null,
		10,
		10,
		10,
		10,
		
		null, // test 3
		true,
		20, // adding 10 seats
		false,
		20
	};
	

	
	@Ignore @Test
	/**
	 * Utility to automate manual testing and verification of many commands.
	 * @throws Exception
	 */
	public void testClientAPI() throws Exception {
		TCPClient client = new TCPClient(SERVICE_HOST, SERVICE_PORT);
		
		int custid = 0;
		for(int i=0; i<RESULT_LIST.length; i++){
			Object[] command = COMMAND_LIST[i];
			for(int j=1; j<COMMAND_LIST[i].length; j++){
				if(COMMAND_LIST[i][j].equals("custid")){
					command = Arrays.copyOf(COMMAND_LIST[i], COMMAND_LIST[i].length);
					command[j] = custid;
				}
			}
			Object response = client.send(command);
			if (command[0].equals("newCustomer"))
			{
				// save the response for later assertions
				custid = (int)response;
			}
			else if(command[0].equals("queryCustomerInfo")){
				// Pretty long string, seems unnecessary to test exact equivalence.
				continue;
			}
			else{
				assertEquals(RESULT_LIST[i], response);
			}
		}
		client.close();
	}
	
	class ConcurrentClient extends TCPClient implements Runnable{
		
		private Object[][] commandList;
		private ArrayList<Object> responses;

		public ConcurrentClient(String serviceHost, int servicePort, Object[][] commandList)
				throws Exception {
			super(serviceHost, servicePort);
			this.commandList = commandList;
			responses = new ArrayList<Object>();
		}
		
		public ArrayList<Object> getResponses(){
			return responses;
		}

		@Override
		public void run() {
			for(Object[] command : commandList){
				try {
					Object response = send(command);
					responses.add(response);
				} catch (ClassNotFoundException | IOException e) {
					e.printStackTrace();
				}
			}
		}
		
	}
	
	@Ignore @Test
	public void TestConcurrentClientRequestsNoConflicts() throws Exception{
		int numClientsToTest = 100;
		
		ConcurrentClient[] clients = new ConcurrentClient[numClientsToTest];
		Thread[] clientThreads = new Thread[numClientsToTest];
		for(int i=0; i<clients.length; i++){
			Object[][] commands = {
					{"addFlight",i,i,i,i},
					{"addCars",i,"loc",i,i},
					{"addRooms",i,"loc",i,i},
					{"newCustomer",i},
					{"newCustomerId",i*2,i},
					{"queryCars",i,"loc"},
					{"queryRooms",i,"loc"},
					{"queryFlight",i,i},
					{"queryCustomerInfo",i,i},
					{"queryFlightPrice",i,i},
					{"queryCarPrice",i,i},
					{"queryRoomPrice",i,i},
					{"reserveFlight",i,i,i},
					{"reserveCar",i,i,"loc",i},
					{"reserveRoom",i,i,"loc",i},
					{"reserveItinerary", i, i, new Vector(Arrays.asList(new String[]{Integer.toString(i)})), "loc", true, true},
					{"deleteFlight",i,i},
					{"deleteCar",i,i},
					{"deleteRoom",i,i},
					{"deleteCustomer",i,i},
					};
			clients[i] = new ConcurrentClient(SERVICE_HOST, SERVICE_PORT, commands);
			clientThreads[i] = new Thread(clients[i]);
		}
		for(Thread t : clientThreads){
			t.start();
		}
		
	}
	
	@Test
	public void TestConcurrentClientRequestsWithConflicts() throws Exception{
		int numClientsToTest = 100;
		TCPClient initClient = new TCPClient(SERVICE_HOST, SERVICE_PORT);
		initClient.send("addFlight",1,1,50,50);
		initClient.send("addRooms",1,"loc",50,50);
		initClient.send("addCars",1,"loc",50,50);
		ConcurrentClient[] clients = new ConcurrentClient[numClientsToTest];
		Thread[] clientThreads = new Thread[numClientsToTest];
		for(int i=0; i<clients.length; i++){
			Object[][] commands = {
					{"newCustomerId",i,i},
					{"reserveItinerary", i, i, new Vector(Arrays.asList(new String[]{"1"})), "loc", true, true},
					};
			clients[i] = new ConcurrentClient(SERVICE_HOST, SERVICE_PORT, commands);
			clientThreads[i] = new Thread(clients[i]);
		}
		for(Thread t : clientThreads){
			t.start();
		}
		int numFailures = 0;
		int numSuccesses = 0;
		for(int i=0; i<clients.length; i++){
			clientThreads[i].join();
			if ((boolean)clients[i].getResponses().get(1) == false){
				numFailures++;
			}
			else{
				numSuccesses++;
			}
		}
		assertEquals(0,(int)initClient.send("queryCars",1,"loc"));
		assertEquals(0,(int)initClient.send("queryRooms",1,"loc"));
		assertEquals(0,(int)initClient.send("queryFlight",1,1));
		assertEquals(50, numFailures);
		assertEquals(50, numSuccesses);
		
		
	}

}
