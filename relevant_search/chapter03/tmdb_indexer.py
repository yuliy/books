#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import click
import json
import requests

from util import (
    print_info, print_warning, print_error,
    ensure_response_ok,
)


def load_tmdb(filename):
    with open(filename, 'r') as f:
        data = f.read()
        return json.loads(data)


def drop_index(host, index):
    resp = requests.delete(f'{host}/{index}')
    ensure_response_ok(resp, 'drop_index')


def create_index(host, index, analysis_settings={}, mapping_settings={}):
    settings = {
        'settings': {
            'number_of_shards': 1,
            'index': {
                'analysis': analysis_settings,
            },
        }
    }

    if mapping_settings:
        settings['mappings'] = mapping_settings

    url = f'{host}/{index}'
    resp = requests.put(
        url,
        headers={'Content-Type': 'application/json'},
        data=json.dumps(settings)
    )
    ensure_response_ok(resp, 'create_index')


def insert_bulk(host, index, movie_dict):
    bulk_movies = ''
    MAX_BULK_SIZE = 100
    bulk_size = 0
    total_inserted = 0

    def do_insert_bulk(host, bulk):
        url = f'{host}/_bulk'
        resp = requests.post(
            url,
            headers={'Content-Type': 'application/json'},
            data=bulk,
        )
        ensure_response_ok(resp, 'insert_bulk')

    for id, movie in movie_dict.items():
        add_cmd = {'index': {
            '_index': index,
            #'_type': 'movie',
            '_id': id,
        }}
        bulk_movies += json.dumps(add_cmd) + '\n' + json.dumps(movie) + '\n'
        bulk_size += 1

        if bulk_size >= MAX_BULK_SIZE:
            do_insert_bulk(host, bulk_movies)
            total_inserted += bulk_size
            print_warning(f'Inserted {total_inserted} of {len(movie_dict)}')
            bulk_movies = ''
            bulk_size = 0

    if bulk_size > 0:
        do_insert_bulk(host, bulk_movies)
        total_inserted += bulk_size
        print_warning(f'Inserted {total_inserted} of {len(movie_dict)}')


@click.command()
@click.option('-f', '--filename', required=True, help='Input filename.')
@click.option('-h', '--host', required=True, help='Elasticsearch host.')
@click.option('-i', '--index', required=True, help='Elasticsearch index name.')
def main(filename, host, index):
    try:
        """
        This tool is intended to index data in file given into Elasticsearch.
        """
        print_info(f'Loading data from file "{filename}" ...')
        data = load_tmdb(filename)

        print_info('Dropping index ...')
        try:
            drop_index(host, index)
        except Exception as e:
            print_warning(e)

        print_info('Create new blank index ...')
        analysis_settings = {
            'filter': {
                'english_stop': {
                    'type': 'stop',
                    'stopwords': '_english_',
                },
                'english_keywords': {
                    'type': 'keyword_marker',
                    'keywords': ['example'],
                },
                'english_stemmer': {
                    'type': 'stemmer',
                    'language': 'english',
                },
                'english_possessive_stemmer': {
                    'type': 'stemmer',
                    'language': 'possessive_english',
                }
            },
            'analyzer': {
                'yy': {
                    'tokenizer': 'standard',
                    'filter': [
                        'english_possessive_stemmer',
                        'lowercase',
                        'english_stop',
                        'english_keywords',
                        'english_stemmer',
                    ]
                }
            },
        }
        #analysis_settings = {}

        mapping_settings = {
            'properties': {
                #'movie': {
                #    'properties': {
                        'title': {
                            'type': 'text',
                            #'analyzer': 'english',
                            'analyzer': 'yy',
                        },
                        'overview': {
                            'type': 'text',
                            #'analyzer': 'english',
                            'analyzer': 'yy',
                        },
                #    }
                #},
            }
        }
        #mapping_settings = {}

        create_index(
            host, index,
            analysis_settings=analysis_settings,
            mapping_settings=mapping_settings
        )

        print_info('Inserting records to index ...')
        insert_bulk(host, index, movie_dict=data)

        print_info('Indexing completed.')
    except Exception as exc:
        print_error(f'Application failed with error!')
        raise exc


if __name__ == '__main__':
    main()
