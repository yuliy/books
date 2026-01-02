#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import click
import json
import os
import requests

from util import (
    print_info, print_warning, print_error,
    ensure_response_ok,
)


def get_tmdb_api_key():
    ENV_VAR = 'TMDB_API_KEY'
    if ENV_VAR not in os.environ:
        raise Exception(f'Environment variable {ENV_VAR} not found!')
    return os.environ[ENV_VAR]


def get_movie_list(tmdb_session, max_movies=10000):
    url = 'https://api.themoviedb.org/3/movie/top_rated'
    movie_ids = []
    PAGE_SIZE = 20
    num_pages = max_movies // PAGE_SIZE
    for page in range(1, num_pages + 1):
        print_warning(f'Fetching movie list: page {page} of {num_pages} (ids fetched: {len(movie_ids)})...')
        http_resp = tmdb_session.get(url, params={'page': page})
        ensure_response_ok(http_resp, 'tmdb:get_movie_list')
        json_resp = json.loads(http_resp.text)
        movies = json_resp['results']
        for movie in movies:
            movie_ids.append(movie['id'])
    return movie_ids


def extract_movies_data(tmdb_session, movie_ids):
    movie_dict = {}
    for idx, movie_id in enumerate(movie_ids):
        try:
            print_warning(f'Fetching movie data: {idx+1} of {len(movie_ids)}')
            url = f'https://api.themoviedb.org/3/movie/{movie_id}'
            http_resp = tmdb_session.get(url, verify=False)
            ensure_response_ok(http_resp, 'tmdb:extract_movies_data')
            movie = json.loads(http_resp.text)
            get_cast_and_crew(tmdb_session, movie_id, movie)
            movie_dict[movie_id] = movie
        except ConnectionError as e:
            print_error(e)
            raise e

    return movie_dict


def get_cast_and_crew(tmdb_session, movie_id, movie):
    url = f'https://api.themoviedb.org/3/movie/{movie_id}/credits'
    http_resp = tmdb_session.get(url)
    ensure_response_ok(http_resp, 'tmdb:get_cast_and_crew')
    credits = json.loads(http_resp.text)
    crew = credits['crew']
    directors = [
        crew_member for crew_member in crew
        if crew_member['job'] == 'Director'
    ]

    movie['cast'] = credits['cast']
    movie['directors'] = directors

def store_results_to_file(movie_dict, filename):
    with open(filename, 'w') as f:
        f.write(json.dumps(movie_dict))


@click.command()
@click.option('-c', '--count', required=True, type=int, help='Number of movies to download')
@click.option('-f', '--filename', required=True, help='Filename to write results to.')
def main(count, filename):
    try:
        """
        This is tmdb crawler - app used to download data from TMDB (The Movie DataBase).
        """
        tmdb_api_key = get_tmdb_api_key()
        tmdb_api = requests.Session()
        tmdb_api.params = {'api_key': tmdb_api_key}

        print_info('### FETCHING MOVIE IDS', bold=True)
        movie_ids = get_movie_list(tmdb_api, max_movies=count)
        print_info(f'Fetched {len(movie_ids)} movies.')

        print_info('### FETCHING MOVIES DATA', bold=True)
        movie_dict = extract_movies_data(tmdb_api, movie_ids)

        print_info('### STORING RESULTS TO FILE', bold=True)
        store_results_to_file(movie_dict, filename)

        print_info('CRAWLING COMPLETED.')
    except Exception as exc:
        print_error(f'Application failed with error!')
        raise exc


if __name__ == '__main__':
    main()
