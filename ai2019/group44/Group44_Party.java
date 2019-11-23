package group44;

import java.util.*;

import genius.core.AgentID;
import genius.core.Bid;
import genius.core.actions.Accept;
import genius.core.actions.Action;
import genius.core.actions.Offer;
import genius.core.issue.Issue;
import genius.core.issue.ValueDiscrete;
import genius.core.parties.AbstractNegotiationParty;
import genius.core.parties.NegotiationInfo;
import genius.core.utility.AbstractUtilitySpace;
import genius.core.utility.AdditiveUtilitySpace;
import genius.core.uncertainty.AdditiveUtilitySpaceFactory;
import genius.core.uncertainty.ExperimentalUserModel;
import genius.core.uncertainty.UserModel;

public class Group44_Party extends AbstractNegotiationParty {

	private UncertaintyModel uncertaintyModel;
	private UserModel userModel;

	private OpponentModel opponentModel;
	private BiddingStrategy biddingStrategy;
	private ArrayList<BidWrapper> opponentBids = new ArrayList<BidWrapper>();

	private Bid lastReceivedBid = null;
	private BidSpace bidSpace = null;

	private int numberOfBids = 0;

	private double totalTimeGiven = 0;
	private double last15RoundsAvgTime = 0;
	private double lastBidTimestamp = 0;
	private double epsilon = 0.2;
	private double[] phaseFractions = new double[] { 28.0 / 36.0, 7.0 / 36.0, 1.0 / 36.0 };

	public enum PHASE {
		ONE, TWO, THREE;
	}

	private PHASE currentPhase;

	@Override
	public void init(NegotiationInfo info) {
		super.init(info);
		System.out.println("The user model is: " + userModel); 
		System.out.println("Discount Factor is " + getUtilitySpace().getDiscountFactor());
		System.out.println("Reservation Value is " + getUtilitySpace().getReservationValueUndiscounted());
		currentPhase = PHASE.ONE;

		// If you need to initialize some variables, please initialize them below
		this.totalTimeGiven = info.getTimeline().getTotalTime();

		// Initialize opponent model
		this.opponentModel = new OpponentModel(((AdditiveUtilitySpace) info.getUtilitySpace()), this.epsilon);

		// Generate our bidspace with a time limit of 45 seconds
		this.bidSpace = new BidSpace(info.getUtilitySpace(), 45);
		this.biddingStrategy = new BiddingStrategy(opponentModel, bidSpace, opponentBids);
		System.out.println("Has finished bid generation ? " + this.bidSpace.isFinishedSort());
		
		// If we are dealing with preference uncertainty, we want to know what we have estimated and how good we are at this
		if (hasPreferenceUncertainty()) {
			System.out.println("We are dealing with preference uncertainty");
			System.out.println("Lowest utility: " + userModel.getBidRanking().getLowUtility() 
				    + ". Highest utility: " + userModel.getBidRanking().getHighUtility());
			System.out.println("The estimated utility space is: " + getUtilitySpace());

			Bid randomBid = getUtilitySpace().getDomain().getRandomBid(rand);
			
			System.out.println("The estimate of the utility of a random bid (" + randomBid	+ ") is: " + getUtility(randomBid));
			
			// This is only possible if we have access to the real user model (if the Experimental Setup is enabled in Genius)
			if (userModel instanceof ExperimentalUserModel) {
				System.out.println("We have access to the real user model.");
				
				ExperimentalUserModel experimentalModel = (ExperimentalUserModel) userModel;
				AbstractUtilitySpace realUtilSpace = experimentalModel.getRealUtilitySpace();

				System.out.println("The real utility space is: " + realUtilSpace);
				System.out.println("The real utility of the random bid is: " + realUtilSpace.getUtility(randomBid));
			}
		}
		
		
	}

	public double concessionValue(double start, double range, double fraction) {
		return start - (range * fraction);
	}

	public double concessionValueByPhase(double time) {
		switch (currentPhase) {
		case ONE:
			return concessionValue(1.0, (1.0 / 8.0), timeFractionByPhase(time));
		case TWO:
			return concessionValue((7.0 / 8.0), (1.0 / 8.0), timeFractionByPhase(time));
		case THREE:
			return concessionValue((5.0 / 8.0), (6.0 / 8.0), timeFractionByPhase(time));
		default:
			return 0;
		}
	}

	public double timeFractionByPhase(double time) {
		switch (currentPhase) {
		case ONE:
			return time / phaseFractions[0];
		case TWO:
			return (time - phaseFractions[0]) / (phaseFractions[1]);
		case THREE:
			return (time - phaseFractions[0] - phaseFractions[1]) / (phaseFractions[2]);
		default:
			return 0;
		}
	}

	public void updatePhase(double time) {
		switch (currentPhase) {
		case ONE:
			if (time > phaseFractions[0]) {
				currentPhase = PHASE.TWO;
			}
			break;
		case TWO:
			if (time > phaseFractions[0] + phaseFractions[1]) {
				currentPhase = PHASE.THREE;
			}
			break;
		case THREE:
			break;
		default:
		}
	}

	public double upperBound(double l) {
		return l + (0.2 * (1 - l));
	}

	@Override
	public Action chooseAction(List<Class<? extends Action>> validActions) {
		// Continue bidspace generation if needed.
		this.bidSpace.continueGeneration();

		// Setup current actions time lower and upperbound
		double l = concessionValueByPhase(getTimeLine().getTime());
		double u = upperBound(l);
		Bid bid = null;
		bid = biddingStrategy.generateBid(l, u, currentPhase, getTimeLine().getTime());

		double estimatedOpponentUtil = this.opponentModel.estimateOpponentUtility(bid);

		try {
			Thread.sleep(20);
		} catch (InterruptedException e) {
			System.out.println("Not allowed to sleep");
		}

		// If the utility of the bid we were going to do is equal to or lower than
		// the utility of the bid just done by the other party, we accept it.
		if (lastReceivedBid != null && l <= this.getUtility(lastReceivedBid)) {
			System.out.println("Accepting bid #1 with phase " + currentPhase + " concession value: " + l
					+ " and bid utility: " + this.getUtility(lastReceivedBid));
			return new Accept(getPartyId(), lastReceivedBid);
		}

		// In phase 3, we accept any bid if we estimate the number of rounds left to be
		// less than 15
		if (currentPhase == PHASE.THREE && this.numRoundsLeft() <= 15) {
			System.out.println("Accepting bid #2 with phase " + currentPhase + " concession value: " + l
					+ " and bid utility: " + this.getUtility(lastReceivedBid));
			return new Accept(getPartyId(), lastReceivedBid);
		}

		// Return a new offer
		return this.actionSetTimestamp(new Offer(getPartyId(), bid));
	}

	// Method to estimate our own utility space, used in case of preference uncertainty
	@Override
	public AbstractUtilitySpace estimateUtilitySpace() {
		AdditiveUtilitySpaceFactory additiveUtilitySpaceFactory = new AdditiveUtilitySpaceFactory(getDomain());
		this.userModel = getUserModel();
		this.uncertaintyModel = new UncertaintyModel();
		
		// Initialize the uncertainty model to deal with preference uncertainty
		this.uncertaintyModel.initEstimation(userModel);
		// Get the estimated weights and utilities from the uncertainty model 
		Map<Issue, Double> weights = uncertaintyModel.getWeights();
		Map<Issue, HashMap<ValueDiscrete, Double>> issueValues = uncertaintyModel.getIssueValues();
		
		// Copy the weights and utilities to the utility space
		for (Issue issue : weights.keySet()) {			
			additiveUtilitySpaceFactory.setWeight(issue, weights.get(issue));
			
			for (ValueDiscrete value : issueValues.get(issue).keySet()) {
				additiveUtilitySpaceFactory.setUtility(issue, value, issueValues.get(issue).get(value));
			}			
		}
		
		return additiveUtilitySpaceFactory.getUtilitySpace();
	}

	// Method to set the timestamp just before choosing an action, allowing us to
	// monitor the time between
	// bids accurately
	public Action actionSetTimestamp(Action action) {
		this.updateLastBidTimestamp();
		return action;
	}

	@Override
	public void receiveMessage(AgentID sender, Action action) {
		super.receiveMessage(sender, action);
		double time = getTimeLine().getTime();
		updatePhase(time);
		if (action instanceof Offer) {
			Bid newBid = ((Offer) action).getBid();
			if (lastReceivedBid != null) {
				this.opponentModel.update(lastReceivedBid, newBid, time);
			}
			lastReceivedBid = newBid;
			this.updateLastBidTimestamp();
			if (currentPhase == PHASE.ONE) {
//                Save opponents bids for phase two and three
				Bid b = ((Offer) action).getBid();
				opponentBids.add(new BidWrapper(b, getUtility(b)));
			}
		}
	}

	@Override
	public String getDescription() {
		return "The negotiation agent of group 44";
	}

	public void updateLastBidTimestamp() {
		numberOfBids++;

		double currentTime = this.getTimeLine().getCurrentTime();
		this.updateLast15RoundsAvgTime(currentTime);

		this.lastBidTimestamp = currentTime;

	}

	public void updateLast15RoundsAvgTime(double timestamp) {
		double timeSpentLastBid = timestamp - this.lastBidTimestamp;
		int n = Math.min(numberOfBids, 15);
		this.last15RoundsAvgTime = this.last15RoundsAvgTime + (timeSpentLastBid - this.last15RoundsAvgTime) / n;

	}

	public double numRoundsLeft() {
		return (this.totalTimeGiven - this.getTimeLine().getCurrentTime()) / this.last15RoundsAvgTime;
	}

	@Override
	public HashMap<String, String> negotiationEnded(Bid acceptedBid) {
//        Negotiation has ended

//        Save data to csv
		biddingStrategy.saveHistoryToCsv("history.csv");
		return super.negotiationEnded(acceptedBid);
	}

}
