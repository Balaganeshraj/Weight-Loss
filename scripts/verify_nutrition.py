#!/usr/bin/env python3
"""Compare distributed values with included immutable source extracts."""
import json,pathlib,importlib.util,math
root=pathlib.Path(__file__).resolve().parents[1]
foods=json.loads((root/'app/src/main/assets/nutrition/catalog.json').read_text())
records=json.loads((root/'docs/nutrition/source-records.json').read_text())
assert len(foods['foods'])==8
mapping={'kcal':('Energy','kcal'),'proteinG':('Protein','g'),'carbohydrateG':('Carbohydrate, by difference','g'),'fatG':('Total lipid (fat)','g'),'fiberG':('Fiber, total dietary','g')}
for f in foods['foods']:
 source=next(r for r in records if f['id']==f'usda-mirror:{r["id"]}')
 assert f['sourceDescription']==source['description']
 assert f['referenceGrams']==100
 for field,(name,unit) in mapping.items():
  values={n['value'] for n in source['nutrients'] if n['description']==name and n['units']==unit}
  assert values=={f[field]},(f['id'],field)
  assert math.isfinite(f[field]) and f[field]>=0
 assert sum(f[n] for n in ('proteinG','carbohydrateG','fatG'))<=101
 assert f['fiberG']<=f['carbohydrateG']
print('8 foods: calories, protein, carbohydrate, fat and fibre match source extracts (40 nutrient checks).')
