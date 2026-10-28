# Offline nutrition reference and technical review

The release contains eight explicitly described reference foods, rather than invented mixed-meal nutrition. Each reference uses **100 g edible portion**, the exact preparation in the source description, energy in kcal, and protein/carbohydrate/fat/fibre in grams. No current retail prices, confidence intervals or clinician approval are fabricated.

## Source and version

- Source: USDA-derived reference data mirrored in Wes McKinney's *Python for Data Analysis* data repository.
- Immutable snapshot: https://raw.githubusercontent.com/wesm/pydata-book/a892791ce5a3831134cfa2989033ecbb9289f1ea/datasets/usda_food/database.json
- Repository commit: `a892791ce5a3831134cfa2989033ecbb9289f1ea`.
- Download SHA-256: `0708114d31e5dfa614ffaeafffbb2f78d0ef966c2624100749b8093f399722f2`.
- App catalogue version: `usda-reference-a892791-v1`.
- `usda-mirror:<id>` identifies a record in that snapshot. It is **not represented as a current FoodData Central ID**.
- Included extracts: `source-records.json`; distributed catalogue: `app/src/main/assets/nutrition/catalog.json`.

The primary USDA API and download site were blocked by this cloud environment's proxy. These records were checked against a pinned secondary mirror of USDA data, not downloaded live from FoodData Central. They are not a current Sri Lankan nutrition corpus. Original USDA factual data are public reference data; confirm provenance/licensing and suitability with the publisher's content reviewer before commercial distribution.

## Checks executed

`scripts/import_nutrition.py` rejects missing/duplicate IDs, missing or conflicting nutrient records, incorrect units, negative/non-finite values and impossible macro totals. Repeated identical nutrient records in the source are deduplicated by value; disagreements fail instead of silently selecting one. `scripts/verify_nutrition.py` compares all 40 distributed nutrient values with their source extracts. The description and preparation must match the same record.

Calories use the source's reported energy. They are not overwritten with a naive 4/4/9 calculation: USDA values may use food-specific factors and rounding. User portions scale source values deterministically by grams/100. Manual entries retain their own label source and show macros as unknown rather than inventing them. Historical entries retain source/version and saved macro totals, so future catalogue changes do not rewrite consumed-food history.

## Review boundary

This is a documented **technical source comparison**, not an independent nutritionist/dietitian review, clinical clearance or guarantee about an individual's meal. Preparation, variety, moisture, added oil and actual portion size affect nutrition. The in-app disclaimer states this. Qualified content review and primary-source reconciliation remain production gates; no reviewer name or signature is invented.

The three recipe ideas are separately labelled illustrative, have no fabricated calories/prices, and filter only their declared allergens. Ingredient labels and cross-contact risks must still be checked.
