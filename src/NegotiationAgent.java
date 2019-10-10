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
    private List<Bid> listOfBids = new ArrayList<Bid>();
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
    	
    	//If the utility of the bid we were going to do is equal to or lower than
    	// the utility of the bid just done by the other party, we accept it.
    	if (lastReceivedBid != null && bidUtil <= this.getUtility(lastReceivedBid)) {
    		return new Accept(getPartyId(), lastReceivedBid); 
    	}
    	
    	if (currentPhase == 3 && this.numRoundsLeft() <= 15) {
    		return new Accept(getPartyId(), lastReceivedBid); 
    	}
    	
    	
        // with 50% chance, counter offer
        // if we are the first party, also offer.
        if (lastReceivedBid == null || !validActions.contains(Accept.class) || Math.random() > 0.5) {
            return this.actionSetTimestamp(new Offer(getPartyId(), bid)); 
        } else {
            return this.actionSetTimestamp(new Accept(getPartyId(), lastReceivedBid)); 
        }
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
    	double currentTime = this.getTimeLine().getCurrentTime();
    	this.updateLast15RoundsAvgTime(currentTime);
    	this.lastBidTimestamp = currentTime;
    }
    
    public void updateLast15RoundsAvgTime(double timestamp) {
    	double timeSpentLastBid = timestamp - this.lastBidTimestamp;
    	int n = Math.min(this.listOfBids.size(), 15);
    	this.last15RoundsAvgTime = 
    			this.last15RoundsAvgTime + (timeSpentLastBid - this.last15RoundsAvgTime) / n;
    }
    
    public double numRoundsLeft() {
    	return (this.totalTimeGiven - this.getTimeLine().getCurrentTime()) / this.last15RoundsAvgTime;
    }
}
