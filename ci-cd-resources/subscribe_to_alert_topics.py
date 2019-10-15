import os, getopt, sys, json

subscribed_topics = []
dryrun = False
email = 'JAQU-CAZ-OPERATIONALMONITORING@informed.com'
includes = ''
excludes = ''

def include_keywords(topic, keywords):
	for keyword in keywords.split(','):
		if topic.find(keyword) == -1:
			return False
	return True

def list_topics_subscribed_by(email):
        global subscribed_topics
        listSubscriptionsCmd = 'aws sns list-subscriptions > subscriptions.json'
        os.system(listSubscriptionsCmd)

        with open('subscriptions.json','r') as f:
                _list = json.load(f)
        for _subscription in _list['Subscriptions']:
                if _subscription['SubscriptionArn'].startswith('arn:aws:sns') \
                        and _subscription['Protocol'].lower() == 'email' \
                        and _subscription['Endpoint'].lower() == str(email).lower():
                        subscribed_topics.append(_subscription['TopicArn'])
	os.system('rm subscriptions.json')

# read commandline arguments
fullCmdArguments = sys.argv

# - further arguments
argumentList = fullCmdArguments[1:]


unixOptions = "i:x:e:dh"
gnuOptions = ["include=", "exclude=", "email=", "dryrun", "help"]

try:
    arguments, values = getopt.getopt(argumentList, unixOptions, gnuOptions)
except getopt.error as err:
    # output error, and return with an error code
    print (str(err))
    sys.exit(2)

# evaluate given options
for currentArgument, currentValue in arguments:
	if currentArgument in ("-i", "--include"):
    		includes = currentValue
	elif currentArgument in ("-x", "--exclude"):
		excludes = currentValue
	elif currentArgument in ("-e", "--email"):
        	email = currentValue
        elif currentArgument in ("-d", "--dryrun"):
                dryrun = True
        elif currentArgument in ("-h", "--help"):
                print 'Usage:\n    python subscribe_to_alert_topic.py --include <topic name keyword> --exclude <topic name keyword> --email <recipient> --dryrun\n' \
			'For example:\n' \
			'  1.To subscribe to all RDS related alert topic in dev environment\n' \
			'    python subscribe_to_alert_topic.py --include rds,dev\n' \
			'  2.To subscribe to all alert topics in dev environment but exclude the dead letter queue\n' \
                        '    python subscribe_to_alert_topic.py --exclude dead,letter\n' \
  			'  3.To perform a dry run\n' \
                        '    python subscribe_to_alert_topic.py --include function,dev --exclude dead,letter --dryrun\n' \

		sys.exit(0)
# get list of topics subscribed by the email
list_topics_subscribed_by(email)

# query list of sns topics
listTopicsCmd = 'aws sns list-topics > topics.json'
os.system(listTopicsCmd)

with open('topics.json','r') as f:
	topics = json.load(f)

count = 0
for topic in topics['Topics']:
 	topicArn = topic['TopicArn']
	topicName = topicArn[slice(topicArn.rfind(':')+1,len(topicArn))]
	if (len(includes.strip()) == 0 or include_keywords(topicName, includes)) and (len(excludes.strip()) == 0 or not(include_keywords(topicName, excludes))):
		subscribeToTopicsCmd =  'aws sns subscribe --topic-arn {} --protocol email --notification-endpoint {}'.format(topicArn,email)
		if topicArn not in subscribed_topics:
			count += 1
	                if dryrun:
				print subscribeToTopicsCmd
			else:
				os.system(subscribeToTopicsCmd)

#clean up
os.system('rm topics.json')
print '{} topics subscribed'.format(count) 
