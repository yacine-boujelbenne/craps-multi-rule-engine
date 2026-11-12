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
ACCESS_SPEC = yaml.safe_load((ROOT / 'docs/contracts/accounts-access-v1.yaml').read_text())
ACCESS_SCHEMAS = json_schema(ACCESS_SPEC['components']['schemas'])
COMMERCIAL_SPEC = yaml.safe_load((ROOT / 'docs/contracts/commercial-setup-v1.yaml').read_text())
COMMERCIAL_SCHEMAS = json_schema(COMMERCIAL_SPEC['components']['schemas'])


def validate(name, payload, schemas=SCHEMAS):
    schema = {'definitions': schemas, '$ref': '#/definitions/' + name}
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

    def test_admin_account_creation_has_no_public_registration(self):
        payload = {'username': 'fictional.operator', 'displayName': 'Fictional operator',
                   'roles': ['OPERATOR'], 'initialPassword': 'fictional-password-42'}
        self.assertFalse(validate('CreateUserRequest', payload, ACCESS_SCHEMAS))
        self.assertNotIn('/api/auth/register', ACCESS_SPEC['paths'])
        self.assertEqual(ACCESS_SPEC['paths']['/api/admin/users']['post']['x-required-role'], 'ADMIN')
        self.assertNotIn('password_hash', ACCESS_SPEC['components']['schemas']['UserResponse']['properties'])

    def test_form_assignments_are_explicit_and_bounded(self):
        self.assertFalse(validate('OperatorAssignmentRequest', {'formKeys': []}, ACCESS_SCHEMAS))
        self.assertFalse(validate('OperatorAssignmentRequest', {'formKeys': ['PACKING_LIST']}, ACCESS_SCHEMAS))
        for keys in [None, ['ARBITRARY_FORM'], ['PACKING_LIST', 'PACKING_LIST']]:
            self.assertTrue(validate('OperatorAssignmentRequest', {'formKeys': keys}, ACCESS_SCHEMAS))
        migration = (ROOT / 'backend/src/main/resources/db/migration/V2__local_accounts_and_form_assignments.sql').read_text()
        for key in ACCESS_SPEC['components']['schemas']['FormKey']['enum']:
            self.assertIn("'" + key + "'", migration)

    def test_updated_project_contract_preserves_data_and_restricts_writes(self):
        updated = yaml.safe_load((ROOT / 'docs/contracts/projects-v2.yaml').read_text())
        self.assertEqual(updated['components']['schemas'], SPEC['components']['schemas'])
        self.assertEqual(updated['paths']['/api/projects']['post']['x-required-role'], 'ADMIN')
        self.assertEqual(updated['paths']['/api/projects/{id}']['patch']['x-edit-form'], 'PROJECT_SETUP')
        self.assertEqual(updated['security'], [{'sessionAuth': []}])

    def test_commercial_parties_are_separate_nullable_scalar_roles(self):
        self.assertFalse(validate('UpdateCommercialPartiesRequest', {'expectedVersion': 0,
            'supplierId': None, 'clientId': None, 'importerId': None, 'consigneeId': None}, COMMERCIAL_SCHEMAS))
        self.assertTrue(validate('UpdateCommercialPartiesRequest', {'expectedVersion': 0,
            'supplierId': []}, COMMERCIAL_SCHEMAS))
        self.assertFalse(validate('CreateOrganizationRequest', {'legalName': 'Fictional partner',
            'roles': ['CLIENT', 'IMPORTER', 'CONSIGNEE']}, COMMERCIAL_SCHEMAS))
        self.assertTrue(validate('CreateOrganizationRequest', {'legalName': 'Fictional partner',
            'roles': [0]}, COMMERCIAL_SCHEMAS))

    def test_legacy_unconfigured_organization_remains_readable(self):
        record = {'id': '00000000-0000-0000-0000-000000000010', 'version': 0,
                  'legalName': 'Fictional existing organization', 'countryCode': None,
                  'addressText': None, 'registrationNumber': None, 'roles': [], 'active': True,
                  'createdAt': '2026-11-12T00:00:00Z', 'updatedAt': '2026-11-12T00:00:00Z'}
        self.assertFalse(validate('OrganizationResponse', record, COMMERCIAL_SCHEMAS))
        self.assertTrue(validate('CreateOrganizationRequest', {'legalName': record['legalName'], 'roles': []}, COMMERCIAL_SCHEMAS))

    def test_catalog_decimals_are_bounded_strings(self):
        good = {'code': 'SYNTHETIC', 'name': 'Fictional grade',
                'defaultCartonNetKg': '11.125', 'defaultPricePerKg': '4.1234', 'defaultCurrency': 'USD'}
        self.assertFalse(validate('CreateGradeRequest', good, COMMERCIAL_SCHEMAS))
        for key, values in [('defaultCartonNetKg', [11.125, '0', '0.000', '-1', '1.2345', '1e1']),
                            ('defaultPricePerKg', [4.1234, '-1', '1.23456', '1e1'])]:
            for value in values:
                bad = {**good, key: value}
                self.assertTrue(validate('CreateGradeRequest', bad, COMMERCIAL_SCHEMAS), (key, value))

    def test_catalog_writes_are_admin_only_and_parties_require_setup(self):
        for path, methods in COMMERCIAL_SPEC['paths'].items():
            for verb in ['post', 'patch']:
                if verb in methods:
                    if path.endswith('/commercial-parties'):
                        self.assertEqual(methods[verb]['x-edit-form'], 'PROJECT_SETUP')
                    else:
                        self.assertEqual(methods[verb]['x-required-role'], 'ADMIN')
        self.assertNotIn('/api/projects', COMMERCIAL_SPEC['paths'])

    def test_catalog_patch_preserves_missing_and_null_rules(self):
        self.assertFalse(validate('UpdateGradeRequest', {'expectedVersion': 1,
            'defaultPricePerKg': None, 'defaultCurrency': None}, COMMERCIAL_SCHEMAS))
        for bad in [{}, {'expectedVersion': -1}, {'expectedVersion': 0, 'code': None},
                    {'expectedVersion': 0, 'active': None}, {'expectedVersion': 0, 'unownedField': 1}]:
            self.assertTrue(validate('UpdateGradeRequest', bad, COMMERCIAL_SCHEMAS))

    def test_all_operations_have_explicit_problem_responses(self):
        for path in SPEC['paths'].values():
            for method in ['get', 'post', 'patch']:
                if method in path:
                    for code, response in path[method]['responses'].items():
                        if int(code) >= 400:
                            self.assertIn('application/problem+json', response['content'])


if __name__ == '__main__':
    unittest.main(verbosity=2)
