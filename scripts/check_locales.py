#!/usr/bin/env python3
import pathlib,re,xml.etree.ElementTree as ET
root=pathlib.Path(__file__).resolve().parents[1]/'app/src/main/res'
sets={lang:{e.attrib['name']:e.text or '' for e in ET.parse(root/folder/'strings.xml').getroot() if e.tag=='string'} for lang,folder in [('en','values'),('si','values-si'),('ta','values-ta')]}
pattern=r'%\d+\$[sdf]'
for lang,values in sets.items():
 assert values.keys()==sets['en'].keys(),f'{lang}: mismatched resource keys'
 for key,value in values.items():
  assert value.strip(),f'{lang}: empty {key}'
  assert sorted(re.findall(pattern,value))==sorted(re.findall(pattern,sets['en'][key])),f'{lang}: placeholder mismatch {key}'
print(f"All {len(sets['en'])} strings present in English, Sinhala and Tamil; format placeholders match.")
