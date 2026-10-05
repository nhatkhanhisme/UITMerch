#!/usr/bin/env python3
"""Scan npm lockfiles and resolved Maven runtime dependencies. No credentials are required."""
import argparse, datetime, json, pathlib, re, subprocess, sys, tempfile
ROOT = pathlib.Path(__file__).resolve().parents[1]

def run(command, cwd=ROOT):
    result = subprocess.run(command, cwd=cwd, text=True, capture_output=True)
    return result

def audit_npm(directory, production=False):
    command = ['npm', 'audit', '--json'] + (['--omit=dev'] if production else [])
    result = run(command, ROOT / directory)
    try:
        report = json.loads(result.stdout)
    except ValueError:
        raise RuntimeError(f'{directory}: npm audit failed without a report')
    if 'error' in report or 'vulnerabilities' not in report:
        raise RuntimeError(f'{directory}: npm audit failed')
    return report

def direct_advisories(report):
    for package, finding in report['vulnerabilities'].items():
        for advisory in finding.get('via', []):
            if isinstance(advisory, dict):
                yield package, advisory['url'].rstrip('/').split('/')[-1], advisory

def maven_inventory():
    with tempfile.TemporaryDirectory(prefix='uitmerch-security-') as directory:
        inventory = pathlib.Path(directory) / 'dependencies.txt'
        result = run(['./mvnw', '-B', '-q', 'dependency:list', '-DincludeScope=runtime', f'-DoutputFile={inventory}'], ROOT / 'backend')
        if result.returncode:
            raise RuntimeError('Maven dependency resolution failed; scan was not completed')
        packages = []
        for line in inventory.read_text().splitlines():
            line = re.sub(r'\x1b\[[0-9;]*m', '', line)
            match = re.match(r'\s*([^\s:]+):([^:]+):(?:jar|pom):(?:[^:]+:)?([^:\s]+):(compile|runtime)(?:\s|$)', line)
            if match:
                group, artifact, version, scope = match.groups()
                packages.append({'package': {'ecosystem': 'Maven', 'name': f'{group}:{artifact}'}, 'version': version})
        if len(packages) < 20:
            raise RuntimeError('Maven inventory is incomplete')
        return packages

def osv(packages):
    with tempfile.TemporaryDirectory(prefix='uitmerch-osv-') as directory:
        request, response = pathlib.Path(directory)/'request.json', pathlib.Path(directory)/'response.json'
        request.write_text(json.dumps({'queries': packages}))
        result = run(['curl', '-4', '-fsS', '--retry', '2', '--max-time', '90', '-H', 'Content-Type: application/json',
                      '--data-binary', f'@{request}', 'https://api.osv.dev/v1/querybatch', '-o', str(response)])
        if result.returncode:
            raise RuntimeError('OSV could not be reached; Maven scan was not completed')
        data = json.loads(response.read_text())
        if len(data.get('results', [])) != len(packages):
            raise RuntimeError('OSV returned an incomplete result')
        return [{'package': item, 'vulnerabilities': result.get('vulns', [])} for item, result in zip(packages, data['results'])]

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--report', default='security-report.json')
    parser.add_argument('--npm-only', action='store_true')
    args = parser.parse_args()
    today = datetime.datetime.now(datetime.timezone.utc).date()
    exceptions = json.loads((ROOT/'.security/vulnerability-exceptions.json').read_text())['exceptions']
    failures, accepted = [], []
    def allowed(ecosystem, package, advisory, scope):
        for item in exceptions:
            if (item['ecosystem'], item['package'], item['id'], item['scope']) == (ecosystem, package, advisory, scope):
                if datetime.date.fromisoformat(item['expires']) < today:
                    failures.append(f'Expired exception: {advisory}')
                    return False
                accepted.append({'package': package, 'id': advisory, 'reason': item['reason'], 'expires': item['expires']})
                return True
        return False
    report = {'date': today.isoformat(), 'npm': {}, 'maven': [], 'exceptions': accepted, 'failures': failures}
    for directory in ['frontend', 'scripts']:
        full, production = audit_npm(directory), audit_npm(directory, True)
        report['npm'][directory] = {'all': full, 'production': production}
        runtime_ids = {advisory for package, advisory, data in direct_advisories(production)}
        for package, advisory, data in direct_advisories(full):
            scope = 'runtime' if advisory in runtime_ids else 'development'
            if not allowed('npm', package, advisory, scope):
                failures.append(f'{directory}: {package} {advisory} ({scope}, {data["severity"]})')
    if not args.npm_only:
        report['maven'] = osv(maven_inventory())
        for item in report['maven']:
            for advisory in item['vulnerabilities']:
                package = item['package']['package']['name']
                if not allowed('Maven', package, advisory['id'], 'runtime'):
                    failures.append(f'Maven: {package} {advisory["id"]}')
    destination = pathlib.Path(args.report)
    destination.parent.mkdir(parents=True, exist_ok=True)
    destination.write_text(json.dumps(report, indent=2)+'\n')
    print(f'Security scan: {len(failures)} unresolved advisories; {len(accepted)} scoped exceptions. Report: {destination}')
    for failure in failures:
        print(failure)
    return bool(failures)

if __name__ == '__main__':
    try:
        sys.exit(main())
    except (RuntimeError, OSError, ValueError) as error:
        print(f'Security scan incomplete: {error}', file=sys.stderr)
        sys.exit(2)
