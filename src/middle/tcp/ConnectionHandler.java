package middle.tcp;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Calendar;
import java.util.Vector;

import server.Car;
import server.Customer;
import server.Flight;
import server.RMHashtable;
import server.Room;
import server.Trace;

public class ConnectionHandler implements Runnable {

	Socket clientSocket;
	
	Socket flightSocket = null;
	Socket carSocket = null;
	Socket roomSocket = null;

	private String flightRMHostname;
	private String carRMHostname;
	private String roomRMHostname;

	private int flightRMPort;
	private int carRMPort;
	private int roomRMPort;

	private CustomerInfo customerInfo;

	public ConnectionHandler(Socket clientSocket, String[] rmHostanmes, int[] rmPorts, CustomerInfo customerInfo){
		this.clientSocket = clientSocket;
		this.customerInfo = customerInfo;

		flightRMHostname = rmHostanmes[0];
		flightRMPort = rmPorts[0];

		carRMHostname = rmHostanmes[1];
		carRMPort = rmPorts[1];

		roomRMHostname = rmHostanmes[2];
		roomRMPort = rmPorts[2];
	}
	
	private void closeRMSockets() throws IOException{
		if(flightSocket != null){
			flightSocket.close();
		}
		if(roomSocket != null){
			roomSocket.close();
		}
		if(carSocket != null){
			carSocket.close();
		}
	}

	public Object sendToRM(Object... message) throws IOException, ClassNotFoundException{
		Socket rmSocket = null;
		String methodName = (String) message[0];
		if (methodName.contains("Flight")){
			if (flightSocket == null)
				flightSocket = new Socket(flightRMHostname, flightRMPort);
			rmSocket = flightSocket;
		}
		else if (methodName.contains("Room")){
			if (roomSocket == null)
				roomSocket = new Socket(roomRMHostname, roomRMPort);
			rmSocket = roomSocket;
		}
		else if (methodName.contains("Car")){
			if (carSocket == null)
				carSocket = new Socket(carRMHostname, carRMPort);
			rmSocket = flightSocket;
		}
		else{
			Trace.error("Method: " + methodName + " cannot be resolved for proper resource manager dispatch.");
		}
		ObjectInputStream rmInput = new ObjectInputStream(rmSocket.getInputStream());
		ObjectOutputStream rmOutput = new ObjectOutputStream(rmSocket.getOutputStream());
		rmOutput.writeObject(message);
		Object rmInputObj = rmInput.readObject();
		rmInput.close();
		rmOutput.close();
		return rmInputObj;
	}

	@Override
	public void run() {
		try {
			// get thread ID for stamping messages
			String threadId = Long.toString(Thread.currentThread().getId());

			// get client in/out streams
			ObjectInputStream clientInput = new ObjectInputStream(clientSocket.getInputStream());
			Object[] clientInputObj;
			String clientInputMethodName;
			ObjectOutputStream clientOutput = new ObjectOutputStream(clientSocket.getOutputStream());
			Object clientOutputResponse = null;

			// keep talking to the client until they leave us
			while((clientInputObj = (Object[])clientInput.readObject()) != null){
				clientInputMethodName = (String) clientInputObj[0];
				System.out.println( threadId + "| Client request: " + clientInputMethodName);

				if(clientInputMethodName.contains("Flight") && !clientInputMethodName.contains("reserve")){
					clientOutputResponse = sendToRM(clientInputObj);
				}
				else if(clientInputMethodName.contains("Room") && !clientInputMethodName.contains("reserve")){
					clientOutputResponse = sendToRM(clientInputObj);
				}
				else if(clientInputMethodName.contains("Car") && !clientInputMethodName.contains("reserve")){
					clientOutputResponse = sendToRM(clientInputObj);
				}
				else{
					// parse and use manager...
					switch(clientInputMethodName)
					{
					case "newCustomer":
					{
						int id = (int)clientInputObj[1];
						clientOutputResponse = customerInfo.newCustomer(id);
						break;
					}
					case "deleteCustomer":
					{
						int id = (int)clientInputObj[1];
						int customer = (int)clientInputObj[2];
						clientOutputResponse = customerInfo.deleteCustomer(id, customer, this);
						break;
					}
					case "queryCustomerInfo":
					{
						int id = (int)clientInputObj[1];
						int customer = (int)clientInputObj[2];
						clientOutputResponse = customerInfo.queryCustomerInfo(id, customer);
						break;
					}
					case "reserveItinerary":
					{
						int id = (int)clientInputObj[1];
						int customer = (int)clientInputObj[2];
						Vector flightNumbers = (Vector)clientInputObj[3];
						String location = (String)clientInputObj[4];
						boolean car = (boolean)clientInputObj[5];
						boolean room = (boolean)clientInputObj[6];
						clientOutputResponse = customerInfo.reserveItinerary(id, customer, flightNumbers, location, car, room, this);
						break;
					}
					case "newCustomerId":
					{
						int id = (int)clientInputObj[1];
						int customer = (int)clientInputObj[2];
						clientOutputResponse = customerInfo.newCustomerId(id, customer);
						break;
					}
					case "reserveRoom":
					{
						int id = (int)clientInputObj[1];
						int customer = (int)clientInputObj[2];
						String location = (String)clientInputObj[3];
						clientOutputResponse = customerInfo.reserveRoom(id, customer, location, this);
						break;
					}
					case "reserveCar":
					{
						int id = (int)clientInputObj[1];
						int customer = (int)clientInputObj[2];
						String location = (String)clientInputObj[3];
						clientOutputResponse = customerInfo.reserveCar(id, customer, location, this);
						break;
					}
					case "reserveFlight":
					{
						int id = (int)clientInputObj[1];
						int customer = (int)clientInputObj[2];
						int flightNumber = (int)clientInputObj[3];
						clientOutputResponse = customerInfo.reserveFlight(id, customer, flightNumber, this);
						break;
					}
					default:
					{
						Trace.warn("Error: Unknown method name sent from client: " + clientInputMethodName);
						break;
					}
					}
				}
				clientOutput.writeObject(clientOutputResponse);
			}
			closeRMSockets();
			clientInput.close();
			clientOutput.close();
			clientSocket.close();

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
}
