import java.util.List;
import java.util.ArrayList;

import genius.core.AgentID;
import genius.core.Bid;
import genius.core.actions.Accept;
import genius.core.actions.Action;
import genius.core.actions.Offer;
import genius.core.parties.AbstractNegotiationParty;
import genius.core.parties.NegotiationInfo;

public class NegotiationAgent extends AbstractNegotiationParty{

    private Bid lastReceivedBid = null;
    private int numberOfBids = 0;
    private double totalTimeGiven = 0;
    private double last15RoundsAvgTime = 0;
    private double lastBidTimestamp = 0;
    private int currentPhase = 0;

    @Override
    public void init(NegotiationInfo info) {

        super.init(info);

        System.out.println("Discount Factor is " + getUtilitySpace().getDiscountFactor());
        System.out.println("Reservation Value is " + getUtilitySpace().getReservationValueUndiscounted());

        // if you need to initialize some variables, please initialize them
        // below
        this.totalTimeGiven = info.getTimeline().getTotalTime();
       
    }

    @Override
    public Action chooseAction(List<Class<? extends Action>> validActions) {

    	Bid bid = generateRandomBid();
    	double bidUtil = this.getUtility(bid);
    	System.out.println("Utility of our bid: " + bidUtil);
    	System.out.println("Utility of last bid received: " + this.getUtility(lastReceivedBid));
    	
    	//If the utility of the bid we were going to do is equal to or lower than
    	// the utility of the bid just done by the other party, we accept it.
    	if (lastReceivedBid != null && bidUtil <= this.getUtility(lastReceivedBid)) {
    		return new Accept(getPartyId(), lastReceivedBid); 
    	}
    	
    	// In phase 3, we accept any bid if we estimate the number of rounds left to be less than 15
    	if (currentPhase == 3 && this.numRoundsLeft() <= 15) {
    		return new Accept(getPartyId(), lastReceivedBid); 
    	}
    	
    	// Return a new offer
        return this.actionSetTimestamp(new Offer(getPartyId(), bid)); 
    }
    
    //Method to set the timestamp just before choosing an action, allowing us to monitor the time between
    // bids accurately
    public Action actionSetTimestamp(Action action) {
    	this.updateLastBidTimestamp();
    	return action;
    }

    @Override
    public void receiveMessage(AgentID sender, Action action) {
        super.receiveMessage(sender, action);
        if (action instanceof Offer) {
            lastReceivedBid = ((Offer) action).getBid();
            this.updateLastBidTimestamp();
        }
    }

    @Override
    public String getDescription() {
        return "example party group N";
    }
    
    public void updateLastBidTimestamp() {
    	numberOfBids++;
    	
    	double currentTime = this.getTimeLine().getCurrentTime();
    	this.updateLast15RoundsAvgTime(currentTime);
    	
    	System.out.println(("Last bid timestamp: " + this.lastBidTimestamp));
    	this.lastBidTimestamp = currentTime;
    	System.out.println(("New bid timestamp: " + this.lastBidTimestamp));

    }
    
    public void updateLast15RoundsAvgTime(double timestamp) {
    	double timeSpentLastBid = timestamp - this.lastBidTimestamp;
    	int n = Math.min(numberOfBids, 15);
    	this.last15RoundsAvgTime = 
    			this.last15RoundsAvgTime + (timeSpentLastBid - this.last15RoundsAvgTime) / n;
    	
    	System.out.println(("last15RoundsAvgTime: " + this.last15RoundsAvgTime));
    }
    
    public double numRoundsLeft() {
    	return (this.totalTimeGiven - this.getTimeLine().getCurrentTime()) / this.last15RoundsAvgTime;
    }
}
