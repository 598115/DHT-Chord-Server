/**
 * 
 */
package no.hvl.dat110.middleware;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import no.hvl.dat110.rpc.interfaces.NodeInterface;
import no.hvl.dat110.util.LamportClock;
import no.hvl.dat110.util.Util;

/**
 * @author tdoy
 *
 */
public class MutualExclusion {
		
	private static final Logger logger = LogManager.getLogger(MutualExclusion.class);
	/** lock variables */
	private boolean CS_BUSY = false;						// indicate to be in critical section (accessing a shared resource) 
	private boolean WANTS_TO_ENTER_CS = false;				// indicate to want to enter CS
	private List<Message> queueack; 						// queue for acknowledged messages
	private List<Message> mutexqueue;						// queue for storing process that are denied permission. We really don't need this for quorum-protocol
	
	private LamportClock clock;								// lamport clock
	private Node node;
	
	public MutualExclusion(Node node) throws RemoteException {
		this.node = node;
		
		clock = new LamportClock();
		queueack = new ArrayList<Message>();
		mutexqueue = new ArrayList<Message>();
	}
	
	public synchronized void acquireLock() {
		logger.info(node.getNodeName() + " Aquired the lock");
		CS_BUSY = true;
	}
	
	public void releaseLocks() {
		WANTS_TO_ENTER_CS = false;
		CS_BUSY = false;
	}

	public boolean doMutexRequest(Message message, byte[] updates) throws RemoteException {
		
		logger.info(node.nodename + " wants to access CS ");
		// clear the queueack before requesting for votes
		queueack.clear();	
		// clear the mutexqueue
        mutexqueue.clear();	
		// increment clock
		clock.increment();
		// adjust the clock on the message, by calling the setClock on the message
		message.setClock(clock.getClock());		
		// wants to access resource - set the appropriate lock variable
	    WANTS_TO_ENTER_CS = true;
		// start MutualExclusion algorithm
		
			// first, call removeDuplicatePeersBeforeVoting. A peer can hold/contain 2 replicas of a file. This peer will appear twice
           List<Message> activeNodes = removeDuplicatePeersBeforeVoting();
		   int voters = activeNodes.size();
			// multicast the message to activenodes (hint: use multicastMessage)
		    multicastMessage(message, activeNodes); //Param?
			// check that all replicas have replied (permission) - areAllMessagesReturned(int numvoters)?
		   Boolean allReturned = areAllMessagesReturned(voters);                                                 
			// if yes, acquireLock
		   if(allReturned) {
			acquireLock();
		   }
				// send the updates to all replicas by calling node.broadcastUpdatetoPeers
		   node.broadcastUpdatetoPeers(updates);
				// clear the mutexqueue
		mutexqueue.clear();
		// return permission
		return allReturned;
	}
	
	// multicast message to other processes including self
	private void multicastMessage(Message message, List<Message> activenodes) throws RemoteException {
		
		logger.info(node.getNodeName() + " Number of peers to vote = "+activenodes.size());
		
		// iterate over the activenodes
		
		// obtain a stub for each node from the registry
		
		// call onMutexRequestReceived()
        int i = 0;
		while(i < activenodes.size()) {
			NodeInterface stub = Util.getProcessStub(activenodes.get(i).getNodeName(), activenodes.get(i).getPort());
			logger.info("Casting message to " + stub.getNodeName() + " from " + node.getNodeName());                                                                   ///////////? TODO
			stub.onMutexRequestReceived(message);
			i++;
		}
		
	}
	
	public void onMutexRequestReceived(Message message) throws RemoteException {
		
		// increment the local clock
		
       clock.increment();
	   node.getMessage().setClock(clock.getClock());

		// if message is from self, acknowledge, and call onMutexAcknowledgementReceived()

		if(message.getNodeName().equals(node.getNodeName())) {
			logger.info("DECISION " + node.getNodeName() + " Self OK");
			message.setAcknowledged(true);                                 
			onMutexAcknowledgementReceived(message);
		}
			
		int caseid = -1;
		
		/* write if statement to transition to the correct caseid in the doDecisionAlgorithm */
		
			// caseid=0: Receiver is not accessing shared resource and does not want to (send OK to sender)
			if(CS_BUSY == false && WANTS_TO_ENTER_CS == false) {
				caseid = 0;
			}	
			// caseid=1: Receiver already has access to the resource (dont reply but queue the request)
		    else if(CS_BUSY) {
				caseid = 1;
			}
			// caseid=2: Receiver wants to access resource but is yet to - compare own message clock to received message's clock
		    else if(CS_BUSY == false && WANTS_TO_ENTER_CS == true) {
				caseid = 2;
			}
		// check for decision
		doDecisionAlgorithm(message, mutexqueue, caseid);
	}
	
	public void doDecisionAlgorithm(Message message, List<Message> queue, int condition) throws RemoteException {
		
		String procName = message.getNodeName();
		int port = message.getPort();
		
		switch(condition) {
		
			/** case 1: Receiver is not accessing shared resource and does not want to (send OK to sender) */
			case 0: {
				logger.info("DECISION " + node.getNodeName() + " Sent OK back");
				// get a stub for the sender from the registry
				NodeInterface stub = Util.getProcessStub(procName, port);		
				// acknowledge message
				message.setAcknowledged(true);
				// send acknowledgement back by calling onMutexAcknowledgementReceived()
				stub.onMutexAcknowledgementReceived(message);
				break;
			}
		
			/** case 2: Receiver already has access to the resource (dont reply but queue the request) */
			case 1: {
				logger.info("DECISION " + node.getNodeName() + " Didnt reply but queued request");
				queue.add(message);
				// queue this message
				break;
			}
			
			/**
			 *  case 3: Receiver wants to access resource but is yet to (compare own message clock to received message's clock
			 *  the message with lower timestamp wins) - send OK if received is lower. Queue message if received is higher
			 */
			case 2: {
				
				// check the clock of the sending process (note that the correct clock is in the received message)
				int senderClock = message.getClock();			
				// own clock of the receiver (note that the correct clock is in the node's message)
				int receiverClock = node.getMessage().getClock();
				// compare clocks, the lowest wins
				if (senderClock < receiverClock) {      
				   logger.info("DECISION " + node.getNodeName() + " Sent OK because sender won");          //sender won
                   message.setAcknowledged(true);
				   NodeInterface stub = Util.getProcessStub(procName, port);
				   stub.onMutexAcknowledgementReceived(message);
				}
				else if(senderClock > receiverClock) { 
					logger.info("DECISION " + node.getNodeName() + " Didnt send ok because reciever won");           //Sender lost
					 mutexqueue.add(message);                                           
				}
				// if clocks are the same, compare nodeIDs, the lowest wins
				
				// if sender wins, acknowledge the message, obtain a stub and call onMutexAcknowledgementReceived()
				
				// if sender looses, queue it

				else {         ///Same clock, lowest id wins
                   if(message.getNodeID().subtract(node.getNodeID()).signum() < 0) {
					logger.info("DECISION " + node.getNodeName() + " Sent OK because same clock but sender ID lower");
					message.setAcknowledged(true);
					NodeInterface stub = Util.getProcessStub(procName, port);
					stub.onMutexAcknowledgementReceived(message);
				   }
				   else {
					logger.info("DECISION " + node.getNodeName() + " Didnt send ok because reciever on lower ID but same clock");
					mutexqueue.add(message);
				   }
				}

				break;
			}
			
			default: break;
		}
		
	}
	
	public void onMutexAcknowledgementReceived(Message message) throws RemoteException {
		
		// add message to queueack
		queueack.add(message);
		
	}
	
	// multicast release locks message to other processes including self
	public void multicastReleaseLocks(Set<Message> activenodes) {
		logger.info("Releasing locks from = "+activenodes.size());
		
		// iterate over the activenodes
		List<Message> nodes = new LinkedList<>();
		activenodes.forEach(n -> nodes.add(n));
		int i = 0;
		while(i < activenodes.size()) {
			NodeInterface stub = Util.getProcessStub(nodes.get(i).getNodeName(), nodes.get(i).getPort());
			try {
				stub.releaseLocks();
			} catch (RemoteException e) {
				e.printStackTrace();
			}
			i++;
		}	
		// obtain a stub for each node from the registry
		
		// call releaseLocks()	
	}
	
	private boolean areAllMessagesReturned(int numvoters) throws RemoteException {
		logger.info(node.getNodeName()+": size of queueack after vote = "+queueack.size());

		Boolean result = false;
		// check if the size of the queueack is the same as the numvoters
		if(numvoters == queueack.size()) {
			result = true;
		}
		// clear the queueack
		queueack.clear();
		// return true if yes and false if no
		
		return result;
	}
	
	private List<Message> removeDuplicatePeersBeforeVoting() {
		
		List<Message> uniquepeer = new ArrayList<Message>();
		for(Message p : node.activenodesforfile) {
			boolean found = false;
			for(Message p1 : uniquepeer) {
				if(p.getNodeName().equals(p1.getNodeName())) {
					found = true;
					break;
				}
			}
			if(!found)
				uniquepeer.add(p);
		}		
		return uniquepeer;
	}
}
