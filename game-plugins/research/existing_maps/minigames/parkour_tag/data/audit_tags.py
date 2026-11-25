import re
lines = open('README.md', encoding='utf-8').read().splitlines()
out = open('data/audit_tags.txt', 'w', encoding='utf-8')
for i, l in enumerate(lines, 1):
    s = l.strip()
    if len(s) > 90 and not re.search(r'\*\*H\*\*|\*\*M\*\*|\*\*L\*\*', s) and not s.startswith('|') and not s.startswith('#') and not s.startswith('```'):
        out.write(f'{i} {s[:200]}\n')
out.close()
