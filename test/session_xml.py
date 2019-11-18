import argparse
import xml.etree.ElementTree as ET

parser = argparse.ArgumentParser(description='Creates genius negotiation session xml file.')
parser.add_argument('-d', default=60) #deadline
parser.add_argument('-dt', default='TIME') #deadline type (time/rounds)
parser.add_argument('--parties', nargs='+', dest='party_args', required=True) #parties
parser.add_argument('--preferences', nargs='+', dest='profile_args', required=True) #party preferences
parser.add_argument('-rep', default=False) #agent repitition
parser.add_argument('--repeat', default=1) #agent repitition

args = parser.parse_args()

root = ET.Element('multilateralTournamentsConfiguration')
tournaments = ET.SubElement(root, 'tournaments')
tournament = ET.SubElement(tournaments, 'tournament')
deadline = ET.SubElement(tournament, 'deadline')
deadline_value = ET.SubElement(deadline, 'value').text = str(args.d)
ET.SubElement(deadline, 'type').text = str(args.dt)
ET.SubElement(tournament, 'protocolItem', {'hasMediatorProfile': 'false', 'hasMediator': 'false', 'classPath': 'genius.core.protocol.StackedAlternatingOffersProtocol'})
parties = ET.SubElement(tournament, 'partyRepItems')
for i in args.party_args:
    party = ET.SubElement(parties, 'party', {'classPath': i})
    ET.SubElement(party, 'properties')
profiles = ET.SubElement(tournament, 'partyProfileItems')
for i in args.profile_args:
    ET.SubElement(profiles, 'item', {'url': 'file:'+i})
ET.SubElement(tournament, 'repeats').text = str(args.repeat)
ET.SubElement(tournament, 'numberOfPartiesPerSession').text = '2'
ET.SubElement(tournament, 'repetitionAllowed').text = str(args.rep).lower()
ET.SubElement(tournament, 'persistentDataType').text = 'DISABLED'

f = open("test_tournament.xml", "w")
f.write('<?xml version="1.0" encoding="UTF-8" standalone="yes"?>')
f.write(ET.tostring(root).decode("utf-8"))