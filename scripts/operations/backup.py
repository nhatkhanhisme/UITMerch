#!/usr/bin/env python3
"""Encrypted database + Storage bytes backup. No plaintext is uploaded as an artifact."""
import hashlib
import json
import os
from pathlib import Path
import subprocess
import sys
import tarfile
import tempfile
import boto3
from botocore.config import Config


def main():
    os.umask(0o077)
    output = Path(os.environ.get('BACKUP_OUTPUT', 'operations-backup.cms')).resolve()
    recipient = Path(__file__).with_name('backup-recipient.pem')
    client = boto3.client('s3', endpoint_url=os.environ['SUPABASE_STORAGE_ENDPOINT'],
        region_name=os.environ['SUPABASE_STORAGE_REGION'],
        aws_access_key_id=os.environ['SUPABASE_STORAGE_S3_ACCESS_KEY_ID'],
        aws_secret_access_key=os.environ['SUPABASE_STORAGE_S3_SECRET_KEY'],
        config=Config(signature_version='s3v4', retries={'mode': 'standard', 'max_attempts': 5},
                      s3={'addressing_style': 'path'}))
    with tempfile.TemporaryDirectory(prefix='uitmerch-backup-') as temporary:
        root = Path(temporary)
        dump = root / 'database.dump'
        subprocess.run(['pg_dump', '--format=custom', '--no-owner', '--no-privileges',
            '--enable-row-security', '--schema=public', '--schema=extensions',
            '--schema=security_audit', '--schema=app_ops', '--file=' + str(dump)],
            check=True, capture_output=True, timeout=180)
        extension_sql = "SELECT coalesce(json_agg(json_build_object('name',e.extname,'schema',n.nspname)),'[]') FROM pg_extension e JOIN pg_namespace n ON n.oid=e.extnamespace WHERE n.nspname='extensions' AND e.extname IN ('vector','pg_trgm','pgcrypto','uuid-ossp')"
        extension_result = subprocess.run(['psql', '-X', '-qAt', '-v', 'ON_ERROR_STOP=1', '-c', extension_sql],
                                          check=True, capture_output=True, text=True)
        manifest = {'format': 1, 'extensions': json.loads(extension_result.stdout), 'databaseSha256': hashlib.sha256(dump.read_bytes()).hexdigest(),
                    'buckets': [], 'objects': []}
        objects = root / 'objects'
        objects.mkdir()
        # Enumerate all buckets, not only the two configured upload destinations.
        for bucket_info in client.list_buckets()['Buckets']:
            bucket = bucket_info['Name']
            manifest['buckets'].append(bucket)
            for page in client.get_paginator('list_objects_v2').paginate(Bucket=bucket):
                for metadata in page.get('Contents', []):
                    key = metadata['Key']
                    # Never use an untrusted remote object key as a local path.
                    file_name = str(len(manifest['objects'])) + '.bin'
                    target = objects / file_name
                    response = client.get_object(Bucket=bucket, Key=key)
                    digest = hashlib.sha256()
                    size = 0
                    with response['Body'] as stream, target.open('wb') as file:
                        for chunk in iter(lambda: stream.read(1024 * 1024), b''):
                            digest.update(chunk)
                            size += len(chunk)
                            file.write(chunk)
                    if size != metadata['Size']:
                        raise ValueError('Storage object changed during backup; retry a fresh snapshot')
                    manifest['objects'].append({'bucket': bucket, 'key': key, 'file': file_name,
                        'bytes': size, 'sha256': digest.hexdigest(),
                        'contentType': response.get('ContentType', 'application/octet-stream')})
        (root / 'manifest.json').write_text(json.dumps(manifest))
        archive = root / 'backup.tar.gz'
        with tarfile.open(archive, 'w:gz') as tar:
            for path in (dump, root / 'manifest.json', objects):
                tar.add(path, arcname=path.name)
        # AES-GCM AuthEnvelopedData protects both confidentiality and ciphertext integrity.
        subprocess.run(['openssl', 'cms', '-encrypt', '-binary', '-aes-256-gcm',
            '-in', str(archive), '-out', str(output), '-outform', 'DER', str(recipient)],
            check=True, capture_output=True)
        output.chmod(0o600)
        print(json.dumps({'encrypted': True, 'buckets': len(manifest['buckets']),
            'objects': len(manifest['objects']), 'storageBytes': sum(x['bytes'] for x in manifest['objects']),
            'encryptedBytes': output.stat().st_size, 'sha256': hashlib.sha256(output.read_bytes()).hexdigest()}))


if __name__ == '__main__':
    try:
        main()
    except Exception as error:
        print(json.dumps({'backupFailed': type(error).__name__}))
        sys.exit(1)
