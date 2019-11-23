package group44;

import genius.core.Bid;
import genius.core.issue.Issue;
import genius.core.issue.Value;
import genius.core.issue.IssueDiscrete;
import genius.core.issue.ValueDiscrete;
import genius.core.utility.AdditiveUtilitySpace;
import genius.core.issue.Objective;
import genius.core.utility.Evaluator;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

// Class to represent the Opponent Model
public class OpponentModel {

	private Map<Issue, Double> weights;
	private Map<Issue, HashMap<ValueDiscrete, Integer>> valueUtilities;
	private AdditiveUtilitySpace utilSpace;
	private double epsilon;
	
	public OpponentModel(AdditiveUtilitySpace utilSpace, double epsilon) {
		this.utilSpace = new AdditiveUtilitySpace(utilSpace);
		this.initWeights(this.utilSpace);
		this.initValueUtilities(this.utilSpace);
		this.epsilon = epsilon;
	}
	
	// Init the weights map with uniform weights for each issue
	private void initWeights(AdditiveUtilitySpace utilSpace) {
		this.weights = new HashMap<Issue, Double>();
		
		for (Entry<Objective, Evaluator> e : utilSpace.getEvaluators()) {
			this.weights.put(((IssueDiscrete)e.getKey()), 1.0/utilSpace.getEvaluators().size());
		}
	}
	
	// Init the utilities map with a value of 1 for all (issue, value)-pairs
	private void initValueUtilities(AdditiveUtilitySpace utilSpace) {
		this.valueUtilities = new HashMap<Issue, HashMap<ValueDiscrete, Integer>>();

		for (Entry<Objective, Evaluator> e : this.utilSpace.getEvaluators()) {
			IssueDiscrete issue = ((IssueDiscrete) e.getKey());
			this.valueUtilities.put(issue, new HashMap<ValueDiscrete, Integer>());
			for (ValueDiscrete v : issue.getValues()) {
				this.valueUtilities.get(issue).put(v, 1);
			}
		}
	}
	
	// Update the weights and values corresponding to a received bid
	public void update(Bid lastReceivedBid, Bid newBid, double time) {
		updateEpsilon(time);
		
		for (Issue issue : newBid.getIssues()) {
			ValueDiscrete issueValue = (ValueDiscrete) newBid.getValue(issue);
			
			// Check if value of issue is unchanged since last bid, if so, update the weights
			if (issueValue.equals((ValueDiscrete)lastReceivedBid.getValue(issue))) {
				this.updateWeights(issue);
			}
			
			// Increment the value utility
			this.incrementValueUtility(issue, issueValue);
		}
	}
	
	// Weight decay over time	
	private void updateEpsilon(double time) {
		double decay = 1.0 - time;
		this.epsilon *= decay;
	}
	
	private void updateWeights(Issue issue) {
		//Increment weight of issue in the bid
		this.weights.replace(issue, this.weights.get(issue) + this.epsilon);
		
		//Sum all weights
		double sumWeights = 0.0;
		for (Issue issueKey : this.weights.keySet()) {
			sumWeights += this.weights.get(issueKey);
		}
		
		//Normalize all weights
		for (Issue issueKey : this.weights.keySet()) {
			this.weights.replace(issueKey, this.weights.get(issueKey) / sumWeights);
		}
	}
	
	// Method to increment the utility of a value
	private void incrementValueUtility(Issue issue, ValueDiscrete issueValue) {
		this.valueUtilities.get(issue).replace(issueValue, 
				this.valueUtilities.get(issue).get(issueValue) + 1);
	}
	
	// Estimate the utility of the bid we are currently making for the opponent
	public double estimateOpponentUtility(Bid bid) {
		double utility = 0.0;
		
		for (Issue issue : bid.getIssues()) {
			HashMap<ValueDiscrete, Integer> issueValues = this.valueUtilities.get(((Issue) issue));
			int maxUtility = 1;
			for (Value issueValue : issueValues.keySet()) {
				maxUtility = Integer.max(maxUtility, issueValues.get(issueValue));
			}
						
			if (issueValues.containsKey(bid.getValue(issue))) {
				utility += weights.get(issue) * (issueValues.get(bid.getValue(issue)) / maxUtility);
			}
		}
		
		return utility;
	}
}
