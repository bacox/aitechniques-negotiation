import argparse
import pandas as pd
import numpy as np
from os import path

def unique(data):
    values = []
    for i in data:
        values.append(i.split('@')[0])
    return np.unique(values).tolist()

def count(data, value):
    amount = 0
    for i in data:
        if i.split('@')[0] == value:
            amount += 1
    return amount

def accumulate_by_agent(data, column, agents, against):
    unique_agents = unique(agents)
    values = np.asarray(data[column])
    accumulated = [[] for _ in range(len(unique_agents))]
    for i in range(len(values)):
        split_name = agents[i].split('@')[0]
        # If current name equals our agent
        if split_name == args.a:
            # Get row to check full name against
            check = data['Agent 1'][i] if i % 2 == 0 else data['Agent 2'][i]
            opponent = against[i].split('@')[0]
            # If opponent is our agent and not right agent for current row, continue
            if (opponent == args.a and check != agents[i]) or opponent != args.a:
                continue
        value = values[i]
        if isinstance(values[i], str):
            value = float(values[i].replace(",", '.'))
        accumulated[unique_agents.index(split_name)].append(value)
    return accumulated


parser = argparse.ArgumentParser(description='Processes csv files created by genius.')
parser.add_argument('-f', required=True)
parser.add_argument('-a', default="Group44_Party")
args = parser.parse_args()

if not path.exists(args.f):
    print("File does not exist")
    exit(0)

data = pd.read_csv(args.f, delimiter=';')
agent1 = data['Agent 1']
agent2 = data['Agent 2']
agents = unique(agent1)

util1 = accumulate_by_agent(data, 'Utility 1', agent1, agent2)
util2 = accumulate_by_agent(data, 'Utility 2', agent2, agent1)
par1 = accumulate_by_agent(data, 'Dist. to Pareto', agent1, agent2)
par2 = accumulate_by_agent(data, 'Dist. to Pareto', agent2, agent1)
nash1 = accumulate_by_agent(data, 'Dist. to Nash', agent1, agent2)
nash2 = accumulate_by_agent(data, 'Dist. to Nash', agent2, agent1)

our_util1 = accumulate_by_agent(data, 'Utility 1', agent2, agent1)
our_util2 = accumulate_by_agent(data, 'Utility 2', agent1, agent2)
our_par1 = accumulate_by_agent(data, 'Dist. to Pareto', agent2, agent1)
our_par2 = accumulate_by_agent(data, 'Dist. to Pareto', agent1, agent2)
our_nash1 = accumulate_by_agent(data, 'Dist. to Nash', agent2, agent1)
our_nash2 = accumulate_by_agent(data, 'Dist. to Nash', agent1, agent2)

# if '_our_' in args.f:
for i in range(len(nash1)):
    nash1[i].extend(nash2[i])
    util1[i].extend(util2[i])
    par1[i].extend(par2[i])
    our_nash1[i].extend(our_nash2[i])
    our_util1[i].extend(our_util2[i])
    our_par1[i].extend(our_par2[i])
nash = nash1
util = util1
par = par1

our_nash = our_nash1
our_util = our_util1
our_par = our_par1
# else:
#     nash = nash1[0].extend(nash2[0]) #np.asarray(nash1) + np.asarray(nash2) #np.add(nash1, nash2)
#     util = np.asarray(util1) + np.asarray(util2)
#     par = np.asarray(par1) + np.asarray(par2)
# Do this with map, because our agent (Group44_Party) has more entries then the rest, np.mean will give an error
print("Util mean and std vs "+args.a)
print(pd.DataFrame([agents, list(map(lambda x: np.mean(x), util)), list(map(lambda x: np.std(x), util))]).to_string(index=False,header=False))
print()
print("Util mean and std "+args.a+" vs agent")
print(pd.DataFrame([agents, list(map(lambda x: np.mean(x), our_util)), list(map(lambda x: np.std(x), our_util))]).to_string(index=False,header=False))
print()
print("Nash min, dist mean and std with "+args.a)
print(pd.DataFrame([agents, list(map(lambda x: np.min(x), nash)), list(map(lambda x: np.mean(x), nash)), list(map(lambda x: np.std(x), nash))]).to_string(index=False,header=False))
print()
print("Pareto min, dist mean and std with "+args.a)
print(pd.DataFrame([agents, list(map(lambda x: np.min(x), par)), list(map(lambda x: np.mean(x), par)), list(map(lambda x: np.std(x), par))]).to_string(index=False,header=False))
print()
print("Negotiations per agent: ", count(agent1, agents[1])+count(agent2, agents[1]))