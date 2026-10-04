from pathlib import Path
import requests

cfg = {}
for line in Path('.env').read_text(encoding='utf-8').splitlines():
    if not line or line.startswith('#') or '=' not in line:
        continue
    k, v = line.split('=', 1)
    cfg[k.strip()] = v.strip()

base = cfg['CONFLUENCE_BASE_URL'].rstrip('/')
email = cfg['CONFLUENCE_USER_EMAIL']
token = cfg['CONFLUENCE_API_TOKEN']
space_id = '11272200'
title = 'EPMCDMETST-67217 Product Catalog SDLC Report'

html = '''
<p><strong>Summary</strong></p>
<ul>
  <li>Product catalog feature implemented for EPMCDMETST-67217.</li>
  <li>Includes keyword search, category filtering, price filtering, sorting, pagination, and validation.</li>
  <li>Requirements, architecture, design review, implementation plan, and summary docs were generated.</li>
</ul>
<p><strong>Verification</strong></p>
<ul>
  <li>Backend: <code>cd ecom-project &amp;&amp; ./mvnw -q -Dtest=ProductControllerIT test</code></li>
  <li>Frontend: <code>cd ecom-front/ecom-catalog-react &amp;&amp; npm test</code></li>
</ul>
<p><strong>Artifacts</strong></p>
<ul>
  <li><code>requirements.md</code></li>
  <li><code>architecture.md</code></li>
  <li><code>design-review.md</code></li>
  <li><code>impl-plan.md</code></li>
  <li><code>COMPLETION_SUMMARY.md</code></li>
</ul>
<p><strong>Pull Request</strong></p>
<ul>
  <li><a href="https://github.com/sdixit07/capstoneproject_githubCopilot/pull/2">PR #2</a></li>
</ul>
'''

check = requests.get(
    base + '/wiki/api/v2/pages',
    params={'spaceId': space_id, 'title': title},
    auth=(email, token),
    timeout=30,
)
print('CHECK_STATUS', check.status_code)
print(check.text[:1000])

if check.ok and check.json().get('results'):
    print('PAGE_ALREADY_EXISTS')
    print(check.json()['results'][0].get('id'))
else:
    payload = {
        'spaceId': space_id,
        'status': 'current',
        'title': title,
        'body': {'representation': 'storage', 'value': html},
    }
    resp = requests.post(base + '/wiki/api/v2/pages', json=payload, auth=(email, token), timeout=30)
    print('CREATE_STATUS', resp.status_code)
    print(resp.text[:1500])

