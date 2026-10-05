#!/usr/bin/env python3
"""Check private aggregate job health and exercise durable audit persistence."""
import datetime as dt
import json
import http.cookiejar
import os
import subprocess
import sys
import time
import urllib.error
import urllib.request
import uuid


def query(sql):
    result = subprocess.run(['psql', '-X', '-qAt', '-v', 'ON_ERROR_STOP=1', '-c', sql],
                            check=True, capture_output=True, text=True, timeout=30)
    return result.stdout.strip()


def main():
    base = os.environ.get('UITMERCH_PUBLIC_URL', 'https://uitmerch.vercel.app').rstrip('/')
    if not base.startswith('https://'):
        raise ValueError('Production probe requires HTTPS')
    trace = 'ops-' + str(uuid.uuid4())
    body = json.dumps({'email': 'operations-probe@example.invalid', 'password': 'invalid-probe-password'}).encode()
    opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))
    probe_ok = False
    for attempt in range(6):
        try:
            with opener.open(urllib.request.Request(base + '/api/v1/auth/csrf', headers={'Origin': base}), timeout=45) as response:
                csrf = json.load(response)['data']['csrfToken']
            request = urllib.request.Request(base + '/api/v1/auth/login', data=body,
                headers={'Content-Type': 'application/json', 'X-Trace-Id': trace,
                         'Origin': base, 'X-CSRF-TOKEN': csrf})
            with opener.open(request, timeout=45) as response:
                response.read(1024)
        except urllib.error.HTTPError as error:
            if error.code == 401:
                probe_ok = True
                break
            if error.code < 500:
                break
        except (urllib.error.URLError, TimeoutError):
            pass
        if attempt < 5:
            time.sleep(15)
    failures = []
    if not probe_ok:
        failures.append('public_auth_probe_failed')
    elif query("SELECT app_ops.audit_probe_seen('" + trace + "')") != 't':
        failures.append('durable_audit_probe_missing')
    report = json.loads(query('SELECT app_ops.health_report()'))
    for field in ('overduePendingOrders', 'recentDeadJobs', 'stalledJobs', 'cronFailures'):
        if report[field] != 0:
            failures.append(field)
    now = dt.datetime.now(dt.timezone.utc)
    for field, minutes in [('expiryLastSuccess', 5), ('retentionLastSuccess', 90)]:
        last = report[field]
        if not last or now - dt.datetime.fromisoformat(last) > dt.timedelta(minutes=minutes):
            failures.append(field)
    print(json.dumps({'healthy': not failures, 'failures': failures, 'report': report}))
    if failures:
        return 1
    return 0


if __name__ == '__main__':
    try:
        sys.exit(main())
    except Exception as error:
        # Connection and HTTP exception messages can contain secrets: print only their type.
        print(json.dumps({'healthy': False, 'failure': type(error).__name__}))
        sys.exit(1)
