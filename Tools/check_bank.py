from collections import Counter
from pathlib import Path
import json, re, unicodedata

path = Path(__file__).resolve().parents[1] / 'Millionaire/Resources/questions.json'
bank = json.loads(path.read_text())
def norm(text):
    return ' '.join(re.sub(r'[^\w\s]', '', unicodedata.normalize('NFKC', text)).split())
assert len(bank) >= 1500, f'Expected at least 1500 questions, found {len(bank)}'
assert len(set(q['id'] for q in bank)) == len(bank)
assert len(set(norm(q['text']) for q in bank)) == len(bank)
for q in bank:
    assert all(isinstance(q[k], str) and q[k].strip() for k in ('id','text','category','explanation','source')), q
    assert len(q['options']) == len({' '.join(o.split()) for o in q['options']}) == 4, q
    assert all(isinstance(o,str) and o.strip() for o in q['options']), q
    assert type(q['correct']) is int and 0 <= q['correct'] <= 3, q
    assert q['difficulty'] in range(1,6), q
print(len(bank), 'unique questions')
print('Categories:', dict(Counter(q['category'] for q in bank)))
print('Difficulty:', dict(Counter(q['difficulty'] for q in bank)))
