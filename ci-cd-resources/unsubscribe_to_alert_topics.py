import os, getopt, sys, json

subscription_arns = []
dryrun = False
email = 'JAQU-CAZ-OPERATIONALMONITORING@informed.com'
includes = ''
excludes = ''

def include_keywords(topic, keywords):
	for keyword in keywords.split(','):
		if topic.find(keyword) == -1:
			return False
	return True

def exclude_keywords(topic, keywords):
	for keyword in keywords.split(','):
		if topic.find(keyword) >= 0:
			return True
	return False

def list_topics_subscribed_by(email, includes, excludes):
        global subscription_arns
        listSubscriptionsCmd = 'aws sns list-subscriptions > subscriptions.json'
        os.system(listSubscriptionsCmd)

        with open('subscriptions.json','r') as f:
                _list = json.load(f)
        for _subscription in _list['Subscriptions']:
          topicArn = _subscription['TopicArn']
          topicName = topicArn[slice(topicArn.rfind(':')+1,len(topicArn))]
          if _subscription['SubscriptionArn'].startswith('arn:aws:sns') \
                  and _subscription['Protocol'].lower() == 'email' \
                  and _subscription['Endpoint'].lower() == str(email).lower() \
                  and (len(includes.strip()) == 0 or include_keywords(topicName, includes)) and (len(excludes.strip()) == 0 or not(exclude_keywords(topicName, excludes))):
                  subscription_arns.append(_subscription['TopicArn'])
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
                print 'Usage:\n python unsubscribe_to_alert_topics.py --include <topic name keyword> --exclude <topic name keyword> --email <recipient> --dryrun\n' \
			'For example:\n' \
			'  1.To unsubscribe a specific email to all RDS related alert topic in dev environment\n' \
			'    python unsubscribe_to_alert_topics.py --include rds,dev --email <recipient>\n' \
			'  2.To unsubscribe a specific email to all alert topics in dev environment but exclude the dead letter queue\n' \
      '    python unsubscribe_to_alert_topics.py --exclude dead,letter --email <recipient>\n' \
			'  3.To perform a dry run\n' \
      '    python unsubscribe_to_alert_topics.py --include function,dev --exclude dead,letter --email <recipient> --dryrun\n' \

		sys.exit(0)
# get list of topics subscribed by the email
list_topics_subscribed_by(email, includes, excludes)

count = 0
for subscription_arn in subscription_arns:
  unsubscribeToTopicsCmd =  'aws sns unsubscribe --subscription-arn {}'.format(subscription_arn)
  count += 1
  if dryrun:
    print unsubscribeToTopicsCmd
  else:
    os.system(unsubscribeToTopicsCmd)

#clean up
print '{} topics unsubscribed'.format(count) 