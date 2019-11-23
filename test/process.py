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

def accumulate_by_agent(data, column, agents):
    unique_agents = unique(agents)
    values = np.asarray(data[column])
    accumulated = [[] for _ in range(len(unique_agents))]
    for i in range(len(values)):
        accumulated[unique_agents.index(agents[i].split('@')[0])].append(values[i])
    return accumulated


parser = argparse.ArgumentParser(description='Processes csv files created by genius.')
parser.add_argument('-f', required=True)
args = parser.parse_args()

if not path.exists(args.f):
    print("File does not exist")
    exit(0)

data = pd.read_csv(args.f, delimiter=';')
agent1 = data['Agent 1']
agent2 = data['Agent 2']
agents = unique(agent1)

util1 = accumulate_by_agent(data, 'Utility 1', agent1)
util2 = accumulate_by_agent(data, 'Utility 2', agent2)

# Do this with map, because our agent (Group44_Party) has more entries then the rest, np.mean will give an error
util1_mean = list(map(lambda x: np.mean(x), util1))
util2_mean = list(map(lambda x: np.mean(x), util2))
util1_std = list(map(lambda x: np.std(x), util2))
util2_std = list(map(lambda x: np.std(x), util2))
print(agents)
print(util1_mean)
print(util1_std)
print(util2_mean)
print(util2_std)
print("Negotiations per agent: ", count(agent1, agents[0])+count(agent2, agents[0]))