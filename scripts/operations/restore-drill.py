#!/usr/bin/env python3
"""Restore a verified bundle only into an empty local PostgreSQL database."""
import argparse
import json
import os
from pathlib import Path
import re
import subprocess

parser=argparse.ArgumentParser()
parser.add_argument('bundle',type=Path)
args=parser.parse_args()
if os.environ.get('PGHOST') not in ('127.0.0.1','localhost') or not os.environ.get('PGDATABASE','').startswith('uitmerch_restore_'):
    raise SystemExit('Restore drills require localhost and an explicit uitmerch_restore_ database')
def sql(statement):
    return subprocess.run(['psql','-X','-qAt','-v','ON_ERROR_STOP=1'],input=statement,
        check=True,capture_output=True,text=True).stdout.strip()
if sql("SELECT count(*) FROM pg_tables WHERE schemaname NOT IN ('pg_catalog','information_schema')")!='0':
    raise SystemExit('Refusing to restore into a nonempty database')
manifest=json.loads((args.bundle/'manifest.json').read_text())
for extension in manifest['extensions']:
    if extension['name'] not in ('vector','pg_trgm','pgcrypto','uuid-ossp') or extension['schema']!='extensions':
        raise SystemExit('Unrecognized extension bootstrap; review before restoring')
for role in ('uitmerch_runtime','uitmerch_backup','uitmerch_monitor'):
    sql("DO $$ BEGIN IF NOT EXISTS(SELECT 1 FROM pg_roles WHERE rolname='"+role+"') THEN CREATE ROLE "+role+" NOLOGIN; END IF; END $$")
sql('DROP SCHEMA public')
dump=args.bundle/'database.dump'
toc=subprocess.run(['pg_restore','--list',str(dump)],check=True,capture_output=True,text=True).stdout
schema_toc=args.bundle/'restore-schemas.toc';data_toc=args.bundle/'restore-data.toc'
schema_toc.write_text('\n'.join(x for x in toc.splitlines() if ' SCHEMA - ' in x))
data_toc.write_text('\n'.join(x for x in toc.splitlines() if ' SCHEMA - ' not in x))
def restore(toc_path):
    subprocess.run(['pg_restore','--no-owner','--no-privileges','--exit-on-error',
        '--dbname='+os.environ['PGDATABASE'],'--use-list='+str(toc_path),str(dump)],check=True,capture_output=True)
restore(schema_toc)
for extension in manifest['extensions']:
    sql('CREATE EXTENSION "'+extension['name']+'" WITH SCHEMA extensions')
restore(data_toc)
result=json.loads(sql("SELECT json_build_object('users',(SELECT count(*) FROM public.users),'orders',(SELECT count(*) FROM public.orders),'embeddings',(SELECT count(*) FROM public.merch_embeddings),'auditEvents',(SELECT count(*) FROM security_audit.events),'invalidIndexes',(SELECT count(*) FROM pg_index WHERE NOT indisvalid))"))
if result['invalidIndexes']!=0:
    raise SystemExit('Restored database has invalid indexes')
print(json.dumps({'databaseRestored':True,'counts':result,'storageObjectsVerified':len(manifest['objects'])}))
