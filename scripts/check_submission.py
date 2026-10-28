#!/usr/bin/env python3
from pathlib import Path
root=Path(__file__).resolve().parents[1]
kit=(root/'PLAY_STORE_SUBMISSION.md').read_text()
for name,limit in [('short',80),('full',4000)]:
 text=(root/f'release/store-listing/{name}-description.txt').read_text().strip()
 assert 0<len(text)<=limit
 assert text in kit
 print(f'{name}: {len(text)}/{limit} characters; matches submission kit')
for marker in ['Data Safety','Health Apps','Privacy policy template','[PUBLISHER_LEGAL_NAME]','[SUPPORT_EMAIL]','[PUBLIC_HTTPS_PRIVACY_POLICY_URL]']:
 assert marker in kit,marker
