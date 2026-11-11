#!/usr/bin/env python3
"""Offline contract checks; requires PyYAML and jsonschema, no running backend."""
import copy
import json
from pathlib import Path
import unittest

import yaml
from jsonschema import Draft4Validator, FormatChecker

ROOT = Path(__file__).resolve().parents[2]
SPEC = yaml.safe_load((ROOT / 'docs/contracts/projects-v1.yaml').read_text())


def json_schema(value):
    if isinstance(value, list):
        return [json_schema(item) for item in value]
    if not isinstance(value, dict):
        return value
    result = {key: json_schema(item) for key, item in value.items() if key != 'nullable'}
    if value.get('nullable'):
        result['type'] = [result['type'], 'null']
    if '$ref' in result:
        result['$ref'] = result['$ref'].replace('#/components/schemas/', '#/definitions/')
    return result


SCHEMAS = json_schema(SPEC['components']['schemas'])


def validate(name, payload):
    schema = {'definitions': SCHEMAS, '$ref': '#/definitions/' + name}
    return list(Draft4Validator(schema, format_checker=FormatChecker()).iter_errors(payload))


class ProjectContractChecks(unittest.TestCase):
    def test_reference_only_creation_and_progressive_setup(self):
        self.assertFalse(validate('CreateProjectRequest', {'reference': 'DEMO-001'}))
        self.assertFalse(validate('CreateProjectRequest', {
            'reference': 'DEMO-001', 'exporterId': None, 'producerIds': [],
            'expectedDepartureDate': '2020-01-01', 'destinationCountryCode': 'TN'}))

    def test_reject_missing_blank_and_malformed_reference(self):
        for payload in [{}, {'reference': ''}, {'reference': None}, {'reference': 'A' * 81},
                        {'reference': 'INTERNAL SPACE'}]:
            with self.subTest(payload=payload):
                self.assertTrue(validate('CreateProjectRequest', payload))

    def test_unknown_fields_and_client_status_are_rejected(self):
        for field in ['status', 'createdBy', 'shipmentId', 'id']:
            self.assertTrue(validate('CreateProjectRequest', {'reference': 'DEMO-001', field: 'arbitrary'}))

    def test_patch_distinguishes_clearable_and_nonnullable_fields(self):
        self.assertFalse(validate('UpdateProjectRequest', {'expectedVersion': 0, 'exporterId': None, 'producerIds': []}))
        for payload in [{}, {'expectedVersion': -1}, {'expectedVersion': None},
                        {'expectedVersion': 0, 'reference': None}, {'expectedVersion': 0, 'producerIds': None}]:
            with self.subTest(payload=payload):
                self.assertTrue(validate('UpdateProjectRequest', payload))

    def test_reference_formats_and_dates(self):
        for field, value in [('exporterId', 'not-a-uuid'), ('expectedDepartureDate', '2026-02-30'),
                             ('destinationCountryCode', 'TUN')]:
            self.assertTrue(validate('CreateProjectRequest', {'reference': 'DEMO-001', field: value}))

    def test_producer_ids_are_a_set(self):
        producer = '00000000-0000-0000-0000-000000000001'
        self.assertTrue(validate('CreateProjectRequest', {'reference': 'DEMO-001', 'producerIds': [producer, producer]}))

    def test_complete_draft_response_is_well_defined(self):
        example = json.loads((ROOT / 'docs/contracts/examples/project-draft.json').read_text())
        self.assertFalse(validate('ProjectResponse', example))
        invalid = copy.deepcopy(example)
        del invalid['exporterId']
        self.assertTrue(validate('ProjectResponse', invalid))
        invalid = copy.deepcopy(example)
        invalid['status'] = 'FINALIZED'
        self.assertTrue(validate('ProjectResponse', invalid))

    def test_empty_page_is_valid(self):
        self.assertFalse(validate('ProjectPage', {'items': [], 'page': 0, 'size': 20, 'totalElements': 0, 'totalPages': 0}))

    def test_schema_matches_database_status_vocabulary(self):
        migration = (ROOT / 'backend/src/main/resources/db/migration/V1__architecture_foundations.sql').read_text()
        for status in SPEC['components']['schemas']['ProjectStatus']['enum']:
            self.assertIn("'" + status + "'", migration)
        self.assertIn("DEFAULT 'DRAFT_DATA'", migration)

    def test_all_operations_have_explicit_problem_responses(self):
        for path in SPEC['paths'].values():
            for method in ['get', 'post', 'patch']:
                if method in path:
                    for code, response in path[method]['responses'].items():
                        if int(code) >= 400:
                            self.assertIn('application/problem+json', response['content'])


if __name__ == '__main__':
    unittest.main(verbosity=2)
