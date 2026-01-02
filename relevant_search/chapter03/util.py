#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import click
import rich.console
import rich.table


def print_info(msg, bold=False):
    click.secho(msg, fg='green', bold=bold)


def print_warning(msg, bold=False):
    click.secho(msg, fg='yellow', bold=bold)


def print_error(msg, bold=False):
    click.secho(msg, fg='red', bold=bold)

def print_debug(msg, bold=False):
    click.secho(msg, fg='blue', bold=bold)


def ensure_response_ok(resp, operation):
    if resp.status_code != 200:
        raise Exception(
            f'API call failed! operation={operation} status={resp.status_code} response={resp.text}')


def create_rich_console():
    return rich.console.Console()

def create_rich_table():
    return rich.table.Table(
        show_header=True,
        show_lines=True,
        header_style='bold yellow',
        border_style='green',
    )

