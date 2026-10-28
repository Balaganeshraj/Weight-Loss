#!/usr/bin/env python3
"""Extract source records without inventing nutrients; pinned mirror, no app networking."""
import hashlib, json, pathlib, sys
ROOT = pathlib.Path(__file__).resolve().parents[1]
SOURCE = 'https://raw.githubusercontent.com/wesm/pydata-book/a892791ce5a3831134cfa2989033ecbb9289f1ea/datasets/usda_food/database.json'
SELECTED = {1129:'egg', 8121:'oats', 9003:'apple', 9040:'banana', 9050:'blueberries', 9200:'orange', 20045:'rice', 16070:'lentils'}
NUTRIENTS = {'kcal':('Energy','kcal'), 'proteinG':('Protein','g'), 'carbohydrateG':('Carbohydrate, by difference','g'), 'fatG':('Total lipid (fat)','g'), 'fiberG':('Fiber, total dietary','g')}
def build(path):
    raw = path.read_bytes()
    selected = [r for r in json.loads(raw) if r['id'] in SELECTED]
    assert len(selected) == len(SELECTED), 'Missing or duplicate source IDs'
    foods=[]
    for record in sorted(selected,key=lambda r:r['id']):
        nutrients={}
        for field,(description,unit) in NUTRIENTS.items():
            values={n['value'] for n in record['nutrients'] if n['description']==description and n['units']==unit}
            assert len(values)==1, f'Conflicting/missing {field} for {record["id"]}'
            value=values.pop()
            assert isinstance(value,(int,float)) and value>=0
            nutrients[field]=value
        assert sum(nutrients[k] for k in ('proteinG','carbohydrateG','fatG'))<=101
        assert nutrients['fiberG']<=nutrients['carbohydrateG']
        foods.append(dict(id=f'usda-mirror:{record["id"]}',key=SELECTED[record['id']],sourceDescription=record['description'],referenceGrams=100,**nutrients))
    return dict(schemaVersion=1,version='usda-reference-a892791-v1',sourceUrl=SOURCE,sourceSha256=hashlib.sha256(raw).hexdigest(),basis='Per 100 g edible portion; preparation exactly as source description',review='Automated source comparison, duplicate consistency, units and bounds checked. Not a dietitian or clinical approval; primary USDA service unavailable in this environment.',foods=foods),selected
if __name__=='__main__':
    data,records=build(pathlib.Path(sys.argv[1]))
    (ROOT/'app/src/main/assets/nutrition/catalog.json').write_text(json.dumps(data,indent=2,ensure_ascii=False)+'\n')
    (ROOT/'docs/nutrition/source-records.json').write_text(json.dumps(records,indent=2,ensure_ascii=False)+'\n')
    print(f'Imported and checked {len(data["foods"])} exact source records. SHA-256: {data["sourceSha256"]}')
