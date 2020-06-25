#!/usr/bin/python3

import sys
import argparse
import json
import subprocess


def run_aws_command(args):
    cmd = ['aws'] + args + ['--profile', 'target_acc']
    print(' '.join(cmd))
    try:
        result = subprocess.run(cmd, stdout=subprocess.PIPE)
        result.check_returncode()
        return json.loads(result.stdout)
    except subprocess.CalledProcessError:
        sys.exit(result.returncode)


def subscribe(topic, email):
    return run_aws_command([
        'sns', 'subscribe',
        '--topic-arn', topic,
        '--protocol', 'email',
        '--notification-endpoint', email])


def unsubscribe(subscription):
    try:
        result = subprocess.run([
            'aws', 'sns', 'unsubscribe',
            '--subscription-arn', subscription], stdout=subprocess.PIPE)
        result.check_returncode()
    except subprocess.CalledProcessError:
        sys.exit(result.returncode)


def get_subscribed_topics(topics, email, ignore_pending):
    for sub in run_aws_command(['sns', 'list-subscriptions'])['Subscriptions']:
        arn = sub['SubscriptionArn']
        topic_arn = sub['TopicArn']
        if (
                topic_arn in topics and
                sub['Protocol'].lower() == 'email' and
                sub['Endpoint'].lower() == email.lower() and (
                    arn.startswith('arn:aws:sns') or (
                        not ignore_pending and arn == 'PendingConfirmation'
                    )
                )):
            yield topic_arn, arn


def get_topics(includes, excludes):
    for topic in run_aws_command(['sns', 'list-topics'])['Topics']:
        arn = topic['TopicArn']
        is_included = True
        for include in includes or []:
            if include not in arn:
                is_included = False
        for exclude in excludes or []:
            if exclude in arn:
                is_included = False
        if is_included:
            print('Topic found:', arn)
            yield arn


def main(args):
    email = args.email[0]
    topics = set(get_topics(args.include, args.exclude))
    print()

    subscriptions = get_subscribed_topics(topics, email, args.ignore_pending)

    if args.unsubscribe: # unsubscribe
        len_subscriptions = 0
        for topic, subscription in subscriptions:
            len_subscriptions += 1
            print('Unsubscribed from', topic)
            if not args.dryrun:
                unsubscribe(subscription)

        print()
        print('found', len(topics), 'included topics')
        print('unsubscribed from', len_subscriptions, 'subscribed topics')

    else: # subscribe
        subscribed_topics = set(t for t, s in subscriptions)
        unsubscribed_topics = topics - subscribed_topics
        for topic in unsubscribed_topics:
            print('Subscribed to', topic)
            if not args.dryrun:
                subscribe(topic, email)

        print()
        print('found', len(topics), 'included topics')
        print('found', len(subscribed_topics), 'subscribed topics')
        print('subscribed to', len(unsubscribed_topics), 'unsubscribed topics')


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Subscribe to AWS alert topics.')
    parser.add_argument(
            'email', metavar='<email>', type=str, nargs=1,
            help='Email address to subscribe topics to')
    parser.add_argument(
            '--include', '-i', metavar='<keyword>', type=str, action='append',
            help='Include topics containing all of these keywords')
    parser.add_argument(
            '--exclude', '-e', metavar='<keyword>', type=str, action='append',
            help='Exclude topics containing any of these keywords')
    parser.add_argument(
            '--dryrun', action='store_true',
            help='Test run command without actually subscribing')
    parser.add_argument(
            '--ignore-pending', action='store_true',
            help='Consider subscriptions pending confirmation as subscribed')
    parser.add_argument(
            '--unsubscribe', action='store_true',
            help='Unsubscribe from subscribed topics found')
    args = parser.parse_args()
    if args.unsubscribe:
        args.ignore_pending = True
    main(args)