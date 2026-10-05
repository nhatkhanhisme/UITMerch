#!/usr/bin/env python3
"""Decrypt offline and verify database archive and every Storage byte; never writes production."""
import argparse
import hashlib
import json
from pathlib import Path
import subprocess
import tarfile
import tempfile

parser = argparse.ArgumentParser()
parser.add_argument('backup', type=Path)
parser.add_argument('--private-key', type=Path, required=True)
parser.add_argument('--output', type=Path, required=True, help='New, private directory for restore drill')
args = parser.parse_args()
args.output.mkdir(mode=0o700, parents=True, exist_ok=False)
with tempfile.TemporaryDirectory() as temporary:
    archive = Path(temporary) / 'backup.tar.gz'
    subprocess.run(['openssl', 'cms', '-decrypt', '-binary', '-inform', 'DER', '-in', str(args.backup),
                    '-inkey', str(args.private_key), '-out', str(archive)], check=True, capture_output=True)
    with tarfile.open(archive, 'r:gz') as tar:
        tar.extractall(args.output, filter='data')
manifest = json.loads((args.output / 'manifest.json').read_text())
assert hashlib.sha256((args.output / 'database.dump').read_bytes()).hexdigest() == manifest['databaseSha256']
for item in manifest['objects']:
    path = args.output / 'objects' / item['file']
    assert path.parent == args.output / 'objects'
    assert path.stat().st_size == item['bytes']
    assert hashlib.sha256(path.read_bytes()).hexdigest() == item['sha256']
subprocess.run(['pg_restore', '--list', str(args.output / 'database.dump')],
               check=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
print(json.dumps({'verified': True, 'storageObjects': len(manifest['objects'])}))
