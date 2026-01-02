#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import click
import json
import requests

from util import (
    print_info, print_warning, print_error, print_debug,
    create_rich_console,
    create_rich_table,
    ensure_response_ok,
)


RICH_CONSOLE = create_rich_console()


def explain_query_dsl(host, index, type, query_dsl):
    url = f'{host}/{index}/_validate/query?explain' if type is None\
        else f'{host}/{index}/{type}/_validate/query?explain'

    http_resp = requests.get(
        url,
        headers={'Content-Type': 'application/json'},
        data=json.dumps(query_dsl),
    )
    ensure_response_ok(http_resp, 'elastic:explain_query_dsl')
    print_info('\nQuery DSL explain:', bold=True)

    resp_data = json.loads(http_resp.text)
    print_warning( json.dumps(resp_data, indent=True) )


def analyze_query(host, index, type, user_query):
    #url = f'{host}/{index}/_analyze?format=yaml'
    url = f'{host}/{index}/_analyze?format=json'
    http_resp = requests.get(
        url,
        headers={'Content-Type': 'application/json'},
        data=json.dumps({
            'analyzer': 'standard',
            'text': user_query,
        }),
    )
    ensure_response_ok(http_resp, 'elastic:analyze_query')

    print_info('\nUser query analyze:', bold=True)
    #obj = yaml.safe_load(http_resp.text)
    obj = json.loads(http_resp.text)
    table = create_rich_table()
    for column in ['position', 'token', 'start\noffset', 'end\noffset', 'type']:
        table.add_column(column)

    for token in obj['tokens']:
        table.add_row(
            str(token['position']),
            token['token'],
            str(token['start_offset']),
            str(token['end_offset']),
            token['type'],
        )

    RICH_CONSOLE.print(table)


def search_query_dsl(host, index, type, query_dsl, explain_results=False):
    url = f'{host}/{index}/_search' if type is None \
        else f'{host}/{index}/{type}/_search'

    http_resp = requests.get(
        url,
        headers={'Content-Type': 'application/json'},
        data=json.dumps(query_dsl)
    )
    ensure_response_ok(http_resp, 'elastic:search_query_dsl')
    search_hits = json.loads(http_resp.text)['hits']

    print_info('\nSearch results:', bold=True)
    table = create_rich_table()
    table.add_column('Num', justify='right')
    table.add_column('Relevance\nScore')
    table.add_column('Movie Title')
    if explain_results:
        table.add_column('Explain')

    for idx, hit in enumerate(search_hits['hits']):
        score = hit['_score']
        title = hit['_source']['title']
        row = [str(idx+1), str(score), title]
        if explain_results:
            row.append(json.dumps(hit['_explanation'], indent=True))

        table.add_row(*row)

    RICH_CONSOLE.print(table)


@click.command()
@click.option('-h', '--host', required=True, help='Elasticsearch host.')
@click.option('-i', '--index', required=True, help='Elasticsearch index name.')
@click.option('-q', '--user-query', required=True, help="User query.")
@click.option('--explain/--no-explain', default=False, help='Explain Query DSL.')
@click.option('--explain-results/--no-explain-results', default=False,
    help='Detailed explanation for each result')
@click.option('--analyze/--no-analyze', default=False, help='Analyze user query.')
def main(host, index, user_query, explain, explain_results, analyze):
    try:
        """
        Simple tool to send search query to Elasticsearch.
        """
        #index_type = 'movie'
        index_type = None

        print_info('User query:', bold=True)
        print_warning(user_query)

        query_dsl = {
            'query': {
                'multi_match': {
                    'query': user_query,
                    'fields': ['title^10', 'overview'],
                    'analyzer': 'yy',
                },
            },
            #'from': 0,
            #'size': 10,
        }
        print_info('\nQuery DSL:', bold=True)
        print_warning( json.dumps(query_dsl, indent=True) )

        if explain:
            explain_query_dsl(host, index, index_type, query_dsl)

        if analyze:
            analyze_query(host, index, index_type, user_query)

        query_dsl['explain'] = True
        search_query_dsl(host, index, index_type, query_dsl, explain_results)

    except Exception as exc:
        print_error(f'Application failed with error!')
        raise exc


if __name__ == '__main__':
    main()

