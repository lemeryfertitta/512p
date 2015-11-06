// -------------------------------
// Adapted from Kevin T. Manley
// CSE 593
// -------------------------------

package server;

import java.util.*;
import javax.jws.WebService;


@WebService(endpointInterface = "server.ws.ResourceManager")
public class ResourceManagerImpl implements server.ws.ResourceManager {
    
    protected RMHashtable m_itemHT = new RMHashtable();
    
    
    // Basic operations on RMItem //
    
    // Read a data item.
    private RMItem readData(int id, String key) {
        synchronized(m_itemHT) {
            return (RMItem) m_itemHT.get(key);
        }
    }

    // Write a data item.
    private void writeData(int id, String key, RMItem value) {
        synchronized(m_itemHT) {
            m_itemHT.put(key, value);
        }
    }
    
    // Remove the item out of storage.
    protected RMItem removeData(int id, String key) {
        synchronized(m_itemHT) {
            return (RMItem) m_itemHT.remove(key);
        }
    }
    
    
    // Basic operations on ReservableItem //
    
    // Delete the entire item.
    protected boolean deleteItem(int id, String key) {
        Trace.info("RM::deleteItem(" + id + ", " + key + ") called.");
        ReservableItem curObj = (ReservableItem) readData(id, key);
        // Check if there is such an item in the storage.
        if (curObj == null) {
            Trace.warn("RM::deleteItem(" + id + ", " + key + ") failed: " 
                    + " item doesn't exist.");
            return false;
        } else {
            if (curObj.getReserved() == 0) {
                removeData(id, curObj.getKey());
                Trace.info("RM::deleteItem(" + id + ", " + key + ") OK.");
                return true;
            }
            else {
                Trace.info("RM::deleteItem(" + id + ", " + key + ") failed: "
                        + "some customers have reserved it.");
                return false;
            }
        }
    }
    
    // Query the number of available seats/rooms/cars.
    protected int queryNum(int id, String key) {
        Trace.info("RM::queryNum(" + id + ", " + key + ") called.");
        ReservableItem curObj = (ReservableItem) readData(id, key);
        int value = 0;  
        if (curObj != null) {
            value = curObj.getCount();
        }
        Trace.info("RM::queryNum(" + id + ", " + key + ") OK: " + value);
        return value;
    }    
    
    // Query the price of an item.
    protected int queryPrice(int id, String key) {
        Trace.info("RM::queryPrice(" + id + ", " + key + ") called.");
        ReservableItem curObj = (ReservableItem) readData(id, key);
        int value = 0; 
        if (curObj != null) {
            value = curObj.getPrice();
        }
        Trace.info("RM::queryPrice(" + id + ", " + key + ") OK: $" + value);
        return value;
    }

    // Reserve an item.
  /*  protected boolean reserveItem(int id, int customerId, 
                                  String key, String location) {
        Trace.info("RM::reserveItem(" + id + ", " + customerId + ", " 
                + key + ", " + location + ") called.");
        // Read customer object if it exists (and read lock it).
        Customer cust = (Customer) readData(id, Customer.getKey(customerId));
        if (cust == null) {
            Trace.warn("RM::reserveItem(" + id + ", " + customerId + ", " 
                   + key + ", " + location + ") failed: customer doesn't exist.");
            return false;
        } 
        
        // Check if the item is available.
        ReservableItem item = (ReservableItem) readData(id, key);
        if (item == null) {
            Trace.warn("RM::reserveItem(" + id + ", " + customerId + ", " 
                    + key + ", " + location + ") failed: item doesn't exist.");
            return false;
        } else if (item.getCount() == 0) {
            Trace.warn("RM::reserveItem(" + id + ", " + customerId + ", " 
                    + key + ", " + location + ") failed: no more items.");
            return false;
        } else {
            // Do reservation.
            cust.reserve(key, location, item.getPrice());
            writeData(id, cust.getKey(), cust);
            
            // Decrease the number of available items in the storage.
            item.setCount(item.getCount() - 1);
            item.setReserved(item.getReserved() + 1);
            
            Trace.warn("RM::reserveItem(" + id + ", " + customerId + ", " 
                    + key + ", " + location + ") OK.");
            return true;
        }
    }
    
    */
    // Flight operations //
    
    // Create a new flight, or add seats to existing flight.
    // Note: if flightPrice <= 0 and the flight already exists, it maintains 
    // its current price.
    @Override
    public boolean addFlight(int id, int flightNumber, 
                             int numSeats, int flightPrice) {
		synchronized(m_itemHT)
		{
	        Trace.info("RM::addFlight(" + id + ", " + flightNumber 
	                + ", $" + flightPrice + ", " + numSeats + ") called.");
	        Flight curObj = (Flight) readData(id, Flight.getKey(flightNumber));
	        if (curObj == null) {
	            // Doesn't exist; add it.
	            Flight newObj = new Flight(flightNumber, numSeats, flightPrice);
	            writeData(id, newObj.getKey(), newObj);
	            Trace.info("RM::addFlight(" + id + ", " + flightNumber 
	                    + ", $" + flightPrice + ", " + numSeats + ") OK.");
	        } else {
	            // Add seats to existing flight and update the price.
	            curObj.setCount(curObj.getCount() + numSeats);
	            if (flightPrice > 0) {
	                curObj.setPrice(flightPrice);
	            }
	            writeData(id, curObj.getKey(), curObj);
	            Trace.info("RM::addFlight(" + id + ", " + flightNumber 
	                    + ", $" + flightPrice + ", " + numSeats + ") OK: "
	                    + "seats = " + curObj.getCount() + ", price = $" + flightPrice);
	        }
	        return(true);			
		}
    }

    @Override
    public boolean deleteFlight(int id, int flightNumber) {
		synchronized(m_itemHT)
		{
	        return deleteItem(id, Flight.getKey(flightNumber));			
		}
    }

    // Returns the number of empty seats on this flight.
    @Override
    public int queryFlight(int id, int flightNumber) {
        return queryNum(id, Flight.getKey(flightNumber));
    }

    // Returns price of this flight.
    public int queryFlightPrice(int id, int flightNumber) {
        return queryPrice(id, Flight.getKey(flightNumber));
    }
    
    public boolean flightExists(int id, int flightNumber) {
		synchronized(m_itemHT)
		{
	        Flight curObj = (Flight) readData(id, Flight.getKey(flightNumber));
	        return curObj != null;
		}
    }

    /*
    // Returns the number of reservations for this flight. 
    public int queryFlightReservations(int id, int flightNumber) {
        Trace.info("RM::queryFlightReservations(" + id 
                + ", #" + flightNumber + ") called.");
        RMInteger numReservations = (RMInteger) readData(id, 
                Flight.getNumReservationsKey(flightNumber));
        if (numReservations == null) {
            numReservations = new RMInteger(0);
       }
        Trace.info("RM::queryFlightReservations(" + id + 
                ", #" + flightNumber + ") = " + numReservations);
        return numReservations.getValue();
    }
    */
    
    /*
    // Frees flight reservation record. Flight reservation records help us 
    // make sure we don't delete a flight if one or more customers are 
    // holding reservations.
    public boolean freeFlightReservation(int id, int flightNumber) {
        Trace.info("RM::freeFlightReservations(" + id + ", " 
                + flightNumber + ") called.");
        RMInteger numReservations = (RMInteger) readData(id, 
                Flight.getNumReservationsKey(flightNumber));
        if (numReservations != null) {
            numReservations = new RMInteger(
                    Math.max(0, numReservations.getValue() - 1));
        }
        writeData(id, Flight.getNumReservationsKey(flightNumber), numReservations);
        Trace.info("RM::freeFlightReservations(" + id + ", " 
                + flightNumber + ") OK: reservations = " + numReservations);
        return true;
    }
    */


    // Car operations //

    // Create a new car location or add cars to an existing location.
    // Note: if price <= 0 and the car location already exists, it maintains 
    // its current price.
    @Override
    public boolean addCars(int id, String location, int numCars, int carPrice) {
		synchronized(m_itemHT)
        {
			Trace.info("RM::addCars(" + id + ", " + location + ", " 
	                + numCars + ", $" + carPrice + ") called.");
	        Car curObj = (Car) readData(id, Car.getKey(location));
	        if (curObj == null) {
	            // Doesn't exist; add it.
	            Car newObj = new Car(location, numCars, carPrice);
	            writeData(id, newObj.getKey(), newObj);
	            Trace.info("RM::addCars(" + id + ", " + location + ", " 
	                    + numCars + ", $" + carPrice + ") OK.");
	        } else {
	            // Add count to existing object and update price.
	            curObj.setCount(curObj.getCount() + numCars);
	            if (carPrice > 0) {
	                curObj.setPrice(carPrice);
	            }
	            writeData(id, curObj.getKey(), curObj);
	            Trace.info("RM::addCars(" + id + ", " + location + ", " 
	                    + numCars + ", $" + carPrice + ") OK: " 
	                    + "cars = " + curObj.getCount() + ", price = $" + carPrice);
	        }
	        return(true);
        }
    }

    // Delete cars from a location.
    @Override
    public boolean deleteCars(int id, String location) {
		synchronized(m_itemHT)
		{
	        return deleteItem(id, Car.getKey(location));			
		}
    }

    // Returns the number of cars available at a location.
    @Override
    public int queryCars(int id, String location) {
        return queryNum(id, Car.getKey(location));
    }

    // Returns price of cars at this location.
    @Override
    public int queryCarsPrice(int id, String location) {
        return queryPrice(id, Car.getKey(location));
    }
    
    public boolean carExists(int id, String location) {
		synchronized(m_itemHT)
		{
			Car curObj = (Car) readData(id, Car.getKey(location));
	        return curObj != null;
		}
    }
    

    // Room operations //

    // Create a new room location or add rooms to an existing location.
    // Note: if price <= 0 and the room location already exists, it maintains 
    // its current price.
    @Override
    public boolean addRooms(int id, String location, int numRooms, int roomPrice) {
		synchronized(m_itemHT)
		{
	        Trace.info("RM::addRooms(" + id + ", " + location + ", " 
	                + numRooms + ", $" + roomPrice + ") called.");
	        Room curObj = (Room) readData(id, Room.getKey(location));
	        if (curObj == null) {
	            // Doesn't exist; add it.
	            Room newObj = new Room(location, numRooms, roomPrice);
	            writeData(id, newObj.getKey(), newObj);
	            Trace.info("RM::addRooms(" + id + ", " + location + ", " 
	                    + numRooms + ", $" + roomPrice + ") OK.");
	        } else {
	            // Add count to existing object and update price.
	            curObj.setCount(curObj.getCount() + numRooms);
	            if (roomPrice > 0) {
	                curObj.setPrice(roomPrice);
	            }
	            writeData(id, curObj.getKey(), curObj);
	            Trace.info("RM::addRooms(" + id + ", " + location + ", " 
	                    + numRooms + ", $" + roomPrice + ") OK: " 
	                    + "rooms = " + curObj.getCount() + ", price = $" + roomPrice);
	        }
	        return(true);
		}
    }

    // Delete rooms from a location.
    @Override
    public boolean deleteRooms(int id, String location) {
		synchronized(m_itemHT)
		{
	        return deleteItem(id, Room.getKey(location));			
		}
    }

    // Returns the number of rooms available at a location.
    @Override
    public int queryRooms(int id, String location) {
        return queryNum(id, Room.getKey(location));
    }
    
    // Returns room price at this location.
    @Override
    public int queryRoomsPrice(int id, String location) {
        return queryPrice(id, Room.getKey(location));
    }
    
    public boolean roomExists(int id, String location) {
		synchronized(m_itemHT)
		{
			Room curObj = (Room) readData(id, Room.getKey(location));
	        return curObj != null;
		}
    }

    // Customer operations //

    @Override
    public int newCustomer(int id) {
    	System.out.println("Should not be called");
    	return -1;
    }

    // This method makes testing easier.
    @Override
    public boolean newCustomerId(int id, int customerId) {
    	System.out.println("Should not be called");
    	return false;
    }

    // Delete customer from the database. 
    @Override
    public boolean deleteCustomer(int id, int customerId) {
    	System.out.println("Should not be called");
    	return false;
    }
	
	@Override
	public void deleteReservationWithKey(int id, String key, int count) {
		synchronized(m_itemHT)
		{
	        ReservableItem item = (ReservableItem) readData(id, key);
	        item.setReserved(item.getReserved() - count);
	        item.setCount(item.getCount() + count);
	        Trace.info("RM::deleteCustomer(" + id + "): reserved/available = " 
	                + item.getReserved() + "/" + item.getCount());			
		}
	}

    // Return data structure containing customer reservation info. 
    // Returns null if the customer doesn't exist. 
    // Returns empty RMHashtable if customer exists but has no reservations.
    public RMHashtable getCustomerReservations(int id, int customerId) {
        Trace.info("RM::getCustomerReservations(" + id + ", " 
                + customerId + ") called.");
        Customer cust = (Customer) readData(id, Customer.getKey(customerId));
        if (cust == null) {
            Trace.info("RM::getCustomerReservations(" + id + ", " 
                    + customerId + ") failed: customer doesn't exist.");
            return null;
        } else {
            return cust.getReservations();
        }
    }

    // Return a bill.
    @Override
    public String queryCustomerInfo(int id, int customerId) {
        Trace.info("RM::queryCustomerInfo(" + id + ", " + customerId + ") called.");
        Customer cust = (Customer) readData(id, Customer.getKey(customerId));
        if (cust == null) {
            Trace.warn("RM::queryCustomerInfo(" + id + ", " 
                    + customerId + ") failed: customer doesn't exist.");
            // Returning an empty bill means that the customer doesn't exist.
            return "";
        } else {
            String s = cust.printBill();
            Trace.info("RM::queryCustomerInfo(" + id + ", " + customerId + "): \n");
            System.out.println(s);
            return s;
        }
    }

    // Add flight reservation to this customer.  
    @Override
    public boolean reserveFlight(int id, int customerId, int flightNumber) {
  //      return reserveItem(id, customerId, 
    //            Flight.getKey(flightNumber), String.valueOf(flightNumber));
		synchronized(m_itemHT)
		{
			String key = Flight.getKey(flightNumber);
			String location = String.valueOf(flightNumber);
		
		    ReservableItem item = (ReservableItem) readData(id, key);
		    if (item == null) {
		        Trace.warn("RM::reserveFlight(" + id + ", " + customerId + ", " 
		                + key + ", " + location + ") failed: item doesn't exist.");
		        return false;
		    } else if (item.getCount() == 0) {
		        Trace.warn("RM::reserveItem(" + id + ", " + customerId + ", " 
		                + key + ", " + location + ") failed: no more items.");
		        return false;
		    } else {            
		        // Decrease the number of available items in the storage.
		        item.setCount(item.getCount() - 1);
		        item.setReserved(item.getReserved() + 1);
        
		        Trace.warn("RM::reserveItem(" + id + ", " + customerId + ", " 
		                + key + ", " + location + ") OK.");
		        return true;
			}		
		}
    }

    // Add car reservation to this customer. 
    @Override
    public boolean reserveCar(int id, int customerId, String location) {
		synchronized(m_itemHT)
		{
			String key = Car.getKey(location);
	        ReservableItem item = (ReservableItem) readData(id, key);
	        if (item == null) {
	            Trace.warn("RM::reserveCar(" + id + ", " + customerId + ", " 
	                    + key + ", " + location + ") failed: item doesn't exist.");
	            return false;
	        } else if (item.getCount() == 0) {
	            Trace.warn("RM::reserveCar(" + id + ", " + customerId + ", " 
	                    + key + ", " + location + ") failed: no more items.");
	            return false;
	        } else {            
	            // Decrease the number of available items in the storage.
	            item.setCount(item.getCount() - 1);
	            item.setReserved(item.getReserved() + 1);
            
	            Trace.warn("RM::reserveItem(" + id + ", " + customerId + ", " 
	                    + key + ", " + location + ") OK.");
	            return true;
			}			
		}
     //   return reserveItem(id, customerId, Car.getKey(location), location);
    }

    // Add room reservation to this customer. 
    @Override
    public boolean reserveRoom(int id, int customerId, String location) {
		synchronized(m_itemHT)
		{
			String key = Room.getKey(location);
		    ReservableItem item = (ReservableItem) readData(id, key);
		    if (item == null) {
		        Trace.warn("RM::reserveRoom(" + id + ", " + customerId + ", " 
		                + key + ", " + location + ") failed: item doesn't exist.");
		        return false;
		    } else if (item.getCount() == 0) {
		        Trace.warn("RM::reserveItem(" + id + ", " + customerId + ", " 
		                + key + ", " + location + ") failed: no more items.");
		        return false;
		    } else {            
		        // Decrease the number of available items in the storage.
		        item.setCount(item.getCount() - 1);
		        item.setReserved(item.getReserved() + 1);
        
		        Trace.warn("RM::reserveItem(" + id + ", " + customerId + ", " 
		                + key + ", " + location + ") OK.");
		        return true;
			}
		}
    //    return reserveItem(id, customerId, Room.getKey(location), location);
	}
    

    // Reserve an itinerary.
    @Override
    public boolean reserveItinerary(int id, int customerId, Vector flightNumbers,
                                    String location, boolean car, boolean room) {
        return false;
    }

	@Override
	public int start() {
		// DO NOTHING: implemented on MW.
		return -1;
	}

	@Override
	public void commit(int tid) {
		// DO NOTHING: implemented on MW.
	}

	@Override
	public void abort(int tid) {
		// DO NOTHING : implemented on MW
	}

	@Override
	public void shutdown() {
		System.exit(0);
	}

}
