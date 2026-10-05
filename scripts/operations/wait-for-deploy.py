#!/usr/bin/env python3
"""Do not mark a deploy healthy because the previous container still answers HTTP."""
import json
import os
import sys
import time
import urllib.request
base=os.environ['RENDER_PUBLIC_API_URL'].rstrip('/')
expected=os.environ['GITHUB_SHA']
for attempt in range(40):
    try:
        with urllib.request.urlopen(base+'/',timeout=10) as response:
            current=json.load(response)['data'].get('buildCommit')
        if current==expected:
            with urllib.request.urlopen(base+'/api/v1/public/campaigns?size=1',timeout=10) as response:
                if response.status==200:
                    print('Tested commit is serving the public API')
                    sys.exit(0)
    except Exception:
        pass
    if attempt<39:
        time.sleep(15)
raise SystemExit('Timed out waiting for the tested commit and public API readiness')
